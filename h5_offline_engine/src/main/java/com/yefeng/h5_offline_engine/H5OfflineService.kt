package com.yefeng.h5_offline_engine

import android.app.DownloadManager
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.hzy.libp7zip.P7ZipApi
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.regex.Pattern


class H5OfflineService : IntentService("H5OfflineService") {
    companion object {
        private const val TAG = "H5OfflineService"
        private const val ACTION_UNZIP_INNER_ZIPS = "ACTION_UNZIP_INNER_ZIPS"
        private const val ACTION_CHECK_UPDATE = "ACTION_CHECK_UPDATE"
        private const val ACTION_START_DOWNLOAD = "ACTION_START_DOWNLOAD"
        private const val ACTION_DOWNLOAD_COMPLETED = "ACTION_DOWNLOAD_COMPLETE"
        private const val ACTION_CLEAR = "ACTION_CLEAR"
        private const val PARAMS = "PARAMS"
        private const val PARAMS2 = "PARAMS2"

        fun clear(context: Context) {
            val intent = Intent(context, H5OfflineService::class.java).apply {
                action = ACTION_CLEAR
            }
            context.startService(intent)
        }

        fun checkUpdate(context: Context, url: String) {
            val intent = Intent(context, H5OfflineService::class.java).apply {
                action = ACTION_CHECK_UPDATE
                putExtra(PARAMS, url)
            }
            context.startService(intent)
        }

        @Suppress("unused")
        fun download(context: Context, list: ArrayList<String>) {
            val intent = Intent(context, H5OfflineService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putStringArrayListExtra(PARAMS, list)
            }
            context.startService(intent)
        }

        fun downloadCompleted(context: Context, localPath: String, remoteUrl: String) {
            val intent = Intent(context, H5OfflineService::class.java).apply {
                action = ACTION_DOWNLOAD_COMPLETED
                putExtra(PARAMS, localPath)
                putExtra(PARAMS2, remoteUrl)
            }
            context.startService(intent)
        }

        fun unzipInnerH5Zips(context: Context, innerZipsPath: String) {
            val intent = Intent(context, H5OfflineService::class.java).apply {
                action = ACTION_UNZIP_INNER_ZIPS
                putExtra(PARAMS, innerZipsPath)
            }
            context.startService(intent)
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        try {
            when (intent?.action) {
                ACTION_UNZIP_INNER_ZIPS ->
                    intent.getStringExtra(PARAMS)?.run {
                        if (this.isEmpty()) {
                            return
                        }
                        unzipInnerZips(this)
                    }
                ACTION_CLEAR -> clear()
                ACTION_CHECK_UPDATE -> {
                    intent.getStringExtra(PARAMS)?.run {
                        if (this.isEmpty()) {
                            return
                        }
                        checkUpdate(this)
                    }
                }
                ACTION_START_DOWNLOAD -> {
                    intent.getStringArrayListExtra(PARAMS)?.run {
                        if (this.isEmpty()) {
                            return
                        }
                        downloadFiles(this)
                    }
                }
                ACTION_DOWNLOAD_COMPLETED -> {
                    val localPath = intent.getStringExtra(PARAMS)
                    val remoteUrl = intent.getStringExtra(PARAMS2)
                    if (localPath.isNotEmpty() && remoteUrl.isNotEmpty()) {
                        onDownloadCompleted(localPath, remoteUrl)
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun unzipInnerZips(innerZipsPath: String) {
        val files = assets.list(innerZipsPath)
        if (files.isNullOrEmpty()) {
            return
        }
        var input: InputStream? = null
        var output: OutputStream? = null
        try {
            val sp = H5OfflineUtil.getDownloadSp(this)
            for (fileName in files) {
                val inputPath = innerZipsPath + File.separator + fileName
                if (sp.contains(inputPath)) {
                    continue
                }
                val rootDir = H5OfflineUtil.getRootDir(this).absolutePath
                input = assets.open(inputPath)
                val fileOutPath = rootDir + File.separator + fileName
                val fileOut = File(fileOutPath)
                output = FileOutputStream(fileOut)
                input.copyTo(output)
                // unzip file
                val cmd = Command.getExtractCmd(fileOutPath, rootDir)
                P7ZipApi.executeCommand(cmd)
                H5OfflineUtil.log("unzip file:$fileName")
                // delete file
                val deleteResult = fileOut.delete()
                H5OfflineUtil.log("delete file:$fileName $deleteResult")
                // save to sp
                sp.edit().putString(inputPath, fileOutPath).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (null != input) {
                try {
                    input.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (null != output) {
                try {
                    output.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Throws(Exception::class)
    fun checkUpdate(url: String) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            return
        }
        val configJson = JSONArray(response.body!!.string())
        if (configJson.length() == 0) {
            return
        }
        // check dir is exist
        val sp = H5OfflineUtil.getDownloadSp(this)
        val downloadList = ArrayList<String>()
        for (index in 0 until configJson.length()) {
            val item = configJson.getString(index)
            H5OfflineUtil.log("$index $item")
            if (sp.contains(item)) {
                H5OfflineUtil.log("已更新 $item")
            } else {
                downloadList.add(item)
            }
        }
        if (downloadList.isNotEmpty()) {
            downloadFiles(downloadList)
        }
    }

    @Throws(Exception::class)
    fun downloadFiles(downloadList: ArrayList<String>) {
        val sp = H5OfflineUtil.getDownloadSp(this)
        val mgr = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        for (remoteUrl in downloadList) {
            if (sp.contains(remoteUrl)) {
                continue
            }
            val req = DownloadManager.Request(Uri.parse(remoteUrl))
            req.setAllowedOverRoaming(true)
            req.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            req.setTitle("H5 Offline Engine")
            req.setDescription("downloading files...")
            req.setVisibleInDownloadsUi(false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                req.setRequiresDeviceIdle(false)
                req.setRequiresCharging(false)
            }
            val fileName = remoteUrl.substring(remoteUrl.lastIndexOf(File.separator))
            val file = File(H5OfflineUtil.getRootDir(this), fileName)
            req.setDestinationUri(Uri.fromFile(file))
            val id = mgr.enqueue(req)
            H5OfflineUtil.log("start download,id=$id $remoteUrl")
        }
    }

    @Throws(Exception::class)
    private fun onDownloadCompleted(localPath: String, remoteUrl: String) {
        val fileName = localPath.substring(localPath.lastIndexOf(File.separator) + 1)
        val rootDir = H5OfflineUtil.getRootDir(this)
        val file = File(rootDir, fileName)
        val filePath = file.absolutePath
        H5OfflineUtil.log("download success:$filePath", TAG)
        // unzip file
        val cmd = Command.getExtractCmd(filePath, rootDir.absolutePath)
        P7ZipApi.executeCommand(cmd)
        H5OfflineUtil.log("unzip file:$fileName")
        // delete zip
        val deleteResult = file.delete()
        H5OfflineUtil.log("delete file:$fileName $deleteResult")
        // save to sp
        val sp = H5OfflineUtil.getDownloadSp(this)
        sp.edit().putString(remoteUrl, localPath).apply()
        // if file is not a patch, check old version files and delete
        if (!fileName.startsWith(H5OfflineConfig.PATCH_PRE)) {
            checkOldVersionFiles(rootDir.absolutePath, fileName)
        }
    }

    @Throws(Exception::class)
    private fun checkOldVersionFiles(rootDir: String, fileName: String) {
        val matcher = Pattern.compile("\\d+(\\.\\d+){2}.7z").matcher(fileName)
        if (!matcher.find()) {
            return
        }
        val dirs = fileName.split("_")
        if (dirs.isNullOrEmpty() || dirs.size <= 1) {
            return
        }
        val versionDir = dirs[dirs.size - 1].substring(0, dirs[dirs.size - 1].lastIndexOf("."))
        var versionParentDir = ""
        for (index in 0 until dirs.size - 1) {
            versionParentDir += dirs[index] + File.separator
        }
        val versionParent = File(rootDir, versionParentDir)
        if (!versionParent.exists()) {
            return
        }
        val children = versionParent.listFiles()
        if (children.isNullOrEmpty() || children.size <= 1) {
            return
        }
        H5OfflineUtil.log("version dir:$versionDir")
        for (child in children) {
            if (child.name != versionDir) {
                H5OfflineUtil.log("delete dir:$child.absolutePath")
                deleteRecursive(child)
            }
        }
    }

    @Throws(Exception::class)
    fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory)
            for (child in fileOrDirectory.listFiles())
                deleteRecursive(child)

        fileOrDirectory.delete()
    }

    @Throws(Exception::class)
    fun clear() {
        deleteRecursive(H5OfflineUtil.getRootDir(this))
        //clear sp
        H5OfflineUtil.getDownloadSp(this).edit().clear().apply()
        H5OfflineUtil.log("clear all cache")
    }
}