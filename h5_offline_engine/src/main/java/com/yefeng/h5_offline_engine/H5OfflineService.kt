package com.yefeng.h5_offline_engine

import android.app.DownloadManager
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.hzy.libp7zip.P7ZipApi
import java.io.File
import java.util.regex.Pattern


class H5OfflineService : IntentService("H5OfflineService") {
    companion object {
        private const val TAG = "H5OfflineService"
        private const val ACTION_CHECK_UPDATE = "ACTION_CHECK_UPDATE"
        private const val ACTION_START_DOWNLOAD = "ACTION_START_DOWNLOAD"
        private const val ACTION_DOWNLOAD_COMPLETED = "ACTION_DOWNLOAD_COMPLETE"
        private const val PARAMS = "PARAMS"

        fun checkUpdate(context: Context, url: String) {
            val intent = Intent(context, H5OfflineService::class.java).apply {
                action = ACTION_CHECK_UPDATE
                putExtra(PARAMS, url)
            }
            context.startService(intent)
        }

        fun download(context: Context, list: ArrayList<String>) {
            val intent = Intent(context, H5OfflineService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putStringArrayListExtra(PARAMS, list)
            }
            context.startService(intent)
        }

        fun downloadCompleted(context: Context, localPath: String) {
            val intent = Intent(context, H5OfflineService::class.java).apply {
                action = ACTION_DOWNLOAD_COMPLETED
                putExtra(PARAMS, localPath)
            }
            context.startService(intent)
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        try {
            when (intent?.action) {
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
                    intent.getStringExtra(PARAMS)?.run {
                        if (this.isEmpty()) {
                            return
                        }
                        onDownloadCompleted(this)
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    fun checkUpdate(url: String) {
        val fileName = url.substring(url.lastIndexOf("/") + 1, url.length)
        // check dir is exist

    }

    @Throws(Exception::class)
    fun downloadFiles(downloadList: ArrayList<String>) {
        val sp = H5OfflineUtil.getDownloadSp(this)
        val mgr = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        for (remoteUrl in downloadList) {
            //todo for test
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
    private fun onDownloadCompleted(localPath: String) {
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
        // check old version files and delete
        checkOldVersionFiles(rootDir.absolutePath, fileName)
    }

    @Throws(Exception::class)
    private fun checkOldVersionFiles(rootDir: String, fileName: String) {
        val matcher = Pattern.compile(".+\\d+(\\.\\d+){2}.+").matcher(fileName)
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
}