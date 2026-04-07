package com.rend1x.composeanimatedlist

import androidx.compose.ui.unit.dp
import com.rend1x.composeanimatedlist.animation.EnterSpec
import com.rend1x.composeanimatedlist.animation.ExitSpec
import com.rend1x.composeanimatedlist.core.PresenceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemShellProgressBoundsTest {

    @Test
    fun `values near target are clamped to expected bounds`() {
        val enter = EnterSpec.FadeAndSlide(offset = 16.dp, durationMillis = 240)
        val exit = ExitSpec.FadeAndSlide(
            offset = 16.dp,
            durationMillis = 200,
        )
        val initialPx = 160f
        val exitTargetPx = -160f

        val enteringHigh = itemLifecycleProgress(
            presence = PresenceState.Entering,
            enter = enter,
            exit = exit,
            alpha = 1.00001f,
            translationY = 0f,
            initialEnterOffsetPx = initialPx,
            exitTargetOffsetPx = exitTargetPx,
            sizeProgress = 1f,
            placementAnimated = true,
        )
        assertEquals(1f, enteringHigh.visibilityProgress, 0f)
        assertEquals(1f, enteringHigh.placementProgress, 0f)

        val enteringLow = itemLifecycleProgress(
            presence = PresenceState.Entering,
            enter = enter,
            exit = exit,
            alpha = -0.001f,
            translationY = initialPx * 2f,
            initialEnterOffsetPx = initialPx,
            exitTargetOffsetPx = exitTargetPx,
            sizeProgress = -0.05f,
            placementAnimated = true,
        )
        assertEquals(0f, enteringLow.visibilityProgress, 0f)
        assertEquals(0f, enteringLow.placementProgress, 0f)

        val exiting = itemLifecycleProgress(
            presence = PresenceState.Exiting,
            enter = enter,
            exit = exit,
            alpha = 0.9999f,
            translationY = exitTargetPx * 0.99f,
            initialEnterOffsetPx = initialPx,
            exitTargetOffsetPx = exitTargetPx,
            sizeProgress = 0.9999f,
            placementAnimated = true,
        )
        assertTrue(exiting.visibilityProgress in 0f..1f)
        assertTrue(exiting.placementProgress in 0f..1f)
    }

    @Test
    fun `slide progress helpers stay in zero one for extreme translations`() {
        assertEquals(1f, slideProgressFromTranslation(0f, 100f), 0f)
        assertEquals(0f, slideProgressFromTranslation(100f, 100f), 0f)
        assertEquals(1f, slideProgressTowardTarget(0f, -50f), 0f)
        assertEquals(0f, slideProgressTowardTarget(-50f, -50f), 0f)
    }
}
