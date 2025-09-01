package com.skdev.browser

// This is an old version just put it in a file for now...

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var webauthnBridge: CustomWebAuthnBridge? = null

    // CONFIGURE YOUR WEBSITE URL HERE
    private val targetUrl = "https://norled.unisea.cloud/#/start/office-start"

    companion object {
        private const val TAG = "WebAuthnBrowser"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)

        setupWebView()
        setupWebAuthnBridge()

        webView.loadUrl(targetUrl)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    private fun setupWebAuthnBridge() {
        try {
            webauthnBridge = CustomWebAuthnBridge(this, webView)
            Log.i(TAG, "Custom WebAuthn bridge initialized successfully")
            Toast.makeText(this, "WebAuthn ready (custom implementation)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Custom WebAuthn bridge", e)
            Toast.makeText(this, "Custom WebAuthn bridge initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            userAgentString = WebSettings.getDefaultUserAgent(this@MainActivity)
            allowFileAccess = true
            allowContentAccess = true
        }
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if ("https" != url.scheme) {
                    Log.w(TAG, "Blocked non-HTTPS URL: $url")
                    Toast.makeText(view.context, "Error: Only HTTPS connections are allowed for WebAuthn.", Toast.LENGTH_LONG).show()
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                webauthnBridge?.onPageStarted(url)
                Log.i(TAG, "Page started loading: $url")
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                webauthnBridge?.onPageFinished(url)
                Log.i(TAG, "Page finished loading: $url")
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                Log.i(TAG, "onPermissionRequest for: ${request.resources.joinToString()}")
                request.grant(request.resources)
            }
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, true)
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}