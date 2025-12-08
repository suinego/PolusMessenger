package com.example.scanview

class DI(private val deps: ScanViewTreeManagerDeps) {
    fun createManager(): ScanViewTreeManager = ScanViewTreeManagerFactory.create(deps)

    fun startScanningAndIntercepting() {
        val mgr = createManager()
        mgr.startIntercepting()
    }
}