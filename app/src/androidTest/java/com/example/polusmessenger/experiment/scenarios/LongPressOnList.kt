package com.example.polusmessenger.experiment.scenarios

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.polusmessenger.R
import com.example.polusmessenger.experiment.base.BaseExperimentTest

class LongPressOnList : BaseExperimentTest() {

    override val scenarioName = "LongPress"

    override fun runScenario() {
        pause(1_500)

        repeat(4) {
            try {
                onView(withId(R.id.recyclerChats))
                    .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick()))
                pause(800)
            } catch (_: Exception) {}

            safeBack()
            pause(600)
        }
    }
}
