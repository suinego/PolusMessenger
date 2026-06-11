@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.polusmessenger.presentation.redux

import android.util.Log
import com.example.polusmessenger.domain.Chat
import com.example.polusmessenger.domain.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

abstract class BaseEpic : Epic {
    inline fun <reified T : Action> Flow<Action>.ofType(): Flow<T> =
        filterIsInstance<T>()
}

class LoadChatsEpic(
    private val getChats: suspend (Int) -> Pair<List<Chat>, Int>
) : BaseEpic() {

    override fun act(actions: Flow<Action>): Flow<Action> =
        actions.ofType<AppAction.LoadChats>()
            .flatMapLatest {
                flow<Action> {
                    val (chats, total) = getChats(0)
                    emit(AppAction.ChatsLoaded(chats, total))
                }.catch { e ->
                    emit(AppAction.LoadChatsFailed(e.message ?: "Ошибка загрузки чатов"))
                }
            }
}

class LoadMoreChatsEpic(
    private val getChats: suspend (Int) -> Pair<List<Chat>, Int>
) : BaseEpic() {

    override fun act(actions: Flow<Action>): Flow<Action> =
        actions.ofType<AppAction.LoadMoreChats>()
            .flatMapConcat { action ->
                flow<Action> {
                    val (chats, total) = getChats(action.offset)
                    emit(AppAction.MoreChatsLoaded(chats, total))
                }.catch { e ->
                    emit(AppAction.LoadChatsFailed(e.message ?: "Ошибка загрузки следующей страницы"))
                }
            }
}

class LoadMessagesEpic(
    private val getMessages: suspend (Int) -> Pair<Chat, List<Message>>
) : BaseEpic() {

    override fun act(actions: Flow<Action>): Flow<Action> =
        actions.ofType<AppAction.LoadMessages>()
            .onEach { Log.d("Epic", "Загрузка сообщений получена") }
            .flatMapConcat { action ->
                flow {
                    try {
                        val (chat, messages) = getMessages(action.chatId)
                        Log.d("Epic", "Загружено сообщений: ${messages.size} для chatId=${chat.id}")
                        emit(AppAction.MessagesLoaded(chat.id, messages))
                    } catch (e: Exception) {
                        Log.e("Epic", "Загрузка сообщений провалена", e)
                        emit(AppAction.MessagesLoadFailed(action.chatId, e.message ?: "unknown"))
                    }
                }
            }
}

class SendMessageEpic(
    private val getState: () -> AppState,
    private val sendMessage: suspend (Int, String) -> List<Message>
) : BaseEpic() {

    override fun act(actions: Flow<Action>): Flow<Action> =
        actions.ofType<AppAction.SendMessage>()
            .flatMapConcat { action ->
                flow {
                    val chatId = action.chatId
                    val text = action.text
                    val currentMessages = getState().messages[chatId] ?: emptyList()
                    val tempMessage = Message(
                        id = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                        text = text
                    )
                    emit(AppAction.MessagesLoaded(chatId, currentMessages + tempMessage))

                    try {
                        val serverMessages = withContext(Dispatchers.IO) {
                            sendMessage(chatId, text)
                        }
                        emit(AppAction.MessagesLoaded(chatId, serverMessages))
                    } catch (e: Exception) {
                        Log.e("SendMessageEpic", "отправка не отработала", e)
                        emit(AppAction.MessagesLoaded(chatId, currentMessages))
                        emit(AppAction.MessageSendFailed(e.localizedMessage ?: "Отправка не отработала"))
                    }
                }
            }
}

class CreateChatEpic(
    private val createChat: suspend (String) -> List<Chat>
) : BaseEpic() {

    override fun act(actions: Flow<Action>): Flow<Action> =
        actions.ofType<AppAction.CreateChat>()
            .onEach { Log.d("Epic", "CreateChat получен: ${it.name}") }
            .flatMapConcat { action ->
                flow {
                    try {
                        val allChats = createChat(action.name)
                        emit(AppAction.ChatsLoaded(allChats, allChats.size))
                        val newChat = allChats.lastOrNull()
                        if (newChat != null) {
                            emit(AppAction.SelectChat(newChat.id))
                        }
                    } catch (e: Exception) {
                        Log.e("CreateChatEpic", "Создание чата провалено", e)
                        emit(AppAction.ChatCreationFailed(e.message ?: "unknown"))
                    }
                }
            }
}
