package com.example.polusmessenger.presentation.chat

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.polusmessenger.R
import com.example.polusmessenger.di.AppModule
import com.example.polusmessenger.presentation.redux.AppAction
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.widget.EditText
import android.widget.Button
import android.widget.Toast

class ChatFragment : Fragment(R.layout.fragment_chat) {
    private val store = AppModule.store
    private lateinit var messagesAdapter: MessagesAdapter
    companion object {
        private const val ARG_CHAT_ID = "chat_id"

        fun newInstance(chatId: Int): ChatFragment {
            val fragment = ChatFragment()
            fragment.arguments = Bundle().apply {
                putInt(ARG_CHAT_ID, chatId)
            }
            return fragment
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val chatIdFromArgs = arguments?.getInt(ARG_CHAT_ID)
        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerMessages)
        messagesAdapter = MessagesAdapter()
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = messagesAdapter

        val edit = view.findViewById<EditText>(R.id.editMessage)
        val sendBtn = view.findViewById<Button>(R.id.btnSend)
        sendBtn.setOnClickListener {
            val text = edit.text.toString()
            val chatId = store.getState().selectedChatId
            if (chatId != null && text.isNotBlank()) {
                store.dispatch(AppAction.SendMessage(chatId, text))
                edit.setText("")
            } else Toast.makeText(requireContext(), "Выберите чат и введите текст", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            store.state.collectLatest { state ->
                val selected = state.selectedChatId
                if (selected != null) {
                    messagesAdapter.submitList(state.messages[selected] ?: emptyList())
                } else messagesAdapter.submitList(emptyList())
            }
        }
    }
}