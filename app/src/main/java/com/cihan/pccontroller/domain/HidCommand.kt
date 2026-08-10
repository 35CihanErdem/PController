package com.cihan.pccontroller.domain

import com.cihan.pccontroller.bluetooth.ConsumerAction
import com.cihan.pccontroller.bluetooth.KeyCode

/** HID katmanına gönderilecek düşük seviye komut. Platform bilgisi taşımaz. */
sealed class HidCommand {
    data class Key(
        val key: KeyCode,
        val modifiers: Int = 0
    ) : HidCommand()

    data class Consumer(
        val action: ConsumerAction
    ) : HidCommand()
}
