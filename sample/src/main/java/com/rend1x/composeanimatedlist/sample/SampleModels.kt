package com.rend1x.composeanimatedlist.sample

internal data class DemoItem(val id: Int)

internal data class TagChip(val id: Int, val label: String)

internal enum class EnterKind { None, Fade, SlideVertical, FadeAndSlide }

internal enum class ExitKind { None, Fade, SlideVertical, FadeAndSlide }

internal enum class PlacementKind { None, Animated }

internal enum class SamplePage { Basics, Semantics, Advanced, Stress }
