package com.example.util

/**
 * AdMob Configuration for ScanDoc.
 *
 * Current IDs are official Google AdMob Test IDs.
 * Replace with your real AdMob App ID and Ad Unit IDs before publishing to Google Play:
 * 1. App ID in AndroidManifest.xml: <meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" ... />
 * 2. Update the Unit IDs below:
 */
object AdMobConfig {
    // Official Google Test IDs for Android:
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    // Set to false in production after inserting real IDs
    val IS_TEST_MODE = true

    // Active Ad Unit IDs used throughout the app
    val BANNER_AD_UNIT_ID = TEST_BANNER_ID
    val INTERSTITIAL_AD_UNIT_ID = TEST_INTERSTITIAL_ID
    val REWARDED_AD_UNIT_ID = TEST_REWARDED_ID

    // Minimum intervals to protect user experience
    const val MIN_ACTIONS_BETWEEN_INTERSTITIALS = 3
    const val MIN_MILLIS_BETWEEN_INTERSTITIALS = 90_000L // 90 seconds
}
