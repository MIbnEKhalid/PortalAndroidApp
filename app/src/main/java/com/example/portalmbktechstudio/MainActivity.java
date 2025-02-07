package com.example.portalmbktechstudio;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import android.content.SharedPreferences;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    WebView myWeb;
    ProgressBar progressBar;
    TextView debugMessage; // TextView for displaying debug messages
    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_SKIP_UPDATE = "SkipUpdate";
    private static final String TAG = "WebViewDebug";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myWeb = findViewById(R.id.myWeb);
        progressBar = findViewById(R.id.progressBar);
        debugMessage = findViewById(R.id.debugMessage); // Initialize debugMessage TextView

        debugMessage.setVisibility(View.GONE); // Hide initially

        myWeb.getSettings().setJavaScriptEnabled(true);
        myWeb.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                // Show error details in the debug message
                debugMessage.setVisibility(View.VISIBLE);
                debugMessage.setText("Error: " + description + "\nURL: " + failingUrl);
                Log.e(TAG, "WebView error: " + description);
            }
        });

        myWeb.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        try {
            myWeb.loadUrl("https://portal.mbktechstudio.com/");
            Log.i(TAG, "Loading URL: https://portal.mbktechstudio.com/");
        } catch (Exception e) {
            debugMessage.setVisibility(View.VISIBLE);
            debugMessage.setText("Failed to load URL: " + e.getMessage());
            Log.e(TAG, "Failed to load WebView URL", e);
        }

        // Always check for updates on app launch
        checkForUpdates();
    }

    private boolean isVersionOutdated(String currentVersion, String latestVersion) {
        return currentVersion.compareTo(latestVersion) < 0;
    }

    private void checkForUpdates() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Reset SkipUpdate preference to ensure dialog shows every time the app opens
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_SKIP_UPDATE, false);
        editor.apply();

        new Thread(() -> {
            try {
                URL url = new URL("https://api.mbktechstudio.com/api/poratlAppVersion");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                String latestVersion = jsonResponse.getString("VersionNumber");
                String updateUrl = jsonResponse.getString("Url");

                String currentVersion = "1.1";

                if (isVersionOutdated(currentVersion, latestVersion)) {
                    runOnUiThread(() -> showUpdateDialog(updateUrl, currentVersion, latestVersion));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking for updates", e);
            }
        }).start();
    }

    private void showUpdateDialog(String updateUrl, String currentVersion, String latestVersion) {
        String message = "Current Version: " + currentVersion + "\nNew Version: " + latestVersion + "\n\nA new version of the app is available. Please update to the latest version.";

        new AlertDialog.Builder(this)
                .setTitle("Update Available")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Update", (dialog, which) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
                    startActivity(browserIntent);
                })
                .setNegativeButton("Skip", (dialog, which) -> {
                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putBoolean(KEY_SKIP_UPDATE, true);
                    editor.apply();

                    dialog.dismiss();
                })
                .show();
    }
}
