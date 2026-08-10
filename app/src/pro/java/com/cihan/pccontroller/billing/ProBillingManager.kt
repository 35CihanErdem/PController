package com.cihan.pccontroller.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.cihan.pccontroller.domain.ProStore

/**
 * Yalnızca Pro APK (`com.cihan.pccontroller.pro`).
 * Tek seferlik `pro_unlock` — Play Billing + yerel ProStore.
 */
class ProBillingManager(
    context: Context,
    private val proStore: ProStore,
    private val onProChanged: (unlocked: Boolean, showToast: Boolean) -> Unit
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private var productDetails: ProductDetails? = null
    private var ready = false

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun start() {
        if (billingClient.isReady) {
            ready = true
            queryProductDetails()
            restorePurchases()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                ready = result.responseCode == BillingClient.BillingResponseCode.OK
                Log.d(TAG, "Billing setup: ${result.responseCode} ${result.debugMessage}")
                if (ready) {
                    queryProductDetails()
                    restorePurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                ready = false
                Log.w(TAG, "Billing disconnected")
            }
        })
    }

    fun end() {
        try {
            billingClient.endConnection()
        } catch (_: Exception) {
        }
        ready = false
    }

    fun restorePurchases(onResult: ((RestoreResult) -> Unit)? = null) {
        if (!billingClient.isReady) {
            onResult?.invoke(RestoreResult.Unavailable)
            return
        }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "queryPurchases failed: ${result.debugMessage}")
                onResult?.invoke(RestoreResult.Error)
                return@queryPurchasesAsync
            }
            val owned = purchases.any { it.hasProUnlock() && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            if (owned) {
                purchases.filter { it.hasProUnlock() }.forEach { acknowledgeIfNeeded(it) }
                val already = proStore.isProUnlocked
                proStore.unlockFromPurchase()
                if (!already) onProChanged(true, false)
                onResult?.invoke(RestoreResult.Restored)
            } else {
                onResult?.invoke(RestoreResult.NotFound)
            }
        }
    }

    fun launchPurchase(activity: Activity, onUnavailable: () -> Unit) {
        if (!billingClient.isReady) {
            onUnavailable()
            return
        }
        val details = productDetails
        if (details == null) {
            queryProductDetails {
                val d = productDetails
                if (d == null) onUnavailable()
                else startFlow(activity, d, onUnavailable)
            }
            return
        }
        startFlow(activity, details, onUnavailable)
    }

    private fun startFlow(activity: Activity, details: ProductDetails, onUnavailable: () -> Unit) {
        val priceHint = details.oneTimePurchaseOfferDetails?.formattedPrice
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "launchBillingFlow: ${result.debugMessage}")
            onUnavailable()
        } else {
            Log.d(TAG, "launchBillingFlow ok, price hint=$priceHint")
        }
    }

    private fun queryProductDetails(onDone: (() -> Unit)? = null) {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(ProStore.PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        billingClient.queryProductDetailsAsync(params) { result, detailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = detailsList.firstOrNull()
                Log.d(TAG, "Product details: ${productDetails?.productId}")
            } else {
                Log.w(TAG, "queryProductDetails: ${result.debugMessage}")
            }
            onDone?.invoke()
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.hasProUnlock() &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    ) {
                        acknowledgeIfNeeded(purchase)
                        proStore.unlockFromPurchase()
                        onProChanged(true, true)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                Log.d(TAG, "Purchase canceled")
            else ->
                Log.w(TAG, "Purchase update: ${result.responseCode} ${result.debugMessage}")
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            Log.d(TAG, "acknowledge: ${result.responseCode}")
        }
    }

    private fun Purchase.hasProUnlock(): Boolean =
        products.contains(ProStore.PRODUCT_ID)

    enum class RestoreResult { Restored, NotFound, Unavailable, Error }

    companion object {
        private const val TAG = "ProBilling"
    }
}
