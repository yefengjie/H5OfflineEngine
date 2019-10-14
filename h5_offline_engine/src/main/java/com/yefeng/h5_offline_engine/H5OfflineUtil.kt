package com.yefeng.h5_offline_engine

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder

object H5OfflineUtil {
    fun checkH5Config(context: Context, configPath: String) {
        val httpClient = OkHttpClient()
        httpClient.runCatching {
            val request = Request.Builder().url(configPath).build()
            val response = httpClient.newCall(request).execute()
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun encodePath(path: String?): String {
        if (path.isNullOrBlank()) {
            return ""
        }
        return URLEncoder.encode(path, "utf-8")
    }

    @Throws(Exception::class)
    fun getRootDir(context: Context): File {
        return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
    }

    fun log(msg: String, tag: String = "H5OfflineEngine") {
        if (H5OfflineEngine.debug) {
            Log.d(tag, msg)
        }
    }

    fun getDownloadSp(context: Context): SharedPreferences {
        return context.getSharedPreferences("h5_offline_engine_downloaded", Context.MODE_PRIVATE)
    }
}