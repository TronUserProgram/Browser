package com.skdev.browser

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import de.cotech.hw.fido2.WebViewWebauthnBridge
import de.cotech.hw.fido2.ui.WebauthnDialogOptions
import de.cotech.hw.intent.usb.R as hwsecurityUsb
import de.cotech.hw.intent.nfc.R as hwsecurityNfc

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var webAuthnBridge: WebViewWebauthnBridge? = null
    private var popupDialog: Dialog? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)

        // Debugging for WebAuthn inspection
        WebView.setWebContentsDebuggingEnabled(true)

        // Configure WebView settings for popup support and WebAuthn
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)

            // Security hardening
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        // Initialize WebAuthn bridge with options
        val webauthnOptions = WebauthnDialogOptions.builder()
            .setAllowKeyboard(true) // Allow keyboard input fallback

        webAuthnBridge = WebViewWebauthnBridge
            .createInstanceForWebView(this, webView, webauthnOptions)

        // Set up WebViewClient with proper WebAuthn delegation
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                webAuthnBridge?.delegateOnPageStarted(view, url, favicon)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    request?.let { webAuthnBridge?.delegateShouldInterceptRequest(view, it) }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        // Set up WebChromeClient for popup handling
        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                val popupWebView = WebView(this@MainActivity)

                with(popupWebView.settings) {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    setSupportMultipleWindows(true)

                    // Same security rules
                    allowFileAccess = false
                    allowContentAccess = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                }

                // Create dialog for popup
                popupDialog = Dialog(
                    this@MainActivity,
                    android.R.style.Theme_Material_Light_Dialog_NoActionBar_MinWidth
                )
                popupDialog?.setContentView(popupWebView)
                popupDialog?.setOnDismissListener {
                    popupWebView.destroy()
                }
                popupDialog?.show()

                // Set up popup WebView client
                popupWebView.webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        webAuthnBridge?.delegateOnPageStarted(view, url, favicon)
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            request?.let { webAuthnBridge?.delegateShouldInterceptRequest(view, it) }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url.toString()
                        if (url.contains("callback") || url.contains("success")) {
                            closePopup()
                            return true
                        }
                        return false
                    }
                }

                popupWebView.webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView?) {
                        closePopup()
                    }
                }

                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = popupWebView
                resultMsg?.sendToTarget()

                return true
            }

            override fun onCloseWindow(window: WebView?) {
                super.onCloseWindow(window)
                closePopup()
            }
        }

        // Load the main URL
        val url = "https://norled.unisea.cloud"
        webView.loadUrl(url)

        // Back button handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (popupDialog?.isShowing == true) {
                    closePopup()
                } else if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }


    private fun closePopup() {
        popupDialog?.dismiss()
        popupDialog = null
    }

    override fun onDestroy() {
        super.onDestroy()
        closePopup()
        webAuthnBridge = null
        webView.destroy()
    }
}