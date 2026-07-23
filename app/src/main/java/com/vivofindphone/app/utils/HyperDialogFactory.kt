package com.vivofindphone.app.utils

import android.content.Context
import android.graphics.drawable.InsetDrawable
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import com.vivofindphone.app.R

/**
 * 澎湃OS 风格对话框工厂
 * 
 * 统一对话框外观：
 * - 24dp+ 大圆角
 * - 毛玻璃半透明背景
 * - 按钮文字使用主题色
 * - 适当边距
 */
object HyperDialogFactory {

    /**
     * 创建澎湃OS风格的 AlertDialog Builder
     */
    fun create(context: Context): AlertDialog.Builder {
        val builder = AlertDialog.Builder(context, R.style.HyperAlertDialog)

        // 设置全局按钮颜色
        builder.setOnDismissListener { }
        
        return builder
    }

    /**
     * 应用圆角和边距到已创建的对话框
     */
    fun styleDialog(dialog: AlertDialog) {
        dialog.window?.apply {
            // 圆角
            setBackgroundDrawableResource(R.drawable.bg_glass_dialog)

            // 两侧留边距（让对话框不贴屏幕边缘）
            val margin = (24 * dialog.context.resources.displayMetrics.density).toInt()
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            attributes = attributes.apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                // 两侧各留 margin
                horizontalMargin = margin / resources.displayMetrics.widthPixels.toFloat()
            }
        }

        // 按钮文字颜色
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(context.getColor(R.color.hyper_primary))
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                setTextColor(context.getColor(R.color.hyper_text_tertiary))
            }
        }
    }
}
