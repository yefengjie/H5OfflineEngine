//package com.yefeng.h5_offline_engine
//
//import android.content.Context
//import android.content.Context.MODE_PRIVATE
//import android.content.SharedPreferences
//import android.os.AsyncTask
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import org.json.JSONArray
//import org.json.JSONObject
//import java.io.File
//
//class H5OfflineUpdateTask : AsyncTask<Any, Int, Boolean>() {
//
//    private val mStrOtherFiles = "otherFiles"
//    private val mStrProjectFiles = "projectFiles"
//
//    private val mDownLoadLists = ArrayList<Array<String>>()
//
//    override fun doInBackground(vararg param: Any): Boolean {
//        val client = OkHttpClient()
//        client.runCatching {
//            val context = param[0] as Context
//            val sp =
//                context.getSharedPreferences(H5OfflineConfig.SP_H5_OFFLINE_CONFIG, MODE_PRIVATE)
//            // request config
//            val request = Request.Builder().url(param[1].toString()).build()
//            val response = client.newCall(request).execute()
//            if (!response.isSuccessful) {
//                return false
//            }
//            val configJson = JSONObject(response.body.toString())
//
//            // check other files
//            configJson.getJSONObject(mStrOtherFiles)?.run {
//                checkOtherFiles(sp, this)
//            }
//
//            // check zip files
//            configJson.getJSONArray("projects")?.run {
//                checkProjects(sp, this)
//            }
//
//            //download files
//            if (mDownLoadLists.isEmpty()) {
//                return true
//            }
//            H5OfflineDownloader.downloadFiles(context, mDownLoadLists)
//        }.onFailure {
//            it.printStackTrace()
//        }
//        return true
//    }
//
//    /**
//     * check if need to update project offline files
//     */
//    @Throws(Exception::class)
//    private fun checkProjects(sp: SharedPreferences, projectsArray: JSONArray) {
//        for (i in 0 until projectsArray.length()) {
//            val remoteVersion = projectsArray.getJSONObject(i).getString("version")
//            val name = projectsArray.getJSONObject(i).getString("name")
//            val zipPath = projectsArray.getJSONObject(i).getString("zipPath")
//            if (remoteVersion.isNullOrBlank() || name.isNullOrBlank() || zipPath.isNullOrBlank()) {
//                continue
//            }
//            val localVersion = sp.getString(name, "-1")
//            if (localVersion != remoteVersion) {
//                // clear local files cache
//                deleteOfflineFile(name)
//                // download new files
//                mDownLoadLists.add(
//                    arrayOf(
//                        zipPath,
//                        getDir(zipPath.substring(zipPath.lastIndexOf("/"))),
//                        remoteVersion,
//                        mStrProjectFiles
//                    )
//                )
//            }
//        }
//    }
//
//    /**
//     * check if need to update other files
//     */
//    @Throws(Exception::class)
//    private fun checkOtherFiles(sp: SharedPreferences, otherJson: JSONObject) {
//        val localVersion = sp.getString(H5OfflineConfig.SP_OTHER_FILES_VERSION, "-1")
//        val remoteVersion = otherJson.getString("version")
//        if (localVersion != remoteVersion) {
//            // clear local other files cache
//            sp.getStringSet(H5OfflineConfig.SP_H5_OFFLINE_CONFIG, null)?.run {
//                for (path in this) {
//                    deleteOfflineFile(
//                        mStrOtherFiles + File.separator + H5OfflineUtil.encodePath(
//                            path
//                        )
//                    )
//                }
//            }
//            // download new files
//            otherJson.getJSONArray("paths")?.run {
//                for (i in 0 until this.length()) {
//                    val url = this.getString(i)
//                    val fileName =
//                        getDir(mStrOtherFiles + File.separator + H5OfflineUtil.encodePath(url))
//                    if (!url.isNullOrEmpty() && !remoteVersion.isNullOrEmpty()) {
//                        mDownLoadLists.add(arrayOf(url, fileName, remoteVersion, mStrOtherFiles))
//                    }
//                }
//            }
//        }
//    }
//
//    /**
//     * delete cache file
//     */
//    @Throws(Exception::class)
//    private fun deleteOfflineFile(path: String) {
//        deleteOfflineFile(File(getDir(path)))
//    }
//
//    /**
//     * delete cache file
//     */
//    @Throws(Exception::class)
//    private fun deleteOfflineFile(file: File) {
//        if (!file.exists()) {
//            return
//        }
//        if (file.isDirectory) {
//            val files = file.listFiles()
//            for (f in files) {
//                deleteOfflineFile(f)
//            }
//        } else {
//            file.delete()
//        }
//    }
//
//    /**
//     * get dir
//     */
//    private fun getDir(path: String): String {
//        return H5OfflineConfig.H5_OFFLINE_ROOT_DIR + File.separator + path
//    }
//}