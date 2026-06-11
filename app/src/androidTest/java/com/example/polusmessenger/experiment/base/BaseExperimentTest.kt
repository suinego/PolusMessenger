package com.example.polusmessenger.experiment.base

import android.util.Log
import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.example.polusmessenger.MainActivity
import com.example.polusmessenger.R
import com.example.polusmessenger.experiment.metrics.CsvExporter
import com.example.polusmessenger.experiment.metrics.ExperimentResult
import com.example.polusmessenger.experiment.metrics.MetricsCollector
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
abstract class BaseExperimentTest {

    companion object {
        const val REPETITIONS = 5
        private const val TAG = "Experiment"

        private val ALL_STRATEGIES: Map<String, RecordingStrategy> = mapOf(
            "Baseline"    to RecordingStrategy.Baseline,
            "S1_Bitmap"   to RecordingStrategy.S1Bitmap(),
            "S2_ViewNode" to RecordingStrategy.S2ViewNode(),
            "S3_ScanView" to RecordingStrategy.S3ScanView
        )

        fun resolveStrategies(): List<RecordingStrategy> {
            val arg = InstrumentationRegistry.getArguments()
                .getString("strategies", "S1_Bitmap,S2_ViewNode,S3_ScanView")
            return arg.split(",").mapNotNull { ALL_STRATEGIES[it.trim()] }
                .ifEmpty { listOf(RecordingStrategy.S1Bitmap(), RecordingStrategy.S2ViewNode(), RecordingStrategy.S3ScanView) }
        }
    }

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    protected val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    abstract val scenarioName: String

    abstract fun runScenario()

    @Test
    fun runExperiment() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Thread.sleep(3_000)

        val allResults = mutableListOf<ExperimentResult>()
        val strategies = resolveStrategies()

        for (strategy in strategies) {
            Log.i(TAG, "═══ ${strategy.name} × $scenarioName ═══")
            for (run in 1..REPETITIONS) {
                resetToHome()
                Thread.sleep(1_000)

                val result = runSingleRun(strategy, run)
                allResults.add(result)

                Log.i(TAG, "[${strategy.name}] run=$run | " +
                    "data=${"%.1f".format(result.dataKB)} KB | " +
                    "avgFrame=${"%.1f".format(result.avgFrameMs)} ms | " +
                    "jank=${result.jankCount}/${result.totalFrames}")

                Thread.sleep(2_000)
            }
        }

        CsvExporter.append(context, allResults)
    }

    private fun runSingleRun(strategy: RecordingStrategy, run: Int): ExperimentResult {
        var metricsCollector: MetricsCollector? = null
        val startMs = System.currentTimeMillis()

        activityRule.scenario.onActivity { activity ->
            metricsCollector = MetricsCollector(activity.window).also { it.start() }
            strategy.start(activity)
        }

        runScenario()

        var strategyMetrics: StrategyMetrics? = null
        var frameResult: MetricsCollector.FrameResult? = null
        activityRule.scenario.onActivity { activity ->
            strategyMetrics = strategy.stop(activity)
            frameResult = metricsCollector!!.stop()
        }

        return ExperimentResult(
            strategy     = strategy.name,
            scenario     = scenarioName,
            run          = run,
            avgFrameMs   = frameResult!!.avgFrameMs,
            p95FrameMs   = frameResult!!.p95FrameMs,
            jankCount    = frameResult!!.jankCount,
            totalFrames  = frameResult!!.totalFrames,
            dataKB       = strategyMetrics!!.dataKB,
            captureCount = strategyMetrics!!.captureCount,
            durationMs   = System.currentTimeMillis() - startMs
        )
    }

    protected fun resetToHome() {
        activityRule.scenario.onActivity { activity ->
            activity.supportFragmentManager.popBackStack(
                null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        }
        Thread.sleep(500)
        try {
            Espresso.onView(withId(R.id.bottomNav))
                .perform(selectNavItem(R.id.nav_chats))
            Thread.sleep(700)
        } catch (_: Exception) {}
    }

    protected fun safeBack() {
        activityRule.scenario.onActivity { activity ->
            val fm = activity.supportFragmentManager
            if (fm.backStackEntryCount > 0) {
                fm.popBackStack()
            }
        }
        Thread.sleep(400)
    }

    protected fun selectNavItem(itemId: Int): ViewAction = object : ViewAction {
        override fun getDescription() = "select bottom nav item: $itemId"
        override fun getConstraints(): Matcher<View> = isDisplayed()
        override fun perform(uiController: UiController, view: View) {
            (view as BottomNavigationView).selectedItemId = itemId
            uiController.loopMainThreadUntilIdle()
        }
    }

    protected fun pause(ms: Long = 400) = Thread.sleep(ms)

    protected fun swipeUp(steps: Int = 10) {
        val w = device.displayWidth
        val h = device.displayHeight
        device.swipe(w / 2, h * 7 / 10, w / 2, h * 3 / 10, steps)
    }

    protected fun swipeDown(steps: Int = 10) {
        val w = device.displayWidth
        val h = device.displayHeight
        device.swipe(w / 2, h * 3 / 10, w / 2, h * 7 / 10, steps)
    }

    protected fun tapCenter() {
        device.click(device.displayWidth / 2, device.displayHeight / 2)
    }
}
