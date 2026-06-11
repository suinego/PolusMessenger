package com.example.polusmessenger.experiment.scenarios

import com.example.polusmessenger.experiment.base.BaseExperimentTest

class SlowScrollChatList : BaseExperimentTest() {

    override val scenarioName = "SlowScroll"

    override fun runScenario() {
        pause(1_500)

        repeat(3) {
            swipeUp(steps = 60)
            pause(1_200)
        }
        repeat(3) {
            swipeDown(steps = 60)
            pause(1_200)
        }

        pause(500)
    }
}
