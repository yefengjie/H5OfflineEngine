package com.yefeng.h5_offline_engine

import android.app.DownloadManager
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.hzy.libp7zip.P7ZipApi
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.regex.Pattern


class H5OfflineService : IntentService("H5OfflineService") {
    companion object {
        private const val TAG = "H5OfflineService"
        private const val ACTION_UNZIP_INNER_ZIPS = "ACTION_UNZIP_INNER_ZIPS"
        private const val ACTION_START_DOWNLOAD = "ACTION_START_DOWNLOAD"
        private const val ACTION_DOWNLOAD_COMPLETED = "ACTION_DOWNLOAD_COMPLETE"
        private const val ACTION_CLEAR = "ACTION_CLEAR"
        private const val PARAMS = "PARAMS"

        /**
         * clear local offline files
         */
        fun clear(context: Context) {
            val intent = Intent(context, H5OfflineService::class.java).apply {
                action = ACTION_CLEAR
            }
            context.startService(intent)
        }

        /**
         * download remote zip files
         */
        fun download(context: Context, list: ArrayList<String>) {
            val intent = Intent(context, H5OfflineService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putStringArrayListExtra(PARAMS, list)
            }
            context.startService(intent)
        }

        /**
         * on zip files download completed
         */
        fun downloadCompleted(context: Context, localPath: String) {
            val intent = Intent(context, H5OfflineService::class.java).apply {
                action = ACTION_DOWNLOAD_COMPLETED
                putExtra(PARAMS, localPath)
            }
            context.startService(intent)
        }

        /**
         * unzip inner h5 file to offline file path
         */
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
                    if (localPath.isNotEmpty()) {
                        onDownloadCompleted(localPath)
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    /**
     * unzip inner h5 file to offline file path
     */
    private fun unzipInnerZips(innerZipsPath: String) {
        val files = assets.list(innerZipsPath)
        if (files.isNullOrEmpty()) {
            return
        }
        var input: InputStream? = null
        var output: OutputStream? = null
        try {
            val downloadedFiles = H5OfflineUtil.getDownloadedFiles(this)
            for (fileName in files) {
                val inputPath = innerZipsPath + File.separator + fileName
                if (downloadedFiles.contains(fileName)) {
                    H5OfflineUtil.log("already unzipped: $fileName", TAG)
                    continue
                }
                val rootDir = H5OfflineUtil.getRootDir(this).absolutePath
                input = assets.open(inputPath)
                val fileOutPath = rootDir + File.separator + fileName
                val fileOut = File(fileOutPath)
                output = FileOutputStream(fileOut)
                input.copyTo(output)
                // if file is not a patch, check old version files and delete
                if (!fileName.startsWith(H5OfflineConfig.INCREMENT_PRE)) {
                    checkOldVersionFiles(rootDir, fileName)
                }
                // unzip file
                val cmd = Command7z.getExtractCmd(fileOutPath, rootDir)
                P7ZipApi.executeCommand(cmd)
                H5OfflineUtil.log("unzip file:$fileName", TAG)
                // save to sp
                H5OfflineUtil.putDownloadedFile(this, fileName)
                // delete file
                val deleteResult = fileOut.delete()
                H5OfflineUtil.log("delete file:$fileName $deleteResult", TAG)
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


    /**
     * download remote zip files
     */
    @Throws(Exception::class)
    fun downloadFiles(downloadList: ArrayList<String>) {
        val downloadedFiles = H5OfflineUtil.getDownloadedFiles(this)
        val mgr = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        for (remoteUrl in downloadList) {
            val fileName = remoteUrl.substring(remoteUrl.lastIndexOf(File.separator) + 1)
            if (downloadedFiles.contains(fileName)) {
                H5OfflineUtil.log("already updated: $fileName", TAG)
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
            val file = File(H5OfflineUtil.getRootDir(this), fileName)
            req.setDestinationUri(Uri.fromFile(file))
            val id = mgr.enqueue(req)
            H5OfflineUtil.log("start download,id=$id $fileName $remoteUrl", TAG)
        }
    }

    /**
     * on zip files download completed, unzip files
     */
    @Throws(Exception::class)
    private fun onDownloadCompleted(localPath: String) {
        val fileName = localPath.substring(localPath.lastIndexOf(File.separator) + 1)
        val rootDir = H5OfflineUtil.getRootDir(this)
        val file = File(rootDir, fileName)
        val filePath = file.absolutePath
        H5OfflineUtil.log("download success:$filePath", TAG)
        // if file is not a patch, check old version files and delete
        if (!fileName.startsWith(H5OfflineConfig.INCREMENT_PRE)) {
            checkOldVersionFiles(rootDir.absolutePath, fileName)
        }
        // unzip file
        val cmd = Command7z.getExtractCmd(filePath, rootDir.absolutePath)
        P7ZipApi.executeCommand(cmd)
        H5OfflineUtil.log("unzip file:$fileName", TAG)
        // save to sp
        H5OfflineUtil.putDownloadedFile(this, fileName)
        // delete zip
        val deleteResult = file.delete()
        H5OfflineUtil.log("delete file:$fileName $deleteResult", TAG)
        // send offline files updated broadcast
        // you can do something after you receiver this broadcast
        sendBroadcast(Intent(H5OfflineConfig.BROADCAST_OFFLINE_FILES_UPDATED))
    }

    /**
     * check if has old version files and delete it
     */
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
        var versionParentDir = ""
        for (index in 0 until dirs.size - 1) {
            versionParentDir += dirs[index] + File.separator
        }
        val versionParent = File(rootDir, versionParentDir)
        if (!versionParent.exists()) {
            return
        }
        deleteRecursive(versionParent)
        H5OfflineUtil.log("delete dir:${versionParent.absolutePath}", TAG)
    }

    /**
     * recursive delete dir
     */
    @Throws(Exception::class)
    fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory)
            for (child in fileOrDirectory.listFiles())
                deleteRecursive(child)

        fileOrDirectory.delete()
    }

    /**
     * clear local offline files
     */
    @Throws(Exception::class)
    fun clear() {
        deleteRecursive(H5OfflineUtil.getRootDir(this))
        //clear sp
        H5OfflineUtil.clearDownloadSp(this)
        H5OfflineUtil.log("clear all cache", TAG)
    }
}