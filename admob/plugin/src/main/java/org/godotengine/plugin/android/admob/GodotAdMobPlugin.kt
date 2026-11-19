package org.godotengine.plugin.android.admob

import android.app.Activity
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot
import java.util.concurrent.atomic.AtomicBoolean

class GodotAdMobPlugin(godot: Godot) : GodotPlugin(godot) {

    companion object {
        private const val TAG = "GodotAdMob"

        // Google's official test ad unit IDs.
        const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

        // Banner positions for GDScript convenience.
        const val BANNER_TOP = 0
        const val BANNER_BOTTOM = 1
    }

    private val isInitialized = AtomicBoolean(false)
    private val isMobileAdsInitCalled = AtomicBoolean(false)

    private var adView: AdView? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    // Godot signals exposed to GDScript.
    private val initializedSignal = SignalInfo("initialized")
    private val adLoadedSignal = SignalInfo("ad_loaded", String::class.java)
    private val adFailedToLoadSignal = SignalInfo("ad_failed_to_load", Integer::class.java)
    private val adShownSignal = SignalInfo("ad_shown", String::class.java)
    private val adClosedSignal = SignalInfo("ad_closed", String::class.java)
    private val rewardedEarnedSignal = SignalInfo("rewarded_earned", String::class.java, Integer::class.java)

    override fun getPluginName(): String = BuildConfig.GODOT_PLUGIN_NAME

    override fun getPluginSignals(): Set<SignalInfo> {
        return setOf(
            initializedSignal,
            adLoadedSignal,
            adFailedToLoadSignal,
            adShownSignal,
            adClosedSignal,
            rewardedEarnedSignal
        )
    }

    override fun onMainDestroy() {
        runOnHostThread {
            destroyBannerInternal()
            interstitialAd = null
            rewardedAd = null
        }
        super.onMainDestroy()
    }

    // ---------------------------------------------------------------------
    // Public API (called from GDScript via the plugin singleton)
    // ---------------------------------------------------------------------

    /**
     * Initialize Google Mobile Ads with optional test device IDs and UMP consent flow.
     *
     * @param testDeviceIdsCsv Comma- or newline-separated hashed test device IDs (optional).
     * @param useConsentFlow If true, runs UMP consent gathering before initializing Mobile Ads.
     */
    @UsedByGodot
    fun initialize(testDeviceIdsCsv: String = "", useConsentFlow: Boolean = true) {
        val activity = getActivity()
        if (activity == null) {
            Log.e(TAG, "initialize: activity is null")
            emitInitialized(false)
            return
        }

        val testDeviceIds = parseTestDeviceIds(testDeviceIdsCsv)

        runOnHostThread {
            try {
                // Configure test devices if provided.
                if (testDeviceIds.isNotEmpty()) {
                    val config = RequestConfiguration.Builder()
                        .setTestDeviceIds(testDeviceIds)
                        .build()
                    MobileAds.setRequestConfiguration(config)
                }

                if (useConsentFlow) {
                    requestConsentAndInitialize(activity)
                } else {
                    initializeMobileAds(activity)
                }
            } catch (e: Exception) {
                Log.e(TAG, "initialize failed", e)
                emitInitialized(false)
            }
        }
    }

    @UsedByGodot
    fun loadBanner(adUnitId: String = TEST_BANNER_AD_UNIT_ID, position: Int = BANNER_BOTTOM) {
        val activity = getActivity() ?: run {
            Log.e(TAG, "loadBanner: activity is null")
            emitSignal(adFailedToLoadSignal.name, -1)
            return
        }

        runOnHostThread {
            try {
                destroyBannerInternal()

                val view = AdView(activity)
                view.adUnitId = adUnitId
                view.setAdSize(AdSize.BANNER)
                view.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d(TAG, "Banner loaded")
                        emitSignal(adLoadedSignal.name, "banner")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Banner failed to load: ${error.code} ${error.message}")
                        emitSignal(adFailedToLoadSignal.name, error.code)
                    }

                    override fun onAdOpened() {
                        emitSignal(adShownSignal.name, "banner")
                    }

                    override fun onAdClosed() {
                        emitSignal(adClosedSignal.name, "banner")
                    }
                }

                val layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.gravity = if (position == BANNER_TOP) {
                    Gravity.TOP or Gravity.CENTER_HORIZONTAL
                } else {
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                }

                val container = getContentRoot(activity)
                container.addView(view, layoutParams)
                adView = view
                view.loadAd(AdRequest.Builder().build())
            } catch (e: Exception) {
                Log.e(TAG, "loadBanner failed", e)
                emitSignal(adFailedToLoadSignal.name, -1)
            }
        }
    }

    @UsedByGodot
    fun showBanner() {
        runOnHostThread {
            adView?.visibility = View.VISIBLE
        }
    }

    @UsedByGodot
    fun hideBanner() {
        runOnHostThread {
            adView?.visibility = View.GONE
        }
    }

    @UsedByGodot
    fun destroyBanner() {
        runOnHostThread {
            destroyBannerInternal()
        }
    }

    @UsedByGodot
    fun loadInterstitial(adUnitId: String = TEST_INTERSTITIAL_AD_UNIT_ID) {
        val activity = getActivity() ?: run {
            Log.e(TAG, "loadInterstitial: activity is null")
            emitSignal(adFailedToLoadSignal.name, -1)
            return
        }

        runOnHostThread {
            try {
                val request = AdRequest.Builder().build()
                InterstitialAd.load(
                    activity,
                    adUnitId,
                    request,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            interstitialAd = ad
                            attachInterstitialCallbacks(ad)
                            Log.d(TAG, "Interstitial loaded")
                            emitSignal(adLoadedSignal.name, "interstitial")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            interstitialAd = null
                            Log.w(TAG, "Interstitial failed to load: ${error.code} ${error.message}")
                            emitSignal(adFailedToLoadSignal.name, error.code)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "loadInterstitial failed", e)
                emitSignal(adFailedToLoadSignal.name, -1)
            }
        }
    }

    @UsedByGodot
    fun showInterstitial() {
        val activity = getActivity() ?: run {
            Log.e(TAG, "showInterstitial: activity is null")
            return
        }
        runOnHostThread {
            val ad = interstitialAd
            if (ad != null) {
                ad.show(activity)
            } else {
                Log.w(TAG, "showInterstitial: ad not ready")
            }
        }
    }

    @UsedByGodot
    fun loadRewarded(adUnitId: String = TEST_REWARDED_AD_UNIT_ID) {
        val activity = getActivity() ?: run {
            Log.e(TAG, "loadRewarded: activity is null")
            emitSignal(adFailedToLoadSignal.name, -1)
            return
        }

        runOnHostThread {
            try {
                val request = AdRequest.Builder().build()
                RewardedAd.load(
                    activity,
                    adUnitId,
                    request,
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            rewardedAd = ad
                            attachRewardedCallbacks(ad)
                            Log.d(TAG, "Rewarded loaded")
                            emitSignal(adLoadedSignal.name, "rewarded")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            rewardedAd = null
                            Log.w(TAG, "Rewarded failed to load: ${error.code} ${error.message}")
                            emitSignal(adFailedToLoadSignal.name, error.code)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "loadRewarded failed", e)
                emitSignal(adFailedToLoadSignal.name, -1)
            }
        }
    }

    @UsedByGodot
    fun showRewarded() {
        val activity = getActivity() ?: run {
            Log.e(TAG, "showRewarded: activity is null")
            return
        }
        runOnHostThread {
            val ad = rewardedAd
            if (ad != null) {
                ad.show(activity) { rewardItem: RewardItem ->
                    emitSignal(rewardedEarnedSignal.name, rewardItem.type, rewardItem.amount)
                }
            } else {
                Log.w(TAG, "showRewarded: ad not ready")
            }
        }
    }

    @UsedByGodot
    fun isInitialized(): Boolean = isInitialized.get()

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    private fun parseTestDeviceIds(csv: String): List<String> {
        if (csv.isBlank()) return emptyList()
        return csv.split(',', '\n', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun requestConsentAndInitialize(activity: Activity) {
        try {
            val params = ConsentRequestParameters.Builder().build()
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    // Consent info updated successfully.
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                        // Consent gathering complete (form may or may not have been shown).
                        initializeMobileAdsIfAllowed(activity, consentInformation)
                    }
                },
                { requestConsentError ->
                    Log.w(TAG, "Consent info update failed: ${requestConsentError.message}")
                    // Proceed based on cached/previous consent if possible.
                    initializeMobileAdsIfAllowed(activity, consentInformation)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Consent flow failed, proceeding to MobileAds init", e)
            initializeMobileAds(activity)
        }
    }

    private fun initializeMobileAdsIfAllowed(activity: Activity, consentInformation: ConsentInformation) {
        if (consentInformation.canRequestAds()) {
            initializeMobileAds(activity)
        } else {
            // Consent not yet sufficient to request ads; still initialize once allowed in future
            // sessions. We emit initialized=false so the game can react/retry as needed.
            Log.w(TAG, "Consent not sufficient to request ads")
            emitInitialized(false)
        }
    }

    private fun initializeMobileAds(activity: Activity) {
        if (isMobileAdsInitCalled.getAndSet(true)) {
            // Already initializing/initialized; just report current state.
            emitInitialized(isInitialized.get())
            return
        }

        // MobileAds.initialize should be called on the main thread; this runs on host/UI thread.
        MobileAds.initialize(activity) { _ ->
            isInitialized.set(true)
            Log.i(TAG, "MobileAds initialized")
            emitInitialized(true)
        }
    }

    private fun emitInitialized(success: Boolean) {
        emitSignal(initializedSignal.name)
        // Also log success for clarity; the signal itself has no payload per spec.
        if (!success) {
            Log.w(TAG, "AdMob not fully initialized (consent or init issue)")
        }
    }

    private fun attachInterstitialCallbacks(ad: InterstitialAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                emitSignal(adShownSignal.name, "interstitial")
            }

            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                emitSignal(adClosedSignal.name, "interstitial")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                Log.w(TAG, "Interstitial failed to show: ${adError.code} ${adError.message}")
                emitSignal(adFailedToLoadSignal.name, adError.code)
            }
        }
    }

    private fun attachRewardedCallbacks(ad: RewardedAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                emitSignal(adShownSignal.name, "rewarded")
            }

            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                emitSignal(adClosedSignal.name, "rewarded")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                Log.w(TAG, "Rewarded failed to show: ${adError.code} ${adError.message}")
                emitSignal(adFailedToLoadSignal.name, adError.code)
            }
        }
    }

    private fun destroyBannerInternal() {
        val view = adView ?: return
        try {
            view.adListener = object : AdListener() {}
            val parent = view.parent
            if (parent is ViewGroup) {
                parent.removeView(view)
            }
            view.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "destroyBannerInternal error", e)
        } finally {
            adView = null
        }
    }

    private fun getContentRoot(activity: Activity): ViewGroup {
        return activity.findViewById(android.R.id.content)
            ?: (activity.window.decorView as ViewGroup)
    }
}
