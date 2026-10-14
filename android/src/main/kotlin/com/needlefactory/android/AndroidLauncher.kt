package com.needlefactory.android

import android.os.Build
import android.os.Bundle
import android.content.res.Configuration
import android.view.View
import android.view.WindowInsets
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.needlefactory.game.BuildInfo
import com.needlefactory.game.NeedleFactoryGame
import com.needlefactory.game.PlatformFeedback
import com.needlefactory.simulation.FactorySession
import com.needlefactory.game.SafeArea
import com.needlefactory.content.FactoryConfig
import com.needlefactory.save.WorkshopRuntime
import java.io.File

class AndroidLauncher : AndroidApplication() {
    private data class Retained(val session: FactorySession, val workshop: WorkshopRuntime?)
    private lateinit var game: NeedleFactoryGame
    private var needleTilt: NeedleTiltSensor? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Color.rgb(16,30,36)))
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = false
            renderUnderCutout = true
            useAccelerometer = false
            useCompass = false
            disableAudio = false
            maxSimultaneousSounds = 16
        }
        val feedback = object : PlatformFeedback {
            override fun found() { window.decorView.post {
                window.decorView.performHapticFeedback(if(Build.VERSION.SDK_INT>=30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
            } }
            private fun pulse(ms:Long,amplitude:Int) { window.decorView.post {
                val vibrator=getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                if(vibrator?.hasVibrator()==true) {
                    if(Build.VERSION.SDK_INT>=26) vibrator.vibrate(android.os.VibrationEffect.createOneShot(ms,amplitude))
                    else { @Suppress("DEPRECATION") vibrator.vibrate(ms) }
                }
            } }
            override fun strawImpact()=pulse(24,65)
            override fun needleLost()=pulse(130,170)
        }

        val seed = if (BuildConfig.DEBUG && intent.hasExtra("seed")) intent.getLongExtra("seed", 1L) else System.nanoTime()
        // Separate QA progress keeps physical automation from buying the owner's first tools.
        // Same rules/store/UI; this selects only a debug-private save file, never grants credits.
        val profileFile=File(filesDir,if(BuildConfig.DEBUG && intent.getBooleanExtra("qaProfile",false))
            "workshop-qa-v1.json" else "workshop-v1.json")
        val retained = lastNonConfigurationInstance as? Retained
        game = NeedleFactoryGame(BuildInfo(BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE, BuildConfig.DEBUG),
            feedback, seed = seed, factorySession = retained?.session ?: FactorySession(
                if(BuildConfig.DEBUG && intent.getBooleanExtra("referenceLine",false)) FactoryConfig.grabProducts() else FactoryConfig.pitProducts(),firstSeed = seed),
            saveFile = profileFile,
            workshopRuntime = retained?.workshop ?: WorkshopRuntime(
                com.needlefactory.save.WorkshopPersistence(com.needlefactory.save.WorkshopStore(profileFile),seed),seed,true,
                !(BuildConfig.DEBUG && intent.getBooleanExtra("referenceLine",false))))
        initialize(game, config)
        needleTilt = NeedleTiltSensor(this, game.needleLighting)
        window.decorView.keepScreenOn = true
        configureWindow()
        // Full-window GL; safe insets move the HUD/camera content, never shrink the SurfaceView.
        val content = findViewById<View>(android.R.id.content)
        content.setBackgroundColor(Color.rgb(16,30,36))
        content.setPadding(0,0,0,0)
        content.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(0,0,0,0)
            val area = if (Build.VERSION.SDK_INT >= 30) {
                val safe = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                SafeArea(safe.left,safe.top,safe.right,safe.bottom)
            } else {
                @Suppress("DEPRECATION")
                SafeArea(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            // Samsung Edge Panel is not reported as a system inset. Keep touches 24dp
            // from either side without disabling system gestures or shrinking the Surface.
            val gestureSafe=area.withSideMargin(kotlin.math.round(24f*resources.displayMetrics.density).toInt())
            postRunnable { game.updateSafeArea(gestureSafe) }
            insets
        }
        content.requestApplyInsets()
    }

    @Suppress("DEPRECATION")
    override fun onRetainNonConfigurationInstance(): Any = Retained(game.factorySession,game.workshopRuntime)

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configureWindow()
        val content = findViewById<View>(android.R.id.content)
        content.post { content.requestApplyInsets() }
    }

    @Suppress("DEPRECATION")
    private fun configureWindow() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            window.insetsController?.setSystemBarsAppearance(0,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_FULLSCREEN
            if (Build.VERSION.SDK_INT >= 28) window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }

    override fun onResume() {
        super.onResume()
        configureWindow()
        window.decorView.requestApplyInsets()
        needleTilt?.start()
    }

    override fun onPause() {
        needleTilt?.stop()
        super.onPause()
    }

    override fun onDestroy() {
        needleTilt?.stop()
        if(!isChangingConfigurations) game.workshopRuntime?.persistence?.close()
        super.onDestroy()
    }
}
