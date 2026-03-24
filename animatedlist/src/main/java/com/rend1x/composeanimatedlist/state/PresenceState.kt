package com.rend1x.composeanimatedlist.state

/**
 * Tracks lifecycle of an item inside the internal render list.
 */
internal enum class PresenceState {
    Entering,
    Present,
    Exiting,
}
