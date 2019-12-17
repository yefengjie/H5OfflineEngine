package com.yefeng.h5_offline_engine

import android.content.Context
import android.net.Uri
import android.os.Build
import android.webkit.WebResourceResponse
import java.io.File

/**
 * innerH5OfflineZips offline engine
 *
 * demo url: http://localhost:8080/demoApp/demoProject/1.0.0/offline.html?q=q
 */
object H5OfflineEngine {

    var debug = true
    /**
     * init offline engine
     * @param context context
     * @param innerZipsPath inner zips path, package in app assets dir
     * @param isDebug show debug log
     */
    fun init(
        context: Context,
        innerZipsPath: String? = null,
        isDebug: Boolean = true
    ) {
        if (!innerZipsPath.isNullOrEmpty()) {
            H5OfflineService.unzipInnerH5Zips(context, innerZipsPath)
        }
        debug = isDebug
    }

    /**
     * download online zips
     */
    fun download(context: Context, downloadList: ArrayList<String>) {
        H5OfflineService.download(context, downloadList)
    }

    /**
     * clear cache
     */
    fun clear(context: Context) {
        H5OfflineService.clear(context)
    }

    /**
     * intercept resource and replace it with local files
     */
    fun interceptResource(uri: Uri?, context: Context): WebResourceResponse? {
        if (uri == null) {
            return null
        }
        uri.runCatching {
            val host = uri.host
            val port = uri.port
            val pathSegments = uri.pathSegments
            var localPath = H5OfflineUtil.getRootDir(context).absolutePath + File.separator + host
            if (port != 80) {
                // because file name is not support :   so we use .. to replace it
                localPath += "..$port"
            }
            for (segment in pathSegments) {
                localPath += File.separator + segment
            }
            val file = File(localPath)
            if (!file.exists()) {
                return null
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                WebResourceResponse(
                    getMime(uri),
                    "UTF-8",
                    200,
                    "OK",
                    HashMap<String, String>(),
                    file.inputStream()
                )
            } else {
                WebResourceResponse(
                    context.contentResolver.getType(uri),
                    "UTF-8",
                    file.inputStream()
                )
            }
        }.onFailure { exception ->
            exception.printStackTrace()
        }
        return null
    }

    private fun getMime(currentUri: Uri): String? {
        var mime = "text/html"
        val path = currentUri.path
        if (path.isNullOrBlank()) {
            return mime
        }
        if (path.endsWith(".css")) {
            mime = "text/css"
        } else if (path.endsWith(".json")) {
            mime = "application/json"
        } else if (path.endsWith(".js")) {
            mime = "application/x-javascript"
        } else if (path.endsWith(".jpg") || path.endsWith(".gif") ||
            path.endsWith(".png") || path.endsWith(".jpeg") ||
            path.endsWith(".webp") || path.endsWith(".bmp")
        ) {
            mime = "image/*"
        }
        return mime
    }
}