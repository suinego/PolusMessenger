package com.example.scanview.api

import android.app.Activity
import android.content.Context
import android.view.View
interface ScanViewManagerDeps {
    val context: Context //понятно
    val rootViewProvider: () -> View?
    val logger: ((tag: String, msg: String) -> Unit)? //уберем
    val activityProvider: (() -> Activity)?
}
