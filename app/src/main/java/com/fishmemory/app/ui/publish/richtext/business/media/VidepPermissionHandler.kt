package com.fishmemory.app.ui.publish.richtext.business.media

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 发布页相册/视频读权限与相机权限的检查与说明对话框。
 * [ActivityResultLauncher] 仍由 Activity 注册，此处只封装分支逻辑，避免与媒资流程混杂。
 */
class VidepPermissionHandler(
    private val activity: AppCompatActivity,
) {

    /**
     * 图片读权限：已授权则打开相册；否则 rationale 或直接请求。
     */
    fun checkAndRequestImageReadPermission(
        onGranted: () -> Unit,
        requestLauncher: ActivityResultLauncher<String>,
    ) {
        val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(activity, permissionToRequest) == PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permissionToRequest)) {
            showImageReadRationale(permissionToRequest, requestLauncher)
        } else {
            requestLauncher.launch(permissionToRequest)
        }
    }

    private fun showImageReadRationale(
        permission: String,
        requestLauncher: ActivityResultLauncher<String>,
    ) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("需要存储权限")
            .setMessage("需要存储权限才能访问您的相册，选择图片添加到文章中")
            .setPositiveButton("确定") { _, _ ->
                requestLauncher.launch(permission)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 视频读权限：已授权则 [onGranted]（打开视频选择器）。
     */
    fun checkAndRequestVideoReadPermission(
        onGranted: () -> Unit,
        requestLauncher: ActivityResultLauncher<String>,
    ) {
        val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(activity, permissionToRequest) == PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permissionToRequest)) {
            MaterialAlertDialogBuilder(activity)
                .setTitle("需要存储权限")
                .setMessage("需要存储权限才能访问您的视频，选择视频添加到文章中")
                .setPositiveButton("确定") { _, _ -> requestLauncher.launch(permissionToRequest) }
                .setNegativeButton("取消", null)
                .show()
        } else {
            requestLauncher.launch(permissionToRequest)
        }
    }

    /**
     * 相机权限：已授权则 [onGranted]（启动拍照）。
     */
    fun checkCameraAndLaunch(
        onGranted: () -> Unit,
        requestLauncher: ActivityResultLauncher<String>,
    ) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else {
            requestLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}