package com.yefeng.h5offlineengine

import android.content.Context
import com.yefeng.h5_offline_engine.H5OfflineEngine
import com.yefeng.h5_offline_engine.H5OfflineUtil
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

object TestUtil {
    /**
     * check if need to update local offline files
     */
    fun checkUpdate(context: Context, url: String) {
        val request = Request.Builder().url(url).build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful || response.body() == null) {
                    return
                }
                val configJson = JSONArray(response.body()!!.string())
                if (configJson.length() == 0) {
                    return
                }
                // check dir is exist
                val downloadList = ArrayList<String>()
                for (index in 0 until configJson.length()) {
                    val item = configJson.getString(index)
                    H5OfflineUtil.log("$index $item", "TestUtil")
                    downloadList.add(item)
                }
                if (downloadList.isNotEmpty()) {
                    H5OfflineEngine.download(context, downloadList)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
            }
        })
    }
}