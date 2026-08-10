package com.cihan.pccontroller.edition

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.cihan.pccontroller.BuildConfig
import com.cihan.pccontroller.R
import com.cihan.pccontroller.domain.ProStore

/**
 * Free APK — Billing yok; kullanıcıyı Pro uygulamaya yönlendirir.
 */
object EditionSupport {

    fun start(
        @Suppress("UNUSED_PARAMETER") activity: AppCompatActivity,
        @Suppress("UNUSED_PARAMETER") proStore: ProStore,
        @Suppress("UNUSED_PARAMETER") onUnlocked: (Boolean, Boolean) -> Unit
    ) {
        // Free’de satın alma yok
    }

    fun stop() {
    }

    fun showUpsell(
        activity: AppCompatActivity,
        @Suppress("UNUSED_PARAMETER") proStore: ProStore,
        @Suppress("UNUSED_PARAMETER") onUnlocked: (Boolean, Boolean) -> Unit,
        @Suppress("UNUSED_PARAMETER") onDebugUnlock: () -> Unit
    ) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.pro_title)
            .setMessage(R.string.pro_message)
            .setPositiveButton(R.string.pro_buy) { _, _ -> openProApp(activity) }
            .setNegativeButton(R.string.pro_later, null)
            .show()
    }

    private fun openProApp(activity: AppCompatActivity) {
        val pkg = BuildConfig.PRO_PACKAGE
        val launch = activity.packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            activity.startActivity(launch)
            return
        }
        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
            )
        } catch (_: ActivityNotFoundException) {
            try {
                activity.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
                    )
                )
            } catch (_: Exception) {
                Toast.makeText(activity, R.string.pro_store_open_failed, Toast.LENGTH_LONG).show()
            }
        }
    }
}
