package com.yefeng.h5_offline_engine

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor

class H5OfflineDownloadReceiver : BroadcastReceiver() {
    private val mTag = "H5OfflineDownloadReceiver"
    override fun onReceive(context: Context?, intent: Intent?) {
        if (null == intent || null == context || intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE
        ) {
            return
        }
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (id < 0) {
            return
        }
        var cursor: Cursor? = null
        try {
            val mgr = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query()
            query.setFilterById(id)
            cursor = mgr.query(query)
            if (cursor.moveToFirst()) {
                when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val localPath = cursor.getString(
                            cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        )
                        if (localPath.isNotEmpty()) {
                            H5OfflineService.downloadCompleted(context, localPath)
                        }
                        val remoteUrl = cursor.getString(
                            cursor.getColumnIndex(DownloadManager.COLUMN_URI)
                        )
                        if (remoteUrl.isNotEmpty()) {
                            // save download info
                            val sp = H5OfflineUtil.getDownloadSp(context)
                            sp.edit().putLong(remoteUrl, id).apply()
                        }
                    }
                    DownloadManager.STATUS_FAILED -> H5OfflineUtil.log("download failed", mTag)
                    DownloadManager.STATUS_PAUSED -> H5OfflineUtil.log("download paused", mTag)
                    DownloadManager.STATUS_PENDING -> H5OfflineUtil.log("download pending", mTag)
                    DownloadManager.STATUS_RUNNING -> H5OfflineUtil.log("download running", mTag)
                    else -> H5OfflineUtil.log("download unknown status", mTag)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (null != cursor && !cursor.isClosed) {
                cursor.close()
            }
        }
    }
}