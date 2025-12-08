package com.example.polusmessenger.di

import com.example.polusmessenger.data.api.RetrofitService
import com.example.polusmessenger.domain.repository.ChatRepository
import com.example.polusmessenger.domain.usecase.*
import com.example.polusmessenger.presentation.redux.*
import com.example.scanview.ScanViewTreeManager
import com.example.scanview.ScanViewTreeManagerFactory
import com.example.scanview.ScanViewTreeManagerDeps
import android.app.Application
import android.util.Log
import android.view.View

object AppModule {

    private const val BASE_URL = "http://emil-international.ru/"
    private val oauthProvider = { "0123456789" }

    val api by lazy { RetrofitService.create(BASE_URL, oauthProvider) }
    val chatRepository by lazy { ChatRepository(api) }
    val getChatsUseCase by lazy { GetChatsUseCase(chatRepository) }
    val createChatUseCase by lazy { CreateChatUseCase(chatRepository) }
    val getMessagesUseCase by lazy { GetMessagesUseCase(chatRepository) }
    val sendMessageUseCase by lazy { SendMessageUseCase(chatRepository) }

    var store: Store<AppState>
        private set

    private val mws by lazy { listOf(LoggingMiddleware<AppState>()) }

    private val epics by lazy {
        listOf<Epic>(
            LoadChatsEpic(getChatsUseCase::invoke),
            LoadMessagesEpic { chatId ->
                val (chat, messages) = getMessagesUseCase(chatId)
                chat to messages
            },
            SendMessageEpic({ store.getState() }, sendMessageUseCase::invoke),
            CreateChatEpic(createChatUseCase::invoke)
        )
    }

    init {
        store = Store(
            initialState = AppState(),
            reducer = ::appReducer,
            middlewares = mws,
            epics = epics
        )
    }

    val interactor by lazy { ChatInteractor(store) }

    lateinit var appContext: Application
        private set

    fun init(app: Application) {
        appContext = app
    }

    private var _rootViewProvider: (() -> View?)? = null
    private var _onViewInteraction: ((com.example.scanview.ViewInteractionInfo) -> Unit)? = null

    private val scanDeps by lazy {
        object : ScanViewTreeManagerDeps {
            override val context = appContext
            override val rootViewProvider: () -> View? = { _rootViewProvider?.invoke() }
            override val logger: ((tag: String, msg: String) -> Unit)? =
                { tag, msg -> Log.d(tag, msg) }
            override val onViewInteraction: ((com.example.scanview.ViewInteractionInfo) -> Unit)? =
                { info ->
                    _onViewInteraction?.invoke(info)
                }
        }
    }

    val scanManager: ScanViewTreeManager by lazy {
        ScanViewTreeManagerFactory.create(scanDeps)
    }

    fun setScanRootProvider(provider: (() -> View?)?) {
        _rootViewProvider = provider
    }

    fun setOnViewInteractionListener(listener: ((com.example.scanview.ViewInteractionInfo) -> Unit)?) {
        _onViewInteraction = listener
    }

    fun startGlobalScanWithRootProvider(provider: () -> View?) {
        setScanRootProvider(provider)
        scanManager.startIntercepting()
    }

    fun stopGlobalScan() {
        scanManager.stopIntercepting()
        setScanRootProvider(null)
        setOnViewInteractionListener(null)
    }
}
