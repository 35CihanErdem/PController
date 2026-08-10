package com.cihan.pccontroller.domain

import android.content.Context
import com.cihan.pccontroller.BuildConfig

/**
 * Pro erişimi.
 *
 * Free APK (`IS_PRO_APP=false`): Mouse/Klavye yok; Pro uygulamaya yönlendirilir.
 * Pro APK (`IS_PRO_APP=true`): `pro_unlock` satın alma / restore sonrası açılır.
 */
class ProStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Satın alma veya debug unlock ile işaretlendi mi (yalnız Pro APK’da anlamlı). */
    var isProUnlocked: Boolean
        get() = prefs.getBoolean(KEY_UNLOCKED, false)
        private set(value) {
            prefs.edit().putBoolean(KEY_UNLOCKED, value).apply()
        }

    val isPro: Boolean
        get() {
            if (!BuildConfig.IS_PRO_APP) return false
            return isProUnlocked || BuildConfig.DEBUG_FORCE_PRO
        }

    fun unlockFromPurchase() {
        if (!BuildConfig.IS_PRO_APP) return
        isProUnlocked = true
    }

    /** Yalnızca Pro debug test. */
    fun unlockForDebug() {
        if (!BuildConfig.IS_PRO_APP) return
        isProUnlocked = true
    }

    fun lockForDebug() {
        isProUnlocked = false
    }

    companion object {
        private const val PREFS = "pccontroller_pro"
        private const val KEY_UNLOCKED = "pro_unlocked"

        /** Play Console’da Pro uygulaması için one-time product id */
        const val PRODUCT_ID = "pro_unlock"
    }
}
