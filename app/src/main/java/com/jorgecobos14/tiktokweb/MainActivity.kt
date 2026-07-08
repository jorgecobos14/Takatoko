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

    // Dominios de tracking/analytics que se bloquean a nivel de red
    // para no gastar CPU/datos en telemetría que no necesitas.
    private val blockedPatterns = listOf(
        "analytics", "log/", "mon.tiktokv", "ads", "/monitor/",
        "log.tiktok", "abtest", "webcast/log"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        // Cookies persistentes habilitadas ANTES de cargar nada
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setGeolocationEnabled(false)
            saveFormData = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
        }

        webView.isSoundEffectsEnabled = false

        // Compositing nativo del WebView; si notas jank en hardware muy viejo,
        // prueba cambiar esto a View.LAYER_TYPE_NONE.
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // Limpieza inicial: todo menos cookies (esas ya persisten aparte)
        webView.clearCache(true)
        webView.clearHistory()
        WebStorage.getInstance().deleteAllData()

        webView.webViewClient = object : WebViewClient() {
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

        webView.webChromeClient = WebChromeClient()

        webView.loadUrl("https://www.tiktok.com")
    }

    override fun onPause() {
        super.onPause()
        // Al salir de la app: limpia todo el cache/storage en disco,
        // pero las cookies de sesión se quedan.
        webView.clearCache(true)
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().flush()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // false = solo limpia RAM cache, no toca lo que ya está en disco
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
