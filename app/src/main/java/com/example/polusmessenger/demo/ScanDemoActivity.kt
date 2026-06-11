package com.example.polusmessenger.demo

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.polusmessenger.R
import com.example.scanview.api.ScanViewManager
import com.example.scanview.api.ScanViewManagerDeps
import com.example.scanview.api.ScanViewManagerFactory
import com.example.scanview.ui.ScanViewResultsActivity

class ScanDemoActivity : AppCompatActivity() {

    private val scanViewManager: ScanViewManager by lazy {
        ScanViewManagerFactory.create(object : ScanViewManagerDeps {
            override val context = this@ScanDemoActivity
            override val rootViewProvider: () -> View? = { window.decorView.rootView }
            override val activityProvider: (() -> android.app.Activity) = { this@ScanDemoActivity }
            override val logger: ((String, String) -> Unit) = { tag, msg -> Log.d(tag, msg) }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_demo)

        supportActionBar?.title = "ScanView Demo"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<Button>(R.id.btnShowResults).setOnClickListener {
            val history = scanViewManager.getHistory()
            ScanViewResultsActivity.setData(history)
            startActivity(ScanViewResultsActivity.createIntent(this))
        }
    }

    override fun onResume() {
        super.onResume()
        scanViewManager.startRecording()
    }

    override fun onPause() {
        super.onPause()
        scanViewManager.stopRecording()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
