# AndroidTools

| Field | Value |
|---|---|
| Package | `com.mondev.app` |
| Version | `1.0` |
| Min SDK | API 24 |
| Language | Kotlin |

## Included Libraries
- **Navigation Component** — Bottom nav with Home/Dashboard/Profile fragments
- **ProGuard** — enabled on release builds

## Getting Started in Android Studio

1. Extract this ZIP
2. Open **Android Studio** → `File > Open` → select `AndroidTools/`
3. Wait for Gradle sync to finish
4. Edit `app/src/main/java/com/mondev/app/MainActivity.kt`
5. Edit the layout at `app/src/main/res/layout/activity_main.xml`
6. Run on emulator or device

## Build via GitHub Actions (No PC Required)

1. Create a new repository on github.com
2. Upload/push the contents of `AndroidTools/` to the `main` branch
3. GitHub Actions will automatically build the APK
4. Download the APK from the **Actions → Artifacts** tab

> **Debug APK**: `AndroidTools-debug`  
> **Release APK**: `AndroidTools-release-unsigned`

## Project Structure

```
AndroidTools/
├── app/src/main/
│   ├── java/com/mondev/app/
│   │   ├── MainActivity.kt
│   │   └── Home/Dashboard/ProfileFragment.kt
│   └── res/layout/activity_main.xml
├── .github/workflows/build.yml
├── .gitignore
└── app/build.gradle
```
