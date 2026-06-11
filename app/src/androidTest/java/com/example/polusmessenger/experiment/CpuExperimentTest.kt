package com.example.polusmessenger.experiment

import android.util.Log
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.recyclerview.widget.RecyclerView
import com.example.polusmessenger.MainActivity
import com.example.polusmessenger.R
import com.example.polusmessenger.experiment.metrics.CpuSampler
import com.example.polusmessenger.experiments.BitmapCaptureRecorder
import com.example.polusmessenger.experiments.PeriodicViewNodeRecorder
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CpuExperimentTest {

    companion object {
        private const val TAG         = "CpuExperiment"
        const val RESULT_TAG          = "CPU_RESULT"
        private const val REPETITIONS = 4

        private val STRATEGIES = listOf("Baseline", "S1_Bitmap", "S2_ViewNode", "S3_ScanView")
    }

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val device by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    private val cpuSampler = CpuSampler(intervalMs = 500)

    private var s1Recorder: BitmapCaptureRecorder? = null
    private var s2Recorder: PeriodicViewNodeRecorder? = null

    @Test
    fun runCpuExperiment() {
        Thread.sleep(3_000)

        Log.i(RESULT_TAG, "strategy,run,avgCpuPct,maxCpuPct,sampleCount")

        for (strategy in STRATEGIES) {
            Log.i(TAG, "═══ CPU: $strategy ═══")

            for (run in 1..REPETITIONS) {
                resetToHome()
                Thread.sleep(1_000)

                activityRule.scenario.onActivity { activity ->
                    startStrategy(activity, strategy)
                }

                cpuSampler.start("$strategy,run$run")

                runMixedScenario()

                activityRule.scenario.onActivity { activity ->
                    stopStrategy(activity, strategy)
                }

                val cpu = cpuSampler.stop()
                Log.i(RESULT_TAG,
                    "$strategy,$run,${"%.2f".format(cpu.avgCpuPct)}," +
                    "${"%.2f".format(cpu.maxCpuPct)},${cpu.sampleCount}")
                Log.i(TAG,
                    "[$strategy] run=$run avg=${"%.1f".format(cpu.avgCpuPct)}% " +
                    "max=${"%.1f".format(cpu.maxCpuPct)}%")

                Thread.sleep(2_000)
            }
        }
    }

    private fun startStrategy(activity: MainActivity, strategy: String) {
        activity.scanViewManager.stopRecording()

        when (strategy) {
            "Baseline"   -> {  }

            "S1_Bitmap"  -> {
                s1Recorder = BitmapCaptureRecorder(
                    rootViewProvider = { activity.window.decorView.rootView },
                    fps = 2
                ).also { it.start() }
            }

            "S2_ViewNode" -> {
                s2Recorder = PeriodicViewNodeRecorder(
                    rootViewProvider = { activity.window.decorView.rootView },
                    manager = activity.scanViewManager,
                    fps = 2
                ).also { it.start() }
            }

            "S3_ScanView" -> {
                activity.scanViewManager.clearHistory()
                activity.scanViewManager.startRecording()
            }
        }
    }

    private fun stopStrategy(activity: MainActivity, strategy: String) {
        when (strategy) {
            "S1_Bitmap"   -> { s1Recorder?.stop(); s1Recorder = null }
            "S2_ViewNode" -> { s2Recorder?.stop(); s2Recorder = null }
            "S3_ScanView" -> activity.scanViewManager.stopRecording()
            "Baseline"    -> {}
        }
    }

    private fun runMixedScenario() {
        pause(1_000)
        repeat(2) {
            swipeUp(steps = 8);  pause(400)
            swipeDown(steps = 8); pause(400)

            try {
                onView(withId(R.id.recyclerChats))
                    .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
                pause(900)
                onView(withId(R.id.editMessage))
                    .perform(click(), clearText(), typeText("Тест"), closeSoftKeyboard())
                pause(300)
                onView(withId(R.id.btnSend)).perform(click())
                pause(500)
                swipeUp(steps = 15); pause(300)
            } catch (_: Exception) {}

            safeBack(); pause(600)

            onView(withId(R.id.bottomNav)).perform(selectNavItem(R.id.nav_profile))
            pause(700)
            onView(withId(R.id.bottomNav)).perform(selectNavItem(R.id.nav_chats))
            pause(600)
        }
    }

    private fun resetToHome() {
        activityRule.scenario.onActivity { activity ->
            activity.supportFragmentManager.popBackStack(
                null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        }
        pause(500)
        try {
            onView(withId(R.id.bottomNav)).perform(selectNavItem(R.id.nav_chats))
            pause(700)
        } catch (_: Exception) {}
    }

    private fun safeBack() {
        activityRule.scenario.onActivity { activity ->
            val fm = activity.supportFragmentManager
            if (fm.backStackEntryCount > 0) fm.popBackStack()
        }
        pause(400)
    }

    private fun selectNavItem(itemId: Int): ViewAction = object : ViewAction {
        override fun getDescription() = "select nav $itemId"
        override fun getConstraints(): Matcher<View> = isDisplayed()
        override fun perform(uiController: UiController, view: View) {
            (view as BottomNavigationView).selectedItemId = itemId
            uiController.loopMainThreadUntilIdle()
        }
    }

    private fun swipeUp(steps: Int = 10) {
        val w = device.displayWidth; val h = device.displayHeight
        device.swipe(w / 2, h * 7 / 10, w / 2, h * 3 / 10, steps)
    }

    private fun swipeDown(steps: Int = 10) {
        val w = device.displayWidth; val h = device.displayHeight
        device.swipe(w / 2, h * 3 / 10, w / 2, h * 7 / 10, steps)
    }

    private fun pause(ms: Long) = Thread.sleep(ms)
}
