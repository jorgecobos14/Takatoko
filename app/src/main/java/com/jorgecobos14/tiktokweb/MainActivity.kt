package com.jorgecobos14.tiktokweb

import android.content.ComponentCallbacks2
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private val blockedPatterns = listOf<String>()

    private val blockedHosts = listOf(
        "onelink.me", "appsflyer.com", "adjust.com", "branch.io"
    )

    private val googleAuthHosts = listOf(
        "accounts.google.com", "accounts.youtube.com"
    )

    private fun openInSystemBrowser(url: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setGeolocationEnabled(false)
            saveFormData = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(true)
        }

        webView.isSoundEffectsEnabled = false
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        webView.clearCache(true)
        webView.clearHistory()
        WebStorage.getInstance().deleteAllData()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val uri = request.url
                val scheme = uri.scheme ?: ""
                val host = uri.host ?: ""
                if (scheme != "http" && scheme != "https") {
                    return true
                }
                if (blockedHosts.any { host.contains(it) }) {
                    return true
                }
                if (googleAuthHosts.any { host.contains(it) }) {
                    openInSystemBrowser(uri.toString())
                    return true
                }
                return false
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                val uri = android.net.Uri.parse(url)
                val scheme = uri.scheme ?: ""
                val host = uri.host ?: ""
                if (scheme != "http" && scheme != "https") {
                    return true
                }
                if (blockedHosts.any { host.contains(it) }) {
                    return true
                }
                if (googleAuthHosts.any { host.contains(it) }) {
                    openInSystemBrowser(url)
                    return true
                }
                return false
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val url = request.url.toString()
                if (blockedPatterns.any { url.contains(it) }) {
                    return WebResourceResponse("text/plain", "utf-8", null)
                }
                return null
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message
            ): Boolean {
                val transport = resultMsg.obj as WebView.WebViewTransport
                val tempWebView = WebView(this@MainActivity)
                tempWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        v: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        val uri = request.url
                        val scheme = uri.scheme ?: ""
                        val host = uri.host ?: ""
                        if (scheme != "http" && scheme != "https") {
                            return true
                        }
                        if (blockedHosts.any { host.contains(it) }) {
                            return true
                        }
                        if (googleAuthHosts.any { host.contains(it) }) {
                            openInSystemBrowser(uri.toString())
                            return true
                        }
                        view.loadUrl(uri.toString())
                        return true
                    }
                }
                transport.webView = tempWebView
                resultMsg.sendToTarget()
                return true
            }
        }

        webView.loadUrl("https://www.tiktok.com")
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                webView.clearCache(false)
            }
        }
    }

    override fun onDestroy() {
        webView.clearCache(true)
        webView.clearHistory()
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().flush()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
