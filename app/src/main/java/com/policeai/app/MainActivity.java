package com.policeai.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.ViewGroup;

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

        // JavaScript
        settings.setJavaScriptEnabled(true);

        // Storage
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // Mobile display
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);

        // Disable zoom controls
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        // Allow audio/media
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Normal WebView client
        webView.setWebViewClient(new WebViewClient());

        // Handle microphone permission
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(
                    final PermissionRequest request) {

                runOnUiThread(() -> {

                    if (request == null) {
                        return;
                    }

                    String[] resources = request.getResources();

                    if (resources == null) {
                        return;
                    }

                    for (String resource : resources) {

                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE
                                .equals(resource)) {

                            if (checkSelfPermission(
                                    Manifest.permission.RECORD_AUDIO)
                                    == PackageManager.PERMISSION_GRANTED) {

                                request.grant(new String[]{
                                        PermissionRequest
                                                .RESOURCE_AUDIO_CAPTURE
                                });

                            } else {

                                pendingPermissionRequest = request;

                                requestPermissions(
                                        new String[]{
                                                Manifest.permission
                                                        .RECORD_AUDIO
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

        // Ask for microphone permission
        requestAudioPermission();

        // Load the local Police AI interface
        webView.loadUrl(
                "file:///android_asset/index.html"
        );
    }

    private void requestAudioPermission() {

        if (android.os.Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

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
            int[] grantResults) {

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

        if (webView != null && webView.canGoBack()) {

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
