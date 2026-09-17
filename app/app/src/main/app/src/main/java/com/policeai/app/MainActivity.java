package com.policeai.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.view.ViewGroup;

import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

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

        WebSettings settings = webView.getSettings();

        // JavaScript is required by Police AI
        settings.setJavaScriptEnabled(true);

        // Web app storage
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // Mobile display
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        // Voice/audio
        settings.setMediaPlaybackRequiresUserGesture(false);

        /*
         * Securely load index.html from:
         *
         * app/src/main/assets/index.html
         *
         * using an HTTPS-style local origin.
         */
        final WebViewAssetLoader assetLoader =
                new WebViewAssetLoader.Builder()
                        .addPathHandler(
                                "/assets/",
                                new WebViewAssetLoader
                                        .AssetsPathHandler(this)
                        )
                        .build();

        webView.setWebViewClient(
                new WebViewClientCompat() {

                    @Override
                    public android.webkit.WebResourceResponse
                    shouldInterceptRequest(
                            WebView view,
                            android.webkit.WebResourceRequest request
                    ) {
                        return assetLoader.shouldInterceptRequest(
                                request.getUrl()
                        );
                    }

                    @Override
                    public android.webkit.WebResourceResponse
                    shouldInterceptRequest(
                            WebView view,
                            String url
                    ) {
                        return assetLoader.shouldInterceptRequest(
                                android.net.Uri.parse(url)
                        );
                    }
                }
        );

        webView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public void onPermissionRequest(
                            final PermissionRequest request
                    ) {

                        runOnUiThread(() -> {

                            for (String resource :
                                    request.getResources()) {

                                if (PermissionRequest
                                        .RESOURCE_AUDIO_CAPTURE
                                        .equals(resource)) {

                                    if (checkSelfPermission(
                                            Manifest.permission
                                                    .RECORD_AUDIO
                                    ) == PackageManager
                                            .PERMISSION_GRANTED) {

                                        request.grant(
                                                new String[]{
                                                        PermissionRequest
                                                                .RESOURCE_AUDIO_CAPTURE
                                                }
                                        );

                                    } else {

                                        pendingPermissionRequest =
                                                request;

                                        requestAudioPermission();
                                    }

                                    return;
                                }
                            }
                        });
                    }
                }
        );

        requestAudioPermission();

        // Load bundled Police AI interface
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
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

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
            webView.destroy();
        }

        super.onDestroy();
    }
}
