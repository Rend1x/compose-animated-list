package com.rend1x.composeanimatedlist

import org.junit.Assert.assertEquals
import org.junit.Test

class AnimatedListOrientationLayoutTest {
    @Test
    fun `vertical orientation animates height and keeps width`() {
        val size = mainAxisLayoutSize(
            orientation = AnimatedListOrientation.Vertical,
            width = 80,
            height = 40,
            sizeProgress = 0.5f,
        )

        assertEquals(MainAxisLayoutSize(width = 80, height = 20), size)
    }

    @Test
    fun `horizontal orientation animates width and keeps height`() {
        val size = mainAxisLayoutSize(
            orientation = AnimatedListOrientation.Horizontal,
            width = 80,
            height = 40,
            sizeProgress = 0.5f,
        )

        assertEquals(MainAxisLayoutSize(width = 40, height = 40), size)
    }

    @Test
    fun `main axis layout size clamps progress`() {
        assertEquals(
            MainAxisLayoutSize(width = 0, height = 40),
            mainAxisLayoutSize(
                orientation = AnimatedListOrientation.Horizontal,
                width = 80,
                height = 40,
                sizeProgress = -1f,
            ),
        )
        assertEquals(
            MainAxisLayoutSize(width = 80, height = 40),
            mainAxisLayoutSize(
                orientation = AnimatedListOrientation.Horizontal,
                width = 80,
                height = 40,
                sizeProgress = 2f,
            ),
        )
    }
}
