package com.example.polusmessenger.presentation.chat

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.polusmessenger.R
import com.example.polusmessenger.di.AppModule
import com.example.polusmessenger.presentation.redux.AppAction
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatFragment : Fragment(R.layout.fragment_chat) {

    private val store = AppModule.store
    private lateinit var adapter: MessagesAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var edit: EditText
    private lateinit var sendBtn: Button

    //вызываем после onCreateView!
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler = view.findViewById(R.id.recyclerMessages)
        edit = view.findViewById(R.id.editMessage)
        sendBtn = view.findViewById(R.id.btnSend)
        adapter = MessagesAdapter()
        recycler.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        recycler.adapter = adapter

        setupClickListeners()
        observeViewState()
        loadMessagesForCurrentChat()
    }

    private fun setupClickListeners() {
        sendBtn.setOnClickListener {
            val chatId = store.getState().selectedChatId
            val text = edit.text.toString().trim()
            if (chatId != null && text.isNotBlank()) {
                lifecycleScope.launch { store.dispatch(AppAction.SendMessage(chatId, text)) }
                edit.setText("")
            } else if (chatId == null) {
                Toast.makeText(requireContext(), "Чат не выбран", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Введите сообщение", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewState() {
        viewLifecycleOwner.lifecycleScope.launch{
            repeatOnLifecycle(Lifecycle.State.CREATED){  //когда достиг состояния created
                store.states.collect {
                    state ->
                    val chatId = state.selectedChatId
                    val chat = state.chats.find {it.id == chatId}
                    val msg = state.messages[chatId].orEmpty()
                    adapter.submitList(msg)
                    if(msg.isNotEmpty()){
                        recycler.scrollToPosition(msg.size - 1)
                    }
                }
            }
        }

    }

    private fun loadMessagesForCurrentChat() {
        val chatId = store.getState().selectedChatId
        if (chatId != null) {
            lifecycleScope.launch { store.dispatch(AppAction.LoadMessages(chatId)) }
        }
    }
}
