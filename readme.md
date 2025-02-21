# Portal MBK Tech Studio App
A simple Android app that displays the WebView of [portal.mbktechstudio.com](https://portal.mbktechstudio.com). This is an Android Studio project.

## Bug
### Known Issue

When the page loads successfully after a network error, the debug message and reload button do not disappear.

## Overview

The app acts as a wrapper around your website. It loads your site in a WebView while providing:
- A smooth user experience with a green loader indicating page loading progress.
- Version control using a REST API. The API checks if the current app version is up-to-date; if not, the user is prompted to update.
- Portal availability check through the same API. If the portal is down, the user is notified and redirected to the main website ([mbktechstudio.com](https://mbktechstudio.com)).
- Navigation control using the mobile back button to load the previous web page.

## Features

- **WebView Display:** Loads and displays the website directly in the app.
- **Version Control:** Uses a REST API endpoint (`https://api.mbktechstudio.com/api/poratlAppVersion`) to verify the current version. If the version is lower than the latest, an update prompt is displayed. If the user selects the "Skip Update" option, the notification will appear again on the next app start.
- **Portal Health Check:** Checks the `PortaLive` status through the API. If the portal is not active, the app informs the user and redirects them to the main site.
- **Loading Indicator:** A progress bar (green loader) indicates the page loading status.
- **Back Navigation:** Allows users to navigate to the previous web page using the mobile back button.

## API Response Example

The REST API returns a response similar to the following:

```json
{
  "VersionNumber": "1.1.2",
  "Url": "https://download.portal.mbktechstudio.com/Assets/portal_mbktechstudio.apk",
  "PortaLive": "true"
}
```

**Note:**

- `VersionNumber`: Indicates the latest app version available.
- `Url`: APK download URL when an update is available.
- `PortaLive`: A value of `"true"` means the portal site is live; any other value indicates that the portal is not active.

## How It Works

### Launch & Display:

- On launch, the app immediately loads the webpage through the WebView.
- The progress bar (green loader) is visible until the webpage is fully loaded.
- **Example Image:**

  <img style="height:400px; width:180px;" src="https://raw.githubusercontent.com/MIbnEKhalid/PortalAndroidApp/refs/heads/img/loader.gif">

### Version & Health Check:

- The app calls the update API to fetch the latest version details and portal status.
- If the app’s version is outdated, an update dialog is shown.
- **Example Image:**

  <img style="height:400px; width:180px;" src="https://raw.githubusercontent.com/MIbnEKhalid/PortalAndroidApp/refs/heads/img/update.jpg">
- If the user chooses to "Skip Update," the update notification will reappear the next time the app starts.
- If the portal is not live (`PortaLive` is not `"true"`), a notice dialog informs the user and redirects them to the main website.
- **Example Image:**

  <img style="height:400px; width:180px;" src="https://raw.githubusercontent.com/MIbnEKhalid/PortalAndroidApp/refs/heads/img/webDown.jpg">

### User Interaction:

- In case of load errors, a debug message and a reload button are displayed to allow the user to retry loading the webpage.
- **Example Image:**

  <img style="height:400px; width:180px;" src="https://raw.githubusercontent.com/MIbnEKhalid/PortalAndroidApp/refs/heads/img/error.jpg">
- Users can navigate to the previous web page using the mobile back button.

## License

**Note:** Only The Source Code Of This Website Is Covered Under The **[MIT License](https://opensource.org/license/mit)**.  
The Project Documentation Covered Under The **[Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License](https://creativecommons.org/licenses/by-nc-sa/4.0/)** But Some **Images, Blog Posts, And Other Content Are NOT  
Covered Under This License And Remain The Intellectual Property Of The Author**.

See the [LICENSE](LICENSE.md) file for details.
 
## Contact

For questions or contributions, please contact Muhammad Bin Khalid at [mbktechstudio.com/Support](https://mbktechstudio.com/Support/?Project=WebPortalApp), [support@mbktechstudio.com](mailto:support@mbktechstudio.com) or [chmuhammadbinkhalid28.com](mailto:chmuhammadbinkhalid28.com).
