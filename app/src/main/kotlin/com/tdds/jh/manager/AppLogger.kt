package com.tdds.jh

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

/**
 * 应用日志管理器
 * 用于记录应用运行日志,包含系统时间
 * 保留最近3天的日志,按天轮换
 * 记录应用存储空间增长情况
 */
object AppLogger {
    private var logFile: File? = null
    // 优化时间格式，只显示时分秒，避免过长
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    // 日志轮换:按天轮换，保留最近3天的日志
    private const val MAX_LOG_DAYS = 3

    // SharedPreferences keys
    private const val LOGGER_PREFS_NAME = "app_logger_prefs"
    private const val LAST_LOG_DAY_KEY = "last_log_day"

    // 存储空间记录相关
    private const val STORAGE_PREFS_NAME = "app_storage_tracker"
    private const val LAST_STORAGE_SIZE_KEY = "last_storage_size"
    private const val LAST_STORAGE_LOG_TIME_KEY = "last_storage_log_time"
    private const val STORAGE_LOG_INTERVAL_MS = 5 * 60 * 1000L // 5分钟记录一次

    // 操作标记，用于追踪存储增长
    private var lastOperation: String = "初始化"
    private var lastOperationTime: Long = System.currentTimeMillis()

    /**
     * 获取当前日期字符串
     */
    private fun getCurrentDayString(): String {
        return dayFormat.format(Date())
    }

    /**
     * 获取日志文件名
     */
    private fun getLogFileName(dayString: String): String {
        return "${dayString}.txt"
    }

    /**
     * 初始化日志系统
     * 在应用启动时调用
     * 保留最近3天的日志
     */
    fun init(context: Context, versionName: String = "", versionCode: Int = 0) {
        // 使用外部存储的 Android/data/com.tdds.jh/log/ 目录
        val logDir = File(context.getExternalFilesDir(null)?.parentFile, "log")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        val prefs = context.getSharedPreferences(LOGGER_PREFS_NAME, Context.MODE_PRIVATE)
        val lastDay = prefs.getString(LAST_LOG_DAY_KEY, "")
        val currentDay = getCurrentDayString()

        // 清理旧日志文件,只保留最近3天
        cleanupOldLogs(logDir, currentDay)

        // 创建当前日期的日志文件
        val currentLogFile = File(logDir, getLogFileName(currentDay))
        logFile = currentLogFile

        // 如果是新文件或日期变化了,写入日志头
        val isNewDay = lastDay != currentDay
        if (!currentLogFile.exists() || currentLogFile.length() == 0L || isNewDay) {
            log("=".repeat(50))
            log("应用启动")
            log("Zzjhq3685634009")
            log("应用版本: $versionName ($versionCode)")
            log("日志日期: ${currentDay}")
            log("日志文件: ${currentLogFile.absolutePath}")
            log("系统时间: ${dateFormat.format(Date())}")
            logDeviceInfo(context)
            log("=".repeat(50))
        } else {
            // 追加到现有日志
            log("-".repeat(30))
            log("应用重新启动")
            log("系统时间: ${dateFormat.format(Date())}")
            log("-".repeat(30))
        }

        // 保存当前日期
        prefs.edit().putString(LAST_LOG_DAY_KEY, currentDay).apply()
    }

    /**
     * 清理旧日志文件,只保留最近3天
     */
    private fun cleanupOldLogs(logDir: File, currentDay: String) {
        val logFiles = logDir.listFiles { file ->
            file.isFile && file.name.matches(Regex("\\d{8}\\.txt"))
        } ?: return

        // 按修改时间排序,删除最旧的
        if (logFiles.size > MAX_LOG_DAYS) {
            logFiles.sortBy { it.lastModified() }
            val filesToDelete = logFiles.size - MAX_LOG_DAYS
            for (i in 0 until filesToDelete) {
                logFiles[i].delete()
            }
        }
    }

    /**
     * 记录设备信息
     * 包括运行内存、存储空间和系统版本等关键信息
     * @param context 应用上下文
     */
    private fun logDeviceInfo(context: Context) {
        try {
            // 运行内存信息
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            val totalMem = memoryInfo.totalMem / (1024 * 1024) // MB
            val availMem = memoryInfo.availMem / (1024 * 1024) // MB
            val usedMem = totalMem - availMem
            val memUsagePercent = (usedMem * 100 / totalMem).toInt()

            log("运行内存: 总计 ${totalMem}MB, 可用 ${availMem}MB, 已用 ${usedMem}MB (${memUsagePercent}%)")

            // 存储空间信息
            val statFs = StatFs(context.getExternalFilesDir(null)?.path ?: context.filesDir.path)
            val blockSize = statFs.blockSizeLong
            val totalBlocks = statFs.blockCountLong
            val availableBlocks = statFs.availableBlocksLong

            val totalStorage = (totalBlocks * blockSize) / (1024 * 1024 * 1024) // GB
            val availStorage = (availableBlocks * blockSize) / (1024 * 1024 * 1024) // GB
            val usedStorage = totalStorage - availStorage
            val storageUsagePercent = if (totalStorage > 0) (usedStorage * 100 / totalStorage).toInt() else 0

            log("存储空间: 总计 ${totalStorage}GB, 可用 ${availStorage}GB, 已用 ${usedStorage}GB (${storageUsagePercent}%)")

            // Android版本信息
            log("Android版本: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            log("设备型号: ${Build.MANUFACTURER} ${Build.MODEL}")

        } catch (e: Exception) {
            log("获取设备信息失败: ${e.message}", "WARN")
        }
    }

    /**
     * 记录日志（异步写入文件，避免阻塞主线程）
     * @param message 日志消息
     * @param level 日志级别 (DEBUG, INFO, WARN, ERROR)
     */
    fun log(message: String, level: String = "INFO") {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] [$level] $message\n"

        // 异步写入文件，避免阻塞主线程
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                logFile?.appendText(logEntry)
            } catch (e: Exception) {
                android.util.Log.e("AppLogger", "写入日志文件失败: ${e.message}")
            }
        }

        // 同时输出到控制台（同步）
        android.util.Log.d("AppLogger", logEntry.trim())
    }

    /**
     * 记录调试日志
     */
    fun d(message: String) {
        log(message, "DEBUG")
    }

    /**
     * 记录信息日志
     */
    fun i(message: String) {
        log(message, "INFO")
    }

    /**
     * 记录警告日志
     */
    fun w(message: String) {
        log(message, "WARN")
    }

    /**
     * 记录错误日志
     */
    fun e(message: String, throwable: Throwable? = null) {
        log(message, "ERROR")
        throwable?.let {
            log("异常: ${it.message}", "ERROR")
            log("堆栈: ${it.stackTraceToString()}", "ERROR")
        }
    }

    /**
     * 获取日志文件目录
     */
    fun getLogDir(context: Context): File {
        return File(context.getExternalFilesDir(null)?.parentFile, "log")
    }

    /**
     * 获取所有日志文件列表(按时间倒序)
     */
    fun getLogFiles(context: Context): List<File> {
        val logDir = getLogDir(context)
        return logDir.listFiles { file ->
            file.isFile && file.name.matches(Regex("\\d{8}\\.txt"))
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * 获取当前日志文件
     */
    fun getCurrentLogFile(context: Context): File {
        val prefs = context.getSharedPreferences(LOGGER_PREFS_NAME, Context.MODE_PRIVATE)
        val currentDay = prefs.getString(LAST_LOG_DAY_KEY, getCurrentDayString()) ?: getCurrentDayString()
        return File(getLogDir(context), getLogFileName(currentDay))
    }

    /**
     * 读取当前日期日志内容
     */
    fun readCurrentLog(context: Context): String {
        return try {
            val logFile = getCurrentLogFile(context)
            if (logFile.exists()) {
                logFile.readText()
            } else {
                "日志文件不存在"
            }
        } catch (e: Exception) {
            "读取日志失败: ${e.message}"
        }
    }

    /**
     * 读取最近日志内容(所有日期)
     */
    fun readRecentLogs(context: Context): String {
        return try {
            val logFiles = getLogFiles(context)
            if (logFiles.isEmpty()) {
                "暂无日志文件"
            } else {
                val sb = StringBuilder()
                logFiles.forEach { file ->
                    val date = file.name.substringBefore(".")
                    sb.appendLine("=== ${date} ===")
                    sb.appendLine(file.readText())
                    sb.appendLine()
                }
                sb.toString()
            }
        } catch (e: Exception) {
            "读取日志失败: ${e.message}"
        }
    }

    /**
     * 标记操作，用于追踪存储增长
     * 在关键操作前调用，记录操作类型
     */
    fun markOperation(operation: String) {
        lastOperation = operation
        lastOperationTime = System.currentTimeMillis()
        d("[存储追踪] 开始操作: $operation")
    }

    /**
     * 记录应用存储空间使用情况
     * 自动检测存储增长并记录
     */
    fun logStorageUsage(context: Context, operationTag: String = "") {
        try {
            val currentTime = System.currentTimeMillis()
            val prefs = context.getSharedPreferences(STORAGE_PREFS_NAME, Context.MODE_PRIVATE)
            val lastLogTime = prefs.getLong(LAST_STORAGE_LOG_TIME_KEY, 0)

            // 计算应用各目录大小
            val appDir = context.getExternalFilesDir(null)?.parentFile?.parentFile?.parentFile
            val dataSize = calculateDirSize(context.filesDir)
            val cacheSize = calculateDirSize(context.cacheDir)
            val externalFilesSize = calculateDirSize(context.getExternalFilesDir(null))
            val externalCacheSize = calculateDirSize(context.externalCacheDir)

            // 计算工作目录和临时目录大小
            val workImagesDir = File(context.filesDir, "WorkImages")
            val tempDir = context.cacheDir
            val presetsDir = File(context.filesDir, "Presets")
            val packagesDir = File(context.filesDir, "Packages")
            val draftDir = File(context.filesDir, "Draft")

            val workImagesSize = calculateDirSize(workImagesDir)
            val tempSize = calculateDirSize(tempDir)
            val presetsSize = calculateDirSize(presetsDir)
            val packagesSize = calculateDirSize(packagesDir)
            val draftSize = calculateDirSize(draftDir)

            val totalAppSize = dataSize + cacheSize + externalFilesSize + externalCacheSize

            // 获取上次记录的存储大小
            val lastStorageSize = prefs.getLong(LAST_STORAGE_SIZE_KEY, 0)
            val sizeDiff = totalAppSize - lastStorageSize

            // 构建日志信息
            val tag = operationTag.ifEmpty { lastOperation }
            val timeDiff = currentTime - lastOperationTime
            val timeDiffStr = if (timeDiff < 60000) "${timeDiff / 1000}秒" else "${timeDiff / 60000}分钟"

            if (sizeDiff > 1024 * 1024) { // 增长超过1MB才记录
                val diffMB = sizeDiff / (1024 * 1024)
                w("[存储增长] 操作: $tag | 增长: ${diffMB}MB | 耗时: $timeDiffStr")
                w("[存储详情] 总计: ${formatSize(totalAppSize)}")
                w("  - 数据目录: ${formatSize(dataSize)}")
                w("  - 缓存目录: ${formatSize(cacheSize)}")
                w("  - 外部文件: ${formatSize(externalFilesSize)}")
                w("  - 外部缓存: ${formatSize(externalCacheSize)}")
                w("  - 工作图片: ${formatSize(workImagesSize)}")
                w("  - 临时文件: ${formatSize(tempSize)}")
                w("  - 预设文件: ${formatSize(presetsSize)}")
                w("  - 图包文件: ${formatSize(packagesSize)}")
                w("  - 草稿文件: ${formatSize(draftSize)}")
            } else if (currentTime - lastLogTime > STORAGE_LOG_INTERVAL_MS || operationTag.isNotEmpty()) {
                // 定期记录或强制记录
                i("[存储状态] 操作: $tag | 总计: ${formatSize(totalAppSize)}")
                d("  数据: ${formatSize(dataSize)}, 缓存: ${formatSize(cacheSize)}, 外部: ${formatSize(externalFilesSize)}")
                d("  工作: ${formatSize(workImagesSize)}, 临时: ${formatSize(tempSize)}, 预设: ${formatSize(presetsSize)}")
            }

            // 保存当前存储大小
            prefs.edit()
                .putLong(LAST_STORAGE_SIZE_KEY, totalAppSize)
                .putLong(LAST_STORAGE_LOG_TIME_KEY, currentTime)
                .apply()

        } catch (e: Exception) {
            e("记录存储使用情况失败", e)
        }
    }

    /**
     * 计算目录大小
     * 使用迭代方式替代递归，避免大目录导致栈溢出
     */
    private fun calculateDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0
        return try {
            var size = 0L
            val queue = ArrayDeque<File>()
            queue.add(dir)
            
            while (queue.isNotEmpty()) {
                val currentDir = queue.removeFirst()
                currentDir.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        queue.add(file)
                    } else {
                        size += file.length()
                    }
                }
            }
            size
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 格式化文件大小
     */
    private fun formatSize(size: Long): String {
        return when {
            size >= 1024 * 1024 * 1024 -> String.format("%.2f GB", size / (1024.0 * 1024 * 1024))
            size >= 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024))
            size >= 1024 -> String.format("%.2f KB", size / 1024.0)
            else -> "$size B"
        }
    }
}
