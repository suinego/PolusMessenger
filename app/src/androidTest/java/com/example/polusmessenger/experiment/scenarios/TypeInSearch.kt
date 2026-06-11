package com.example.polusmessenger.experiment.scenarios

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.polusmessenger.R
import com.example.polusmessenger.experiment.base.BaseExperimentTest

class TypeInSearch : BaseExperimentTest() {

    override val scenarioName = "TypeSearch"

    private val queries = listOf("привет", "тест", "данные")

    override fun runScenario() {
        pause(1_500)

        for (query in queries) {
            onView(withId(R.id.searchChats))
                .perform(click(), clearText(), typeText(query))
            pause(800)

            onView(withId(R.id.searchChats))
                .perform(clearText())
            pause(400)
        }

        onView(withId(R.id.searchChats)).perform(closeSoftKeyboard())
        pause(300)
    }
}
