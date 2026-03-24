package ren.pj.animatedlist

/**
 * Tracks lifecycle of an item inside the internal render list.
 */
internal enum class PresenceState {
    Entering,
    Present,
    Exiting,
}
