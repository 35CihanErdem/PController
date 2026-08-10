package com.cihan.pccontroller.edition

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.cihan.pccontroller.BuildConfig
import com.cihan.pccontroller.R
import com.cihan.pccontroller.billing.ProBillingManager
import com.cihan.pccontroller.domain.ProStore

/**
 * Pro APK — Google Play Billing + pro_unlock + geri yükle.
 */
object EditionSupport {

    private var billing: ProBillingManager? = null

    fun start(activity: AppCompatActivity, proStore: ProStore, onUnlocked: (Boolean, Boolean) -> Unit) {
        if (billing != null) return
        billing = ProBillingManager(activity, proStore, onUnlocked).also { it.start() }
    }

    fun stop() {
        billing?.end()
        billing = null
    }

    fun showUpsell(
        activity: AppCompatActivity,
        @Suppress("UNUSED_PARAMETER") proStore: ProStore,
        onUnlocked: (Boolean, Boolean) -> Unit,
        onDebugUnlock: () -> Unit
    ) {
        val items = mutableListOf(
            activity.getString(R.string.pro_buy),
            activity.getString(R.string.pro_restore)
        )
        if (BuildConfig.DEBUG) {
            items.add(activity.getString(R.string.pro_unlock_soon))
        }
        AlertDialog.Builder(activity)
            .setTitle(R.string.pro_title)
            .setMessage(R.string.pro_message)
            .setItems(items.toTypedArray()) { _, which ->
                when (which) {
                    0 -> launchPurchase(activity)
                    1 -> restore(activity, onUnlocked)
                    2 -> if (BuildConfig.DEBUG) onDebugUnlock()
                }
            }
            .setNegativeButton(R.string.pro_later, null)
            .show()
    }

    private fun launchPurchase(activity: AppCompatActivity) {
        val client = billing
        if (client == null) {
            Toast.makeText(activity, R.string.pro_billing_unavailable, Toast.LENGTH_LONG).show()
            return
        }
        client.launchPurchase(activity) {
            Toast.makeText(activity, R.string.pro_billing_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun restore(activity: AppCompatActivity, onUnlocked: (Boolean, Boolean) -> Unit) {
        val client = billing
        if (client == null) {
            Toast.makeText(activity, R.string.pro_billing_unavailable, Toast.LENGTH_LONG).show()
            return
        }
        client.restorePurchases { result ->
            activity.runOnUiThread {
                when (result) {
                    ProBillingManager.RestoreResult.Restored -> {
                        onUnlocked(true, false)
                        Toast.makeText(activity, R.string.pro_restore_ok, Toast.LENGTH_SHORT).show()
                    }
                    ProBillingManager.RestoreResult.NotFound ->
                        Toast.makeText(activity, R.string.pro_restore_none, Toast.LENGTH_LONG).show()
                    ProBillingManager.RestoreResult.Unavailable,
                    ProBillingManager.RestoreResult.Error ->
                        Toast.makeText(activity, R.string.pro_billing_unavailable, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
