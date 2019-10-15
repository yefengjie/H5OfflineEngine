package com.yefeng.h5_offline_engine

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

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

    @Throws(Exception::class)
    fun getRootDir(context: Context): File {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val rootDir = downloadDir!!.absolutePath + File.separator + "H5OfflineEngine"
        val file = File(rootDir)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
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