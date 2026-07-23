package com.vivofindphone.app.ui.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vivofindphone.app.R
import com.vivofindphone.app.ui.main.MainActivity

/**
 * 启动闪屏页 - 澎湃OS 生命感美学风格
 * 
 * 特征：
 * - 柔和渐变背景
 * - 图标淡入 + 缩放弹性动画
 * - 标题渐入
 * - 自动跳转主界面
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var iconView: ImageView
    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 全屏沉浸式
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        setContentView(R.layout.activity_splash)

        iconView = findViewById(R.id.splashIcon)
        titleView = findViewById(R.id.splashTitle)
        subtitleView = findViewById(R.id.splashSubtitle)

        // 初始状态：透明 + 缩小
        iconView.alpha = 0f
        iconView.scaleX = 0.6f
        iconView.scaleY = 0.6f
        titleView.alpha = 0f
        titleView.translationY = 30f
        subtitleView.alpha = 0f
        subtitleView.translationY = 20f

        // 启动动画序列
        playEntranceAnimation()

        // 2秒后跳转
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            // 淡出动画
            playExitAnimation()
        }, 2000)
    }

    /**
     * 入场动画 - 弹性缩放 + 淡入
     */
    private fun playEntranceAnimation() {
        // 图标：弹性缩放 + 淡入
        val scaleX = ObjectAnimator.ofFloat(iconView, "scaleX", 0.6f, 1.15f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(iconView, "scaleY", 0.6f, 1.15f, 1.0f)
        val alpha = ObjectAnimator.ofFloat(iconView, "alpha", 0f, 1f)

        scaleX.duration = 800
        scaleY.duration = 800
        alpha.duration = 600

        // 使用弹性插值器
        val overshoot = android.view.animation.OvershootInterpolator(1.5f)
        scaleX.interpolator = overshoot
        scaleY.interpolator = overshoot

        scaleX.start()
        scaleY.start()
        alpha.start()

        // 标题：延迟淡入 + 上移
        titleView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 副标题：更晚淡入
        subtitleView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(700)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    /**
     * 退出动画 - 整体淡出
     */
    private fun playExitAnimation() {
        val rootView = findViewById<View>(R.id.splashRoot)
        rootView.animate()
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    finish()
                    // 禁用默认转场动画，更流畅
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            })
            .start()
    }

    // 防止用户点击返回键跳过
    override fun onBackPressed() {
        // 启动页不允许返回
    }
}
