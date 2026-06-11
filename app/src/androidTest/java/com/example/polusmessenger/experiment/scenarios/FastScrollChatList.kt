package com.example.polusmessenger.experiment.scenarios

import com.example.polusmessenger.experiment.base.BaseExperimentTest

class FastScrollChatList : BaseExperimentTest() {

    override val scenarioName = "FastScroll"

    override fun runScenario() {
        pause(1_500)

        repeat(10) {
            swipeUp(steps = 5)
            pause(250)
        }
        repeat(10) {
            swipeDown(steps = 5)
            pause(250)
        }

        pause(500)
    }
}
