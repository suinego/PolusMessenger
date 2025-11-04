package com.example.polusmessenger

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.polusmessenger.R
import com.example.polusmessenger.di.AppModule
import com.example.polusmessenger.presentation.chat.ChatFragment
import com.example.polusmessenger.presentation.list.ChatListFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val store = AppModule.store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.container, ChatListFragment())
                setReorderingAllowed(true)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                store.state.collect { state ->
                    val chatId = state.selectedChatId
                    if (chatId != null) {
                        supportFragmentManager.commit {
                            replace(R.id.container, ChatFragment.newInstance(chatId))
                            setReorderingAllowed(true)
                            addToBackStack(null)
                        }
                    }
                }
            }
        }
    }
}
