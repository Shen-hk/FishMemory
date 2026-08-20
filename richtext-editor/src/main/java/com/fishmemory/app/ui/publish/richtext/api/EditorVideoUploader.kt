package com.fishmemory.app.ui.publish.richtext.api

/**
 * Video upload extension point for host apps.
 *
 * The editor only cares about progress and final URL; concrete networking,
 * authentication, object storage, and retry policy belong to the host app.
 */
interface EditorVideoUploader {

    fun enqueueUpload(blockId: String, localUri: String, callback: Callback)

    fun cancelUpload(blockId: String)

    interface Callback {
        fun onProgress(blockId: String, progress: Int)
        fun onSuccess(blockId: String, remoteUrl: String)
        fun onError(blockId: String, throwable: Throwable)
    }
}
