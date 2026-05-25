package com.tdds.jh.ui.tierlist

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast

import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background

import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.runtime.key
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import kotlin.math.abs
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import com.tdds.jh.ui.theme.LocalExtendedColors
import com.tdds.jh.ui.theme.ThemeManager
import com.tdds.jh.resource.ResourceManager
import com.tdds.jh.resource.PackageManager as ResourcePackageManager
import com.tdds.jh.resource.PackageItem
import com.tdds.jh.resource.ImportTarget
import com.tdds.jh.resource.ZipPasswordRequiredException
import com.tdds.jh.manager.ImageResourceManager
import com.tdds.jh.manager.AppLogger
import com.tdds.jh.manager.PresetManager
import com.tdds.jh.manager.PresetData
import com.tdds.jh.ui.tierlist.components.ManagePresetsDialog
import com.tdds.jh.ui.tierlist.components.PresetNameDialog
import com.tdds.jh.ui.tierlist.components.PresetListDialog
import com.tdds.jh.ui.tierlist.components.LoadingDialog
import com.tdds.jh.ui.tierlist.components.ImportOverwriteDialog
import com.tdds.jh.ui.tierlist.components.DraftRestoreDialog
import com.tdds.jh.bitmap.TierImage
import com.tdds.jh.bitmap.TierItem
import com.tdds.jh.bitmap.generateTierListBitmap
import com.tdds.jh.R

import com.tdds.jh.ui.dialog.AboutDialog
import com.tdds.jh.ui.dialog.PreviewDialog

import com.tdds.jh.ui.dialog.edit.ColorPickerDialog
import com.tdds.jh.ui.dialog.edit.EditAuthorDialog
import com.tdds.jh.ui.dialog.edit.EditTierNameDialog
import com.tdds.jh.ui.dialog.edit.EditTitleDialog
import com.tdds.jh.ui.dialog.settings.ProgramSettingsDialog
import com.tdds.jh.ui.dialog.settings.LanguageSelectionDialog
import com.tdds.jh.ui.tierlist.components.ResourceManageDialog
import com.tdds.jh.ui.tierlist.components.SettingsMenuDialog
import com.tdds.jh.ui.tierlist.components.ManagePackagesDialog
import com.tdds.jh.ui.tierlist.components.PackageConfirmDialog
import com.tdds.jh.ui.tierlist.components.ImportPackageDialog
import com.tdds.jh.ui.tierlist.components.ZipPasswordDialog
import com.tdds.jh.ui.tierlist.components.ImportTargetDialog
import com.tdds.jh.ui.tierlist.components.InstructionsDialog
import com.tdds.jh.ui.tierlist.components.ImageActionDialog
import com.tdds.jh.ui.tierlist.components.ImageViewDialog
import com.tdds.jh.ui.tierlist.components.MoveImageDialog
import com.tdds.jh.ui.tierlist.components.EditImageNameDialog
import com.tdds.jh.ui.tierlist.components.SetBadgeDialog
import com.tdds.jh.ui.tierlist.components.BadgePreviewArea
import com.tdds.jh.ui.tierlist.components.ImageCropDialog
import com.tdds.jh.ui.tierlist.components.CropState
import com.tdds.jh.ui.tierlist.components.SwipeableTierRow
import com.tdds.jh.ui.tierlist.components.DraggableImage
import com.tdds.jh.ui.tierlist.components.DraggablePendingImageItem
import com.tdds.jh.ui.tierlist.components.FloatingDragImage
import com.tdds.jh.ui.tierlist.components.AddTierButton
import com.tdds.jh.ui.tierlist.components.AuthorInfoSection
import com.tdds.jh.ui.tierlist.components.TierListDialogs
import com.tdds.jh.ui.tierlist.state.DialogState
import com.tdds.jh.ui.tierlist.state.DialogHandlers
import com.tdds.jh.ui.tierlist.components.PendingImagesSection
import com.tdds.jh.ui.tierlist.model.PresetOperation
import com.tdds.jh.ui.tierlist.model.TierListConfig
import com.tdds.jh.ui.tierlist.service.SettingsService
import com.tdds.jh.ui.tierlist.utils.saveBitmapToGallery
import com.tdds.jh.ui.tierlist.utils.shareBitmap
import com.tdds.jh.ui.tierlist.utils.saveTierListImage
import com.tdds.jh.ui.tierlist.utils.PermissionUtils
import com.tdds.jh.ui.tierlist.utils.withStoragePermission
import com.tdds.jh.ui.tierlist.utils.ImageOperationUtils
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowDropDown
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.ui.graphics.BlendMode
import java.io.FileOutputStream
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.LocalTextStyle
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat

import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.CachePolicy
import com.tdds.jh.ui.theme.MyApplicationTheme
import com.tdds.jh.ui.toast.ToastHost
import com.tdds.jh.ui.toast.showToastWithoutIcon
import com.tdds.jh.domain.utils.TextUtils
import com.tdds.jh.domain.utils.ColorUtils
import com.tdds.jh.domain.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID
import kotlin.math.roundToInt

// 手势类型枚举
private enum class GestureType {
    LongPress,      // 长按
    VerticalDrag,   // 垂直拖动
    HorizontalSwipe // 水平滑动
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TierListMakerApp(
    isDarkTheme: Boolean = false,
    followSystemTheme: Boolean = true,
    disableCustomFont: Boolean = false,
    onDisableCustomFontChange: ((Boolean) -> Unit)? = null,
    onThemeChange: (Boolean) -> Unit = {},
    onFollowSystemThemeChange: ((Boolean) -> Unit)? = null,
    onRegisterSaveDraftCallback: ((() -> Unit) -> Unit)? = null,
    onSaveDraftForResourceManager: (() -> Unit)? = null,
    onSkipDraftSave: (() -> Unit)? = null,
    onResumeDraftSave: (() -> Unit)? = null,
    onExitApp: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    // 记录应用启动日志
    LaunchedEffect(Unit) {
        AppLogger.i("TierListMakerApp 启动")
        AppLogger.i("系统版本: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        AppLogger.i("设备: ${Build.MANUFACTURER} ${Build.MODEL}")
    }

    // 设置服务
    val settingsService = remember { SettingsService(context) }

    // 默认梯度模板 - 根据语言选择
    val currentLocale = context.resources.configuration.locales[0]
    val isChinese = currentLocale.language == "zh"

    val defaultTiers = TierListConfig.getDefaultTiers(isChinese)

    // 当前梯度列表
    val tiers = remember { mutableStateListOf<TierItem>().apply { addAll(defaultTiers) } }

    // 每个梯度的图片
    val tierImages = remember { mutableStateListOf<TierImage>() }

    // 程序设置状态
    var disableClickAdd by remember { mutableStateOf(settingsService.disableClickAdd) }
    // 调节浮显：水平偏移 0-300dp（默认125），垂直偏移 0-150dp（默认85）
    var floatOffsetX by remember { mutableStateOf(settingsService.floatOffsetX) }
    var floatOffsetY by remember { mutableStateOf(settingsService.floatOffsetY) }
    // 外置小图开关：启用时小图标显示在图片右侧
    var externalBadgeEnabled by remember { mutableStateOf(settingsService.externalBadgeEnabled) }
    // 下置命名开关：启用时图片命名显示在图片下方
    var nameBelowImage by remember { mutableStateOf(settingsService.nameBelowImage) }

    // 待添加的图片（允许重复URI，通过哈希查重实现文件复用）
    var pendingImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    
    // ZIP导入的待添加图片
    var zipPendingImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pendingZipUri by remember { mutableStateOf<Uri?>(null) }
    
    // 图包管理状态
    var selectedPackage by remember { mutableStateOf<PackageItem.Imported?>(null) }
    var selectedPackageImageCount by remember { mutableStateOf(0) }
    var isImportingPackage by remember { mutableStateOf(false) }  // 防止重复导入
    var isExportingPackage by remember { mutableStateOf(false) }  // 图包导出状态

    // 标题状态
    var tierListTitle by remember { mutableStateOf(context.getString(R.string.default_title)) }

    // 作者信息状态
    var authorName by remember { mutableStateOf("") }

    // 语言设置状态
    val shouldShowLanguageOnFirstLaunch = settingsService.showLanguageOnFirstLaunch
    var currentLanguage by remember { mutableStateOf(settingsService.currentLanguage) }
    var languageChanged by remember { mutableStateOf(false) }

    // 语言切换 - 使用 recreate 重启 Activity 来应用新语言
    LaunchedEffect(languageChanged) {
        if (languageChanged) {
            languageChanged = false
            // 保存语言设置
            settingsService.saveLanguage(currentLanguage)
            // 重启 Activity 以应用新语言
            (context as ComponentActivity).recreate()
        }
    }

    // 对话框状态（使用新的 DialogState 集中管理）
    val dialogState = remember { DialogState() }

    // 拖拽选中状态
    var selectedImageForDrag by remember { mutableStateOf<TierImage?>(null) }
    
    // 层级图片拖拽状态
    var draggingTierImage by remember { mutableStateOf<TierImage?>(null) }
    var draggingTierImagePosition by remember { mutableStateOf(Offset.Zero) }
    var draggingTierImageTarget by remember { mutableStateOf<String?>(null) }
    
    // 待分级区位置
    var pendingSectionRect by remember { mutableStateOf<android.graphics.Rect?>(null) }

    // 层级位置跟踪（用于拖放）- 必须在 presetFilePicker 之前定义
    var tierRowPositions by remember { mutableStateOf<Map<String, android.graphics.Rect>>(emptyMap()) }

    // 删除层级对话框状态
    var tierToDelete by remember { mutableStateOf<TierItem?>(null) }

    // 预设管理器
    val presetManager = remember { PresetManager(context) }

    // 预设管理状态
    var pendingPresetName by remember { mutableStateOf("") }
    var isExportingPreset by remember { mutableStateOf(false) }
    var isImportingPreset by remember { mutableStateOf(false) }
    var isSavingPreset by remember { mutableStateOf(false) }
    var presetOperation by remember { mutableStateOf<PresetOperation?>(null) }

    // 草稿恢复状态
    // 只存储草稿配置数据，不解压图片，等待用户确认后再解压
    var draftConfigData by remember { mutableStateOf<PresetData?>(null) }
    // 是否跳过草稿恢复（用于外部导入预设时）
    var skipDraftRestore by remember { mutableStateOf(false) }



    // 检查是否是从外部打开 .tdds 或 .zip 文件
    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity
        activity?.intent?.let { intent ->
            // 处理 VIEW 动作（文件管理器等）
            if (intent.action == Intent.ACTION_VIEW) {
                val dataUri = intent.data
                if (dataUri != null) {
                    // 使用 FileUtils 获取文件名来检查扩展名
                    val fileName = FileUtils.getFileNameFromUri(context, dataUri)
                    val uriString = dataUri.toString()
                    val isTddsFile = (fileName?.endsWith(".tdds", ignoreCase = true) == true) ||
                            uriString.endsWith(".tdds", ignoreCase = true) ||
                            uriString.contains(".tdds", ignoreCase = true)
                    val isZipFile = (fileName?.endsWith(".zip", ignoreCase = true) == true) ||
                            uriString.endsWith(".zip", ignoreCase = true) ||
                            uriString.contains(".zip", ignoreCase = true)
                    
                    if (isTddsFile) {
                        AppLogger.i("从外部打开 .tdds 文件 (VIEW): $dataUri, fileName: $fileName")
                        isImportingPreset = true
                        skipDraftRestore = true
                        try {
                            val importResult = withContext(Dispatchers.IO) {
                                presetManager.importPreset(dataUri)
                            }
                            when (importResult.status) {
                                PresetManager.ImportStatus.SUCCESS,
                                PresetManager.ImportStatus.ALREADY_EXISTS -> {
                                    val result = withContext(Dispatchers.IO) {
                                        presetManager.applyPreset(importResult.presetFile)
                                    }
                                    tiers.clear()
                                    tiers.addAll(result.tiers.map { tierData ->
                                        TierItem(tierData.label, try {
                                            Color(android.graphics.Color.parseColor("#${tierData.color}"))
                                        } catch (e: Exception) { Color.Gray })
                                    })
                                    tierImages.removeAll { true }
                                    result.tierImages.forEach { appliedImage ->
                                        tierImages.add(TierImage(
                                            id = appliedImage.id,
                                            tierLabel = appliedImage.tierLabel,
                                            uri = appliedImage.uri,
                                            name = appliedImage.name,
                                            badgeUri = appliedImage.badgeUri,
                                            badgeUri2 = appliedImage.badgeUri2,
                                            badgeUri3 = appliedImage.badgeUri3,
                                            originalUri = appliedImage.originalUri,
                                            cropPositionX = appliedImage.cropPositionX,
                                            cropPositionY = appliedImage.cropPositionY,
                                            cropScale = appliedImage.cropScale,
                                            isCropped = appliedImage.isCropped,
                                            cropRatio = appliedImage.cropRatio,
                                            useCustomCrop = appliedImage.useCustomCrop,
                                            customCropWidth = appliedImage.customCropWidth,
                                            customCropHeight = appliedImage.customCropHeight
                                        ))
                                    }
                                    pendingImages = result.pendingImages
                                    tierListTitle = result.title
                                    authorName = result.author
                                    settingsService.cropPositionX = result.cropPositionX
                                    settingsService.cropPositionY = result.cropPositionY
                                    settingsService.customCropWidth = result.customCropWidth
                                    settingsService.customCropHeight = result.customCropHeight
                                    settingsService.useCustomCropSize = result.useCustomCropSize
                                    settingsService.cropRatio = result.cropRatio
                                    // 清理草稿文件（保留工作目录中的图片）
                                    presetManager.cleanupDraftOnly()
                                    showToastWithoutIcon(
                                        context,
                                        context.getString(R.string.preset_import_success),
                                        Toast.LENGTH_SHORT
                                    )
                                    AppLogger.i("外部打开 .tdds 文件并应用预设成功: ${result.title}")
                                }
                                PresetManager.ImportStatus.NEEDS_OVERWRITE -> {
                                    dialogState.pendingImportResult = importResult
                                    dialogState.showImportOverwriteDialog = true
                                    AppLogger.i("外部打开 .tdds 文件需要覆盖确认")
                                }
                            }
                        } catch (e: Exception) {
                            AppLogger.e("外部打开 .tdds 文件失败", e)
                            showToastWithoutIcon(
                                context,
                                context.getString(R.string.preset_import_failed, e.message),
                                Toast.LENGTH_SHORT
                            )
                        } finally {
                            isImportingPreset = false
                        }
                        // 清除 intent 避免重复处理
                        activity.intent = null
                        return@LaunchedEffect
                    }
                    
                    // 处理 .zip 图包文件
                    if (isZipFile) {
                        AppLogger.i("从外部打开 .zip 图包文件 (VIEW): $dataUri, fileName: $fileName")
                        isImportingPreset = true
                        skipDraftRestore = true
                        try {
                            // 保存图包到图包目录
                            val savedPackageFile = withContext(Dispatchers.IO) {
                                presetManager.saveImportedPackage(dataUri, fileName ?: "imported_${System.currentTimeMillis()}.zip")
                            }
                            if (savedPackageFile != null) {
                                AppLogger.i("外部图包导入成功: ${savedPackageFile.name}")
                                showToastWithoutIcon(
                                    context,
                                    context.getString(R.string.package_import_success, savedPackageFile.nameWithoutExtension),
                                    Toast.LENGTH_SHORT
                                )
                            } else {
                                AppLogger.e("外部图包导入失败: 保存文件返回null")
                                showToastWithoutIcon(
                                    context,
                                    context.getString(R.string.package_import_failed),
                                    Toast.LENGTH_SHORT
                                )
                            }
                        } catch (e: Exception) {
                            AppLogger.e("外部打开 .zip 图包文件失败", e)
                            showToastWithoutIcon(
                                context,
                                context.getString(R.string.package_import_failed, e.message),
                                Toast.LENGTH_SHORT
                            )
                        } finally {
                            isImportingPreset = false
                        }
                        // 清除 intent 避免重复处理
                        activity.intent = null
                        return@LaunchedEffect
                    }
                }
            }

            // 处理 SEND 动作（QQ等应用分享）
            if (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE) {
                val dataUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (dataUri != null) {
                    // 使用 FileUtils 获取文件名来检查扩展名
                    val fileName = FileUtils.getFileNameFromUri(context, dataUri)
                    val uriString = dataUri.toString()
                    val isTddsFile = (fileName?.endsWith(".tdds", ignoreCase = true) == true) ||
                            uriString.endsWith(".tdds", ignoreCase = true) ||
                            uriString.contains(".tdds", ignoreCase = true)
                    val isZipFile = (fileName?.endsWith(".zip", ignoreCase = true) == true) ||
                            uriString.endsWith(".zip", ignoreCase = true) ||
                            uriString.contains(".zip", ignoreCase = true)
                    
                    if (isTddsFile) {
                        AppLogger.i("从外部分享打开 .tdds 文件 (SEND): $dataUri, fileName: $fileName")
                        isImportingPreset = true
                        skipDraftRestore = true
                        try {
                            val importResult = withContext(Dispatchers.IO) {
                                presetManager.importPreset(dataUri)
                            }
                            when (importResult.status) {
                                PresetManager.ImportStatus.SUCCESS,
                                PresetManager.ImportStatus.ALREADY_EXISTS -> {
                                    val result = withContext(Dispatchers.IO) {
                                        presetManager.applyPreset(importResult.presetFile)
                                    }
                                    tiers.clear()
                                    tiers.addAll(result.tiers.map { tierData ->
                                        TierItem(tierData.label, try {
                                            Color(android.graphics.Color.parseColor("#${tierData.color}"))
                                        } catch (e: Exception) { Color.Gray })
                                    })
                                    tierImages.removeAll { true }
                                    result.tierImages.forEach { appliedImage ->
                                        tierImages.add(TierImage(
                                            id = appliedImage.id,
                                            tierLabel = appliedImage.tierLabel,
                                            uri = appliedImage.uri,
                                            name = appliedImage.name,
                                            badgeUri = appliedImage.badgeUri,
                                            badgeUri2 = appliedImage.badgeUri2,
                                            badgeUri3 = appliedImage.badgeUri3,
                                            originalUri = appliedImage.originalUri,
                                            cropPositionX = appliedImage.cropPositionX,
                                            cropPositionY = appliedImage.cropPositionY,
                                            cropScale = appliedImage.cropScale,
                                            isCropped = appliedImage.isCropped,
                                            cropRatio = appliedImage.cropRatio,
                                            useCustomCrop = appliedImage.useCustomCrop,
                                            customCropWidth = appliedImage.customCropWidth,
                                            customCropHeight = appliedImage.customCropHeight
                                        ))
                                    }
                                    pendingImages = result.pendingImages
                                    tierListTitle = result.title
                                    authorName = result.author
                                    settingsService.cropPositionX = result.cropPositionX
                                    settingsService.cropPositionY = result.cropPositionY
                                    settingsService.customCropWidth = result.customCropWidth
                                    settingsService.customCropHeight = result.customCropHeight
                                    settingsService.useCustomCropSize = result.useCustomCropSize
                                    settingsService.cropRatio = result.cropRatio
                                    // 清理草稿文件（保留工作目录中的图片）
                                    presetManager.cleanupDraftOnly()
                                    showToastWithoutIcon(
                                        context,
                                        context.getString(R.string.preset_import_success),
                                        Toast.LENGTH_SHORT
                                    )
                                    AppLogger.i("外部分享打开 .tdds 文件并应用预设成功: ${result.title}")
                                }
                                PresetManager.ImportStatus.NEEDS_OVERWRITE -> {
                                    dialogState.pendingImportResult = importResult
                                    dialogState.showImportOverwriteDialog = true
                                    AppLogger.i("外部分享打开 .tdds 文件需要覆盖确认")
                                }
                            }
                        } catch (e: Exception) {
                            AppLogger.e("外部分享打开 .tdds 文件失败", e)
                            showToastWithoutIcon(
                                context,
                                context.getString(R.string.preset_import_failed, e.message),
                                Toast.LENGTH_SHORT
                            )
                        } finally {
                            isImportingPreset = false
                        }
                        // 清除 intent 避免重复处理
                        activity.intent = null
                        return@LaunchedEffect
                    }
                    
                    // 处理 .zip 图包文件
                    if (isZipFile) {
                        AppLogger.i("从外部分享打开 .zip 图包文件 (SEND): $dataUri, fileName: $fileName")
                        isImportingPreset = true
                        skipDraftRestore = true
                        try {
                            // 保存图包到图包目录
                            val savedPackageFile = withContext(Dispatchers.IO) {
                                presetManager.saveImportedPackage(dataUri, fileName ?: "imported_${System.currentTimeMillis()}.zip")
                            }
                            if (savedPackageFile != null) {
                                AppLogger.i("外部图包导入成功: ${savedPackageFile.name}")
                                showToastWithoutIcon(
                                    context,
                                    context.getString(R.string.package_import_success, savedPackageFile.nameWithoutExtension),
                                    Toast.LENGTH_SHORT
                                )
                            } else {
                                AppLogger.e("外部图包导入失败: 保存文件返回null")
                                showToastWithoutIcon(
                                    context,
                                    context.getString(R.string.package_import_failed),
                                    Toast.LENGTH_SHORT
                                )
                            }
                        } catch (e: Exception) {
                            AppLogger.e("外部分享打开 .zip 图包文件失败", e)
                            showToastWithoutIcon(
                                context,
                                context.getString(R.string.package_import_failed, e.message),
                                Toast.LENGTH_SHORT
                            )
                        } finally {
                            isImportingPreset = false
                        }
                        // 清除 intent 避免重复处理
                        activity.intent = null
                        return@LaunchedEffect
                    }
                }
            }
        }
    }

    // 检查是否存在草稿（仅在当前没有编辑内容时显示恢复对话框）
    LaunchedEffect(Unit) {
        // 如果跳过草稿恢复标志已设置，直接返回
        if (skipDraftRestore) {
            AppLogger.i("跳过草稿恢复检查（从外部导入预设或图包）")
            return@LaunchedEffect
        }
        if (presetManager.hasDraft()) {
            // 检查当前是否已经有编辑内容
            val hasCurrentContent = tierListTitle != context.getString(R.string.default_title) ||
                    authorName.isNotEmpty() ||
                    tierImages.isNotEmpty() ||
                    pendingImages.isNotEmpty() ||
                    tiers.size != defaultTiers.size ||
                    tiers.zip(defaultTiers).any { (current, default) ->
                        current.label != default.label || current.color != default.color
                    }

            // 只有当前没有编辑内容时才显示恢复对话框
            if (!hasCurrentContent) {
                // 只读取草稿配置，不解压图片（等待用户确认后再解压）
                val draftConfig = presetManager.readDraftConfig()
                if (draftConfig != null) {
                    draftConfigData = draftConfig
                    dialogState.showDraftRestoreDialog = true
                    AppLogger.i("发现草稿且当前无编辑内容，显示恢复对话框（未解压）")
                }
            } else {
                AppLogger.i("发现草稿但当前已有编辑内容，跳过恢复对话框")
                // 清理草稿，因为用户正在编辑新内容
                presetManager.cleanupDraft()
            }
        }
    }

    // 注册草稿保存回调到 Activity
    DisposableEffect(Unit) {
        onRegisterSaveDraftCallback?.invoke {
            // 检查是否有编辑内容（非默认状态）
            val hasContent = tierListTitle != context.getString(R.string.default_title) ||
                    authorName.isNotEmpty() ||
                    tierImages.isNotEmpty() ||
                    pendingImages.isNotEmpty() ||
                    tiers.size != defaultTiers.size ||
                    tiers.zip(defaultTiers).any { (current, default) ->
                        current.label != default.label || current.color != default.color
                    }

            if (hasContent) {
                // 保存草稿 - 使用 runBlocking 确保保存完成
                // 注意：不进行任何文件复制操作，直接打包工作目录中的现有文件
                kotlinx.coroutines.runBlocking {
                    try {
                        AppLogger.markOperation("保存草稿")
                        AppLogger.logStorageUsage(context, "保存草稿前")
                        val presetData = presetManager.createPresetData(
                            title = tierListTitle,
                            author = authorName,
                            tiers = tiers,
                            tierImages = tierImages,
                            pendingImages = pendingImages,
                            cropPositionX = settingsService.cropPositionX,
                            cropPositionY = settingsService.cropPositionY,
                            customCropWidth = settingsService.customCropWidth,
                            customCropHeight = settingsService.customCropHeight,
                            useCustomCropSize = settingsService.useCustomCropSize,
                            cropRatio = settingsService.cropRatio
                        )
                        presetManager.saveDraft(presetData)
                        AppLogger.i("双击退出时保存草稿: $tierListTitle")
                        AppLogger.logStorageUsage(context, "保存草稿后")
                    } catch (e: Exception) {
                        AppLogger.e("保存草稿失败", e)
                    }
                }
            } else {
                // 没有内容，清理草稿
                presetManager.cleanupDraft()
                AppLogger.i("没有编辑内容，清理草稿")
            }
        }
        onDispose { }
    }

    // 预设文件选择器
    val presetFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // 先设置状态显示加载对话框
            isImportingPreset = true
            scope.launch {
                // 让出时间片，确保UI有时间显示加载对话框
                yield()
                AppLogger.markOperation("导入预设")
                AppLogger.logStorageUsage(context, "导入预设前")
                try {
                    // 在后台线程执行耗时操作
                    val importResult = withContext(Dispatchers.IO) {
                        presetManager.importPreset(it)
                    }
                    when (importResult.status) {
                        PresetManager.ImportStatus.SUCCESS -> {
                            // 导入成功，应用预设数据
                            val result = withContext(Dispatchers.IO) {
                                presetManager.applyPreset(importResult.presetFile)
                            }
                            // 更新UI状态
                            tiers.clear()
                            tiers.addAll(result.tiers.map { tierData ->
                                TierItem(tierData.label, try {
                                    Color(android.graphics.Color.parseColor("#${tierData.color}"))
                                } catch (e: Exception) { Color.Gray })
                            })
                            // 强制清除所有图片数据
                            tierImages.removeAll { true }
                            // 添加新的图片数据
                            result.tierImages.forEach { appliedImage ->
                                tierImages.add(TierImage(
                                    id = appliedImage.id,
                                    tierLabel = appliedImage.tierLabel,
                                    uri = appliedImage.uri,
                                    name = appliedImage.name,
                                    badgeUri = appliedImage.badgeUri,
                                    badgeUri2 = appliedImage.badgeUri2,
                                    badgeUri3 = appliedImage.badgeUri3,
                                    originalUri = appliedImage.originalUri,
                                    cropPositionX = appliedImage.cropPositionX,
                                    cropPositionY = appliedImage.cropPositionY,
                                    cropScale = appliedImage.cropScale,
                                    isCropped = appliedImage.isCropped,
                                    cropRatio = appliedImage.cropRatio,
                                    useCustomCrop = appliedImage.useCustomCrop,
                                    customCropWidth = appliedImage.customCropWidth,
                                    customCropHeight = appliedImage.customCropHeight
                                ))
                            }
                            pendingImages = result.pendingImages
                            tierListTitle = importResult.presetData.title
                            authorName = importResult.presetData.author
                            // 清理旧的裁剪设置并应用新的
                            settingsService.clearCropSettings()
                            settingsService.customCropWidth = result.customCropWidth
                            settingsService.customCropHeight = result.customCropHeight
                            settingsService.useCustomCropSize = result.useCustomCropSize
                            settingsService.cropRatio = result.cropRatio
                            // 清空层级位置信息,确保使用预设中的层级标签
                            tierRowPositions = emptyMap()
                            showToastWithoutIcon(context, context.getString(R.string.preset_import_success))
                            AppLogger.i("导入预设成功: ${importResult.presetData.title}")
                            AppLogger.logStorageUsage(context, "导入预设后")

                            // 静默保存覆盖原预设文件（转换为WebP格式）
                            withContext(Dispatchers.IO) {
                                try {
                                    val workImagesDir = presetManager.getWorkImagesDirectory()
                                    val newPresetData = presetManager.createPresetData(
                                        title = tierListTitle,
                                        author = authorName,
                                        tiers = tiers,
                                        tierImages = tierImages,
                                        pendingImages = pendingImages,
                                        cropPositionX = settingsService.cropPositionX,
                                        cropPositionY = settingsService.cropPositionY,
                                        customCropWidth = settingsService.customCropWidth,
                                        customCropHeight = settingsService.customCropHeight,
                                        useCustomCropSize = settingsService.useCustomCropSize,
                                        cropRatio = settingsService.cropRatio
                                    )
                                    presetManager.exportPreset(
                                        presetName = tierListTitle,
                                        presetData = newPresetData,
                                        tempDir = workImagesDir,
                                        outputFile = importResult.presetFile
                                    )
                                    AppLogger.i("静默覆盖预设成功: ${importResult.presetFile.name}")
                                } catch (e: Exception) {
                                    AppLogger.e("静默覆盖预设失败", e)
                                }
                            }
                        }
                        PresetManager.ImportStatus.NEEDS_OVERWRITE -> {
                            // 需要询问是否覆盖
                            isImportingPreset = false
                            dialogState.pendingImportResult = importResult
                            dialogState.showImportOverwriteDialog = true
                            AppLogger.i("导入预设需要覆盖确认: ${importResult.presetData.title}")
                        }
                        PresetManager.ImportStatus.ALREADY_EXISTS -> {
                            // 完全相同的预设已存在
                            isImportingPreset = false
                            val result = withContext(Dispatchers.IO) {
                                presetManager.applyPreset(importResult.presetFile)
                            }
                            // 更新UI状态
                            tiers.clear()
                            tiers.addAll(result.tiers.map { tierData ->
                                TierItem(tierData.label, try {
                                    Color(android.graphics.Color.parseColor("#${tierData.color}"))
                                } catch (e: Exception) { Color.Gray })
                            })
                            tierImages.clear()
                            tierImages.addAll(result.tierImages.map { appliedImage ->
                                TierImage(
                                    id = appliedImage.id,
                                    tierLabel = appliedImage.tierLabel,
                                    uri = appliedImage.uri,
                                    name = appliedImage.name,
                                    badgeUri = appliedImage.badgeUri,
                                    badgeUri2 = appliedImage.badgeUri2,
                                    badgeUri3 = appliedImage.badgeUri3,
                                    originalUri = appliedImage.originalUri,
                                    cropPositionX = appliedImage.cropPositionX,
                                    cropPositionY = appliedImage.cropPositionY,
                                    cropScale = appliedImage.cropScale,
                                    isCropped = appliedImage.isCropped,
                                    cropRatio = appliedImage.cropRatio,
                                    useCustomCrop = appliedImage.useCustomCrop,
                                    customCropWidth = appliedImage.customCropWidth,
                                    customCropHeight = appliedImage.customCropHeight
                                )
                            })
                            pendingImages = result.pendingImages
                            tierListTitle = importResult.presetData.title
                            authorName = importResult.presetData.author
                            // 清理旧的裁剪设置并应用新的
                            settingsService.clearCropSettings()
                            settingsService.customCropWidth = result.customCropWidth
                            settingsService.customCropHeight = result.customCropHeight
                            settingsService.useCustomCropSize = result.useCustomCropSize
                            settingsService.cropRatio = result.cropRatio
                            // 清空层级位置信息,确保使用预设中的层级标签
                            tierRowPositions = emptyMap()
                            showToastWithoutIcon(context, context.getString(R.string.preset_already_loaded))
                            AppLogger.i("加载已存在的预设: ${importResult.presetData.title}")
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e("导入预设失败", e)
                    showToastWithoutIcon(
                        context,
                        context.getString(R.string.preset_import_failed, e.message),
                        Toast.LENGTH_LONG
                    )
                }
                isImportingPreset = false
                // 恢复草稿保存
                onResumeDraftSave?.invoke()
            }
        } ?: run {
            // 用户取消了选择，恢复草稿保存
            onResumeDraftSave?.invoke()
        }
    }

    // 预设导出文件创建器
    val presetExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            // 先设置状态显示加载对话框
            isExportingPreset = true
            scope.launch {
                // 让出时间片，确保UI有时间显示加载对话框
                yield()
                AppLogger.markOperation("导出预设")
                AppLogger.logStorageUsage(context, "导出预设前")
                try {
                    // 在后台线程执行耗时操作
                    val presetData = withContext(Dispatchers.IO) {
                        presetManager.createPresetData(
                            title = tierListTitle,
                            author = authorName,
                            tiers = tiers,
                            tierImages = tierImages,
                            pendingImages = pendingImages,
                            cropPositionX = settingsService.cropPositionX,
                             cropPositionY = settingsService.cropPositionY,
                            customCropWidth = settingsService.customCropWidth,
                            customCropHeight = settingsService.customCropHeight,
                            useCustomCropSize = settingsService.useCustomCropSize,
                            cropRatio = settingsService.cropRatio
                        )
                    }
                    val outputFile = File(context.cacheDir, "${pendingPresetName}.tdds")
                    withContext(Dispatchers.IO) {
                        presetManager.exportPreset(presetData, outputFile)

                        // 复制到用户选择的位置
                        // 使用 "rwt" 模式确保可以覆盖已存在的文件
                        context.contentResolver.openOutputStream(uri, "rwt")?.use { output ->
                            java.io.FileInputStream(outputFile).use { input ->
                                input.copyTo(output)
                            }
                        }
                        outputFile.delete()
                    }

                    showToastWithoutIcon(context, context.getString(R.string.preset_export_success))
                    AppLogger.i("导出预设成功: $pendingPresetName")
                    AppLogger.logStorageUsage(context, "导出预设后")
                } catch (e: Exception) {
                    AppLogger.e("导出预设失败", e)
                    showToastWithoutIcon(
                        context,
                        context.getString(R.string.preset_export_failed, e.message),
                        Toast.LENGTH_LONG
                    )
                }
                isExportingPreset = false
                pendingPresetName = ""
                // 恢复草稿保存
                onResumeDraftSave?.invoke()
            }
        } ?: run {
            // 用户取消了选择，恢复草稿保存
            onResumeDraftSave?.invoke()
        }
    }

    // 图包导出文件创建器
    val packageExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { outputUri ->
            dialogState.packageToExport?.let { packageItem ->
                dialogState.isExportingPackage = true
                scope.launch {
                    AppLogger.markOperation("导出图包")
                    AppLogger.logStorageUsage(context, "导出图包前")
                    try {
                        val success = ResourcePackageManager.exportPackageAsWebP(
                            context,
                            packageItem.file,
                            outputUri
                        )
                        if (success) {
                            showToastWithoutIcon(context, context.getString(R.string.package_export_success))
                            AppLogger.i("导出图包成功: ${packageItem.name}")
                        } else {
                            showToastWithoutIcon(
                                context,
                                context.getString(R.string.package_export_failed),
                                Toast.LENGTH_LONG
                            )
                        }
                        AppLogger.logStorageUsage(context, "导出图包后")
                    } catch (e: Exception) {
                        AppLogger.e("导出图包失败", e)
                        showToastWithoutIcon(
                            context,
                            context.getString(R.string.package_export_failed, e.message),
                            Toast.LENGTH_LONG
                        )
                    }
                    dialogState.isExportingPackage = false
                    dialogState.packageToExport = null
                }
            }
        }
    }

    // 待添加图片拖动状态
    var isDraggingPendingImage by remember { mutableStateOf(false) }
    var draggedPendingImageUri by remember { mutableStateOf<Uri?>(null) }

    // 图片选择器（多选）- 支持WebP转换和哈希查重
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            AppLogger.markOperation("选择图片")
            AppLogger.logStorageUsage(context, "选择图片前")

            // 在协程中处理图片导入（支持WebP转换和哈希查重）
            scope.launch {
                val workImagesDir = presetManager.getWorkImagesDirectory()
                val imagesDir = File(workImagesDir, "images")
                imagesDir.mkdirs()

                // 构建已有文件的哈希映射表
                val existingHashes = mutableMapOf<String, File>()
                imagesDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        try {
                            val hash = ImageResourceManager.calculateQuickHash(file)
                            existingHashes[hash] = file
                        } catch (e: Exception) {
                            // 忽略计算失败的文件
                        }
                    }
                }

                val importedUris = mutableListOf<Uri>()
                var convertedCount = 0
                var reusedCount = 0

                uris.forEach { uri ->
                    val result = ImageResourceManager.importImageWithWebPAndHash(
                        context,
                        uri,
                        imagesDir,
                        existingHashes,
                        convertToWebP = true
                    )

                    if (result.fileName != null) {
                        val file = File(imagesDir, result.fileName)
                        importedUris.add(Uri.fromFile(file))
                        if (result.isConvertedToWebP) convertedCount++
                        if (result.isReused) reusedCount++
                    }
                }

                pendingImages = importedUris
                AppLogger.i("选择图片: ${importedUris.size} 张 (WebP转换: $convertedCount, 复用: $reusedCount)")
                AppLogger.logStorageUsage(context, "选择图片后")

                // 重置防重复点击状态
                dialogState.isImagePickerLaunching = false
                // 恢复草稿保存
                onResumeDraftSave?.invoke()
            }
        } else {
            // 重置防重复点击状态
            dialogState.isImagePickerLaunching = false
            // 恢复草稿保存
            onResumeDraftSave?.invoke()
        }
    }

    // 图片选择器（多选，用于添加到待分级区域）- 支持WebP转换和哈希查重
    val addToPendingPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            AppLogger.markOperation("添加图片")
            AppLogger.logStorageUsage(context, "添加图片前")

            // 在协程中处理图片导入（支持WebP转换和哈希查重）
            scope.launch {
                val workImagesDir = presetManager.getWorkImagesDirectory()
                val imagesDir = File(workImagesDir, "images")
                imagesDir.mkdirs()

                // 构建已有文件的哈希映射表（包括现有的pendingImages）
                val existingHashes = mutableMapOf<String, File>()
                imagesDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        try {
                            val hash = ImageResourceManager.calculateQuickHash(file)
                            existingHashes[hash] = file
                        } catch (e: Exception) {
                            // 忽略计算失败的文件
                        }
                    }
                }

                val importedUris = mutableListOf<Uri>()
                var convertedCount = 0
                var reusedCount = 0

                uris.forEach { uri ->
                    // 检查URI是否已经在pendingImages中（简单URI去重）
                    if (uri in pendingImages) {
                        reusedCount++
                        return@forEach
                    }

                    val result = ImageResourceManager.importImageWithWebPAndHash(
                        context,
                        uri,
                        imagesDir,
                        existingHashes,
                        convertToWebP = true
                    )

                    if (result.fileName != null) {
                        val file = File(imagesDir, result.fileName)
                        importedUris.add(Uri.fromFile(file))
                        if (result.isConvertedToWebP) convertedCount++
                        if (result.isReused) reusedCount++
                    }
                }

                // 追加到现有待分级图片中（允许重复URI，通过哈希查重实现文件复用）
                pendingImages = pendingImages + importedUris
                AppLogger.i("添加图片到待分级区域: ${importedUris.size} 张 (WebP转换: $convertedCount, 复用: $reusedCount)，现有 ${pendingImages.size} 张")
                AppLogger.logStorageUsage(context, "添加图片后")

                // 显示添加成功提示
                showToastWithoutIcon(context, context.getString(R.string.images_added, importedUris.size))

                // 重置防重复点击状态
                dialogState.isImagePickerLaunching = false
                // 恢复草稿保存
                onResumeDraftSave?.invoke()
            }
        } else {
            // 重置防重复点击状态
            dialogState.isImagePickerLaunching = false
            // 恢复草稿保存
            onResumeDraftSave?.invoke()
        }
    }

    // 图片选择器（单选，用于替换）
    val replaceImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && dialogState.imageToReplace != null) {
            val index = tierImages.indexOfFirst { it.id == dialogState.imageToReplace!!.id }
            if (index != -1) {
                val oldImage = tierImages[index]
                
                // 在协程中处理文件复制
                scope.launch {
                    try {
                        // 将新图片复制到工作目录
                        val workImagesDir = File(presetManager.getWorkImagesDirectory(), PresetManager.IMAGES_FOLDER_NAME)
                        workImagesDir.mkdirs()
                        
                        // 生成新文件名
                        val newFileName = "replaced_${System.currentTimeMillis()}.webp"
                        val destFile = File(workImagesDir, newFileName)
                        
                        // 复制图片到工作目录
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        val newUri = Uri.fromFile(destFile)
                        
                        // 返回原图到待分级区域（增加计数）
                        val uriToReturn = oldImage.originalUri ?: oldImage.uri
                        pendingImages = pendingImages + uriToReturn
                        
                        // 替换为新图片，使用工作目录中的文件URI
                        tierImages[index] = tierImages[index].copy(
                            uri = newUri,
                            originalUri = newUri,
                            cropPositionX = 0.5f,
                            cropPositionY = 0.5f,
                            cropScale = 1.0f,
                            isCropped = false,
                            cropRatio = 0f,
                            useCustomCrop = false,
                            customCropWidth = 0,
                            customCropHeight = 0
                        )
                        
                        AppLogger.i("替换图片: 新图片已复制到工作目录并替换，文件名: $newFileName")
                    } catch (e: Exception) {
                        AppLogger.e("替换图片失败: ${e.message}", e)
                        // 如果复制失败，回退到原来的方式（直接使用URI）
                        val uriToReturn = oldImage.originalUri ?: oldImage.uri
                        pendingImages = pendingImages + uriToReturn
                        tierImages[index] = tierImages[index].copy(
                            uri = uri,
                            originalUri = uri,
                            cropPositionX = 0.5f,
                            cropPositionY = 0.5f,
                            cropScale = 1.0f,
                            isCropped = false,
                            cropRatio = 0f,
                            useCustomCrop = false,
                            customCropWidth = 0,
                            customCropHeight = 0
                        )
                    }
                }
            }
            dialogState.imageToReplace = null
        }
    }

    // 图片选择器（单选，用于选择小图标设置到具体槽位）
    val badgeImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            if (dialogState.badgeSelectionTarget == 0) {
                // 添加小图标到工作目录（不设置到具体图片）- 单选情况
                scope.launch {
                    try {
                        val workBadgesDir = File(presetManager.getWorkImagesDirectory(), PresetManager.BADGES_FOLDER_NAME)
                        workBadgesDir.mkdirs()
                        // 从URI获取原始文件名，保留用户排序
                        val originalFileName = FileUtils.getFileNameFromUri(context, uri)
                        val fileName = originalFileName ?: "${System.currentTimeMillis()}.png"
                        val destFile = File(workBadgesDir, fileName)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        AppLogger.i("添加小图标到工作目录: ${destFile.absolutePath}")
                        // 刷新对话框以显示新添加的小图标
                        dialogState.badgeDialogRefreshKey++
                        showToastWithoutIcon(context, context.getString(R.string.badge_added))
                    } catch (e: Exception) {
                        AppLogger.e("添加小图标失败: ${e.message}")
                        showToastWithoutIcon(context, context.getString(R.string.badge_add_failed, e.message), Toast.LENGTH_LONG)
                    } finally {
                        // 重置防重复点击状态
                        dialogState.isBadgePickerLaunching = false
                        // 恢复草稿保存
                        onResumeDraftSave?.invoke()
                    }
                }
            } else if (dialogState.imageForBadge != null) {
                // 为图片添加小图标，同时保存到工作目录
                scope.launch {
                    try {
                        // 先将小图标复制到工作目录
                        val workBadgesDir = File(presetManager.getWorkImagesDirectory(), PresetManager.BADGES_FOLDER_NAME)
                        workBadgesDir.mkdirs()
                        // 从URI获取原始文件名，保留用户排序
                        val originalFileName = FileUtils.getFileNameFromUri(context, uri)
                        val fileName = originalFileName ?: "${System.currentTimeMillis()}.png"
                        val destFile = File(workBadgesDir, fileName)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        // 使用工作目录中的文件URI
                        val workUri = Uri.fromFile(destFile)
                        val index = tierImages.indexOfFirst { it.id == dialogState.imageForBadge!!.id }
                        if (index != -1) {
                            when (dialogState.badgeSelectionTarget) {
                                1 -> tierImages[index] = tierImages[index].copy(badgeUri = workUri)
                                2 -> tierImages[index] = tierImages[index].copy(badgeUri2 = workUri)
                                3 -> tierImages[index] = tierImages[index].copy(badgeUri3 = workUri)
                            }
                            AppLogger.i("为图片添加小图标${dialogState.badgeSelectionTarget}: ${dialogState.imageForBadge!!.id}, 已保存到工作目录")
                            // 更新 imageForBadge 为最新的对象引用
                            dialogState.imageForBadge = tierImages[index]
                        }
                        // 刷新小图标预览区域
                        dialogState.badgeDialogRefreshKey++
                    } catch (e: Exception) {
                        AppLogger.e("添加小图标到工作目录失败: ${e.message}")
                        // 如果保存失败，仍然使用原始URI
                        val index = tierImages.indexOfFirst { it.id == dialogState.imageForBadge!!.id }
                        if (index != -1) {
                            when (dialogState.badgeSelectionTarget) {
                                1 -> tierImages[index] = tierImages[index].copy(badgeUri = uri)
                                2 -> tierImages[index] = tierImages[index].copy(badgeUri2 = uri)
                                3 -> tierImages[index] = tierImages[index].copy(badgeUri3 = uri)
                            }
                            dialogState.imageForBadge = tierImages[index]
                        }
                        // 刷新小图标预览区域
                        dialogState.badgeDialogRefreshKey++
                    } finally {
                        // 重置选择目标（在协程完成后执行）
                        dialogState.badgeSelectionTarget = 0
                        // 重置防重复点击状态
                        dialogState.isBadgePickerLaunching = false
                        // 恢复草稿保存
                        onResumeDraftSave?.invoke()
                    }
                }
            } else {
                // imageForBadge 为 null，重置状态
                dialogState.badgeSelectionTarget = 0
                dialogState.isBadgePickerLaunching = false
                // 恢复草稿保存
                onResumeDraftSave?.invoke()
            }
        } else {
            // uri 为 null，重置状态
            dialogState.badgeSelectionTarget = 0
            dialogState.isBadgePickerLaunching = false
            // 恢复草稿保存
            onResumeDraftSave?.invoke()
        }
    }

    // 图片选择器（多选，用于批量添加小图标到工作目录，最多20张）
    val badgeImagePickerMultiple = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                var successCount = 0
                var failCount = 0
                uris.forEach { uri ->
                    try {
                        val workBadgesDir = File(presetManager.getWorkImagesDirectory(), PresetManager.BADGES_FOLDER_NAME)
                        workBadgesDir.mkdirs()
                        // 从URI获取原始文件名，保留用户排序
                        val originalFileName = FileUtils.getFileNameFromUri(context, uri)
                        val fileName = originalFileName ?: "${System.currentTimeMillis()}_${successCount}.png"
                        val destFile = File(workBadgesDir, fileName)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        successCount++
                    } catch (e: Exception) {
                        failCount++
                        AppLogger.e("批量添加小图标失败: ${e.message}")
                    }
                }
                // 刷新对话框以显示新添加的小图标
                dialogState.badgeDialogRefreshKey++
                // 使用统计日志格式
                AppLogger.i("批量添加小图标完成: 成功${successCount}张${if (failCount > 0) ", 失败${failCount}张" else ""}")
                if (successCount > 0) {
                    showToastWithoutIcon(context, context.getString(R.string.badges_added, successCount))
                }
                if (failCount > 0) {
                    showToastWithoutIcon(context, context.getString(R.string.badges_add_failed_partial, failCount), Toast.LENGTH_LONG)
                }
            }
        }
        // 重置防重复点击状态
        dialogState.isBadgePickerLaunching = false
        // 恢复草稿保存
        onResumeDraftSave?.invoke()
    }

    // 权限申请（不自动打开图片选择器）
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 权限申请完成后，如果是首次启动则显示语言选择对话框
        if (shouldShowLanguageOnFirstLaunch) {
            dialogState.showLanguageDialog = true
            // 标记已显示过语言选择对话框
            settingsService.showLanguageOnFirstLaunch = false
        }
        // 重置所有防重复点击状态（权限申请完成后，无论成功与否）
        dialogState.isBadgePickerLaunching = false
        dialogState.isImagePickerLaunching = false
        AppLogger.d("权限申请完成，重置防重复点击状态: granted=$isGranted")
    }

    // 应用启动时检查权限（仅申请权限，不自动打开图片选择器）
    LaunchedEffect(Unit) {
        if (!PermissionUtils.hasStoragePermission(context)) {
            PermissionUtils.requestStoragePermission(permissionLauncher)
        } else {
            // 已有权限，如果是首次启动则显示语言选择对话框
            if (shouldShowLanguageOnFirstLaunch) {
                dialogState.showLanguageDialog = true
                // 标记已显示过语言选择对话框
                settingsService.showLanguageOnFirstLaunch = false
            }
        }
    }

    // 对话框事件处理器（必须在 permissionLauncher 和 imagePicker 等定义之后初始化）
    val dialogHandlers = remember {
        DialogHandlers(
            context = context,
            dialogState = dialogState,
            scope = scope,
            settingsService = settingsService,
            presetManager = presetManager,
            tierImages = tierImages,
            onTierImagesChange = { /* tierImages 是 mutableStateList，变更会自动触发重组 */ },
            onPendingImagesChange = { pendingImages = it },
            onSkipDraftSave = onSkipDraftSave,
            onResumeDraftSave = onResumeDraftSave,
            imagePicker = imagePicker,
            replaceImagePicker = replaceImagePicker,
            launchBadgePicker = { target ->
                if (!dialogState.isBadgePickerLaunching) {
                    dialogState.isBadgePickerLaunching = true
                    dialogState.badgeSelectionTarget = target
                    dialogState.imageForBadge?.let { image ->
                        AppLogger.i("选择小图标$target: ${image.id}")
                    }
                    withStoragePermission(
                        context = context,
                        permissionLauncher = permissionLauncher,
                        onSkipDraftSave = onSkipDraftSave
                    ) {
                        badgeImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
            },
            launchBadgePickerForAdding = {
                if (!dialogState.isBadgePickerLaunching) {
                    dialogState.isBadgePickerLaunching = true
                    dialogState.badgeSelectionTarget = 0 // 0 表示添加到工作目录，不设置到具体槽位
                    AppLogger.i("添加新小图标到工作目录（多选模式，最多20张）")
                    withStoragePermission(
                        context = context,
                        permissionLauncher = permissionLauncher,
                        onSkipDraftSave = onSkipDraftSave
                    ) {
                        badgeImagePickerMultiple.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
            },
            deleteBadge = { target ->
                val index = tierImages.indexOfFirst { it.id == dialogState.imageForBadge?.id }
                if (index != -1) {
                    tierImages[index] = when (target) {
                        1 -> tierImages[index].copy(badgeUri = null)
                        2 -> tierImages[index].copy(badgeUri2 = null)
                        3 -> tierImages[index].copy(badgeUri3 = null)
                        else -> tierImages[index]
                    }
                    AppLogger.d("删除小图标$target - 图片ID: ${dialogState.imageForBadge?.id}")
                    dialogState.imageForBadge = tierImages[index]
                }
            },
            deleteBadgeFile = { badgeUri, pm ->
                try {
                    AppLogger.d("尝试删除小图标 - URI: $badgeUri, Path: ${badgeUri.path}")
                    val path = badgeUri.path ?: run {
                        AppLogger.w("小图标URI path为空")
                        return@DialogHandlers false
                    }
                    val file = File(path)
                    AppLogger.d("小图标文件对象 - 路径: ${file.absolutePath}, 是否存在: ${file.exists()}")
                    if (file.exists()) {
                        val deleted = file.delete()
                        if (deleted) {
                            AppLogger.i("删除小图标文件成功: ${file.name}")
                        } else {
                            AppLogger.w("删除小图标文件失败: ${file.name} (文件可能正在被使用或权限不足)")
                        }
                        deleted
                    } else {
                        AppLogger.w("小图标文件不存在: ${file.absolutePath}")
                        false
                    }
                } catch (e: Exception) {
                    AppLogger.e("删除小图标文件异常: ${e.message}", e)
                    false
                }
            }
        )
    }

    // 双击返回退出应用
    var backPressedTime by remember { mutableLongStateOf(0L) }
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime > 2000) {
            backPressedTime = currentTime
        } else {
            // 双击退出：保存草稿并清理资源
            onExitApp?.invoke()
        }
    }

    val extendedColors = LocalExtendedColors.current

    // 草稿恢复对话框
    if (dialogState.showDraftRestoreDialog && draftConfigData != null) {
        DraftRestoreDialog(
            title = draftConfigData!!.title,
            author = draftConfigData!!.author,
            onDismiss = {
                // 取消恢复，清理草稿
                presetManager.cleanupDraft()
                dialogState.showDraftRestoreDialog = false
                draftConfigData = null
                AppLogger.i("用户取消恢复草稿")
            },
            onRestore = {
                // 用户确认恢复，先关闭恢复对话框，显示加载对话框
                dialogState.showDraftRestoreDialog = false
                dialogState.showDraftLoadingDialog = true
                AppLogger.i("用户确认恢复草稿，开始加载...")

                scope.launch {
                    // 让出时间片，确保UI有时间显示加载对话框
                    yield()
                    try {
                        val draftFile = presetManager.obtainDraftFile()
                        if (draftFile != null) {
                            // 在后台线程执行耗时操作
                            val result = withContext(Dispatchers.IO) {
                                presetManager.restoreDraft(draftFile)
                            }
                            // 更新UI状态
                            tiers.clear()
                            tiers.addAll(result.tiers.map { tierData ->
                                TierItem(tierData.label, try {
                                    Color(android.graphics.Color.parseColor("#${tierData.color}"))
                                } catch (e: Exception) { Color.Gray })
                            })
                            tierImages.clear()
                            tierImages.addAll(result.tierImages.map { appliedImage ->
                                TierImage(
                                    id = appliedImage.id,
                                    tierLabel = appliedImage.tierLabel,
                                    uri = appliedImage.uri,
                                    name = appliedImage.name,
                                    badgeUri = appliedImage.badgeUri,
                                    badgeUri2 = appliedImage.badgeUri2,
                                    badgeUri3 = appliedImage.badgeUri3,
                                    originalUri = appliedImage.originalUri,
                                    cropPositionX = appliedImage.cropPositionX,
                                    cropPositionY = appliedImage.cropPositionY,
                                    cropScale = appliedImage.cropScale,
                                    isCropped = appliedImage.isCropped,
                                    cropRatio = appliedImage.cropRatio,
                                    useCustomCrop = appliedImage.useCustomCrop,
                                    customCropWidth = appliedImage.customCropWidth,
                                    customCropHeight = appliedImage.customCropHeight
                                )
                            })
                            pendingImages = result.pendingImages
                            tierListTitle = result.title
                            authorName = result.author
                            // 清理旧的裁剪设置并应用新的
                            settingsService.clearCropSettings()
                            settingsService.customCropWidth = result.customCropWidth
                            settingsService.customCropHeight = result.customCropHeight
                            settingsService.useCustomCropSize = result.useCustomCropSize
                            settingsService.cropRatio = result.cropRatio
                            // 清空层级位置信息,确保使用草稿中的层级标签
                            tierRowPositions = emptyMap()
                            dialogState.showDraftLoadingDialog = false
                            draftConfigData = null

                            showToastWithoutIcon(context, context.getString(R.string.draft_restored))
                            AppLogger.i("用户恢复草稿成功")
                        } else {
                            throw IllegalStateException("加载草稿失败")
                        }
                    } catch (e: Exception) {
                        AppLogger.e("恢复草稿失败", e)
                        dialogState.showDraftLoadingDialog = false
                        draftConfigData = null
                        showToastWithoutIcon(context, context.getString(R.string.draft_restore_failed))
                    }
                }
            }
        )
    }

    // 草稿加载中对话框
    if (dialogState.showDraftLoadingDialog) {
        LoadingDialog(message = stringResource(R.string.loading_resources))
    }

    Scaffold(
        containerColor = extendedColors.background,
        topBar = {
            // 使用自定义布局实现标题真正居中
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(extendedColors.background)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // 左侧按钮组
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 主题切换按钮
                    IconButton(onClick = {
                        val newTheme = !isDarkTheme
                        onThemeChange(newTheme)
                        AppLogger.i("切换主题: ${if (newTheme) "深色" else "浅色"}")
                    }) {
                        // 当前深色主题显示太阳(切换到浅色),当前浅色主题显示月亮(切换到深色)
                        val iconRes = if (isDarkTheme) R.drawable.ic_sun_light else R.drawable.ic_moon_light
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = if (isDarkTheme) stringResource(R.string.switch_to_light_theme) else stringResource(R.string.switch_to_dark_theme),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 居中的标题
                Text(
                    text = "-$tierListTitle-",
                    modifier = Modifier.clickable { dialogState.showEditTitleDialog = true },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge
                )

                // 右侧按钮组
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 资源管理按钮
                    IconButton(onClick = { dialogState.showResourceManageDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_resource_manage),
                            contentDescription = stringResource(R.string.resource_management),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // 功能菜单按钮
                    IconButton(onClick = { dialogState.showSettingsMenu = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_menu_light),
                            contentDescription = stringResource(R.string.settings),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        bottomBar = {
            // 底部按钮栏 - 现代化设计
            val buttonHeight = 48.dp
            val buttonFontSize = 16.sp
            val horizontalPadding = 16.dp
            val verticalPadding = 12.dp
            val buttonSpacing = 12.dp
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(extendedColors.background)
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing, Alignment.CenterHorizontally)
            ) {
                // 保存按钮 - 使用轮廓样式
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            dialogState.previewIsDarkTheme = isDarkTheme
                            val bitmap = generateTierListBitmap(context, tiers, tierImages, tierListTitle, authorName, dialogState.previewIsDarkTheme, externalBadgeEnabled, disableCustomFont, nameBelowImage)
                            dialogState.previewBitmap = bitmap
                            dialogState.showPreviewDialog = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outline
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        fontSize = buttonFontSize,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 重置按钮 - 使用轮廓样式
                OutlinedButton(
                    onClick = {
                        // 防止重复点击
                        if (dialogState.isResetting) return@OutlinedButton
                        
                        // 检查是否已经是默认状态（默认模板且无图片）
                        val isDefaultState = tierImages.isEmpty() && 
                                            tiers.size == defaultTiers.size &&
                                            tiers.zip(defaultTiers).all { (current, default) ->
                                                current.label == default.label && current.color == default.color
                                            }
                        
                        if (isDefaultState) {
                            // 已经是默认状态,直接返回不执行任何操作
                            return@OutlinedButton
                        }
                        
                        dialogState.isResetting = true
                        
                        // 将层级中的图片返回到待分级区域（原样返回所有图片，包括重复的）
                        val imagesToReturn = tierImages.map { it.originalUri ?: it.uri }
                        
                        // 重置层级为默认模板
                        tiers.clear()
                        tiers.addAll(defaultTiers)
                        tierImages.clear()
                        // 清空层级位置信息,确保使用默认模板的层级标签
                        tierRowPositions = emptyMap()
                        // 清理裁剪设置
                        settingsService.clearCropSettings()

                        // 将层级图片添加到待分级区域（原样返回所有图片，包括重复的）
                        if (imagesToReturn.isNotEmpty()) {
                            pendingImages = pendingImages + imagesToReturn
                            AppLogger.i("重置时将 ${imagesToReturn.size} 张图片返回到待分级区域")
                        }

                        tierListTitle = context.getString(R.string.default_title)
                        authorName = ""
                        AppLogger.i("重置梯度表完成，待分级区域 ${pendingImages.size} 张图片")
                        showToastWithoutIcon(context, context.getString(R.string.reset_success))
                        
                        // 延迟重置状态，防止快速连续点击
                        scope.launch {
                            delay(500)
                            dialogState.isResetting = false
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outline
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = stringResource(R.string.reset),
                        fontSize = buttonFontSize,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(extendedColors.background)
                .padding(innerPadding)
        ) {
            // 待添加图片区域（固定显示）
            PendingImagesSection(
                images = pendingImages,
                tiers = tiers,
                tierRowPositions = tierRowPositions,
                onClear = {
                        // 清理待添加图片的资源文件（保留层级中正在使用的图片）
                        val clearedCount = pendingImages.size
                        try {
                            val tierImageUris = tierImages.map { it.uri }
                            var cleanedCount = 0
                            pendingImages.forEach { uri ->
                                // 只清理不在层级中使用的图片
                                if (uri !in tierImageUris) {
                                    val fileName = uri.lastPathSegment
                                    if (fileName != null && (fileName.startsWith("imported_") || fileName.startsWith("builtin_"))) {
                                        val file = File(context.filesDir, fileName)
                                        if (file.exists() && file.delete()) {
                                            cleanedCount++
                                        }
                                    }
                                }
                            }
                            AppLogger.i("清空待添加图片 - 清理资源文件: ${cleanedCount}个")
                        } catch (e: Exception) {
                            AppLogger.e("清空待添加图片时清理资源失败", e)
                        }
                        pendingImages = emptyList()
                        // 显示清空提示
                        if (clearedCount > 0) {
                            showToastWithoutIcon(context, context.getString(R.string.images_cleared, clearedCount))
                        }
                    },
                onAdd = {
                    // 打开图片选择器添加图片到待分级区域
                    if (!dialogState.isImagePickerLaunching) {
                        dialogState.isImagePickerLaunching = true
                        withStoragePermission(
                            context = context,
                            permissionLauncher = permissionLauncher,
                            onSkipDraftSave = onSkipDraftSave
                        ) {
                            addToPendingPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    }
                },
                onDragStart = { uri ->
                    isDraggingPendingImage = true
                    draggedPendingImageUri = uri
                },
                onDragEnd = {
                    isDraggingPendingImage = false
                    draggedPendingImageUri = null
                },
                onDropOnTier = { uri, tierLabel ->
                    tierImages.add(TierImage(UUID.randomUUID().toString(), tierLabel, uri))
                    // 只移除待分级区域中的一个匹配URI（支持同一URI的多张图片）
                    val index = pendingImages.indexOfFirst { it == uri }
                    if (index != -1) {
                        pendingImages = pendingImages.toMutableList().apply { removeAt(index) }
                    }
                    AppLogger.i("拖动添加图片到层级: $tierLabel")
                },
                onDeleteImage = { uri ->
                    // 从待选区删除图片（只移除一个匹配项，支持同一URI的多张图片）
                    val index = pendingImages.indexOfFirst { it == uri }
                    if (index != -1) {
                        pendingImages = pendingImages.toMutableList().apply { removeAt(index) }
                    }
                    // 清理资源文件
                    try {
                        val fileName = uri.lastPathSegment
                        if (fileName != null && (fileName.startsWith("imported_") || fileName.startsWith("builtin_"))) {
                            val file = File(context.filesDir, fileName)
                            if (file.exists() && file.delete()) {
                                AppLogger.d("删除待选区图片并清理资源文件: $fileName")
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("删除待选区图片时清理资源失败", e)
                    }
                },
                floatOffsetX = floatOffsetX,
                floatOffsetY = floatOffsetY,
                onPositionUpdate = { rect ->
                    pendingSectionRect = rect
                }
            )
            
            // 梯度表主体
            val listState = rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
                // 交换层级位置
                val fromIndex = from.index
                val toIndex = to.index
                if (fromIndex in tiers.indices && toIndex in tiers.indices) {
                    val tier = tiers.removeAt(fromIndex)
                    tiers.add(toIndex, tier)
                    AppLogger.i("调整层级顺序: $fromIndex -> $toIndex")
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(tiers, key = { _, tier -> tier.label }) { index, tier ->
                    val tierImageList = tierImages.filter { it.tierLabel == tier.label }
                    ReorderableItem(reorderableState, key = tier.label) { isDragging ->
                        SwipeableTierRow(
                            tier = tier,
                            isDragging = isDragging,
                            images = tierImageList,
                        pendingImages = if (isDraggingPendingImage) emptyList() else pendingImages,
                        onTierClick = {
                            dialogState.editingTier = tier
                            dialogState.showEditNameDialog = true
                            AppLogger.i("点击层级编辑名称: ${tier.label}")
                        },
                        onTierLongClick = {
                            // 长按功能未实现，仅作为占位回调
                        },
                        onTierDoubleClick = {
                            dialogState.editingTier = tier
                            dialogState.showColorPickerDialog = true
                            AppLogger.i("双击层级编辑颜色: ${tier.label}")
                        },
                        onAddImage = { uri ->
                            // 正常点击模式
                            tierImages.add(TierImage(UUID.randomUUID().toString(), tier.label, uri))
                            // 只移除待分级区域中的一个匹配URI（支持同一URI的多张图片）
                            val index = pendingImages.indexOfFirst { it == uri }
                            if (index != -1) {
                                pendingImages = pendingImages.toMutableList().apply { removeAt(index) }
                            }
                            AppLogger.i("添加图片到层级: ${tier.label}")
                        },
                        onPositionUpdate = { tierLabel, rect ->
                            tierRowPositions = tierRowPositions + (tierLabel to rect)
                        },
                        selectedImageForDrag = selectedImageForDrag,
                        onImageClick = { image, imgIndex ->
                            if (selectedImageForDrag == null) {
                                // 没有选中图片，单击打开操作对话框
                                dialogState.selectedImageForAction = image
                                dialogState.showImageActionDialog = true
                                AppLogger.i("单击图片打开操作对话框: ${image.tierLabel}")
                            } else if (selectedImageForDrag!!.id == image.id) {
                                // 单击已选中的图片，取消选中
                                selectedImageForDrag = null
                                AppLogger.i("取消选中图片: ${image.tierLabel}")
                            } else {
                                // 已选中图片，单击其他图片交换位置
                                ImageOperationUtils.swapImageContents(
                                    tierImages = tierImages,
                                    fromId = selectedImageForDrag!!.id,
                                    toId = image.id,
                                    onImageForActionUpdate = { updatedImage ->
                                        if (dialogState.selectedImageForAction?.id == updatedImage.id) {
                                            dialogState.selectedImageForAction = updatedImage
                                        }
                                    },
                                    onImageToReplaceUpdate = { updatedImage ->
                                        if (dialogState.imageToReplace?.id == updatedImage.id) {
                                            dialogState.imageToReplace = updatedImage
                                        }
                                    },
                                    onImageForBadgeUpdate = { updatedImage ->
                                        if (dialogState.imageForBadge?.id == updatedImage.id) {
                                            dialogState.imageForBadge = updatedImage
                                        }
                                    }
                                )
                                // 交换后取消选中状态
                                selectedImageForDrag = null
                            }
                        },
                        onImageLongClick = { image, imgIndex ->
                            // 长按功能未实现，仅作为占位回调
                        },
                        onImageDoubleClick = { image, imgIndex ->
                            if (selectedImageForDrag == null) {
                                // 没有选中图片，双击选中图片
                                selectedImageForDrag = image
                                AppLogger.i("双击选中图片: ${image.tierLabel}")
                            } else if (selectedImageForDrag!!.id == image.id) {
                                // 双击已选中的图片，取消选中
                                selectedImageForDrag = null
                                AppLogger.i("双击取消选中图片: ${image.tierLabel}")
                            } else {
                                // 已选中图片，双击其他图片交换位置并取消选中
                                ImageOperationUtils.swapImageContents(
                                    tierImages = tierImages,
                                    fromId = selectedImageForDrag!!.id,
                                    toId = image.id,
                                    onImageForActionUpdate = { updatedImage ->
                                        if (dialogState.selectedImageForAction?.id == updatedImage.id) {
                                            dialogState.selectedImageForAction = updatedImage
                                        }
                                    },
                                    onImageToReplaceUpdate = { updatedImage ->
                                        if (dialogState.imageToReplace?.id == updatedImage.id) {
                                            dialogState.imageToReplace = updatedImage
                                        }
                                    },
                                    onImageForBadgeUpdate = { updatedImage ->
                                        if (dialogState.imageForBadge?.id == updatedImage.id) {
                                            dialogState.imageForBadge = updatedImage
                                        }
                                    }
                                )
                                selectedImageForDrag = null
                            }
                        },
                        onDeleteTier = {
                            // 使用该层级的标签查找当前索引（避免交换图片后索引过期）
                            val currentIndex = tiers.indexOfFirst { it.label == tier.label }
                            if (currentIndex != -1) {
                                // 将该层级的所有图片返回到待放置区（原样返回所有图片，包括重复的）
                                val imagesToReturn = tierImages.filter { it.tierLabel == tier.label }
                                val returnUris = imagesToReturn.map { it.originalUri ?: it.uri }
                                if (returnUris.isNotEmpty()) {
                                    pendingImages = pendingImages + returnUris
                                }
                                val returnedCount = returnUris.size
                                val deletedCount = imagesToReturn.size
                                tiers.removeAt(currentIndex)
                                tierImages.removeAll { it.tierLabel == tier.label }
                                // 从 tierRowPositions 中移除该层级的位置信息
                                tierRowPositions = tierRowPositions - tier.label
                                AppLogger.i("删除层级 - 层级: ${tier.label}, 删除图片: ${deletedCount}张, 返回待放置区: ${returnedCount}张")
                            } else {
                                AppLogger.w("删除层级失败，找不到层级: ${tier.label}")
                            }
                        },
                        onPickImage = {
                            // 防止重复打开图片选择器
                            if (!dialogState.isImagePickerLaunching) {
                                dialogState.isImagePickerLaunching = true
                                withStoragePermission(
                                    context = context,
                                    permissionLauncher = permissionLauncher,
                                    onSkipDraftSave = onSkipDraftSave
                                ) {
                                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            }
                        },
                        disableClickAdd = disableClickAdd,
                        isDraggingPendingImage = isDraggingPendingImage,
                        onMoveSelectedImageToTier = {
                            // 移动选中的图片到当前层级
                            selectedImageForDrag?.let { selectedImage ->
                                ImageOperationUtils.moveImageToTier(
                                    tierImages = tierImages,
                                    imageId = selectedImage.id,
                                    targetTierLabel = tier.label
                                )
                                selectedImageForDrag = null
                            }
                        },
                        // 层级图片拖拽相关参数
                        tierRowPositions = tierRowPositions,
                        draggingTierImage = draggingTierImage,
                        onTierImageDragStart = { image, center ->
                            // 从 tierImages 列表中查找最新的图片对象,确保获取正确的 URI
                            val latestImage = tierImages.find { it.id == image.id } ?: image
                            draggingTierImage = latestImage
                            draggingTierImagePosition = center
                            AppLogger.i("开始拖拽层级图片: ${latestImage.tierLabel}, URI: ${latestImage.uri}")
                        },
                        onTierImageDrag = { center, targetTier ->
                            draggingTierImagePosition = center
                            draggingTierImageTarget = targetTier
                        },
                        onTierImageDragEnd = { image, targetTier ->
                            val index = tierImages.indexOfFirst { it.id == image.id }
                            if (index != -1) {
                                if (targetTier != null && targetTier != image.tierLabel) {
                                // 跨层级移动
                                ImageOperationUtils.moveImageToTier(
                                    tierImages = tierImages,
                                    imageId = image.id,
                                    targetTierLabel = targetTier
                                )
                            } else if (targetTier == null) {
                                    // 检查是否拖到了待分级区域
                                    val currentRect = pendingSectionRect
                                    if (currentRect != null) {
                                        val finalCenter = draggingTierImagePosition
                                        if (finalCenter.x >= currentRect.left && finalCenter.x <= currentRect.right &&
                                            finalCenter.y >= currentRect.top && finalCenter.y <= currentRect.bottom) {
                                            // 将图片移回待分级区域
                                            val removedImage = tierImages.removeAt(index)
                                            val uriToReturn = removedImage.originalUri ?: removedImage.uri

                                            // 注意：不删除小图标文件，因为小图标是全局资源，可能被其他图片使用
                                            // 只从当前图片移除小图标引用，让图片恢复为无小图标状态

                                            // 返回图片到待分级区域
                                            pendingImages = pendingImages + uriToReturn
                                            AppLogger.i("将图片从层级移回待分级区域: ${removedImage.tierLabel} -> pending, URI: $uriToReturn")
                                        }
                                    }
                                }
                            }
                            draggingTierImage = null
                            draggingTierImageTarget = null
                        },
                        // 列表状态用于自动滚动
                        listState = listState
                    )
                    }
                }

                // 添加新层级按钮
                item {
                    AddTierButton(
                        onClick = {
                            val newLabel = ColorUtils.generateNextLabel(tiers.map { it.label })
                            tiers.add(TierItem(newLabel, ColorUtils.generateRandomColor()))
                            AppLogger.i("添加新层级: $newLabel")
                        }
                    )
                }

                // 作者信息输入
                item {
                    AuthorInfoSection(
                        authorName = authorName,
                        onClick = { dialogState.showEditAuthorDialog = true }
                    )
                }
            }
        }
        
        // 浮动显示的层级图片拖拽
        if (draggingTierImage != null) {
            FloatingDragImage(
                uri = draggingTierImage!!.uri,
                position = draggingTierImagePosition,
                dropTarget = draggingTierImageTarget,
                floatOffsetX = floatOffsetX,
                floatOffsetY = floatOffsetY
            )
        }
        
        // 对话框集合
        TierListDialogs(
            dialogState = dialogState,
            handlers = dialogHandlers,
            context = context,
            scope = scope,
            settingsService = settingsService,
            presetManager = presetManager,
            tierImages = tierImages,
            tiers = tiers,
            tierListTitle = tierListTitle,
            authorName = authorName,
            pendingImages = pendingImages,
            defaultTiers = defaultTiers,
            tierRowPositions = tierRowPositions,
            disableClickAdd = disableClickAdd,
            floatOffsetX = floatOffsetX,
            floatOffsetY = floatOffsetY,
            externalBadgeEnabled = externalBadgeEnabled,
            followSystemTheme = followSystemTheme,
            disableCustomFont = disableCustomFont,
            nameBelowImage = nameBelowImage,
            isDarkTheme = isDarkTheme,
            currentLanguage = currentLanguage,
            onTitleChange = { tierListTitle = it },
            onAuthorChange = { authorName = it },
            onTiersChange = { /* tiers 是 mutableStateList，变更会自动触发重组 */ },
            onTierImagesChange = { /* tierImages 是 mutableStateList，变更会自动触发重组 */ },
            onPendingImagesChange = { pendingImages = it },
            onTierRowPositionsChange = { tierRowPositions = it },
            onDisableClickAddChange = { disableClickAdd = it },
            onFloatOffsetXChange = { floatOffsetX = it },
            onFloatOffsetYChange = { floatOffsetY = it },
            onExternalBadgeChange = { externalBadgeEnabled = it },
            onFollowSystemThemeChange = onFollowSystemThemeChange,
            onDisableCustomFontChange = onDisableCustomFontChange,
            onNameBelowImageChange = { nameBelowImage = it },
            onLanguageChange = { 
                currentLanguage = it
                languageChanged = true
            },
            onSkipDraftSave = onSkipDraftSave,
            onResumeDraftSave = onResumeDraftSave,
            presetExportLauncher = presetExportLauncher,
            packageExportLauncher = packageExportLauncher,
            presetFilePicker = presetFilePicker
        )

        // 加载资源中对话框（导入/导出/保存预设/导出图包）
        if (isImportingPreset || isExportingPreset || isSavingPreset || isExportingPackage) {
            LoadingDialog(message = stringResource(R.string.loading_resources))
        }

        // Toast宿主 - 显示全局Toast提示（使用Popup确保在最上层，覆盖对话框）
        ToastHost(isDarkTheme = isDarkTheme)
    }
}

// ImageCropDialog 和 CropState 已移至 components/ImageCropDialog.kt


