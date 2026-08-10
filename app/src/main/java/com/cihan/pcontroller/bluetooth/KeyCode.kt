package com.cihan.pcontroller.bluetooth

/**
 * USB HID keyboard usage IDs (Report ID 1).
 * Karakter değil — fiziksel tuş; Windows aktif düzeni karaktere çevirir.
 */
enum class KeyCode(val usage: Int) {
    NONE(0x00),

    A(0x04),
    B(0x05),
    C(0x06),
    D(0x07),
    E(0x08),
    F(0x09),
    G(0x0A),
    H(0x0B),
    I(0x0C),
    J(0x0D),
    K(0x0E),
    L(0x0F),
    M(0x10),
    N(0x11),
    O(0x12),
    P(0x13),
    Q(0x14),
    R(0x15),
    S(0x16),
    T(0x17),
    U(0x18),
    V(0x19),
    W(0x1A),
    X(0x1B),
    Y(0x1C),
    Z(0x1D),

    DIGIT_1(0x1E),
    DIGIT_2(0x1F),
    DIGIT_3(0x20),
    DIGIT_4(0x21),
    DIGIT_5(0x22),
    DIGIT_6(0x23),
    DIGIT_7(0x24),
    DIGIT_8(0x25),
    DIGIT_9(0x26),
    DIGIT_0(0x27),

    ENTER(0x28),
    ESC(0x29),
    BACKSPACE(0x2A),
    TAB(0x2B),
    SPACE(0x2C),

    MINUS(0x2D),
    EQUALS(0x2E),
    LEFT_BRACKET(0x2F),
    RIGHT_BRACKET(0x30),
    BACKSLASH(0x31),
    SEMICOLON(0x33),
    APOSTROPHE(0x34),
    GRAVE(0x35),
    COMMA(0x36),
    PERIOD(0x37),
    SLASH(0x38),

    CAPS_LOCK(0x39),

    F1(0x3A),
    F2(0x3B),
    F3(0x3C),
    F4(0x3D),
    F5(0x3E),
    F6(0x3F),
    F7(0x40),
    F8(0x41),
    F9(0x42),
    F10(0x43),
    F11(0x44),
    F12(0x45),

    RIGHT_ARROW(0x4F),
    LEFT_ARROW(0x50),
    DOWN_ARROW(0x51),
    UP_ARROW(0x52)
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
