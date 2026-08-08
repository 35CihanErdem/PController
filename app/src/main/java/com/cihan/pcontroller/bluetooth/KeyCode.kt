package com.cihan.pcontroller.bluetooth

/** Keyboard report key codes (Report ID 1). Media keys use [ConsumerAction]. */
enum class KeyCode {
    UP_ARROW,
    DOWN_ARROW,
    LEFT_ARROW,
    RIGHT_ARROW,
    SPACE,
    ESC,
    ENTER,
    F,
    F5,
    F11,
    /** Letters — YouTube/Netflix kısayolları */
    B,
    C,
    J,
    K,
    L,
    M,
    N,
    P,
    NONE
}

/** Consumer Control actions (Report ID 2). */
enum class ConsumerAction {
    VOLUME_UP,
    VOLUME_DOWN,
    PLAY_PAUSE,
    SCAN_NEXT,
    SCAN_PREVIOUS,
    MUTE,
    NONE
}
