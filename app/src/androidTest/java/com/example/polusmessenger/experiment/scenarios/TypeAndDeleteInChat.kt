package com.example.polusmessenger.experiment.scenarios

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.polusmessenger.R
import com.example.polusmessenger.experiment.base.BaseExperimentTest

class TypeAndDeleteInChat : BaseExperimentTest() {

    override val scenarioName = "TypeDelete"

    private val longText = "Проверочный текст для эксперимента ScanView"

    override fun runScenario() {
        pause(1_500)

        try {
            onView(withId(R.id.recyclerChats))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
            pause(1_000)

            repeat(3) {
                onView(withId(R.id.editMessage))
                    .perform(click(), clearText(), typeText(longText))
                pause(600)

                val field = onView(withId(R.id.editMessage))
                repeat(20) {
                    field.perform(pressKey(android.view.KeyEvent.KEYCODE_DEL))
                    pause(40)
                }
                pause(400)

                onView(withId(R.id.editMessage)).perform(clearText())
                pause(300)
            }

            onView(withId(R.id.editMessage)).perform(closeSoftKeyboard())
        } catch (_: Exception) {}

        safeBack()
        pause(600)
    }
}
