package com.badgebot.controller

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import com.badgebot.controller.ble.ControlButton
import com.badgebot.controller.ui.ControlPadContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ControlPadScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun allDirectionButtons_areDisplayed() {
        composeRule.setContent {
            ControlPadContent(
                onButtonPressed = {},
                onButtonReleased = {},
            )
        }

        composeRule.onNodeWithContentDescription("Up").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Down").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Left").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Right").assertIsDisplayed()
    }

    @Test
    fun pressingButton_emitsPressAndReleaseEvents() {
        val pressed = mutableListOf<ControlButton>()
        val released = mutableListOf<ControlButton>()

        composeRule.setContent {
            ControlPadContent(
                onButtonPressed = { pressed += it },
                onButtonReleased = { released += it },
            )
        }

        composeRule.onNodeWithContentDescription("Up").performTouchInput {
            click()
        }

        assertEquals(listOf(ControlButton.UP), pressed)
        assertEquals(listOf(ControlButton.UP), released)
    }
}
