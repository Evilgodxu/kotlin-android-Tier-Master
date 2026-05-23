package com.tdds.jh.ui.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Toast宿主组件
 * 负责在UI层级中显示Toast消息
 * 支持深色/浅色主题适配和动画效果
 * @param isDarkTheme 是否为深色主题
 * @param modifier 修饰符
 */
@Composable
fun ToastHost(
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val toastMessage = ToastManager.currentToast
    val colorScheme = MaterialTheme.colorScheme
    
    // 根据Toast类型和主题选择颜色
    val (backgroundColor, textColor) = when (toastMessage?.type) {
        ToastType.SUCCESS -> {
            val bgColor = if (isDarkTheme) Color(0xFF1B5E20) else Color(0xFFE8F5E8)
            val txtColor = if (isDarkTheme) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
            bgColor to txtColor
        }
        ToastType.WARNING -> {
            val bgColor = if (isDarkTheme) Color(0xFFF57C00) else Color(0xFFFFF3E0)
            val txtColor = if (isDarkTheme) Color(0xFFFFE0B2) else Color(0xFFEF6C00)
            bgColor to txtColor
        }
        ToastType.ERROR -> {
            val bgColor = if (isDarkTheme) Color(0xFFC62828) else Color(0xFFFFEBEE)
            val txtColor = if (isDarkTheme) Color(0xFFFFCDD2) else Color(0xFFD32F2F)
            bgColor to txtColor
        }
        else -> {
            // 默认信息类型
            val bgColor = colorScheme.surfaceVariant.copy(alpha = 0.95f)
            val txtColor = colorScheme.onSurfaceVariant
            bgColor to txtColor
        }
    }
    
    // 使用Popup确保Toast显示在最上层，覆盖对话框
    Popup(
        alignment = Alignment.BottomCenter,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(animationSpec = tween(200)) +
                    slideInVertically(
                        animationSpec = tween(300),
                        initialOffsetY = { it / 2 }
                    ),
            exit = fadeOut(animationSpec = tween(200)) +
                   slideOutVertically(
                       animationSpec = tween(200),
                       targetOffsetY = { it / 2 }
                   )
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = backgroundColor,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    // 底部按钮栏高度约为 48dp + 24dp padding = 72dp，Toast显示在按钮上方
                    .padding(bottom = 88.dp)
            ) {
                Text(
                    text = toastMessage?.message ?: "",
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}