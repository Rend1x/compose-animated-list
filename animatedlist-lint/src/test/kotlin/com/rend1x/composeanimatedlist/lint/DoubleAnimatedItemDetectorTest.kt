package com.rend1x.composeanimatedlist.lint

import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test

class DoubleAnimatedItemDetectorTest {
    @Test
    fun reportsDefaultTransitionSpecWithAnimatedItem() {
        lint()
            .allowMissingSdk()
            .testModes(TestMode.DEFAULT)
            .files(
                animatedColumnStub,
                transitionSpecStub,
                composeRuntimeStub,
                composeUiStub,
                TestFiles.kotlin(
                    """
                    package test.pkg

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier
                    import com.rend1x.composeanimatedlist.AnimatedColumn
                    import com.rend1x.composeanimatedlist.animatedItem

                    @Composable
                    fun Sample(items: List<String>) {
                        AnimatedColumn(
                            items = items,
                            key = { it },
                            content = { item ->
                            Row(Modifier.animatedItem(this))
                            },
                        )
                    }

                    @Composable
                    fun Row(modifier: Modifier) = Unit
                    """.trimIndent(),
                ),
            )
            .issues(DoubleAnimatedItemDetector.ISSUE)
            .run()
            .expect(
                """
                src/test/pkg/test.kt:14: Warning: $expectedWarningMessage [ComposeAnimatedListDoubleAnimation]
                        Row(Modifier.animatedItem(this))
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """.trimIndent(),
            )
    }

    @Test
    fun reportsDefaultRowTransitionSpecWithAnimatedItem() {
        lint()
            .allowMissingSdk()
            .testModes(TestMode.DEFAULT)
            .files(
                animatedColumnStub,
                transitionSpecStub,
                composeRuntimeStub,
                composeUiStub,
                TestFiles.kotlin(
                    """
                    package test.pkg

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier
                    import com.rend1x.composeanimatedlist.AnimatedRow
                    import com.rend1x.composeanimatedlist.animatedItem

                    @Composable
                    fun Sample(items: List<String>) {
                        AnimatedRow(
                            items = items,
                            key = { it },
                            content = { item ->
                            Chip(Modifier.animatedItem(this))
                            },
                        )
                    }

                    @Composable
                    fun Chip(modifier: Modifier) = Unit
                    """.trimIndent(),
                ),
            )
            .issues(DoubleAnimatedItemDetector.ISSUE)
            .run()
            .expect(
                """
                src/test/pkg/test.kt:14: Warning: $expectedWarningMessage [ComposeAnimatedListDoubleAnimation]
                        Chip(Modifier.animatedItem(this))
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """.trimIndent(),
            )
    }

    @Test
    fun allowsNoneTransitionSpecWithAnimatedItem() {
        lint()
            .allowMissingSdk()
            .testModes(TestMode.DEFAULT)
            .files(
                animatedColumnStub,
                transitionSpecStub,
                composeRuntimeStub,
                composeUiStub,
                TestFiles.kotlin(
                    """
                    package test.pkg

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier
                    import com.rend1x.composeanimatedlist.AnimatedColumn
                    import com.rend1x.composeanimatedlist.animation.AnimatedItemDefaults
                    import com.rend1x.composeanimatedlist.animatedItem

                    @Composable
                    fun Sample(items: List<String>) {
                        AnimatedColumn(
                            items = items,
                            key = { it },
                            transitionSpec = AnimatedItemDefaults.none(),
                            content = {
                            Row(Modifier.animatedItem(this))
                            },
                        )
                    }

                    @Composable
                    fun Row(modifier: Modifier) = Unit
                    """.trimIndent(),
                ),
            )
            .issues(DoubleAnimatedItemDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun allowsShellOwnedAnimationWithoutAnimatedItem() {
        lint()
            .allowMissingSdk()
            .testModes(TestMode.DEFAULT)
            .files(
                animatedColumnStub,
                transitionSpecStub,
                composeRuntimeStub,
                composeUiStub,
                TestFiles.kotlin(
                    """
                    package test.pkg

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier
                    import com.rend1x.composeanimatedlist.AnimatedColumn

                    @Composable
                    fun Sample(items: List<String>) {
                        AnimatedColumn(
                            items = items,
                            key = { it },
                            content = {
                            Row(Modifier)
                            },
                        )
                    }

                    @Composable
                    fun Row(modifier: Modifier) = Unit
                    """.trimIndent(),
                ),
            )
            .issues(DoubleAnimatedItemDetector.ISSUE)
            .run()
            .expectClean()
    }

    private val expectedWarningMessage = "AnimatedColumn/AnimatedRow already applies fade/slide with its default or " +
        "non-none transitionSpec. Use AnimatedItemDefaults.none() on the list or remove Modifier.animatedItem " +
        "from this item to avoid compounded opacity/offset."

    private val animatedColumnStub = TestFiles.kotlin(
        "src/com/rend1x/composeanimatedlist/AnimatedColumn.kt",
        """
        package com.rend1x.composeanimatedlist

        import androidx.compose.runtime.Composable
        import androidx.compose.ui.Modifier
        import com.rend1x.composeanimatedlist.animation.AnimatedItemDefaults
        import com.rend1x.composeanimatedlist.animation.AnimatedItemTransitionSpec

        class AnimatedItemScope

        @Composable
        fun <T> AnimatedColumn(
            items: List<T>,
            key: (T) -> Any,
            modifier: Modifier = Modifier,
            transitionSpec: AnimatedItemTransitionSpec = AnimatedItemDefaults.fadeSlide(),
            content: @Composable AnimatedItemScope.(T) -> Unit,
        ) = Unit

        @Composable
        fun <T> AnimatedRow(
            items: List<T>,
            key: (T) -> Any,
            modifier: Modifier = Modifier,
            transitionSpec: AnimatedItemTransitionSpec = AnimatedItemDefaults.fadeSlide(),
            content: @Composable AnimatedItemScope.(T) -> Unit,
        ) = Unit

        fun Modifier.animatedItem(scope: AnimatedItemScope): Modifier = this
        """.trimIndent(),
    )

    private val transitionSpecStub = TestFiles.kotlin(
        "src/com/rend1x/composeanimatedlist/animation/AnimatedItemTransitionSpec.kt",
        """
        package com.rend1x.composeanimatedlist.animation

        class AnimatedItemTransitionSpec

        object AnimatedItemDefaults {
            fun fadeSlide(): AnimatedItemTransitionSpec = AnimatedItemTransitionSpec()
            fun none(): AnimatedItemTransitionSpec = AnimatedItemTransitionSpec()
        }
        """.trimIndent(),
    )

    private val composeRuntimeStub = TestFiles.kotlin(
        "src/androidx/compose/runtime/Composable.kt",
        """
        package androidx.compose.runtime

        annotation class Composable
        """.trimIndent(),
    )

    private val composeUiStub = TestFiles.kotlin(
        "src/androidx/compose/ui/Modifier.kt",
        """
        package androidx.compose.ui

        class Modifier {
            companion object
        }
        """.trimIndent(),
    )
}
