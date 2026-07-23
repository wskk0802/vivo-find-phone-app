package com.vivofindphone.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.vivofindphone.app.R
import com.vivofindphone.app.data.AccountManager
import com.vivofindphone.app.ui.login.LoginActivity
import com.vivofindphone.app.utils.HyperDialogFactory

/**
 * 设置界面 - 澎湃OS 生命感美学风格
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var swBiometric: Switch
    private lateinit var btnEditAccount: Button
    private lateinit var btnClearAll: Button
    private lateinit var btnAbout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 沉浸式状态栏
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        supportActionBar?.title = getString(R.string.title_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val upArrow = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_close_clear_cancel)
        upArrow?.setTint(ContextCompat.getColor(this, R.color.hyper_primary))
        supportActionBar?.setHomeAsUpIndicator(upArrow)

        initViews()
        loadSettings()
        setupListeners()
    }

    private fun initViews() {
        swBiometric = findViewById(R.id.swBiometric)
        btnEditAccount = findViewById(R.id.btnEditAccount)
        btnClearAll = findViewById(R.id.btnClearAll)
        btnAbout = findViewById(R.id.btnAbout)
    }

    private fun loadSettings() {
        swBiometric.isChecked = AccountManager.isBiometricEnabled()
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            swBiometric.isEnabled = false
            swBiometric.text = getString(R.string.biometric_not_supported)
        }
    }

    private fun setupListeners() {
        // 生物识别开关
        swBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                authenticateToEnableBiometric()
            } else {
                AccountManager.setBiometricEnabled(false)
                Toast.makeText(this, getString(R.string.biometric_disabled), Toast.LENGTH_SHORT).show()
            }
        }

        // 编辑账号
        btnEditAccount.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // 清除所有数据
        btnClearAll.setOnClickListener {
            val dialog = HyperDialogFactory.create(this)
                .setTitle(getString(R.string.dialog_clear_title))
                .setMessage(getString(R.string.dialog_clear_msg))
                .setPositiveButton(getString(R.string.dialog_confirm)) { _, _ ->
                    AccountManager.clearCredentials()
                    val webView = android.webkit.WebView(this)
                    webView.clearCache(true)
                    webView.clearHistory()
                    webView.destroy()
                    Toast.makeText(this, "已清除所有数据", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .create()

            HyperDialogFactory.styleDialog(dialog)
            dialog.show()
        }

        // 关于
        btnAbout.setOnClickListener {
            val dialog = HyperDialogFactory.create(this)
                .setTitle(getString(R.string.about_title))
                .setMessage(getString(R.string.about_msg))
                .setPositiveButton(getString(R.string.dialog_confirm), null)
                .create()

            HyperDialogFactory.styleDialog(dialog)
            dialog.show()
        }
    }

    /**
     * 验证指纹后开启生物识别
     */
    private fun authenticateToEnableBiometric() {
        val biometricPrompt = BiometricPrompt(this,
            androidx.core.content.ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    AccountManager.setBiometricEnabled(true)
                    Toast.makeText(this@SettingsActivity, getString(R.string.biometric_enabled), Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    swBiometric.isChecked = false
                    Toast.makeText(this@SettingsActivity, getString(R.string.biometric_failed), Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    swBiometric.isChecked = false
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle_enable))
            .setNegativeButtonText(getString(R.string.biometric_negative))
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
