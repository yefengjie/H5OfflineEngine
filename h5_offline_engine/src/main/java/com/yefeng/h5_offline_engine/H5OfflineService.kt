package com.yefeng.h5_offline_engine

import android.app.DownloadManager
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.hzy.libp7zip.P7ZipApi
import java.io.File


class H5OfflineService : IntentService("H5OfflineService") {
    companion object {
        private const val TAG = "H5OfflineService"
        private const val ACTION_START_DOWNLOAD = "ACTION_START_DOWNLOAD"
        private const val ACTION_DOWNLOAD_COMPLETED = "ACTION_DOWNLOAD_COMPLETE"
        private const val PARAMS = "PARAMS"

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
    private fun onDownloadCompleted(localPath: String) {
        val fileName = localPath.substring(localPath.lastIndexOf(File.separator) + 1)
        val fileDir = H5OfflineUtil.getRootDir(this).absolutePath
        val file = File(fileDir, fileName)
        H5OfflineUtil.log("download success:${file.absolutePath}", TAG)
        // unzip file
        val cmd = Command.getExtractCmd(file.absolutePath, fileDir)
        P7ZipApi.executeCommand(cmd)
        // delete zip
        val deleteResult = file.delete()
        H5OfflineUtil.log("delete file:$fileName $deleteResult")
    }
}