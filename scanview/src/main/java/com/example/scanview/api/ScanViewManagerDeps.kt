package com.example.scanview.api

import android.app.Activity
import android.content.Context
import android.view.View
interface ScanViewManagerDeps {
    val context: Context
    val rootViewProvider: () -> View?
    val logger: ((tag: String, msg: String) -> Unit)?
    val activityProvider: (() -> Activity)?
    val screenNameProvider: (() -> String)? get() = null
    val config: ScanViewConfig get() = ScanViewConfig()
}
