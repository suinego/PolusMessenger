package com.example.scanview

object ScanViewTreeManagerFactory {
    fun create(deps: ScanViewTreeManagerDeps): ScanViewTreeManager {
        return ScanViewTreeManagerImpl(deps)
    }
}