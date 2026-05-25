package com.tdds.jh

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.tdds.jh.manager.AppLogger
import com.tdds.jh.manager.PresetManager
import com.tdds.jh.resource.ResourceManager
import com.tdds.jh.ui.theme.MyApplicationTheme
import com.tdds.jh.ui.theme.ThemeManager
import com.tdds.jh.ui.tierlist.TierListMakerApp
import com.tdds.jh.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    // 草稿保存回调，用于在双击退出时触发保存
    private var saveDraftCallback: (() -> Unit)? = null

    // 标记是否正在执行不需要保存草稿的操作（如打开图片选择器、文件选择器等）
    // 当此标记为 true 时，onUserLeaveHint 不会触发草稿保存
    private var isSkippingDraftSave = false

    // 临时禁用草稿保存，用于执行特定操作时
    private fun skipDraftSaveTemporarily() {
        isSkippingDraftSave = true
        AppLogger.d("临时禁用草稿保存")
    }

    // 恢复草稿保存
    private fun resumeDraftSave() {
        isSkippingDraftSave = false
        AppLogger.d("恢复草稿保存")
    }

    override fun onDestroy() {
        AppLogger.i("MainActivity onDestroy - 开始清理资源")
        saveDraftCallback = null
        AppLogger.i("MainActivity onDestroy - 资源清理完成")
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        AppLogger.i("MainActivity onTrimMemory - 级别: $level")
        when {
            level == 20 -> {
                AppLogger.i("UI不可见")
            }
            level >= 40 -> {
                AppLogger.i("应用进入后台")
            }
            level >= 10 -> {
                AppLogger.i("运行内存低")
            }
        }
        super.onTrimMemory(level)
    }

    override fun onLowMemory() {
        AppLogger.w("MainActivity onLowMemory - 系统内存不足")
        super.onLowMemory()
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        val language = if (isFirstLaunch) {
            val systemLocale = newBase.resources.configuration.locales[0]
            val systemLanguage = systemLocale.language
            val autoLanguage = when (systemLanguage) {
                "zh" -> "zh"
                "en" -> "en"
                "ja" -> "ja"
                "ko" -> "ko"
                "ru" -> "ru"
                "de" -> "de"
                "fr" -> "fr"
                "es" -> "es"
                "ar" -> "ar"
                "pt" -> "pt"
                else -> "zh"
            }
            prefs.edit()
                .putString("language", autoLanguage)
                .putBoolean("is_first_launch", false)
                .putBoolean("show_language_on_first_launch", true)
                .apply()
            autoLanguage
        } else {
            prefs.getString("language", "zh") ?: "zh"
        }
        val locale = java.util.Locale.forLanguageTag(language)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName ?: ""
        val versionCode = packageInfo.longVersionCode.toInt()
        AppLogger.init(this, versionName, versionCode)
        AppLogger.i("MainActivity onCreate")

        AppLogger.markOperation("应用启动")
        AppLogger.logStorageUsage(this, "启动前")

        // 启动时仅清理7天前的日志文件，保留其他缓存以提高启动速度
        ResourceManager.cleanupLogFiles(this)
        AppLogger.i("应用启动 - 已清理过期日志")

        AppLogger.logStorageUsage(this, "启动后")

        enableEdgeToEdge()
        setContent {
            val themeState = ThemeManager.rememberThemeState(this)
            val isDarkTheme = themeState.value.isDarkTheme
            val systemInDarkTheme = ThemeManager.getSystemInDarkTheme()

            val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            var disableCustomFont by remember { mutableStateOf(prefs.getBoolean("disable_custom_font", true)) }

            ThemeManager.ApplyStatusBarTheme(isDarkTheme)

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                disableCustomFont = disableCustomFont
            ) {
                TierListMakerApp(
                    isDarkTheme = isDarkTheme,
                    followSystemTheme = themeState.value.followSystemTheme,
                    disableCustomFont = disableCustomFont,
                    onDisableCustomFontChange = { newValue ->
                        disableCustomFont = newValue
                        prefs.edit().putBoolean("disable_custom_font", newValue).apply()
                        AppLogger.i("设置 禁用字体: $newValue")
                    },
                    onThemeChange = { newTheme ->
                        val newState = ThemeManager.toggleTheme(this, themeState.value)
                        themeState.value = newState
                        AppLogger.i("保存主题设置: ${if (newTheme) "深色" else "浅色"}")
                    },
                    onFollowSystemThemeChange = { newValue ->
                        val newState = ThemeManager.setFollowSystemTheme(this, newValue, systemInDarkTheme)
                        themeState.value = newState
                    },
                    onRegisterSaveDraftCallback = { callback ->
                        saveDraftCallback = callback
                    },
                    onSaveDraftForResourceManager = {
                        saveDraftCallback?.invoke()
                    },
                    onSkipDraftSave = {
                        skipDraftSaveTemporarily()
                    },
                    onResumeDraftSave = {
                        resumeDraftSave()
                    },
                    onExitApp = {
                        exitAppWithCleanup()
                    }
                )
            }
        }
    }

    /**
     * 双击退出时立即返回桌面，同步保存草稿，后台清理资源
     */
    fun exitAppWithCleanup() {
        AppLogger.i("双击退出 - 立即返回桌面")
        finishAffinity()

        AppLogger.i("双击退出 - 开始同步保存草稿")
        try {
            saveDraftCallback?.invoke()
            AppLogger.i("双击退出 - 草稿保存完成")
        } catch (e: Exception) {
            AppLogger.e("双击退出 - 保存草稿失败", e)
        }

        // 退出时不再执行清理操作，保留缓存以便下次快速启动
        // 日志文件会在下次启动时自动清理（7天前的日志）
        AppLogger.i("双击退出 - 后台操作全部完成")
    }

    /**
     * 当用户离开Activity时调用（如按Home键、切换到其他应用）
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        if (isSkippingDraftSave) {
            AppLogger.d("onUserLeaveHint - 跳过草稿保存（正在执行特定操作）")
            return
        }

        AppLogger.i("onUserLeaveHint - 用户离开应用，触发草稿保存")
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    saveDraftCallback?.invoke()
                }
                AppLogger.i("onUserLeaveHint - 草稿保存完成")
            } catch (e: Exception) {
                AppLogger.e("onUserLeaveHint - 保存草稿失败", e)
            }
        }
    }
}
