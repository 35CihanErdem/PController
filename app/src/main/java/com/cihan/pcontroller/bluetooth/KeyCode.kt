package com.cihan.pcontroller.bluetooth

/** Keyboard report key codes (Report ID 1). Media keys use [ConsumerAction]. */
enum class KeyCode {
    UP_ARROW,
    DOWN_ARROW,
    LEFT_ARROW,
    RIGHT_ARROW,
    SPACE,
    ESC,
    F,
    NONE
}

/** Consumer Control actions (Report ID 2). */
enum class ConsumerAction {
    VOLUME_UP,
    VOLUME_DOWN,
    PLAY_PAUSE,
    NONE
}
