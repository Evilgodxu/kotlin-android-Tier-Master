package com.tdds.jh.ui.tierlist.handler

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.tdds.jh.manager.AppLogger
import com.tdds.jh.manager.PresetManager
import com.tdds.jh.resource.PackageItem
import com.tdds.jh.resource.PackageManager as ResourcePackageManager
import com.tdds.jh.ui.tierlist.state.DialogState
import com.tdds.jh.ui.toast.showToastWithoutIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 图包操作处理类
 * 封装图包导入、导出的业务逻辑
 */
class PackageOperationHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val dialogState: DialogState,
    private val presetManager: PresetManager,
    private val onPendingImagesChange: (List<Uri>) -> Unit,
    private val onSkipDraftSave: (() -> Unit)?,
    private val onResumeDraftSave: (() -> Unit)?,
    private val showToast: (String, Int) -> Unit
) {

    // ==================== 图包导出 ====================

    /**
     * 处理图包导出
     */
    fun handleExportPackage(
        uri: Uri?,
        packageItem: PackageItem.Imported?
    ) {
        if (uri == null || packageItem == null) {
            dialogState.packageToExport = null
            onResumeDraftSave?.invoke()
            return
        }

        dialogState.isExportingPackage = true
        onSkipDraftSave?.invoke()

        scope.launch {
            try {
                AppLogger.markOperation("导出图包")
                AppLogger.logStorageUsage(context, "导出图包前")

                val success = withContext(Dispatchers.IO) {
                    ResourcePackageManager.exportPackageAsWebP(
                        context,
                        packageItem.file,
                        uri
                    )
                }

                if (success) {
                    showToast("导出图包成功", android.widget.Toast.LENGTH_SHORT)
                    AppLogger.i("导出图包成功: ${packageItem.name}")
                } else {
                    showToast("导出图包失败", android.widget.Toast.LENGTH_LONG)
                }

                AppLogger.logStorageUsage(context, "导出图包后")

            } catch (e: Exception) {
                AppLogger.e("导出图包失败", e)
                showToast("导出图包失败: ${e.message}", android.widget.Toast.LENGTH_LONG)
            } finally {
                dialogState.isExportingPackage = false
                dialogState.packageToExport = null
                onResumeDraftSave?.invoke()
            }
        }
    }

    /**
     * 启动图包导出流程
     */
    fun startExportPackage(
        packageItem: PackageItem.Imported,
        onLaunchExport: () -> Unit
    ) {
        dialogState.packageToExport = packageItem
        onSkipDraftSave?.invoke()
        onLaunchExport()
    }

    // ==================== 图包导入 ====================

    /**
     * 处理从ZIP导入图片到待分级区域
     */
    fun handleImportZipToPending(
        zipUri: Uri?,
        password: String?,
        currentPendingImages: List<Uri>
    ) {
        if (zipUri == null) {
            dialogState.isImportingPackage = false
            onResumeDraftSave?.invoke()
            return
        }

        dialogState.isImportingPackage = true
        onSkipDraftSave?.invoke()

        scope.launch {
            try {
                AppLogger.markOperation("导入ZIP到待分级区域")

                val workImagesDir = presetManager.getWorkImagesDirectory()
                val imagesDir = java.io.File(workImagesDir, "images")

                val importedUris = withContext(Dispatchers.IO) {
                    ResourcePackageManager.importImagesFromZip(
                        context,
                        zipUri,
                        imagesDir,
                        password
                    )
                }

                // 追加到现有待分级图片
                onPendingImagesChange(currentPendingImages + importedUris)

                showToast("已导入 ${importedUris.size} 张图片", android.widget.Toast.LENGTH_SHORT)
                AppLogger.i("ZIP导入完成: ${importedUris.size} 张图片到待分级区域")

            } catch (e: com.tdds.jh.resource.ZipPasswordRequiredException) {
                AppLogger.w("ZIP需要密码")
                showToast("ZIP文件需要密码", android.widget.Toast.LENGTH_LONG)
                // 保持导入状态，等待用户输入密码
                return@launch
            } catch (e: Exception) {
                AppLogger.e("导入ZIP失败", e)
                showToast("导入失败: ${e.message}", android.widget.Toast.LENGTH_LONG)
            } finally {
                dialogState.isImportingPackage = false
                onResumeDraftSave?.invoke()
            }
        }
    }

    /**
     * 处理从ZIP导入图片到小图标区域
     */
    fun handleImportZipToBadges(
        zipUri: Uri?,
        password: String?,
        onBadgeDialogRefresh: () -> Unit
    ) {
        if (zipUri == null) {
            dialogState.isImportingPackage = false
            onResumeDraftSave?.invoke()
            return
        }

        dialogState.isImportingPackage = true
        onSkipDraftSave?.invoke()

        scope.launch {
            try {
                AppLogger.markOperation("导入ZIP到小图标区域")

                val workBadgesDir = java.io.File(
                    presetManager.getWorkImagesDirectory(),
                    PresetManager.BADGES_FOLDER_NAME
                )

                val importedUris = withContext(Dispatchers.IO) {
                    ResourcePackageManager.importImagesFromZip(
                        context,
                        zipUri,
                        workBadgesDir,
                        password
                    )
                }

                onBadgeDialogRefresh()

                showToast("已导入 ${importedUris.size} 张小图标", android.widget.Toast.LENGTH_SHORT)
                AppLogger.i("ZIP导入完成: ${importedUris.size} 张小图标")

            } catch (e: com.tdds.jh.resource.ZipPasswordRequiredException) {
                AppLogger.w("ZIP需要密码")
                showToast("ZIP文件需要密码", android.widget.Toast.LENGTH_LONG)
                return@launch
            } catch (e: Exception) {
                AppLogger.e("导入ZIP失败", e)
                showToast("导入失败: ${e.message}", android.widget.Toast.LENGTH_LONG)
            } finally {
                dialogState.isImportingPackage = false
                onResumeDraftSave?.invoke()
            }
        }
    }

    /**
     * 获取图包中的图片数量
     */
    fun getPackageImageCount(
        packageItem: PackageItem.Imported,
        onCountCalculated: (Int) -> Unit
    ) {
        scope.launch {
            val count = withContext(Dispatchers.IO) {
                ResourcePackageManager.countImagesInImportedPackage(packageItem.file)
            }
            onCountCalculated(count)
        }
    }

    /**
     * 处理外部图包导入
     */
    fun handleExternalPackageImport(
        uri: Uri,
        fileName: String?,
        onLoadingStateChange: (Boolean) -> Unit
    ) {
        scope.launch {
            try {
                val savedPackageFile = withContext(Dispatchers.IO) {
                    presetManager.saveImportedPackage(uri, fileName ?: "imported_${System.currentTimeMillis()}.zip")
                }
                if (savedPackageFile != null) {
                    AppLogger.i("外部图包导入成功: ${savedPackageFile.name}")
                    showToast("图包导入成功: ${savedPackageFile.nameWithoutExtension}", android.widget.Toast.LENGTH_SHORT)
                } else {
                    AppLogger.e("外部图包导入失败: 保存文件返回null")
                    showToast("图包导入失败", android.widget.Toast.LENGTH_SHORT)
                }
            } catch (e: Exception) {
                AppLogger.e("外部图包导入失败", e)
                showToast("图包导入失败: ${e.message}", android.widget.Toast.LENGTH_SHORT)
            } finally {
                onLoadingStateChange(false)
            }
        }
    }
}

/**
 * 创建并记住 PackageOperationHandler 实例
 */
@Composable
fun rememberPackageOperationHandler(
    scope: CoroutineScope,
    dialogState: DialogState,
    presetManager: PresetManager,
    onPendingImagesChange: (List<Uri>) -> Unit,
    onSkipDraftSave: (() -> Unit)?,
    onResumeDraftSave: (() -> Unit)?,
    showToast: (String, Int) -> Unit
): PackageOperationHandler {
    val context = LocalContext.current
    return remember(scope, dialogState, presetManager) {
        PackageOperationHandler(
            context = context,
            scope = scope,
            dialogState = dialogState,
            presetManager = presetManager,
            onPendingImagesChange = onPendingImagesChange,
            onSkipDraftSave = onSkipDraftSave,
            onResumeDraftSave = onResumeDraftSave,
            showToast = showToast
        )
    }
}
