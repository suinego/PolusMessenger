package com.example.polusmessenger.experiment.scenarios

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.polusmessenger.R
import com.example.polusmessenger.experiment.base.BaseExperimentTest

class RapidTaps : BaseExperimentTest() {

    override val scenarioName = "RapidTaps"

    override fun runScenario() {
        pause(1_200)

        repeat(8) {
            try {
                onView(withId(R.id.recyclerChats))
                    .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
                pause(300)
            } catch (_: Exception) {}
            safeBack()
            pause(200)
        }

        pause(400)
    }
}
