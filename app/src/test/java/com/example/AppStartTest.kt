package com.example

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ActivityScenario

@RunWith(RobolectricTestRunner::class)
class AppStartTest {
    @Test
    fun testAppStarts() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            println("Activity started successfully!")
        }
    }
}
