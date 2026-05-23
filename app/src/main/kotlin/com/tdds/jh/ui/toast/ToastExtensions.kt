package com.tdds.jh.ui.toast

import android.content.Context
import android.widget.Toast

/**
 * Toast扩展函数
 * 提供便捷的Toast调用接口,保持向后兼容性
 */

/**
 * 显示Toast提示 (兼容旧代码)
 * 使用新的Toast系统,但保持相同的函数签名
 * @param context 上下文 (已废弃,保持兼容性)
 * @param message 提示消息
 * @param duration 显示时长 (已废弃,统一使用2秒)
 */
fun showToastWithoutIcon(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
    ToastManager.show(message)
}

/**
 * 显示成功Toast的扩展函数
 * @receiver Context对象
 * @param message 成功消息
 */
fun Context.showSuccessToast(message: String) {
    ToastManager.showSuccess(message)
}

/**
 * 显示警告Toast的扩展函数
 * @receiver Context对象
 * @param message 警告消息
 */
fun Context.showWarningToast(message: String) {
    ToastManager.showWarning(message)
}

/**
 * 显示错误Toast的扩展函数
 * @receiver Context对象
 * @param message 错误消息
 */
fun Context.showErrorToast(message: String) {
    ToastManager.showError(message)
}

/**
 * 显示信息Toast的扩展函数
 * @receiver Context对象
 * @param message 信息消息
 */
fun Context.showInfoToast(message: String) {
    ToastManager.show(message)
}