package com.yefeng.h5offlineengine

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.yefeng.h5_offline_engine.H5OfflineEngine
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
        initWeb()
        init()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWeb() {
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                url: String?
            ): WebResourceResponse? {
                if (!url.isNullOrEmpty()) {
                    return offlineInterceptResource(Uri.parse(url))
                }
                return super.shouldInterceptRequest(view, url)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    null != request
                ) {
                    offlineInterceptResource(request.url)
                }
                return super.shouldInterceptRequest(view, request)
            }
        }
        web.webChromeClient = WebChromeClient()
        web.settings.javaScriptEnabled = true
    }

    private fun offlineInterceptResource(url: Uri): WebResourceResponse? {
        return H5OfflineEngine.interceptResource(url, this)
    }

    private fun init() {
        btnClear.setOnClickListener { H5OfflineEngine.clear(this) }
        btnInit.setOnClickListener {
            H5OfflineEngine.init(
                this,
                "",
                "innerH5OfflineZips"
            )
        }
        btnShowInner100.setOnClickListener {
            web.loadUrl("http://localhost:8080/demoApp/demoProject/1.0.0/offline.html")
        }
        btnDownload110.setOnClickListener {
            H5OfflineEngine.init(
                this,
                "http://localhost:8080/config/H5OfflineConfig.json",
                "innerH5OfflineZips"
            )
        }
        btnShowDownload110.setOnClickListener {
            web.loadUrl("http://localhost:8080/demoApp/demoProject/1.1.0/offline.html")
        }
        btnDownloadPath.setOnClickListener {
            H5OfflineEngine.init(
                this,
                "http://localhost:8080/config/H5OfflinePath.json",
                "innerH5OfflineZips"
            )
        }
        btnShowPath.setOnClickListener {
            web.loadUrl("http://localhost:8080/demoApp/demoProject/1.1.0/offline.html")
        }
    }
}