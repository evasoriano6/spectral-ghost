
package com.spectral.ghost.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SubscriptionManager(private val context: Context) {

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    private val _premiumStatus = MutableStateFlow(false)
    val premiumStatus = _premiumStatus.asStateFlow()

    // PRODUCT IDs
    companion object {
        const val SUB_MONTHLY = "investigator_mensual" // 2.99 EUR
        const val SUB_ANNUAL = "especialista_anual"   // 14.99 EUR (Discounted)
        const val CONSUMABLE_PASS = "daily_pass"      // 0.99 EUR
    }

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Retry logic should be here
            }
        })
    }

    fun launchBillingFlow(activity: Activity, skuId: String, isSubscription: Boolean = true) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(skuId)
                .setProductType(if (isSubscription) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val offerToken = productDetails.subscriptionOfferDetails?.get(0)?.offerToken ?: ""

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                        )
                    )
                    .build()
                
                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
        }
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Validate signature on backend in real app
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                     if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                         _premiumStatus.value = true
                     }
                }
            } else {
                _premiumStatus.value = true
            }
        }
    }

    private fun queryPurchases() {
        // Check active subscriptions
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _premiumStatus.value = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            }
        }
    }
}
