package com.vivofindphone.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 账号信息管理
 * 使用 EncryptedSharedPreferences 安全存储 vivo 账号密码
 */
object AccountManager {

    private const val PREF_FILE = "vivo_find_phone_prefs"
    private const val KEY_ACCOUNT = "vivo_account"
    private const val KEY_PASSWORD = "vivo_password"
    private const val KEY_AUTO_LOGIN = "auto_login"
    private const val KEY_REMEMBER = "remember_password"
    private const val KEY_USE_BIOMETRIC = "use_biometric"

    private lateinit var encryptedPrefs: SharedPreferences

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveAccount(account: String, password: String, remember: Boolean) {
        encryptedPrefs.edit().apply {
            putString(KEY_ACCOUNT, account)
            if (remember) {
                putString(KEY_PASSWORD, password)
            } else {
                putString(KEY_PASSWORD, "")
            }
            putBoolean(KEY_REMEMBER, remember)
            apply()
        }
    }

    fun getAccount(): String = encryptedPrefs.getString(KEY_ACCOUNT, "") ?: ""

    fun getPassword(): String = encryptedPrefs.getString(KEY_PASSWORD, "") ?: ""

    fun isRememberPassword(): Boolean = encryptedPrefs.getBoolean(KEY_REMEMBER, false)

    fun setAutoLogin(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_AUTO_LOGIN, enabled).apply()
    }

    fun isAutoLogin(): Boolean = encryptedPrefs.getBoolean(KEY_AUTO_LOGIN, false)

    fun setBiometricEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_USE_BIOMETRIC, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean = encryptedPrefs.getBoolean(KEY_USE_BIOMETRIC, false)

    fun clearCredentials() {
        encryptedPrefs.edit().apply {
            putString(KEY_ACCOUNT, "")
            putString(KEY_PASSWORD, "")
            putBoolean(KEY_REMEMBER, false)
            putBoolean(KEY_AUTO_LOGIN, false)
            apply()
        }
    }
}
