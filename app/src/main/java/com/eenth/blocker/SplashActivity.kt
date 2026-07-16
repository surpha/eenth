package com.eenth.blocker

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val ivLock = findViewById<ImageView>(R.id.ivLock)
        val tvBrand = findViewById<TextView>(R.id.tvSplashBrand)

        // Start invisible, fade in
        ivLock.alpha = 0f
        tvBrand.alpha = 0f

        // Phase 1: Fade in with "BLOCK" + open lock (0-600ms)
        val fadeInLock = ObjectAnimator.ofFloat(ivLock, View.ALPHA, 0f, 1f).apply { duration = 500 }
        val fadeInText = ObjectAnimator.ofFloat(tvBrand, View.ALPHA, 0f, 1f).apply { duration = 500 }
        val scaleUpX = ObjectAnimator.ofFloat(ivLock, View.SCALE_X, 0.6f, 1f).apply {
            duration = 500
            interpolator = OvershootInterpolator(2f)
        }
        val scaleUpY = ObjectAnimator.ofFloat(ivLock, View.SCALE_Y, 0.6f, 1f).apply {
            duration = 500
            interpolator = OvershootInterpolator(2f)
        }

        AnimatorSet().apply {
            playTogether(fadeInLock, fadeInText, scaleUpX, scaleUpY)
            start()
        }

        // Phase 2: After 800ms - lock closes + text changes to BLOCKIN
        Handler(Looper.getMainLooper()).postDelayed({
            // Swap to closed lock with a satisfying bounce
            ivLock.setImageResource(R.drawable.ic_lock_splash)

            val bounceX = ObjectAnimator.ofFloat(ivLock, View.SCALE_X, 1f, 1.2f, 0.9f, 1f).apply { duration = 400 }
            val bounceY = ObjectAnimator.ofFloat(ivLock, View.SCALE_Y, 1f, 1.2f, 0.9f, 1f).apply { duration = 400 }

            AnimatorSet().apply {
                playTogether(bounceX, bounceY)
                start()
            }

            // Text transition: BLOCK → BLOCKIN
            tvBrand.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction {
                    tvBrand.text = "BLOCKIN"
                    tvBrand.setTextColor(0xFFFF453A.toInt())
                    tvBrand.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }, 900)

        // Phase 3: Navigate to main after full animation
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2000)
    }
}
