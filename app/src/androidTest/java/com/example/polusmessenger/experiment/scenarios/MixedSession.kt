package com.example.polusmessenger.experiment.scenarios

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.polusmessenger.R
import com.example.polusmessenger.experiment.base.BaseExperimentTest

class MixedSession : BaseExperimentTest() {

    override val scenarioName = "MixedSession"

    override fun runScenario() {
        pause(1_500)

        repeat(2) {
            swipeUp(steps = 8)
            pause(400)
            swipeDown(steps = 8)
            pause(400)

            try {
                onView(withId(R.id.recyclerChats))
                    .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
                pause(900)

                onView(withId(R.id.editMessage))
                    .perform(click(), clearText(), typeText("Привет"), closeSoftKeyboard())
                pause(300)
                onView(withId(R.id.btnSend)).perform(click())
                pause(500)

                swipeUp(steps = 15)
                pause(400)
            } catch (_: Exception) {}

            safeBack()
            pause(600)

            onView(withId(R.id.bottomNav)).perform(selectNavItem(R.id.nav_profile))
            pause(700)
            onView(withId(R.id.bottomNav)).perform(selectNavItem(R.id.nav_chats))
            pause(600)
        }
    }
}
