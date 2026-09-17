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
    private static final int AUDIO_PERMISSION_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        webView.setLayoutParams(params);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        // Police AI interface uses JavaScript
        settings.setJavaScriptEnabled(true);

        // Needed for the web app
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // Better mobile experience
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        // Allow audio/video playback
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(final PermissionRequest request) {

                runOnUiThread(() -> {

                    String[] resources = request.getResources();

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

                                requestAudioPermission();
                            }

                            return;
                        }
                    }
                });
            }
        });

        // Request microphone permission for Police AI voice input
        requestAudioPermission();

        // Load the Police AI web interface
        webView.loadUrl(
                "file:///android_asset/index.html"
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
            webView.destroy();
        }

        super.onDestroy();
    }
}
