package com.example.portalmbktechstudio;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageButton;

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
    Button reloadButton;
    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_SKIP_UPDATE = "SkipUpdate";
    private static final String TAG = "WebViewDebug";
    // ImageButton backButton;
    // Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myWeb = findViewById(R.id.myWeb);
        progressBar = findViewById(R.id.progressBar);
        debugMessage = findViewById(R.id.debugMessage); // Initialize debugMessage TextView
        reloadButton = findViewById(R.id.reloadButton);
        debugMessage.setVisibility(View.GONE); // Hide initially
        reloadButton.setVisibility(View.GONE); // Hide initially

        myWeb.getSettings().setJavaScriptEnabled(true);
        myWeb.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                // Show error details in the debug message
                debugMessage.setVisibility(View.VISIBLE);
                reloadButton.setVisibility(View.VISIBLE);
                debugMessage.setText("Error: " + description + "\nURL: " + failingUrl);
                Log.e(TAG, "WebView error: " + description);
            }
        });

        /*backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.GONE); // Initially hidden

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (myWeb.canGoBack()) {
                    myWeb.goBack();
                } else {
                    backButton.setVisibility(View.GONE); // Hide if there's no previous page
                }
            }
        });

// Add WebView navigation listener to control back button visibility
        myWeb.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (myWeb.canGoBack()) {
                    backButton.setVisibility(View.VISIBLE);
                } else {
                    backButton.setVisibility(View.GONE);
                }
            }
        });
*/
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
        } catch (Exception e) {
            debugMessage.setVisibility(View.VISIBLE);
            reloadButton.setVisibility(View.GONE);
            debugMessage.setText("Failed to load URL: " + e.getMessage());
            Log.e(TAG, "Failed to load WebView URL", e);
        }

        reloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Reload the WebView
                myWeb.reload();
                // Hide the debug message and reload button
                debugMessage.setVisibility(View.GONE);
                reloadButton.setVisibility(View.GONE);
            }
        });


        // Always check for updates on app launch
        checkForUpdates();
    }

    @Override
    public void onBackPressed() {
        if (myWeb.canGoBack()) {
            myWeb.goBack();
        } else {
            super.onBackPressed();
        }
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
        Log.d(TAG, "`checkForUpdates` Reset SkipUpdate preference to false");

        new Thread(() -> {
            try {
                Log.d(TAG, "`checkForUpdates` Starting update check...");
                URL url = new URL("https://api.mbktechstudio.com/api/poratlAppVersion");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                Log.d(TAG, "`checkForUpdates` Connected to update API: " + url.toString());

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                Log.d(TAG, "`checkForUpdates` Response from update API: " + response.toString());

                JSONObject jsonResponse = new JSONObject(response.toString());
                String latestVersion = jsonResponse.getString("VersionNumber");
                String updateUrl = jsonResponse.getString("Url");
                String PortalLive = jsonResponse.getString("PortaLive");

                if(PortalLive != "true")
                {
                    runOnUiThread(this::showPortalDownNotice);
                }
                Log.d(TAG, "`checkForUpdates` Latest version retrieved: " + latestVersion);
                Log.d(TAG, "`checkForUpdates` Update URL retrieved: " + updateUrl);

                String currentVersion = "1.2";
                Log.d(TAG, "`checkForUpdates` Current version: " + currentVersion);

                if (isVersionOutdated(currentVersion, latestVersion)) {
                    Log.d(TAG, "`checkForUpdates` Version is outdated. Prompting user to update...");
                    runOnUiThread(() -> showUpdateDialog(updateUrl, currentVersion, latestVersion));
                } else {
                    Log.d(TAG, "`checkForUpdates` App is up-to-date.");
                }
            } catch (Exception e) {
                Log.e(TAG, "`checkForUpdates` Error checking for updates: " + e.getMessage(), e);
            }

        }).start();
    }

    private void showPortalDownNotice() {
        String message = "The portal is currently unavailable. You will be redirected to the main site until the portal is live again.";

        new AlertDialog.Builder(this)
                .setTitle("Portal Unavailable")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Redirect", (dialog, which) -> {
                    String mainSiteUrl = "https://mbktechstudio.com";
                    Log.d(TAG, "Redirecting user to: " + mainSiteUrl);
                    myWeb.loadUrl("https://mbktechstudio.com/");

                })
                .show();
    }
    private void showUpdateDialog(String updateUrl, String currentVersion, String latestVersion) {
        Log.d(TAG, "`showUpdateDialog` Preparing to show update dialog.");
        Log.d(TAG, "`showUpdateDialog` Current Version: " + currentVersion + ", Latest Version: " + latestVersion);
        Log.d(TAG, "`showUpdateDialog` Update URL: " + updateUrl);

        String message = "Current Version: " + currentVersion + "\nNew Version: " + latestVersion +
                "\n\nA new version of the app is available. Please update to the latest version.";

        try {
            new AlertDialog.Builder(this)
                    .setTitle("Update Available")
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton("Update", (dialog, which) -> {
                        Log.d(TAG, "`showUpdateDialog` User clicked Update. Redirecting to: " + updateUrl);
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
                        startActivity(browserIntent);
                    })
                    .setNegativeButton("Skip", (dialog, which) -> {
                        Log.d(TAG, "`showUpdateDialog` User clicked Skip. Saving preference to skip updates.");
                        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                        editor.putBoolean(KEY_SKIP_UPDATE, true);
                        editor.apply();

                        dialog.dismiss();
                    })
                    .show();
            Log.d(TAG, "`showUpdateDialog` Update dialog shown successfully.");
        } catch (Exception e) {
            Log.e(TAG, "`showUpdateDialog` Error displaying update dialog: ", e);
        }
    }
}
