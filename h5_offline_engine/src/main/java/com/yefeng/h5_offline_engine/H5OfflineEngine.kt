package com.yefeng.h5_offline_engine

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.File

/**
 * innerH5OfflineZips offline engine
 *
 * @sample demo url: http://localhost:8080/demoApp/demoProject/1.0.0/offline.html?q=q
 */
object H5OfflineEngine {

    var debug = true
    /**
     * init offline engine
     */
    fun init(context: Context, configUrl: String, innerZipsPath: String? = null) {
        if (!innerZipsPath.isNullOrEmpty()) {
            H5OfflineService.unzipInnerH5Zips(context, innerZipsPath)
        }
        H5OfflineService.checkUpdate(context, configUrl)
    }

    /**
     * clear cache
     */
    fun clear(context: Context) {
        H5OfflineService.clear(context)
    }

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
                localPath += "..$port"
            }
            for (segment in pathSegments) {
                localPath += File.separator + segment
            }
            val file = File(localPath)
            if (!file.exists()) {
                return null
            }
            return WebResourceResponse(
                context.contentResolver.getType(uri),
                "UTF-8",
                file.inputStream()
            )
        }.onFailure { exception ->
            exception.printStackTrace()
        }
        return null
    }
}