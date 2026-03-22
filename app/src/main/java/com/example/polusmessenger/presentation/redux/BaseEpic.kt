package com.example.polusmessenger.presentation.redux

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import com.example.polusmessenger.domain.Chat
import com.example.polusmessenger.domain.Message
import android.util.Log
import com.example.polusmessenger.domain.usecase.CreateChatUseCase

abstract class BaseEpic : Epic {
    inline fun <reified T : Action> Flow<Action>.ofType(): Flow<T> =
        filterIsInstance<T>()
}

class LoadChatsEpic(
    private val getChatsUseCase: suspend () -> List<Chat>
) : BaseEpic() {

    override fun act(actions: Flow<Action>): Flow<Action> =
        actions.ofType<AppAction.LoadChats>()
            .flatMapLatest {
                flow {
                    emit(getChatsUseCase())
                }
                    .map<List<Chat>, Action> { chats ->
                        AppAction.ChatsLoaded(chats)
                    }
                    .catch { e ->
                        emit(AppAction.LoadChatsFailed(e.message ?: "неизвестно"))
                    }
            }
}

class LoadMessagesEpic(
    private val getMessagesUseCase: suspend (Int) -> Pair<Chat, List<Message>>
) : BaseEpic() {

    override fun act(actions: Flow<Action>): Flow<Action> =
        actions.ofType<AppAction.LoadMessages>()
            .onEach { Log.d("Epic", "Загрузка сообщений получена") }
            .flatMapConcat { action ->
                flow {
                    try {
                        val (chat, messages) = getMessagesUseCase(action.chatId)
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
    private val sendMessageUseCase: suspend (Int, String) -> List<Message>
) : BaseEpic() {

    override fun act(actions: Flow<Action>): Flow<Action> {
        return actions.ofType<AppAction.SendMessage>()
            .flatMapConcat { action ->
                flow {
                    val chatId = action.chatId
                    val text = action.text
                    val currentMessages = getState().messages[chatId] ?: emptyList()
                    val tempMessage = Message(
                        id = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                        text = text
                    )

                    val withTemp = currentMessages + tempMessage
                    emit(AppAction.MessagesLoaded(chatId, withTemp))

                    try {
                        val serverMessages = withContext(Dispatchers.IO) {
                            sendMessageUseCase(chatId, text)
                        }
                        emit(AppAction.MessagesLoaded(chatId, serverMessages))
                    } catch (e: Exception) {
                        Log.e("SendMessageEpic", "отправка не отработала", e)
                        val withoutTemp = currentMessages
                        emit(AppAction.MessagesLoaded(chatId, withoutTemp))
                        emit(AppAction.MessageSendFailed(e.localizedMessage ?: "Отправка не отработала"))
                    }
                }
            }
    }
}
class CreateChatEpic(
    private val createChatUseCase: suspend (String) -> Chat
) : BaseEpic() {

    override fun act(actions: Flow<Action>): Flow<Action> =
        actions.ofType<AppAction.CreateChat>()
            .onEach { Log.d("Epic", "CreateChat получен: ${it.name}") }
            .flatMapConcat { action ->
                flow {
                    try {
                        val newChat = createChatUseCase(action.name)
                        emit(AppAction.ChatCreated(newChat))
                    } catch (e: Exception) {
                        Log.e("CreateChatEpic", "ЧАТ УПАЛ", e)
                        emit(AppAction.ChatCreationFailed(e.message ?: "unknown"))
                    }
                }
            }
}

