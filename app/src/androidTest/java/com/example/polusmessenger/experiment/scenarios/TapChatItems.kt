package com.example.polusmessenger.experiment.scenarios

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.recyclerview.widget.RecyclerView
import com.example.polusmessenger.R
import com.example.polusmessenger.experiment.base.BaseExperimentTest

class TapChatItems : BaseExperimentTest() {

    override val scenarioName = "TapChats"

    override fun runScenario() {
        pause(1_500)

        repeat(3) {
            try {
                onView(withId(R.id.recyclerChats))
                    .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
                pause(1_500)
            } catch (_: Exception) {
            }
            safeBack()
            pause(700)
        }
    }
}
