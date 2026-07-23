package com.vivofindphone.app

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import com.vivofindphone.app.data.AccountManager

/**
 * 应用入口
 * 
 * 初始化全局配置：
 * - 账号加密存储
 * - 适配澎湃OS风格沉浸式状态栏
 */
class FindPhoneApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 初始化账号管理器（EncryptedSharedPreferences）
        AccountManager.init(this)
    }
}
