package com.example.polusmessenger.experiment.scenarios

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.polusmessenger.R
import com.example.polusmessenger.experiment.base.BaseExperimentTest

class SendMessages : BaseExperimentTest() {

    override val scenarioName = "SendMessages"

    private val messages = listOf(
        "Тестовое сообщение 1",
        "Привет, это эксперимент",
        "Проверка ScanView",
        "Четвёртое сообщение",
        "Последнее сообщение"
    )

    override fun runScenario() {
        pause(1_500)

        try {
            onView(withId(R.id.recyclerChats))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
            pause(1_200)

            for (msg in messages) {
                onView(withId(R.id.editMessage))
                    .perform(click(), clearText(), typeText(msg), closeSoftKeyboard())
                pause(300)
                onView(withId(R.id.btnSend)).perform(click())
                pause(500)
            }
        } catch (_: Exception) {
        }

        safeBack()
        pause(600)
    }
}
