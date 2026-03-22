package com.example.polusmessenger.presentation.list

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.polusmessenger.R
import com.example.polusmessenger.di.AppModule
import com.example.polusmessenger.presentation.redux.AppAction
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatListFragment : Fragment(R.layout.fragment_chat_list) {

    private val store = AppModule.store
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ChatListAdapter

    // держим ссылку на диалог
    private var createChatDialog: androidx.appcompat.app.AlertDialog? = null

    // сохраняем размер списка чатов в момент открытия диалога — для детекции нового чата

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch { store.dispatch(AppAction.LoadChats) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recycler = view.findViewById(R.id.recyclerChats)
        adapter = ChatListAdapter { chat ->
            lifecycleScope.launch {
                store.dispatch(AppAction.SelectChat(chat.id))
                store.dispatch(AppAction.LoadMessages(chat.id))
            }
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        view.findViewById<View>(R.id.fabCreateChat).setOnClickListener { showCreateChatDialog() }

        observeViewState()
            observeChatCreated()
    }

    private fun observeViewState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                store.states
                    .map { state -> state.chats }
                    .distinctUntilChanged()
                    .collect { chats ->
                        adapter.submitList(chats)
                    }
            }
        }
    }

    // следим за изменением количества чатов; если диалог открыт и список увеличился — закрываем диалог
    private fun observeChatCreated() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                store.states
                    .map { it.selectedChatId }
                    .distinctUntilChanged()
                    .collect { chatId ->
                        if (chatId != null) {
                            store.dispatch(AppAction.LoadMessages(chatId)) // при каждом выборе чата сообщения загрузятся
                        }
                    }
            }
        }
    }
    private fun showCreateChatDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_chat, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.chat_name_input)

        createChatDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Создать новый чат")
            .setView(dialogView)
            .setPositiveButton("Создать", null)
            .setNegativeButton("Отмена") { _, _ ->
                createChatDialog = null
            }
            .create()

        createChatDialog?.setOnShowListener { dlg ->
            val positive = (dlg as androidx.appcompat.app.AlertDialog)
                .getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)

            positive.setOnClickListener {
                val name = nameInput.text?.toString()?.trim()

                if (name.isNullOrBlank()) {
                    Toast.makeText(requireContext(), "Введите название чата", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                lifecycleScope.launch { store.dispatch(AppAction.CreateChat(name)) }
                createChatDialog?.dismiss()
                createChatDialog = null
            }
        }

        createChatDialog?.show()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        createChatDialog?.dismiss()
        createChatDialog = null
    }
}
