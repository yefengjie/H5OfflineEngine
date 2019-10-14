package com.yefeng.h5offlineengine

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.yefeng.h5_offline_engine.H5OfflineService
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val mRuntimePermissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var hasPermission = true
            for (p in mRuntimePermissions) {
                if (ContextCompat.checkSelfPermission(applicationContext, p) !=
                    PermissionChecker.PERMISSION_GRANTED
                ) {
                    hasPermission = false
                    break
                }
            }
            if (!hasPermission) {
                requestPermissions(mRuntimePermissions, 0)
            }
        }

        btnDownload.setOnClickListener { startService() }
    }

    private fun startService() {
        val list = ArrayList<String>()
        list.add("http://localhost:8080/zip/demoApp_demoProject_1.0.0.zip")
//        list.add("http://localhost:8080/zip/demoApp_demoProject_1.1.0.zip")
        H5OfflineService.download(this, list)
    }
}