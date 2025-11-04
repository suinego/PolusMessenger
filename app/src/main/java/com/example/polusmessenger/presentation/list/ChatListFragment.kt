package com.example.polusmessenger.presentation.list

import android.os.Bundle
import android.view.View
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.polusmessenger.R
import com.example.polusmessenger.di.AppModule
import com.example.polusmessenger.presentation.redux.AppAction
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.polusmessenger.presentation.chat.ChatFragment

class ChatListFragment : Fragment(R.layout.fragment_chat_list) {
    private val store = AppModule.store
    private lateinit var recycler: RecyclerView
    private val adapter = ChatListAdapter { chat ->
        store.dispatch(AppAction.SelectChat(chat.id))
        store.dispatch(AppAction.LoadMessages(chat.id))

        parentFragmentManager.beginTransaction()
            .replace(R.id.container, ChatFragment.newInstance(chat.id))
            .addToBackStack(null)
            .commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        store.dispatch(AppAction.LoadChats)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recycler = view.findViewById(R.id.recyclerChats)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        view.findViewById<View>(R.id.fabCreateChat)?.setOnClickListener {
            val name = "Chat ${System.currentTimeMillis() % 1000}"
            store.dispatch(AppAction.CreateChat(name))
            Toast.makeText(requireContext(), "создать чат: $name", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            store.state.collectLatest { state ->
                Log.d("ChatListFragment", "чатов ${state.chats.size} столько: ${state.chats.joinToString { it.name }}")
                adapter.submitList(state.chats)
            }
        }
    }
}