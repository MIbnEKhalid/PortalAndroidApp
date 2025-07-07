package com.example.portalmbktechstudio;

// Import necessary libraries

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.content.SharedPreferences;
import android.util.Log;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.webkit.ConsoleMessage;

public class MainActivity extends AppCompatActivity {
    // Declare UI components
    private WebView myWeb;
    private ProgressBar progressBar;
    private TextView debugMessage;
    private Button reloadButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton settingsButton;

    // SharedPreferences for saving app preferences
    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_SKIP_UPDATE = "SkipUpdate";
    private static final String KEY_SELECTED_WEBSITES = "SelectedWebsites";
    private static final String KEY_STARTUP_WEBSITE = "StartupWebsite";

    // Log tag for debugging
    private static final String TAG = "WebViewDebug";

    // URLs and version constants
    private static String MAIN_URL = "https://portal.mbktechstudio.com/mbkauthe/login";
    private static final String REST_API_URL = "https://api.mbktechstudio.com/api/poratlAppVersion";
    private static final String REDIRECT_URL = "https://mbktechstudio.com";
    private static final String CURRENT_VERSION = "1.6";

    // Predefined websites
    private ArrayList<WebsiteItem> predefinedWebsites;
    private float dX, dY;
    private boolean isDragging = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializePredefinedWebsites();
        initializeViews();
        setupWebView();
        setupReloadButton();
        setupSettingsButton();
        loadStartupWebsite();
        checkForUpdates();
    }

    private void initializePredefinedWebsites() {
        predefinedWebsites = new ArrayList<>();
        predefinedWebsites.add(new WebsiteItem("Web Portal", "https://portal.mbktechstudio.com/mbkauthe/login"));
        predefinedWebsites.add(new WebsiteItem("Main Website", "https://mbktechstudio.com"));
        predefinedWebsites.add(new WebsiteItem("Download Apps", "https://download.mbktechstudio.com"));
        predefinedWebsites.add(new WebsiteItem("Unilib", "https://unilib.mbktechstudio.com"));

        // Load saved selections
        loadWebsiteSelections();
    }

    private void initializeViews() {
        myWeb = findViewById(R.id.myWeb);
        progressBar = findViewById(R.id.progressBar);
        debugMessage = findViewById(R.id.debugMessage);
        reloadButton = findViewById(R.id.reloadButton);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        settingsButton = findViewById(R.id.settingsButton);

        // Hide debug message and reload button initially
        debugMessage.setVisibility(View.GONE);
        reloadButton.setVisibility(View.GONE);
    }

    // Configure WebView settings and load URL
    private void setupWebView() {
        // Enable JavaScript
        myWeb.getSettings().setJavaScriptEnabled(true);
        
        // Enable DOM storage (localStorage and sessionStorage)
        myWeb.getSettings().setDomStorageEnabled(true);
        
        // Enable database storage
        myWeb.getSettings().setDatabaseEnabled(true);
        
        // Note: setAppCacheEnabled() has been deprecated and removed in API 33+
        // Application cache is no longer supported in modern WebView
        // DOM storage (localStorage/sessionStorage) is the recommended alternative
        
        // Allow file access
        myWeb.getSettings().setAllowFileAccess(true);
        
        // Allow content URL access
        myWeb.getSettings().setAllowContentAccess(true);
        
        // Enable mixed content mode (for HTTPS sites that load HTTP resources)
        myWeb.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // Set user agent to ensure proper web compatibility
        myWeb.getSettings().setUserAgentString(myWeb.getSettings().getUserAgentString() + " PortalMBKTechStudio/1.6");
        
        // Enable WebView debugging (for Chrome DevTools)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        
        myWeb.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d(TAG, "Page started loading: " + url);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Page finished loading: " + url);
                
                // Hide error UI if page loads successfully
                debugMessage.setVisibility(View.GONE);
                reloadButton.setVisibility(View.GONE);
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(TAG, "WebView error: " + description + " (Code: " + errorCode + ")");
                // Display error message if loading fails
                showError(description, failingUrl);
            }
            
            @Override
            public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                Log.w(TAG, "SSL Error: " + error.toString());
                // For production, you should properly handle SSL errors
                // For now, proceed to allow the portal to load
                handler.proceed();
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "URL loading: " + url);
                
                // Handle portal URLs within the WebView
                if (url.contains("portal.mbktechstudio.com") || 
                    url.contains("api.mbktechstudio.com") || 
                    url.contains("mbktechstudio.com")) {
                    return false; // Let WebView handle it
                }
                
                // Open external URLs in system browser
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            }
        });
        myWeb.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                // Update progress bar based on page load progress
                updateProgressBar(newProgress);
            }
            
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                // Log frontend console messages to Android LogCat
                String logLevel = getLogLevel(consoleMessage.messageLevel());
                String message = "WebView Console [" + logLevel + "]: " + 
                               consoleMessage.message() + 
                               " -- From line " + consoleMessage.lineNumber() + 
                               " of " + consoleMessage.sourceId();
                
                // Log to Android LogCat based on console message level
                switch (consoleMessage.messageLevel()) {
                    case ERROR:
                        Log.e(TAG, message);
                        // Check if it's a localStorage error and log additional info
                        if (consoleMessage.message().contains("localStorage") || 
                            consoleMessage.message().contains("getItem") || 
                            consoleMessage.message().contains("setItem") || 
                            consoleMessage.message().contains("removeItem")) {
                            Log.e(TAG, "localStorage error detected - DOM Storage enabled: " + 
                                  myWeb.getSettings().getDomStorageEnabled());
                        }
                        break;
                    case WARNING:
                        Log.w(TAG, message);
                        break;
                    case DEBUG:
                        Log.d(TAG, message);
                        break;
                    case LOG:
                    case TIP:
                    default:
                        Log.i(TAG, message);
                        break;
                }
                
                return true; // Return true to consume the message
            }
            
            @Override
            public void onPermissionRequest(android.webkit.PermissionRequest request) {
                // Handle permission requests from web content
                Log.d(TAG, "Permission requested: " + java.util.Arrays.toString(request.getResources()));
                
                // For now, grant common permissions that are safe for web content
                String[] permissions = request.getResources();
                for (String permission : permissions) {
                    if (permission.equals(android.webkit.PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID) ||
                        permission.equals(android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE) ||
                        permission.equals(android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                        request.grant(new String[]{permission});
                        return;
                    }
                }
                
                // Grant all requested permissions for portal functionality
                request.grant(permissions);
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

    // Setup the settings button with draggable functionality
    private void setupSettingsButton() {
        settingsButton.setOnTouchListener(new View.OnTouchListener() {
            private long startTime = 0;
            private boolean isLongPress = false;
            
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startTime = System.currentTimeMillis();
                        isLongPress = false;
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        isDragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;
                        
                        // Keep button within screen bounds
                        if (newX < 0) newX = 0;
                        if (newY < 0) newY = 0;
                        if (newX > getResources().getDisplayMetrics().widthPixels - view.getWidth()) {
                            newX = getResources().getDisplayMetrics().widthPixels - view.getWidth();
                        }
                        if (newY > getResources().getDisplayMetrics().heightPixels - view.getHeight()) {
                            newY = getResources().getDisplayMetrics().heightPixels - view.getHeight();
                        }
                        
                        view.setX(newX);
                        view.setY(newY);
                        isDragging = true;
                        
                        // Check for long press
                        if (System.currentTimeMillis() - startTime > 500 && !isLongPress) {
                            isLongPress = true;
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (isLongPress) {
                            // Long press detected - show quick access
                            showQuickAccessDialog();
                        } else if (!isDragging) {
                            // Regular click - show settings
                            showSettingsDialog();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        ListView websiteListView = dialogView.findViewById(R.id.websiteListView);
        ListView startupListView = dialogView.findViewById(R.id.startupListView);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        
        // Setup website selection list
        WebsiteAdapter websiteAdapter = new WebsiteAdapter(this, predefinedWebsites);
        websiteAdapter.setOnWebsiteClickListener(website -> {
            // Load the selected website
            myWeb.loadUrl(website.getUrl());
            dialog.dismiss();
        });
        websiteListView.setAdapter(websiteAdapter);
        
        // Setup startup website selection list
        ArrayList<WebsiteItem> startupWebsites = new ArrayList<>(predefinedWebsites);
        WebsiteAdapter startupAdapter = new WebsiteAdapter(this, startupWebsites);
        startupAdapter.setStartupSelection(true);
        
        // Set current startup selection
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String startupUrl = prefs.getString(KEY_STARTUP_WEBSITE, MAIN_URL);
        for (int i = 0; i < startupWebsites.size(); i++) {
            if (startupWebsites.get(i).getUrl().equals(startupUrl)) {
                startupAdapter.setSelectedPosition(i);
                break;
            }
        }
        
        startupListView.setAdapter(startupAdapter);
        
        saveButton.setOnClickListener(v -> {
            saveWebsiteSelections();
            saveStartupWebsite(startupAdapter);
            dialog.dismiss();
            
            // Optionally reload with new startup website
            if (startupAdapter.getSelectedPosition() >= 0) {
                String newStartupUrl = startupWebsites.get(startupAdapter.getSelectedPosition()).getUrl();
                if (!newStartupUrl.equals(MAIN_URL)) {
                    myWeb.loadUrl(newStartupUrl);
                }
            }
        });
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    // Method to get only selected websites
    public ArrayList<WebsiteItem> getSelectedWebsites() {
        ArrayList<WebsiteItem> selectedWebsites = new ArrayList<>();
        for (WebsiteItem website : predefinedWebsites) {
            if (website.isSelected()) {
                selectedWebsites.add(website);
            }
        }
        return selectedWebsites;
    }

    // Method to show quick access dialog with only selected websites
    public void showQuickAccessDialog() {
        ArrayList<WebsiteItem> selectedWebsites = getSelectedWebsites();
        
        if (selectedWebsites.isEmpty()) {
            // Show settings dialog if no websites are selected
            showSettingsDialog();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.quick_access_title);
        
        String[] websiteNames = new String[selectedWebsites.size()];
        for (int i = 0; i < selectedWebsites.size(); i++) {
            websiteNames[i] = selectedWebsites.get(i).getTitle();
        }
        
        builder.setItems(websiteNames, (dialog, which) -> {
            WebsiteItem selectedWebsite = selectedWebsites.get(which);
            myWeb.loadUrl(selectedWebsite.getUrl());
        });
        
        builder.setNegativeButton("Settings", (dialog, which) -> showSettingsDialog());
        builder.show();
    }

    private void loadWebsiteSelections() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> selectedUrls = prefs.getStringSet(KEY_SELECTED_WEBSITES, new HashSet<>());
        
        for (WebsiteItem website : predefinedWebsites) {
            website.setSelected(selectedUrls.contains(website.getUrl()));
        }
    }

    private void saveWebsiteSelections() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        Set<String> selectedUrls = new HashSet<>();
        for (WebsiteItem website : predefinedWebsites) {
            if (website.isSelected()) {
                selectedUrls.add(website.getUrl());
            }
        }
        
        editor.putStringSet(KEY_SELECTED_WEBSITES, selectedUrls);
        editor.apply();
    }

    private void saveStartupWebsite(WebsiteAdapter adapter) {
        if (adapter.getSelectedPosition() >= 0) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            String startupUrl = predefinedWebsites.get(adapter.getSelectedPosition()).getUrl();
            editor.putString(KEY_STARTUP_WEBSITE, startupUrl);
            editor.apply();
            
            // Update MAIN_URL for future use
            MAIN_URL = startupUrl;
        }
    }

    private void loadStartupWebsite() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String startupUrl = prefs.getString(KEY_STARTUP_WEBSITE, MAIN_URL);
        MAIN_URL = startupUrl;
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


        // Check if the app version is outdated
        if (isVersionOutdated(CURRENT_VERSION, latestVersion)) {
            runOnUiThread(() -> showUpdateDialog(updateUrl, CURRENT_VERSION, latestVersion));
        }
    }

    // Compare version numbers
    private boolean isVersionOutdated(String currentVersion, String latestVersion) {
        return currentVersion.compareTo(latestVersion) < 0;
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

    // Helper method to convert ConsoleMessage.MessageLevel to readable string
    private String getLogLevel(ConsoleMessage.MessageLevel level) {
        switch (level) {
            case ERROR:
                return "ERROR";
            case WARNING:
                return "WARNING";
            case DEBUG:
                return "DEBUG";
            case LOG:
                return "LOG";
            case TIP:
                return "TIP";
            default:
                return "INFO";
        }
    }
}

