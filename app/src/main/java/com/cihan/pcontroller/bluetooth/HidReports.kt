package com.cihan.pcontroller.bluetooth

/**
 * HID report descriptor and report builders.
 * Report ID 1 = keyboard, Report ID 2 = consumer control (16-bit usage).
 */
object HidReports {

    const val REPORT_ID_KEYBOARD: Int = 1
    const val REPORT_ID_CONSUMER: Int = 2

    /** Volume Increment */
    const val USAGE_VOLUME_UP = 0x00E9

    /** Volume Decrement */
    const val USAGE_VOLUME_DOWN = 0x00EA

    /** Play/Pause */
    const val USAGE_PLAY_PAUSE = 0x00CD

    fun descriptor(): ByteArray = byteArrayOf(
        // Keyboard Collection (Report ID 1)
        0x05.toByte(), 0x01.toByte(),        // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(),        // Usage (Keyboard)
        0xa1.toByte(), 0x01.toByte(),        // Collection (Application)
        0x85.toByte(), 0x01.toByte(),        // Report ID (1)
        0x75.toByte(), 0x01.toByte(),        // Report Size (1)
        0x95.toByte(), 0x08.toByte(),        // Report Count (8)
        0x05.toByte(), 0x07.toByte(),        // Usage Page (Key Codes)
        0x19.toByte(), 0xe0.toByte(),        // Usage Minimum (224)
        0x29.toByte(), 0xe7.toByte(),        // Usage Maximum (231)
        0x15.toByte(), 0x00.toByte(),        // Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),        // Logical Maximum (1)
        0x81.toByte(), 0x02.toByte(),        // Input (Data, Var, Abs) - modifiers
        0x95.toByte(), 0x01.toByte(),        // Report Count (1)
        0x75.toByte(), 0x08.toByte(),        // Report Size (8)
        0x81.toByte(), 0x03.toByte(),        // Input (Const) - reserved
        0x95.toByte(), 0x05.toByte(),        // Report Count (5)
        0x75.toByte(), 0x01.toByte(),        // Report Size (1)
        0x05.toByte(), 0x08.toByte(),        // Usage Page (LEDs)
        0x19.toByte(), 0x01.toByte(),        // Usage Minimum (1)
        0x29.toByte(), 0x05.toByte(),        // Usage Maximum (5)
        0x91.toByte(), 0x02.toByte(),        // Output (Data, Var, Abs)
        0x95.toByte(), 0x01.toByte(),        // Report Count (1)
        0x75.toByte(), 0x03.toByte(),        // Report Size (3)
        0x91.toByte(), 0x03.toByte(),        // Output (Const) - LED padding
        0x95.toByte(), 0x06.toByte(),        // Report Count (6)
        0x75.toByte(), 0x08.toByte(),        // Report Size (8)
        0x15.toByte(), 0x00.toByte(),        // Logical Minimum (0)
        0x25.toByte(), 0xE7.toByte(),        // Logical Maximum (231)
        0x05.toByte(), 0x07.toByte(),        // Usage Page (Key Codes)
        0x19.toByte(), 0x00.toByte(),        // Usage Minimum (0)
        0x29.toByte(), 0xE7.toByte(),        // Usage Maximum (231)
        0x81.toByte(), 0x00.toByte(),        // Input (Data, Array)
        0xc0.toByte(),                       // End Collection

        // Consumer Control (Report ID 2) — 16-bit usage array for media keys
        0x05.toByte(), 0x0C.toByte(),        // Usage Page (Consumer)
        0x09.toByte(), 0x01.toByte(),        // Usage (Consumer Control)
        0xa1.toByte(), 0x01.toByte(),        // Collection (Application)
        0x85.toByte(), 0x02.toByte(),        // Report ID (2)
        0x15.toByte(), 0x00.toByte(),        // Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x03.toByte(), // Logical Maximum (1023)
        0x19.toByte(), 0x00.toByte(),        // Usage Minimum (0)
        0x2A.toByte(), 0xFF.toByte(), 0x03.toByte(), // Usage Maximum (1023)
        0x75.toByte(), 0x10.toByte(),        // Report Size (16)
        0x95.toByte(), 0x01.toByte(),        // Report Count (1)
        0x81.toByte(), 0x00.toByte(),        // Input (Data, Array)
        0xc0.toByte()                        // End Collection
    )

    fun keyboardReport(keyCode: KeyCode): ByteArray {
        val report = ByteArray(8)
        report[2] = when (keyCode) {
            KeyCode.UP_ARROW -> 0x52.toByte()
            KeyCode.DOWN_ARROW -> 0x51.toByte()
            KeyCode.LEFT_ARROW -> 0x50.toByte()
            KeyCode.RIGHT_ARROW -> 0x4F.toByte()
            KeyCode.SPACE -> 0x2C.toByte()
            KeyCode.ESC -> 0x29.toByte()
            KeyCode.F -> 0x09.toByte()
            KeyCode.NONE -> 0x00
        }
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
        ConsumerAction.NONE -> consumerReport(0)
    }
}
