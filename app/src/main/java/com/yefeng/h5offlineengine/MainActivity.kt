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
import com.bumptech.glide.Glide
import com.yefeng.h5_offline_engine.H5OfflineEngine
import com.yefeng.h5_offline_engine.H5OfflineUtil
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val mHost = "localhost:8080"

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
                H5OfflineUtil.log(Thread.currentThread().name, "shouldInterceptRequest")
                if (!url.isNullOrEmpty()) {
                    if (url.lastIndexOf(".jpg") > 0) {
                        return WebResourceResponse(
                            this@MainActivity.contentResolver.getType(Uri.parse(url)),
                            "UTF-8",
                            Glide.with(this@MainActivity).asFile().load(url).submit().get().inputStream()
                        )
                    }
                    return H5OfflineEngine.interceptResource(Uri.parse(url), applicationContext)
                }
                return super.shouldInterceptRequest(view, url)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                H5OfflineUtil.log(Thread.currentThread().name, "shouldInterceptRequest")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    null != request
                ) {
                    if (request.url.toString().lastIndexOf(".jpg") > 0) {
                        return WebResourceResponse(
                            this@MainActivity.contentResolver.getType(request.url),
                            "UTF-8",
                            Glide.with(this@MainActivity).asFile().load(request.url).submit().get().inputStream()
                        )
                    }
                    return H5OfflineEngine.interceptResource(request.url, applicationContext)
                }
                return super.shouldInterceptRequest(view, request)
            }
        }
        web.webChromeClient = WebChromeClient()
        web.settings.javaScriptEnabled = true
    }

    private fun init() {
        btnClear.setOnClickListener {
            H5OfflineEngine.clear(this)
        }
        btnInit.setOnClickListener {
            H5OfflineEngine.init(
                this, // context
                "innerH5OfflineZips", // inner zip assets path
                true // show debug log
            )
        }
        btnShowInner100.setOnClickListener {
            web.loadUrl("http://$mHost/demoApp/demoProject/1.0.0/offline.html")
        }
        btnDownload110.setOnClickListener {
            TestUtil.checkUpdate(this, "http://$mHost/config/H5OfflineConfig.json")
        }
        btnShowDownload110.setOnClickListener {
            web.loadUrl("http://$mHost/demoApp/demoProject/1.1.0/offline.html")
        }
        btnDownloadPath.setOnClickListener {
            TestUtil.checkUpdate(this, "http://$mHost/config/H5OfflinePath.json")
        }
        btnShowPath.setOnClickListener {
            web.loadUrl("http://$mHost/demoApp/demoProject/1.1.0/offline.html")
        }
    }

    override fun onDestroy() {
        web.clearHistory()
        web.clearCache(true)
        web.destroy()
        super.onDestroy()
    }
}