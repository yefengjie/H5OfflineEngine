package com.yefeng.h5_offline_engine

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceResponse
import java.io.File
import java.io.InputStream

/**
 * h5 offline engine
 */
object H5OfflineEngine {

    /**
     * this url is just a demo for offline engine
     */
    private const val mDemoUrl =
        "http://www.google.com/project-name/project-version/filename.html?q=q"
    /**
     * offline h5 version
     * @see mDemoUrl project-version
     */
    private val mH5Version = ""
    /**
     * offline h5 project names
     * @see mDemoUrl project-name
     */
    private val mH5ProjectNames = ArrayList<String>()
    /**
     * offline h5 file extension, only cache files with those extensions
     * @see mDemoUrl filename.html-> .html
     */
    private val mH5FileNameExtension = ArrayList<String>()
    /**
     * offline h5 project hosts
     * @see mDemoUrl www.google.com
     */
    private val mH5ProjectHosts = ArrayList<String>()

    /**
     * other urls need to caches
     */
    private val mOtherCacheUrls = HashMap<String, String>()

    /**
     * offline files dir
     */
    private const val mCacheDir = "h5_offline_files"


    /**
     * init offline engine
     */
    fun init(
        context: Context,
        h5ProjectNames: ArrayList<String>,
        h5ProjectHosts: ArrayList<String>,
        h5StaticFileNameExtension: ArrayList<String>,
        otherCacheUrls: Map<String, String>? = null
    ) {
        this.mH5ProjectNames.addAll(h5ProjectNames)
        this.mH5ProjectHosts.addAll(h5ProjectHosts)
        this.mH5FileNameExtension.addAll(h5StaticFileNameExtension)
        if (!otherCacheUrls.isNullOrEmpty()) {
            mOtherCacheUrls.putAll(otherCacheUrls)
        }
        context.filesDir
        val cacheDir = File(context.filesDir.absolutePath + File.separator + mCacheDir)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    fun interceptResource(uri: Uri?, context: Context?): WebResourceResponse? {
        if (uri == null || context == null) {
            return null
        }
        uri.runCatching {
            val otherUrl = mOtherCacheUrls.getOrElse(this.toString(), { null })
            var cacheFilePath = ""
            if (null != otherUrl) {
                cacheFilePath = otherUrl
            } else {
                val host = uri.host
                val pathSegments = uri.pathSegments
                // check host and path
                if (null == host ||
                    !mH5ProjectHosts.contains(host) ||
                    pathSegments.isNullOrEmpty()
                ) {
                    return null
                }
                // check project name is contains in path
                var projectName: String? = null
                for (path in pathSegments) {
                    if (mH5ProjectNames.contains(path)) {
                        projectName = path
                    }
                    cacheFilePath += "$path/"
                }
                cacheFilePath = cacheFilePath.substring(0, cacheFilePath.length - 1)
                if (projectName.isNullOrEmpty()) {
                    return null
                }
                // check file extension is contains in path
                val fileName = pathSegments[pathSegments.size - 1]
                val fileNameExtension =
                    fileName.substring(fileName.lastIndexOf("."), fileName.length)
                if (!mH5FileNameExtension.contains(fileNameExtension)) {
                    return null
                }
            }
            val inputStream = getFilesFromCache(context, cacheFilePath) ?: return null
            return WebResourceResponse(context.contentResolver.getType(uri), "UTF-8", inputStream)
        }.onFailure { exception ->
            exception.printStackTrace()
            Log.e("haha", "error: " + exception.message)
        }
        return null
    }

    @Throws(Exception::class)
    private fun getFilesFromCache(context: Context, cacheFilePath: String): InputStream? {
        // we just load cache from download dir for test
        val fileName =
            context.filesDir.absolutePath + File.separator + mCacheDir + File.separator + cacheFilePath
        val file = File(fileName)
        return file.inputStream()
    }
}