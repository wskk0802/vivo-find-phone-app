package com.vivofindphone.app.data

/**
 * 全局常量
 */
object Constants {

    /** vivo 云服务首页 */
    const val URL_CLOUD_HOME = "https://yun.vivo.com.cn/"

    /** vivo 查找设备直达页面 */
    const val URL_FIND_DEVICE = "https://yun.vivo.com.cn/find-device"

    /** vivo 查找设备独立域名 */
    const val URL_FIND_PHONE = "https://findphone.vivo.com.cn/"

    /** 登录页面 URL 标识（用于 WebView 判断当前页面） */
    const val LOGIN_URL_IDENTIFIER = "login"

    /** 用户代理字符串（模拟手机浏览器） */
    const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; M2007J3SG) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36 " +
            "VivoFindPhoneApp/1.0"
}
