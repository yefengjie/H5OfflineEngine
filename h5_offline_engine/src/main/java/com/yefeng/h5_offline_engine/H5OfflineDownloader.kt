//package com.yefeng.h5_offline_engine
//
//import android.app.DownloadManager
//import android.content.Context
//import android.content.Context.DOWNLOAD_SERVICE
//import android.net.Uri
//import java.io.File
//
//object H5OfflineDownloader {
//    /**
//     * download files
//     */
//    @Throws(Exception::class)
//    fun downloadFiles(context: Context, downloadList: ArrayList<String>) {
//        val mgr = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
//        for (remoteUrl in downloadList) {
//            val req = DownloadManager.Request(Uri.parse(remoteUrl))
//            req.setAllowedOverRoaming(true)
//            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
//            req.setTitle("H5 Offline Engine")
//            req.setDescription("downloading files...")
//            req.setVisibleInDownloadsUi(false)
//
//            req.setDestinationUri(
//                Uri.parse(
//                    H5OfflineUtil.getCachedDir(context).absolutePath +
//                            remoteUrl.substring(remoteUrl.lastIndexOf(File.separator))
//                )
//            )
//            mgr.enqueue(req)
//        }
//        context.registerReceiver()
//    }
//}