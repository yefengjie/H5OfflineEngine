package com.yefeng.h5_offline_engine

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import java.io.File

object H5OfflineUtil {
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
            Log.e(tag, msg)
        }
    }

    fun getDownloadSp(context: Context): SharedPreferences {
        return context.getSharedPreferences("h5_offline_engine_downloaded", Context.MODE_PRIVATE)
    }
}