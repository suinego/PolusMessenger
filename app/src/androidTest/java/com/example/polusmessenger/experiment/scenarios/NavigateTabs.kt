package com.example.polusmessenger.experiment.scenarios

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.polusmessenger.R
import com.example.polusmessenger.experiment.base.BaseExperimentTest

class NavigateTabs : BaseExperimentTest() {

    override val scenarioName = "NavigateTabs"

    private val tabSequence = listOf(
        R.id.nav_profile,
        R.id.nav_results,
        R.id.nav_chats,
        R.id.nav_profile,
        R.id.nav_chats,
        R.id.nav_results,
        R.id.nav_profile,
        R.id.nav_chats
    )

    override fun runScenario() {
        pause(1_200)

        for (tabId in tabSequence) {
            onView(withId(R.id.bottomNav)).perform(selectNavItem(tabId))
            pause(900)
        }
    }
}
