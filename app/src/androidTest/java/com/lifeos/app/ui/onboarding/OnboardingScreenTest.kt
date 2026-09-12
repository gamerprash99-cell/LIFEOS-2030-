package com.lifeos.app.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lifeos.app.ui.theme.LifeOSTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun landingAndNextAdvanceThroughOnboarding() {
        var finished = false
        composeRule.setContent {
            LifeOSTheme {
                OnboardingScreen(onFinish = { finished = true })
            }
        }

        composeRule.onNodeWithText("Start your journey").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Welcome to LifeOS").assertIsDisplayed()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.onNodeWithText("Your life. Your data.").assertIsDisplayed()

        assertTrue(!finished)
    }

    @Test
    fun restoreActionIsExposedAndSkipCompletes() {
        var restoreClicked = false
        var finished = false
        composeRule.setContent {
            LifeOSTheme {
                OnboardingScreen(
                    onFinish = { finished = true },
                    onRestoreBackup = { restoreClicked = true }
                )
            }
        }

        composeRule.onNodeWithText("Start your journey").performClick()
        composeRule.onNodeWithText("Restore a LifeOS backup").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Skip").assertIsDisplayed().performClick()

        assertTrue(restoreClicked)
        assertTrue(finished)
    }
}
