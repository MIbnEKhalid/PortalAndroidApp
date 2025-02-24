package com.example.portalmbktechstudio;

// Import necessary libraries

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {
    // Declare UI components
    private WebView myWeb;
    private ProgressBar progressBar;
    private TextView debugMessage;
    private Button reloadButton;
    private SwipeRefreshLayout swipeRefreshLayout;

    // SharedPreferences for saving app preferences
    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_SKIP_UPDATE = "SkipUpdate";

    // Log tag for debugging
    private static final String TAG = "WebViewDebug";

    // URLs and version constants
    private static String MAIN_URL = "https://portal.mbktechstudio.com/";
    private static final String REST_API_URL = "https://api.mbktechstudio.com/api/poratlAppVersion";
    private static final String REDIRECT_URL = "https://mbktechstudio.com";
    private static final String CURRENT_VERSION = "1.2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeViews();
        setupWebView();
        setupReloadButton();
        checkForUpdates();
    }

    private void initializeViews() {
        myWeb = findViewById(R.id.myWeb);
        progressBar = findViewById(R.id.progressBar);
        debugMessage = findViewById(R.id.debugMessage);
        reloadButton = findViewById(R.id.reloadButton);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);

        // Hide debug message and reload button initially
        debugMessage.setVisibility(View.GONE);
        reloadButton.setVisibility(View.GONE);
    }

    // Configure WebView settings and load URL
    private void setupWebView() {
        myWeb.getSettings().setJavaScriptEnabled(true);
        myWeb.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // Display error message if loading fails
                showError(description, failingUrl);
            }
        });
        myWeb.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                // Update progress bar based on page load progress
                updateProgressBar(newProgress);
            }
        });

        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            myWeb.reload(); // Reload the current page
            swipeRefreshLayout.setRefreshing(false); // Stop the refresh indicator
        });
        try {
            // Load the main portal URL
            myWeb.loadUrl(MAIN_URL);
        } catch (Exception e) {
            // Handle URL loading exceptions
            showError(e.getMessage(), MAIN_URL);
        }
    }

    // Display error message and show debug info
    private void showError(String description, String url) {
        debugMessage.setVisibility(View.VISIBLE);
        reloadButton.setVisibility(View.VISIBLE);
        debugMessage.setText("Error: " + description + "\nURL: " + url);
        Log.e(TAG, "WebView error: " + description);
    }

    // Update progress bar visibility and progress
    private void updateProgressBar(int newProgress) {
        if (newProgress < 100) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(newProgress);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    // Setup the reload button to refresh the WebView
    private void setupReloadButton() {
        reloadButton.setOnClickListener(view -> {
            myWeb.reload();
            debugMessage.setVisibility(View.GONE);
            reloadButton.setVisibility(View.GONE);
        });
    }

    @Override
    public void onBackPressed() {
        // Handle back button navigation in WebView
        if (myWeb.canGoBack()) {
            myWeb.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // Check for app updates by calling a REST API
    private void checkForUpdates() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SKIP_UPDATE, false).apply(); // Reset skip update preference
        Log.d(TAG, "`checkForUpdates` Reset SkipUpdate preference to false");

        new Thread(() -> {
            try {
                // Establish connection to the update API
                HttpURLConnection connection = (HttpURLConnection) new URL(REST_API_URL).openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                Log.d(TAG, "`checkForUpdates` Connected to update API: " + REST_API_URL);

                // Parse response from the API
                String response = readResponse(connection);
                JSONObject jsonResponse = new JSONObject(response);
                handleUpdateResponse(jsonResponse);
            } catch (Exception e) {
                // Log errors during update check
                Log.e(TAG, "`checkForUpdates` Error checking for updates: " + e.getMessage(), e);
            }
        }).start();
    }

    // Read response from API connection
    private String readResponse(HttpURLConnection connection) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        Log.d(TAG, "`checkForUpdates` Response from update API: " + response.toString());
        return response.toString();
    }

    // Handle the update API response
    private void handleUpdateResponse(JSONObject jsonResponse) throws Exception {
        String latestVersion = jsonResponse.getString("VersionNumber");
        String updateUrl = jsonResponse.getString("Url");
        String portalLive = jsonResponse.getString("PortaLive");

        if (!"true".equals(portalLive)) {
            // Show notice if the portal is down
            Log.d(TAG, "`checkForUpdates` Portal Down");
            runOnUiThread(this::showPortalDownNotice);
        }

        // Check if the app version is outdated
        if (isVersionOutdated(CURRENT_VERSION, latestVersion)) {
            runOnUiThread(() -> showUpdateDialog(updateUrl, CURRENT_VERSION, latestVersion));
        }
    }

    // Compare version numbers
    private boolean isVersionOutdated(String currentVersion, String latestVersion) {
        return currentVersion.compareTo(latestVersion) < 0;
    }

    // Show a dialog when the portal is down
    private void showPortalDownNotice() {
        new AlertDialog.Builder(this)
                .setTitle("Portal Unavailable")
                .setMessage("The portal is currently unavailable. You will be redirected to the main site until the portal is live again.")
                .setCancelable(false)
                .setPositiveButton("Redirect", (dialog, which) -> myWeb.loadUrl(REDIRECT_URL))
                .show();
    }

    // Show an update dialog when a new version is available
    private void showUpdateDialog(String updateUrl, String currentVersion, String latestVersion) {
        new AlertDialog.Builder(this)
                .setTitle("Update Available")
                .setMessage("Current Version: " + currentVersion + "\nNew Version: " + latestVersion +
                        "\n\nA new version of the app is available. Please update to the latest version.")
                .setCancelable(false)
                .setPositiveButton("Update", (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))))
                .setNegativeButton("Skip", (dialog, which) -> getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(KEY_SKIP_UPDATE, true).apply())
                .show();
    }
}


/*



    private void initializeUrl() {
        new Thread(() -> {
            try {
                Log.d(TAG, "`initializeUrl` Connection Check 1");
                HttpURLConnection connection = (HttpURLConnection) new URL("https://api.mbktechstudio.com/api/poratlAppUrl").openConnection();
                Log.d(TAG, "`initializeUrl` Connection Check 2");

                connection.setRequestMethod("GET");
                connection.connect();
                Log.d(TAG, "`initializeUrl` Connected to update API: " + "https://api.mbktechstudio.com/api/poratlAppUrl");

                // Parse response from the API
                String response = readResponse(connection);
                JSONObject jsonResponse = new JSONObject(response);
                String PortalWebUrl = jsonResponse.getString("PortalWebUrl");
                Log.d(TAG, "`initializeUrl` PortalWebUrl: " + PortalWebUrl);

                // Update the WebView on the main thread
                runOnUiThread(() -> {
                    MAIN_URL = PortalWebUrl;
                    myWeb.loadUrl(MAIN_URL);
                });
            } catch (Exception e) {
                Log.e(TAG, "`initializeUrl` Error checking for updates: " + e.getMessage(), e);
                runOnUiThread(() -> showError("Failed to fetch update URL.", ""));
            }
        }).start();
    }



package com.example.portalmbktechstudio;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Apply window insets for a full-screen layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            view.setPadding(
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            );
            return insets;
        });

        // Initialize views
        Button submitButton = findViewById(R.id.SubmitButton);
        EditText editText = findViewById(R.id.editTextText);
        WebView webView = findViewById(R.id.myWeb);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        // Configure WebView with progress tracking
        setupWebView(webView, progressBar);

        // Set up button click listener to load URL and update UI visibility
        submitButton.setOnClickListener(v -> loadUrl(editText, webView, submitButton, progressBar));
    }

private void setupWebView(WebView webView, ProgressBar progressBar) {
    webView.getSettings().setJavaScriptEnabled(true);
    webView.setWebChromeClient(new WebChromeClient() {
        // Update progress bar based on WebView loading progress
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
}

private void loadUrl(EditText editText, WebView webView, Button submitButton, ProgressBar progressBar) {
    String url = editText.getText().toString().trim();
    if (!url.isEmpty()) {
        // Ensure the URL starts with a valid scheme
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        // Hide the input fields and show the WebView
        editText.setVisibility(View.GONE);
        submitButton.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        // Load the URL (progress bar will track progress automatically)
        webView.loadUrl(url);
    }
}
}
*/

