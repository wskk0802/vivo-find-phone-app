package com.vivofindphone.app.ui.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vivofindphone.app.R
import com.vivofindphone.app.data.AccountManager
import com.vivofindphone.app.data.Constants
import com.vivofindphone.app.ui.login.LoginActivity
import com.vivofindphone.app.ui.settings.SettingsActivity
import com.vivofindphone.app.utils.HyperDialogFactory

/**
 * 主界面 - WebView 封装 vivo 云服务查找设备
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var fabRefresh: FloatingActionButton
    private lateinit var fabSettings: FloatingActionButton
    private lateinit var toolbar: Toolbar
    private lateinit var btnToolbarRefresh: ImageButton
    private lateinit var btnToolbarMenu: ImageButton

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isPageLoaded = false

    // 记录登录状态，避免重复自动填充
    private var hasAutoFilled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 沉浸式状态栏：透明 + 浅色文字（因为Toolbar是深色渐变）
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = 0 // 深色文字被渐变覆盖，用亮色

        initViews()
        setupToolbar()
        setupWebView()
        setupSwipeRefresh()
        setupFab()

        // 检查是否需要生物识别验证
        if (AccountManager.isBiometricEnabled()) {
            showBiometricPrompt()
        } else {
            loadFindPhonePage()
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        fabRefresh = findViewById(R.id.fabRefresh)
        fabSettings = findViewById(R.id.fabSettings)
        btnToolbarRefresh = findViewById(R.id.btnToolbarRefresh)
        btnToolbarMenu = findViewById(R.id.btnToolbarMenu)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = "亲友位置"
        // 使用白色标题文字，配合渐变Toolbar背景
        toolbar.setTitleTextColor(android.graphics.Color.WHITE)
        toolbar.setSubtitleTextColor(0xCCFFFFFF.toInt())
        toolbar.subtitle = "vivo 云服务"
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings: WebSettings = webView.settings

        // 基础设置
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        // 用户代理
        settings.userAgentString = Constants.USER_AGENT

        // 缩放设置
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        // 安全设置
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.allowFileAccess = false
        settings.allowContentAccess = false

        // Cookie 设置
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // WebViewClient
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                // 所有链接都在 WebView 内打开
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                toolbar.subtitle = "加载中..."
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                isPageLoaded = true
                hasAutoFilled = false

                // 更新标题
                val title = view?.title ?: ""
                if (title.isNotEmpty() && !title.contains("vivo")) {
                    toolbar.subtitle = title
                } else {
                    toolbar.subtitle = "vivo 云服务"
                }

                // 尝试自动填充登录信息
                autoFillLoginInfo(view, url)
            }
        }

        // WebChromeClient - 处理进度条
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    /**
     * 自动填充登录信息
     * 通过 JavaScript 注入方式填写账号密码
     */
    private fun autoFillLoginInfo(view: WebView?, url: String?) {
        if (view == null || url == null) return
        if (hasAutoFilled) return

        val account = AccountManager.getAccount()
        val password = AccountManager.getPassword()

        if (account.isEmpty() || password.isEmpty()) return

        // 判断是否在登录页面
        if (!url.contains(Constants.LOGIN_URL_IDENTIFIER) &&
            !url.contains("passport") &&
            !url.contains("login")) {
            return
        }

        // 延迟执行，等待页面完全渲染
        mainHandler.postDelayed({
            val js = """
                (function() {
                    // 尝试多种选择器定位输入框
                    var inputs = document.querySelectorAll('input');
                    var accountInput = null;
                    var pwdInput = null;
                    
                    for (var i = 0; i < inputs.length; i++) {
                        var input = inputs[i];
                        var type = input.type || '';
                        var name = (input.name || '').toLowerCase();
                        var id = (input.id || '').toLowerCase();
                        var placeholder = (input.placeholder || '').toLowerCase();
                        
                        // 定位账号输入框
                        if (type === 'text' || type === 'tel' || type === 'email' ||
                            name.indexOf('account') >= 0 || name.indexOf('user') >= 0 ||
                            id.indexOf('account') >= 0 || id.indexOf('user') >= 0 ||
                            placeholder.indexOf('账号') >= 0 || placeholder.indexOf('手机') >= 0 ||
                            placeholder.indexOf('邮箱') >= 0) {
                            accountInput = input;
                        }
                        
                        // 定位密码输入框
                        if (type === 'password' ||
                            name.indexOf('pass') >= 0 || name.indexOf('pwd') >= 0 ||
                            id.indexOf('pass') >= 0 || id.indexOf('pwd') >= 0 ||
                            placeholder.indexOf('密码') >= 0) {
                            pwdInput = input;
                        }
                    }
                    
                    // 如果上面没找到，用位置推测
                    if (!accountInput && inputs.length >= 2) {
                        accountInput = inputs[0];
                    }
                    if (!pwdInput && inputs.length >= 2) {
                        pwdInput = inputs[1];
                    }
                    
                    // 填充账号
                    if (accountInput) {
                        accountInput.focus();
                        var setter = Object.getOwnPropertyDescriptor(
                            window.HTMLInputElement.prototype, 'value'
                        ).set;
                        setter.call(accountInput, '${account.replace("'", "\\'")}');
                        accountInput.dispatchEvent(new Event('input', {bubbles: true}));
                        accountInput.dispatchEvent(new Event('change', {bubbles: true}));
                    }
                    
                    // 填充密码
                    if (pwdInput) {
                        pwdInput.focus();
                        var setter2 = Object.getOwnPropertyDescriptor(
                            window.HTMLInputElement.prototype, 'value'
                        ).set;
                        setter2.call(pwdInput, '${password.replace("'", "\\'")}');
                        pwdInput.dispatchEvent(new Event('input', {bubbles: true}));
                        pwdInput.dispatchEvent(new Event('change', {bubbles: true}));
                    }
                    
                    return (accountInput ? 1 : 0) + (pwdInput ? 2 : 0);
                })();
            """.trimIndent()

            view.evaluateJavascript(js) { result ->
                hasAutoFilled = true
                // result: 1=只填了账号, 2=只填了密码, 3=都填了, 0=都没填
                if (result != null && result != "0" && result != "\"0\"") {
                    Toast.makeText(this, "已自动填充登录信息", Toast.LENGTH_SHORT).show()
                }
            }
        }, 1500)
    }

    private fun setupSwipeRefresh() {
        // 澎湃OS 风格：蓝紫渐变刷新指示器
        swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.hyper_primary),
            ContextCompat.getColor(this, R.color.hyper_accent),
            ContextCompat.getColor(this, R.color.hyper_gradient_end)
        )
        swipeRefresh.setProgressBackgroundColorSchemeColor(
            ContextCompat.getColor(this, R.color.white)
        )
        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }
    }

    private fun setupFab() {
        // 刷新按钮（悬浮）
        fabRefresh.setOnClickListener {
            webView.reload()
            Toast.makeText(this, "正在刷新位置...", Toast.LENGTH_SHORT).show()
        }

        // 设置按钮（悬浮）
        fabSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Toolbar 刷新按钮
        btnToolbarRefresh.setOnClickListener {
            webView.reload()
            Toast.makeText(this, "正在刷新位置...", Toast.LENGTH_SHORT).show()
        }

        // Toolbar 更多菜单
        btnToolbarMenu.setOnClickListener { view ->
            showMoreMenu(view)
        }
    }

    /**
     * 显示更多菜单（澎湃OS 风格底部弹窗）
     */
    private fun showMoreMenu(anchor: View) {
        val items = arrayOf(
            getString(R.string.menu_refresh),
            getString(R.string.menu_account),
            getString(R.string.menu_settings),
            getString(R.string.menu_home),
            getString(R.string.menu_clear_cache),
            getString(R.string.menu_about)
        )

        val dialog = HyperDialogFactory.create(this)
            .setTitle(getString(R.string.menu_title))
            .setItems(items) { _, which ->
                when (which) {
                    0 -> webView.reload()
                    1 -> startActivity(Intent(this, LoginActivity::class.java))
                    2 -> startActivity(Intent(this, SettingsActivity::class.java))
                    3 -> webView.loadUrl(Constants.URL_FIND_DEVICE)
                    4 -> {
                        webView.clearCache(true)
                        Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show()
                    }
                    5 -> showAboutDialog()
                }
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .create()

        HyperDialogFactory.styleDialog(dialog)
        dialog.show()
    }

    /**
     * 关于对话框（澎湃OS 风格）
     */
    private fun showAboutDialog() {
        val dialog = HyperDialogFactory.create(this)
            .setTitle(getString(R.string.about_title))
            .setMessage(getString(R.string.about_msg))
            .setPositiveButton(getString(R.string.dialog_confirm), null)
            .create()

        HyperDialogFactory.styleDialog(dialog)
        dialog.show()
    }

    /**
     * 生物识别验证
     */
    private fun showBiometricPrompt() {
        val biometricPrompt = BiometricPrompt(this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    loadFindPhonePage()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@MainActivity, "生物识别验证失败", Toast.LENGTH_SHORT).show()
                    // 验证失败仍允许使用（降级到手动登录）
                    loadFindPhonePage()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // 用户取消或其他错误，仍允许使用
                    loadFindPhonePage()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("验证身份")
            .setSubtitle("验证指纹以查看亲友位置")
            .setNegativeButtonText("取消")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun loadFindPhonePage() {
        // 检查是否已保存账号
        val account = AccountManager.getAccount()
        if (account.isEmpty()) {
            // 第一次使用，跳转到登录设置页
            val dialog = HyperDialogFactory.create(this)
                .setTitle(getString(R.string.welcome_title))
                .setMessage(getString(R.string.welcome_msg))
                .setPositiveButton(getString(R.string.dialog_confirm)) { _, _ ->
                    startActivity(Intent(this, LoginActivity::class.java))
                    webView.loadUrl(Constants.URL_FIND_DEVICE)
                }
                .setNegativeButton(getString(R.string.dialog_cancel)) { _, _ ->
                    webView.loadUrl(Constants.URL_FIND_DEVICE)
                }
                .setCancelable(false)
                .create()

            HyperDialogFactory.styleDialog(dialog)
            dialog.show()
        } else {
            // 已保存账号，直接加载
            webView.loadUrl(Constants.URL_FIND_DEVICE)
        }
    }

    /**
     * 退出确认对话框（澎湃OS 风格）
     */
    private fun showExitConfirmDialog() {
        val dialog = HyperDialogFactory.create(this)
            .setTitle(getString(R.string.dialog_exit_title))
            .setMessage(getString(R.string.dialog_exit_msg))
            .setPositiveButton(getString(R.string.dialog_confirm)) { _, _ ->
                webView.clearCache(true)
                finish()
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .create()

        HyperDialogFactory.styleDialog(dialog)
        dialog.show()
    }

    /**
     * 处理返回键 - 在 WebView 历史中后退
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            // 如果是在登录页或查找设备页，不后退
            val url = webView.url ?: ""
            if (url.contains("find-device") || url.contains("findphone")) {
                showExitConfirmDialog()
                return true
            }
            webView.goBack()
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showExitConfirmDialog()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onDestroy() {
        // 清理 WebView
        webView.apply {
            stopLoading()
            (parent as? android.view.ViewGroup)?.removeView(this)
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }
}
