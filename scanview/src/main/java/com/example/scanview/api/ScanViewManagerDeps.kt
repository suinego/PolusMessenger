package com.example.scanview.api

import android.app.Activity
import android.content.Context
import android.view.View
interface ScanViewManagerDeps {
    val context: Context
    val rootViewProvider: () -> View?
    val logger: ((tag: String, msg: String) -> Unit)?
    val activityProvider: (() -> Activity)?
    /** Возвращает название текущего экрана/фрагмента для записи в InteractionRecord */
    val screenNameProvider: (() -> String)? get() = null
    /** Конфигурация захвата (глубина ViewNode, семплирование) */
    val config: ScanViewConfig get() = ScanViewConfig()
}
