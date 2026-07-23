package com.vivofindphone.app.ui.login

import android.os.Bundle
import android.text.InputType
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vivofindphone.app.R
import com.vivofindphone.app.data.AccountManager

/**
 * 登录设置界面 - 保存 vivo 账号密码用于自动填充
 * 注意：账号密码仅保存在本地加密存储中，不会上传到任何第三方服务器
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var etAccount: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbRemember: CheckBox
    private lateinit var cbShowPassword: CheckBox
    private lateinit var btnSave: Button
    private lateinit var btnClear: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 沉浸式状态栏（浅色背景 → 深色文字）
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        supportActionBar?.title = "设置 vivo 账号"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // 设置返回箭头颜色
        val upArrow = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_close_clear_cancel)
        upArrow?.setTint(ContextCompat.getColor(this, R.color.hyper_primary))
        supportActionBar?.setHomeAsUpIndicator(upArrow)

        initViews()
        loadExistingData()
        setupListeners()
    }

    private fun initViews() {
        etAccount = findViewById(R.id.etAccount)
        etPassword = findViewById(R.id.etPassword)
        cbRemember = findViewById(R.id.cbRemember)
        cbShowPassword = findViewById(R.id.cbShowPassword)
        btnSave = findViewById(R.id.btnSave)
        btnClear = findViewById(R.id.btnClear)
    }

    private fun loadExistingData() {
        val savedAccount = AccountManager.getAccount()
        if (savedAccount.isNotEmpty()) {
            etAccount.setText(savedAccount)
        }
        if (AccountManager.isRememberPassword()) {
            val savedPwd = AccountManager.getPassword()
            if (savedPwd.isNotEmpty()) {
                etPassword.setText(savedPwd)
            }
            cbRemember.isChecked = true
        }
    }

    private fun setupListeners() {
        // 显示/隐藏密码
        cbShowPassword.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            // 光标移到末尾
            etPassword.setSelection(etPassword.text.length)
        }

        // 保存
        btnSave.setOnClickListener {
            val account = etAccount.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (account.isEmpty()) {
                Toast.makeText(this, "请输入 vivo 账号（手机号/邮箱）", Toast.LENGTH_SHORT).show()
                etAccount.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
                etPassword.requestFocus()
                return@setOnClickListener
            }

            AccountManager.saveAccount(account, password, cbRemember.isChecked)
            AccountManager.setAutoLogin(true)

            Toast.makeText(this, "✅ 保存成功！下次打开自动填充登录", Toast.LENGTH_LONG).show()
            finish()
        }

        // 清除
        btnClear.setOnClickListener {
            etAccount.setText("")
            etPassword.setText("")
            cbRemember.isChecked = false
            cbShowPassword.isChecked = false
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            AccountManager.clearCredentials()
            Toast.makeText(this, "已清除保存的账号信息", Toast.LENGTH_SHORT).show()
            etAccount.requestFocus()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
