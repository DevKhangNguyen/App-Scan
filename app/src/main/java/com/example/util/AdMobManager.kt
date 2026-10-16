package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdMobManager {

    private const val TAG = "AdMobManager"

    private var isInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private var actionCountSinceLastAd = 0
    private var lastInterstitialShowTime = 0L

    private var rewardedAd: RewardedAd? = null
    private var isLoadingRewarded = false

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            MobileAds.initialize(context) { initializationStatus ->
                isInitialized = true
                Log.d(TAG, "AdMob Initialized: $initializationStatus")
                preloadInterstitial(context)
                preloadRewarded(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "AdMob initialization failed (offline or missing gms)", e)
        }
    }

    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null || isLoadingInterstitial) return
        isLoadingInterstitial = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            AdMobConfig.INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    Log.d(TAG, "Interstitial loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                    Log.d(TAG, "Interstitial failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Checks frequency capping and presents Interstitial Ad if eligible.
     * Always calls [onDismiss] whether ad showed or was skipped/failed/offline,
     * ensuring uninterrupted workflow!
     */
    fun showInterstitialIfAllowed(
        activity: Activity,
        forceShow: Boolean = false,
        onDismiss: () -> Unit
    ) {
        actionCountSinceLastAd++
        val currentTime = System.currentTimeMillis()
        val timeElapsed = currentTime - lastInterstitialShowTime

        val canShow = forceShow || (
            actionCountSinceLastAd >= AdMobConfig.MIN_ACTIONS_BETWEEN_INTERSTITIALS &&
                timeElapsed >= AdMobConfig.MIN_MILLIS_BETWEEN_INTERSTITIALS
            )

        val currentAd = interstitialAd
        if (canShow && currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    actionCountSinceLastAd = 0
                    lastInterstitialShowTime = System.currentTimeMillis()
                    preloadInterstitial(activity)
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onDismiss()
                }

                override fun onAdShowedFullScreenContent() {
                    interstitialAd = null
                }
            }
            try {
                currentAd.show(activity)
            } catch (e: Exception) {
                Log.e(TAG, "Error showing interstitial", e)
                onDismiss()
            }
        } else {
            // Not ready, offline, or frequency capped - continue seamlessly
            onDismiss()
        }
    }

    fun preloadRewarded(context: Context) {
        if (rewardedAd != null || isLoadingRewarded) return
        isLoadingRewarded = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            AdMobConfig.REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                    Log.d(TAG, "Rewarded ad loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isLoadingRewarded = false
                    Log.d(TAG, "Rewarded ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onClosed: () -> Unit
    ) {
        val currentAd = rewardedAd
        if (currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    preloadRewarded(activity)
                    onClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    preloadRewarded(activity)
                    onClosed()
                }
            }
            try {
                currentAd.show(activity) {
                    onUserEarnedReward()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing rewarded ad", e)
                onClosed()
            }
        } else {
            // Not ready or offline
            onClosed()
        }
    }
}

/**
 * Composable Banner Ad component.
 * Displays banner cleanly at the bottom without blocking interactive UI.
 */
@Composable
fun AdBannerView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdMobConfig.BANNER_AD_UNIT_ID
                    try {
                        loadAd(AdRequest.Builder().build())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    }
}
