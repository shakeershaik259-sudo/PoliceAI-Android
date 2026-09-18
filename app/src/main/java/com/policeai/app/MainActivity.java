package com.policeai.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {

    private WebView webView;
    private PermissionRequest pendingPermissionRequest;

    private static final int AUDIO_PERMISSION_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        webView.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        setContentView(webView);

        // -------------------------------------------------
        // WEBVIEW SETTINGS
        // -------------------------------------------------

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);

        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        settings.setMediaPlaybackRequiresUserGesture(false);

        // Enable cookies
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // -------------------------------------------------
        // LOCAL APP CONTENT
        // -------------------------------------------------

        final WebViewAssetLoader assetLoader =
                new WebViewAssetLoader.Builder()
                        .addPathHandler(
                                "/assets/",
                                new WebViewAssetLoader.AssetsPathHandler(this)
                        )
                        .build();

        // -------------------------------------------------
        // WEBVIEW CLIENT
        // -------------------------------------------------

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view,
                    WebResourceRequest request
            ) {

                WebResourceResponse response =
                        assetLoader.shouldInterceptRequest(
                                request.getUrl()
                        );

                if (response != null) {
                    return response;
                }

                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view,
                    String url
            ) {

                WebResourceResponse response =
                        assetLoader.shouldInterceptRequest(
                                Uri.parse(url)
                        );

                if (response != null) {
                    return response;
                }

                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error
            ) {

                super.onReceivedError(
                        view,
                        request,
                        error
                );

                // Only show errors for the main page
                if (request.isForMainFrame()) {

                    String description =
                            error.getDescription() != null
                                    ? error.getDescription().toString()
                                    : "Unknown error";

                    view.loadData(
                            "<html>" +
                            "<body style='background:#0b0f14;color:white;font-family:sans-serif;padding:30px'>" +
                            "<h2>Police AI</h2>" +
                            "<p>WebView loading error:</p>" +
                            "<p>" + description + "</p>" +
                            "<p>Please restart the app.</p>" +
                            "</body>" +
                            "</html>",
                            "text/html",
                            "UTF-8"
                    );
                }
            }
        });

        // -------------------------------------------------
        // CHROME CLIENT
        // -------------------------------------------------

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(
                    final PermissionRequest request
            ) {

                runOnUiThread(() -> {

                    if (request == null) {
                        return;
                    }

                    String[] resources =
                            request.getResources();

                    if (resources == null) {
                        return;
                    }

                    for (String resource : resources) {

                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE
                                .equals(resource)) {

                            if (checkSelfPermission(
                                    Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED) {

                                request.grant(
                                        new String[]{
                                                PermissionRequest
                                                        .RESOURCE_AUDIO_CAPTURE
                                        }
                                );

                            } else {

                                pendingPermissionRequest =
                                        request;

                                requestPermissions(
                                        new String[]{
                                                Manifest.permission.RECORD_AUDIO
                                        },
                                        AUDIO_PERMISSION_REQUEST
                                );
                            }

                            return;
                        }
                    }
                });
            }
        });

        // -------------------------------------------------
        // MICROPHONE PERMISSION
        // -------------------------------------------------

        requestAudioPermission();

        // -------------------------------------------------
        // LOAD POLICE AI
        // -------------------------------------------------

        webView.loadUrl(
                "https://appassets.androidplatform.net/assets/index.html"
        );
    }

    private void requestAudioPermission() {

        if (android.os.Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.RECORD_AUDIO
                        },
                        AUDIO_PERMISSION_REQUEST
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == AUDIO_PERMISSION_REQUEST) {

            if (grantResults.length > 0 &&
                    grantResults[0]
                            == PackageManager.PERMISSION_GRANTED) {

                if (pendingPermissionRequest != null) {

                    pendingPermissionRequest.grant(
                            new String[]{
                                    PermissionRequest
                                            .RESOURCE_AUDIO_CAPTURE
                            }
                    );

                    pendingPermissionRequest = null;
                }

            } else {

                pendingPermissionRequest = null;
            }
        }
    }

    @Override
    public void onBackPressed() {

        if (webView != null &&
                webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.stopLoading();

            webView.loadUrl("about:blank");

            webView.clearHistory();

            webView.removeAllViews();

            webView.destroy();

            webView = null;
        }

        super.onDestroy();
    }
}
