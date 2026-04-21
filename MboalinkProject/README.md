# MboaLink Android App 🇨🇲

A native Android wrapper for the MboaLink Flask web app.

---

## ✅ STEP-BY-STEP SETUP

### STEP 1 — Install Android Studio
Download from: https://developer.android.com/studio
Install with default settings (includes Android SDK, emulator tools).

---

### STEP 2 — Open this project
1. Launch Android Studio
2. Click **"Open"** (NOT "New Project")
3. Navigate to and select this `MboaLink/` folder
4. Click **OK** — Android Studio will sync Gradle automatically (takes 2–5 minutes first time)

---

### STEP 3 — Run your Flask backend

You need your Flask backend (`app.py`) running so the app has something to connect to.

**Option A — Run locally (for development/testing):**
```bash
pip install flask requests pillow
python app.py
```
Flask will start at http://localhost:5000

The Android emulator accesses your PC's localhost via the special IP `10.0.2.2`,
which is already set in `MainActivity.kt`:
```kotlin
private val BASE_URL = "http://10.0.2.2:5000"
```

**Option B — Deploy online (for real phone testing or production):**

Deploy to Render.com (free):
1. Push your files to a GitHub repo
2. Go to https://render.com → New → Web Service
3. Connect your repo, set start command: `python app.py`
4. Copy the URL you get (e.g. https://mboalink.onrender.com)

Then open `MainActivity.kt` and change:
```kotlin
// Comment out this line:
// private val BASE_URL = "http://10.0.2.2:5000"

// Uncomment and update this line:
private val BASE_URL = "https://mboalink.onrender.com"
```

---

### STEP 4 — Run on Emulator

1. In Android Studio, click **Tools → Device Manager**
2. Click **"Create Device"**
3. Choose: **Pixel 6** → Next
4. Download system image: **API 34 (Android 14)** → Next → Finish
5. Press the ▶ **Run** button (green triangle at the top)
6. The emulator boots and MboaLink launches automatically

---

### STEP 5 — Run on a Real Android Phone

1. On your phone: go to **Settings → About Phone**
2. Tap **Build Number** 7 times to enable Developer Options
3. Go to **Settings → Developer Options → Enable USB Debugging**
4. Connect phone to PC via USB cable
5. In Android Studio, select your phone from the device dropdown
6. Press ▶ **Run**

> ⚠️ For a real phone, you MUST use Option B (deployed URL) above,
> since your phone cannot reach your PC's localhost.

---

## 📁 Project Structure

```
MboaLink/
├── app/
│   ├── src/main/
│   │   ├── java/cm/mboalink/app/
│   │   │   ├── SplashActivity.kt     ← Branded launch screen
│   │   │   └── MainActivity.kt       ← WebView + offline handling
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_splash.xml
│   │   │   │   └── activity_main.xml
│   │   │   ├── values/
│   │   │   │   ├── colors.xml        ← MboaLink green + orange brand
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── drawable/
│   │   │       └── ic_launcher_foreground.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## 🔧 Features Included

| Feature | Status |
|---|---|
| Branded green/orange splash screen | ✅ |
| Cameroon flag colours | ✅ |
| WebView loads Flask app | ✅ |
| Swipe-down to refresh | ✅ |
| Progress bar while loading | ✅ |
| Offline / no-connection screen | ✅ |
| Back button navigates within app | ✅ |
| File/photo upload support | ✅ |
| WhatsApp / tel: / mailto: links | ✅ |
| Internet + storage permissions | ✅ |

---

## 🚀 Building a Release APK

1. In Android Studio: **Build → Generate Signed Bundle / APK**
2. Choose **APK**
3. Create a new keystore (save the password safely!)
4. Select **release** build variant
5. Click **Finish** — APK appears in `app/release/app-release.apk`

You can then share the APK directly, or upload to Google Play Store.

---

## ❓ Troubleshooting

**"Gradle sync failed"**
→ Check you have internet. Android Studio → File → Invalidate Caches → Restart

**App shows blank white screen**
→ Flask is not running. Start `python app.py` first.

**App shows "No Connection" on emulator**
→ Make sure Flask is running AND the BASE_URL is `http://10.0.2.2:5000`

**App shows "No Connection" on real phone**
→ You need to deploy Flask online (Render.com) and update BASE_URL.

**File upload doesn't work**
→ Accept storage permission when the app asks. Check phone Settings → Apps → MboaLink → Permissions.
