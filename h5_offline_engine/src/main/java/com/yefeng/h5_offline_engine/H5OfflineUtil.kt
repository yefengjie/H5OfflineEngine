package com.yefeng.h5_offline_engine

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import java.io.File


object H5OfflineUtil {
    /**
     * h5 offline root dir
     */
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

    /**
     * log
     */
    fun log(msg: String, tag: String = "H5OfflineEngine") {
        if (H5OfflineEngine.debug) {
            Log.e(tag, msg)
        }
    }

    /**
     * download files sp, used to check files is downloaded
     */
    private fun getDownloadSp(context: Context): SharedPreferences {
        var version = BuildConfig.VERSION_NAME
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            version = pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        log("App Version:$version")
        return context.getSharedPreferences(
            "h5_offline_engine_downloaded_${version}",
            Context.MODE_PRIVATE
        )
    }

    /**
     * clear download sp
     */
    fun clearDownloadSp(context: Context) {
        getDownloadSp(context).edit().clear().apply()
    }

    /**
     * get download files
     */
    fun getDownloadedFiles(context: Context): MutableSet<String> {
        val sp = getDownloadSp(context)
        val set = HashSet<String>()
        if (null != sp.all) {
            set.addAll(sp.all.keys)
        }
        log("downloaded list:$set")
        return set
    }

    /**
     * put download file to sp by app version
     */
    fun putDownloadedFile(context: Context, fileName: String) {
        val sp = getDownloadSp(context)
        sp.edit().putBoolean(fileName, false).apply()
    }
}