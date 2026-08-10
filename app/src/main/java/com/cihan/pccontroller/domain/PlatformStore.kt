package com.cihan.pccontroller.domain

import android.content.Context

/** Seçili platform — UI ve bildirim aynı kaynağı okur. */
class PlatformStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var lastPlatformId: PlatformId
        get() = PlatformCatalog.byIdOrGeneral(prefs.getString(KEY_LAST, null)).id
        set(value) {
            prefs.edit().putString(KEY_LAST, value.name).apply()
        }

    fun profile(): PlatformProfile = PlatformCatalog.byId(lastPlatformId)

    companion object {
        private const val PREFS = "pccontroller_platform"
        private const val KEY_LAST = "last_platform_id"
    }
}
