package com.tdds.jh.ui.toast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Toast消息数据类
 * 包含消息内容和显示配置
 */
data class ToastMessage(
    val message: String,
    val duration: Long = 2000L,
    val type: ToastType = ToastType.INFO
)

/**
 * Toast类型枚举
 * 定义不同类型的Toast样式
 */
enum class ToastType {
    INFO,    // 信息提示
    SUCCESS, // 成功提示
    WARNING, // 警告提示
    ERROR    // 错误提示
}

/**
 * Toast管理器 - 全局单例
 * 负责管理Toast消息的显示和生命周期
 * 使用状态管理实现Compose环境下的Toast显示
 */
@Stable
object ToastManager {
    
    /** 当前显示的Toast消息 */
    private var _currentToast by mutableStateOf<ToastMessage?>(null)
    val currentToast: ToastMessage? get() = _currentToast
    
    /** 是否正在显示Toast */
    val isShowing: Boolean get() = _currentToast != null
    
    /** 当前显示任务 */
    private var currentJob: Job? = null
    
    /** 协程作用域 */
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    /**
     * 显示Toast消息
     * @param message 消息内容
     * @param duration 显示时长(毫秒)
     * @param type Toast类型
     */
    fun show(
        message: String,
        duration: Long = 2000L,
        type: ToastType = ToastType.INFO
    ) {
        // 取消之前的显示任务
        currentJob?.cancel()
        
        // 设置新的Toast消息
        _currentToast = ToastMessage(message, duration, type)
        
        // 启动自动隐藏任务
        currentJob = coroutineScope.launch {
            delay(duration)
            _currentToast = null
        }
    }
    
    /**
     * 显示成功Toast
     * @param message 成功消息
     */
    fun showSuccess(message: String) {
        show(message, type = ToastType.SUCCESS)
    }
    
    /**
     * 显示警告Toast
     * @param message 警告消息
     */
    fun showWarning(message: String) {
        show(message, type = ToastType.WARNING)
    }
    
    /**
     * 显示错误Toast
     * @param message 错误消息
     */
    fun showError(message: String) {
        show(message, type = ToastType.ERROR)
    }
    
    /**
     * 手动隐藏Toast
     */
    fun hide() {
        currentJob?.cancel()
        _currentToast = null
    }
    
    /**
     * 清除所有Toast状态
     */
    fun clear() {
        hide()
    }
}