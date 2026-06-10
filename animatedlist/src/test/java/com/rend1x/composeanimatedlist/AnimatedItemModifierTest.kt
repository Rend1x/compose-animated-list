package com.rend1x.composeanimatedlist

import org.junit.Assert.assertEquals
import org.junit.Test

class AnimatedItemModifierTest {
    @Test
    fun `fade slide maps visibility progress to alpha and y offset`() {
        val values = animatedItemFadeSlideValues(progress = 0.25f, slideRevealPx = 40f)

        assertEquals(0.25f, values.alpha, 0f)
        assertEquals(30f, values.translationY, 0f)
    }

    @Test
    fun `scale interpolates between min and max`() {
        assertEquals(
            0.96f,
            animatedItemScaleValue(progress = 0.5f, minScale = 0.92f, maxScale = 1f),
            0.0001f,
        )
    }

    @Test
    fun `collapse uses placement progress on requested axis`() {
        val vertical = animatedItemCollapseSize(width = 80, height = 40, progress = 0.5f, horizontal = false)
        val horizontal = animatedItemCollapseSize(width = 80, height = 40, progress = 0.5f, horizontal = true)

        assertEquals(80, vertical.width)
        assertEquals(20, vertical.height)
        assertEquals(40, horizontal.width)
        assertEquals(40, horizontal.height)
    }

    @Test
    fun `shared axis y mirrors entering and exiting directions`() {
        val entering = animatedItemSharedAxisYValues(
            progress = 0.25f,
            phase = ItemPhase.Entering,
            distancePx = 40f,
            forward = true,
        )
        val exiting = animatedItemSharedAxisYValues(
            progress = 0.25f,
            phase = ItemPhase.Exiting,
            distancePx = 40f,
            forward = true,
        )

        assertEquals(0.25f, entering.alpha, 0f)
        assertEquals(30f, entering.translationY, 0f)
        assertEquals(0.25f, exiting.alpha, 0f)
        assertEquals(-30f, exiting.translationY, 0f)
    }

    @Test
    fun `swipe out only translates exiting items`() {
        val visible = animatedItemSwipeOutValues(
            progress = 0.25f,
            phase = ItemPhase.Visible,
            offsetPx = 80f,
            fade = true,
        )
        val exiting = animatedItemSwipeOutValues(
            progress = 0.25f,
            phase = ItemPhase.Exiting,
            offsetPx = 80f,
            fade = true,
        )

        assertEquals(1f, visible.alpha, 0f)
        assertEquals(0f, visible.translationX, 0f)
        assertEquals(0.25f, exiting.alpha, 0f)
        assertEquals(60f, exiting.translationX, 0f)
    }
}
