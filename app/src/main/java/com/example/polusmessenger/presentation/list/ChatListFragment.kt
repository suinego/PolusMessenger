package com.example.polusmessenger.presentation.list

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
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
import kotlinx.coroutines.launch

class ChatListFragment : Fragment(R.layout.fragment_chat_list) {

    private val store = AppModule.store
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ChatListAdapter
    private lateinit var searchEdit: EditText

    private var createChatDialog: androidx.appcompat.app.AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch { store.dispatch(AppAction.LoadChats) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recycler = view.findViewById(R.id.recyclerChats)
        searchEdit = view.findViewById(R.id.searchChats)

        adapter = ChatListAdapter { chat ->
            lifecycleScope.launch {
                store.dispatch(AppAction.SelectChat(chat.id))
                store.dispatch(AppAction.LoadMessages(chat.id))
            }
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        setupSearch()
        setupPagination()
        observeChats()

        view.findViewById<View>(R.id.fabCreateChat).setOnClickListener { showCreateChatDialog() }
    }

    private fun setupSearch() {
        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = adapter.filter(s?.toString() ?: "")
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupPagination() {
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lm = rv.layoutManager as? LinearLayoutManager ?: return
                val visible = lm.childCount
                val total = lm.itemCount
                val firstVisible = lm.findFirstVisibleItemPosition()

                val state = store.getState()
                val alreadyLoaded = state.chats.size
                val serverTotal = state.chatsTotal
                val isLoading = state.loadingMore || state.loading

                if (!isLoading && alreadyLoaded < serverTotal &&
                    firstVisible + visible + 5 >= total
                ) {
                    lifecycleScope.launch { store.dispatch(AppAction.LoadMoreChats(alreadyLoaded)) }
                }
            }
        })
    }

    private fun observeChats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                store.states
                    .map { it.chats }
                    .distinctUntilChanged()
                    .collect { chats ->
                        adapter.setChats(chats)
                        val query = searchEdit.text?.toString() ?: ""
                        if (query.isNotBlank()) adapter.filter(query)
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
            .setNegativeButton("Отмена") { _, _ -> createChatDialog = null }
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
