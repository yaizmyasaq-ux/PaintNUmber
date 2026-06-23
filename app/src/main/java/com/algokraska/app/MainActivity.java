package com.algokraska.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int REQUEST_OPEN_IMAGE = 1001;
    private static final int REQUEST_SAVE_PNG = 1002;

    private WebView webView;
    private WebView printWebView;
    private ValueCallback<Uri[]> fileCallback;

    private byte[] pendingPng;

    @SuppressLint({
            "SetJavaScriptEnabled",
            "JavascriptInterface"
    })
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(
                Color.rgb(15, 23, 42)
        );

        getWindow().setNavigationBarColor(
                Color.rgb(2, 6, 23)
        );

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
        );

        webView.addJavascriptInterface(
                new AndroidBridge(),
                "AndroidBridge"
        );

        webView.setWebViewClient(
                new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            WebResourceRequest request
                    ) {
                        Uri uri = request.getUrl();
                        String scheme = uri.getScheme();

                        if ("file".equalsIgnoreCase(scheme)
                                || "data".equalsIgnoreCase(scheme)
                                || "about".equalsIgnoreCase(scheme)) {
                            return false;
                        }

                        try {
                            startActivity(
                                    new Intent(
                                            Intent.ACTION_VIEW,
                                            uri
                                    )
                            );
                        } catch (Exception exception) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Не удалось открыть ссылку",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        return true;
                    }
                }
        );

        webView.setWebChromeClient(
                new WebChromeClient() {
                    @Override
                    public boolean onShowFileChooser(
                            WebView currentWebView,
                            ValueCallback<Uri[]> callback,
                            FileChooserParams parameters
                    ) {
                        if (fileCallback != null) {
                            fileCallback.onReceiveValue(null);
                        }

                        fileCallback = callback;

                        Intent intent = new Intent(
                                Intent.ACTION_OPEN_DOCUMENT
                        );

                        intent.addCategory(
                                Intent.CATEGORY_OPENABLE
                        );

                        intent.setType("image/*");

                        try {
                            startActivityForResult(
                                    intent,
                                    REQUEST_OPEN_IMAGE
                            );

                            return true;
                        } catch (Exception exception) {
                            fileCallback = null;

                            Toast.makeText(
                                    MainActivity.this,
                                    "Не удалось открыть выбор изображения",
                                    Toast.LENGTH_LONG
                            ).show();

                            return false;
                        }
                    }
                }
        );

        webView.loadUrl(
                "file:///android_asset/index.html"
        );
    }

    public final class AndroidBridge {

        @JavascriptInterface
        public void saveDataUrl(
                String dataUrl,
                String suggestedName
        ) {
            try {
                int comma = dataUrl.indexOf(',');

                if (comma < 0) {
                    throw new IllegalArgumentException(
                            "Некорректный PNG"
                    );
                }

                String encoded = dataUrl.substring(comma + 1);

                pendingPng = Base64.decode(
                        encoded,
                        Base64.DEFAULT
                );

                runOnUiThread(() -> {
                    Intent intent = new Intent(
                            Intent.ACTION_CREATE_DOCUMENT
                    );

                    intent.addCategory(
                            Intent.CATEGORY_OPENABLE
                    );

                    intent.setType("image/png");

                    intent.putExtra(
                            Intent.EXTRA_TITLE,
                            sanitizeName(suggestedName)
                    );

                    try {
                        startActivityForResult(
                                intent,
                                REQUEST_SAVE_PNG
                        );
                    } catch (Exception exception) {
                        pendingPng = null;

                        Toast.makeText(
                                MainActivity.this,
                                "Не удалось открыть окно сохранения",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
            } catch (Exception exception) {
                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "Ошибка подготовки PNG: "
                                        + exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        }

        @JavascriptInterface
        public void printHtml(String html) {
            runOnUiThread(() -> openPrintDialog(html));
        }
    }

    private String sanitizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "raskraska-skhema.png";
        }

        return name.replaceAll(
                "[\\\\/:*?\"<>|]",
                "_"
        );
    }

    private void openPrintDialog(String html) {
        if (html == null || html.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Печатный документ пуст",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (printWebView != null) {
            printWebView.destroy();
        }

        printWebView = new WebView(this);

        printWebView.setWebViewClient(
                new WebViewClient() {
                    private boolean started;

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url
                    ) {
                        if (started) {
                            return;
                        }

                        started = true;

                        view.postDelayed(() -> {
                            PrintManager manager =
                                    (PrintManager)
                                            getSystemService(
                                                    PRINT_SERVICE
                                            );

                            PrintDocumentAdapter adapter =
                                    view.createPrintDocumentAdapter(
                                            "АлгоКраска"
                                    );

                            PrintAttributes attributes =
                                    new PrintAttributes.Builder()
                                            .setMediaSize(
                                                    PrintAttributes
                                                            .MediaSize
                                                            .ISO_A4
                                            )
                                            .setColorMode(
                                                    PrintAttributes
                                                            .COLOR_MODE_COLOR
                                            )
                                            .build();

                            manager.print(
                                    "АлгоКраска",
                                    adapter,
                                    attributes
                            );
                        }, 400);
                    }
                }
        );

        printWebView.loadDataWithBaseURL(
                "https://local.algokraska/",
                html,
                "text/html",
                "UTF-8",
                null
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == REQUEST_OPEN_IMAGE) {
            if (fileCallback == null) {
                return;
            }

            Uri[] result =
                    WebChromeClient
                            .FileChooserParams
                            .parseResult(
                                    resultCode,
                                    data
                            );

            fileCallback.onReceiveValue(result);
            fileCallback = null;
            return;
        }

        if (requestCode == REQUEST_SAVE_PNG) {
            if (resultCode == RESULT_OK
                    && data != null
                    && data.getData() != null
                    && pendingPng != null) {

                Uri uri = data.getData();

                try (OutputStream output =
                             getContentResolver()
                                     .openOutputStream(uri)) {

                    if (output == null) {
                        throw new IllegalStateException(
                                "Файл не открылся"
                        );
                    }

                    output.write(pendingPng);
                    output.flush();

                    Toast.makeText(
                            this,
                            "PNG сохранён",
                            Toast.LENGTH_SHORT
                    ).show();
                } catch (Exception exception) {
                    Toast.makeText(
                            this,
                            "Ошибка сохранения: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            pendingPng = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }

        if (printWebView != null) {
            printWebView.destroy();
            printWebView = null;
        }

        if (webView != null) {
            webView.removeJavascriptInterface(
                    "AndroidBridge"
            );

            webView.destroy();
            webView = null;
        }

        pendingPng = null;

        super.onDestroy();
    }
}
