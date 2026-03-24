package com.rend1x.composeanimatedlist.animation

import org.junit.Assert.assertEquals
import org.junit.Test

class TransitionSpecDurationTest {

    @Test
    fun noneSpecs_haveZeroVisibilityDuration() {
        assertEquals(0, EnterSpec.None.visibilityAnimationDurationMillis)
        assertEquals(0, ExitSpec.None.visibilityAnimationDurationMillis)
    }

    @Test
    fun fadeWithZeroDuration_reportsZero() {
        assertEquals(0, EnterSpec.Fade(durationMillis = 0).visibilityAnimationDurationMillis)
        assertEquals(0, ExitSpec.Fade(durationMillis = 0).visibilityAnimationDurationMillis)
    }

    @Test
    fun placementAnimated_zeroDuration_isAllowed() {
        assertEquals(0, PlacementBehavior.Animated(durationMillis = 0).durationMillis)
    }
}
