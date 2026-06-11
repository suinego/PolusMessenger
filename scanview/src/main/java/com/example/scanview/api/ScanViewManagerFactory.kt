package com.example.scanview.api

import com.example.scanview.internal.ScanViewManagerImpl

object ScanViewManagerFactory {
    fun create(deps: ScanViewManagerDeps): ScanViewManager {
        return ScanViewManagerImpl(deps)
    }
}
