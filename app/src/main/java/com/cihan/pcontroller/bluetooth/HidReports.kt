package com.cihan.pcontroller.bluetooth

/**
 * HID report descriptor and report builders.
 * Report ID 1 = keyboard, Report ID 2 = consumer, Report ID 3 = mouse (16-bit X/Y).
 */
object HidReports {

    const val REPORT_ID_KEYBOARD: Int = 1
    const val REPORT_ID_CONSUMER: Int = 2
    const val REPORT_ID_MOUSE: Int = 3

    const val MOUSE_BTN_LEFT = 0x01
    const val MOUSE_BTN_RIGHT = 0x02
    const val MOUSE_BTN_MIDDLE = 0x04

    const val USAGE_VOLUME_UP = 0x00E9
    const val USAGE_VOLUME_DOWN = 0x00EA
    const val USAGE_PLAY_PAUSE = 0x00CD
    const val USAGE_SCAN_NEXT = 0x00B5
    const val USAGE_SCAN_PREVIOUS = 0x00B6
    const val USAGE_MUTE = 0x00E2
    const val MOD_LEFT_CTRL = 0x01
    const val MOD_LEFT_SHIFT = 0x02
    const val MOD_LEFT_ALT = 0x04
    const val MOD_LEFT_GUI = 0x08

    /** Descriptor sürümü — değişince Windows unpair şart */
    const val DESCRIPTOR_VERSION = 3

    fun descriptor(): ByteArray = byteArrayOf(
        // Keyboard (Report ID 1)
        0x05.toByte(), 0x01.toByte(),
        0x09.toByte(), 0x06.toByte(),
        0xa1.toByte(), 0x01.toByte(),
        0x85.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x01.toByte(),
        0x95.toByte(), 0x08.toByte(),
        0x05.toByte(), 0x07.toByte(),
        0x19.toByte(), 0xe0.toByte(),
        0x29.toByte(), 0xe7.toByte(),
        0x15.toByte(), 0x00.toByte(),
        0x25.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x02.toByte(),
        0x95.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x08.toByte(),
        0x81.toByte(), 0x03.toByte(),
        0x95.toByte(), 0x05.toByte(),
        0x75.toByte(), 0x01.toByte(),
        0x05.toByte(), 0x08.toByte(),
        0x19.toByte(), 0x01.toByte(),
        0x29.toByte(), 0x05.toByte(),
        0x91.toByte(), 0x02.toByte(),
        0x95.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x03.toByte(),
        0x91.toByte(), 0x03.toByte(),
        0x95.toByte(), 0x06.toByte(),
        0x75.toByte(), 0x08.toByte(),
        0x15.toByte(), 0x00.toByte(),
        0x25.toByte(), 0xE7.toByte(),
        0x05.toByte(), 0x07.toByte(),
        0x19.toByte(), 0x00.toByte(),
        0x29.toByte(), 0xE7.toByte(),
        0x81.toByte(), 0x00.toByte(),
        0xc0.toByte(),

        // Consumer (Report ID 2)
        0x05.toByte(), 0x0C.toByte(),
        0x09.toByte(), 0x01.toByte(),
        0xa1.toByte(), 0x01.toByte(),
        0x85.toByte(), 0x02.toByte(),
        0x15.toByte(), 0x00.toByte(),
        0x26.toByte(), 0xFF.toByte(), 0x03.toByte(),
        0x19.toByte(), 0x00.toByte(),
        0x2A.toByte(), 0xFF.toByte(), 0x03.toByte(),
        0x75.toByte(), 0x10.toByte(),
        0x95.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x00.toByte(),
        0xc0.toByte(),

        // Mouse (Report ID 3) — 16-bit relative X/Y (Windows uyumu)
        0x05.toByte(), 0x01.toByte(),        // Usage Page (Generic Desktop)
        0x09.toByte(), 0x02.toByte(),        // Usage (Mouse)
        0xa1.toByte(), 0x01.toByte(),        // Collection (Application)
        0x85.toByte(), 0x03.toByte(),        // Report ID (3)
        0x09.toByte(), 0x01.toByte(),        // Usage (Pointer)
        0xa1.toByte(), 0x00.toByte(),        // Collection (Physical)
        0x05.toByte(), 0x09.toByte(),        // Usage Page (Button)
        0x19.toByte(), 0x01.toByte(),
        0x29.toByte(), 0x03.toByte(),
        0x15.toByte(), 0x00.toByte(),
        0x25.toByte(), 0x01.toByte(),
        0x95.toByte(), 0x03.toByte(),
        0x75.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x02.toByte(),        // Buttons
        0x95.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x05.toByte(),
        0x81.toByte(), 0x03.toByte(),        // Padding
        0x05.toByte(), 0x01.toByte(),        // Generic Desktop
        0x09.toByte(), 0x30.toByte(),        // X
        0x09.toByte(), 0x31.toByte(),        // Y
        0x16.toByte(), 0x01.toByte(), 0x80.toByte(), // Logical Min (-32767)
        0x26.toByte(), 0xFF.toByte(), 0x7F.toByte(), // Logical Max (32767)
        0x75.toByte(), 0x10.toByte(),        // Report Size 16
        0x95.toByte(), 0x02.toByte(),        // Report Count 2
        0x81.toByte(), 0x06.toByte(),        // Input Rel
        0x09.toByte(), 0x38.toByte(),        // Wheel
        0x15.toByte(), 0x81.toByte(),        // Logical Min -127
        0x25.toByte(), 0x7F.toByte(),        // Logical Max 127
        0x75.toByte(), 0x08.toByte(),
        0x95.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x06.toByte(),        // Input Rel
        0xc0.toByte(),
        0xc0.toByte()
    )

    fun keyboardReport(keyCode: KeyCode, modifiers: Int = 0): ByteArray =
        keyboardReportUsage(keyCode.usage, modifiers)

    /** Ham HID usage (klavye paneli için). */
    fun keyboardReportUsage(usage: Int, modifiers: Int = 0): ByteArray {
        val report = ByteArray(8)
        report[0] = (modifiers and 0xFF).toByte()
        report[2] = (usage and 0xFF).toByte()
        return report
    }

    fun consumerReport(usage: Int): ByteArray {
        val report = ByteArray(2)
        report[0] = (usage and 0xFF).toByte()
        report[1] = ((usage shr 8) and 0xFF).toByte()
        return report
    }

    fun consumerReport(action: ConsumerAction): ByteArray = when (action) {
        ConsumerAction.VOLUME_UP -> consumerReport(USAGE_VOLUME_UP)
        ConsumerAction.VOLUME_DOWN -> consumerReport(USAGE_VOLUME_DOWN)
        ConsumerAction.PLAY_PAUSE -> consumerReport(USAGE_PLAY_PAUSE)
        ConsumerAction.SCAN_NEXT -> consumerReport(USAGE_SCAN_NEXT)
        ConsumerAction.SCAN_PREVIOUS -> consumerReport(USAGE_SCAN_PREVIOUS)
        ConsumerAction.MUTE -> consumerReport(USAGE_MUTE)
        ConsumerAction.NONE -> consumerReport(0)
    }

    /** 6 byte: buttons, Xlo, Xhi, Ylo, Yhi, wheel */
    fun mouseReport(
        buttons: Int = 0,
        dx: Int = 0,
        dy: Int = 0,
        wheel: Int = 0
    ): ByteArray {
        val x = dx.coerceIn(-32767, 32767)
        val y = dy.coerceIn(-32767, 32767)
        return byteArrayOf(
            (buttons and 0x07).toByte(),
            (x and 0xFF).toByte(),
            ((x shr 8) and 0xFF).toByte(),
            (y and 0xFF).toByte(),
            ((y shr 8) and 0xFF).toByte(),
            wheel.coerceIn(-127, 127).toByte()
        )
    }
}
