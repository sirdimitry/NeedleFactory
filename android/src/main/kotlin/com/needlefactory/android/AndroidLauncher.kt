package com.needlefactory.android

import android.os.Build
import android.os.Bundle
import android.content.res.Configuration
import android.view.View
import android.view.WindowInsets
import android.view.HapticFeedbackConstants
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.needlefactory.game.BuildInfo
import com.needlefactory.game.NeedleFactoryGame
import com.needlefactory.game.PlatformFeedback

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            disableAudio = true
        }
        val feedback = PlatformFeedback {
            window.decorView.post {
                window.decorView.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30)
                    HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }
        val seed = if (BuildConfig.DEBUG && intent.hasExtra("seed")) intent.getLongExtra("seed", 1L) else System.nanoTime()
        initialize(NeedleFactoryGame(BuildInfo(BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE, BuildConfig.DEBUG),
            feedback, seed = seed), config)

        // Android 15/16 enforce edge-to-edge. Keep the GL surface inside system safe areas.
        val content = findViewById<View>(android.R.id.content)
        content.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val safe = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        content.requestApplyInsets()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val content = findViewById<View>(android.R.id.content)
        content.post { content.requestApplyInsets() }
    }
}
