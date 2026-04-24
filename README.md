# Nlkkn

| Field | Value |
|---|---|
| Package | `com.tes.my` |
| Version | `1.0` |
| Min SDK | API 28 |
| Language | Java |

## Included Libraries
- **RecyclerView** — `MyAdapter` + `item_list.xml`
- **Retrofit 2** — `ApiClient` + `ApiService` (replace BASE_URL)
- **Room Database** — `User` entity + `UserDao` + `AppDatabase`
- **ViewModel + LiveData** — `MainViewModel`
- **Glide** — image loading library
- **Navigation Component** — Bottom nav with Home/Dashboard/Profile fragments
- **Hilt** — dependency injection (`MyApplication` + `di/AppModule`)
- **ProGuard** — enabled on release builds

## Getting Started in Android Studio

1. Extract this ZIP
2. Open **Android Studio** → `File > Open` → select `Nlkkn/`
3. Wait for Gradle sync to finish
4. Edit `app/src/main/java/com/tes/my/MainActivity.java`
5. Edit the layout at `app/src/main/res/layout/activity_main.xml`
6. Run on emulator or device

## Build via GitHub Actions (No PC Required)

1. Create a new repository on github.com
2. Upload/push the contents of `Nlkkn/` to the `main` branch
3. GitHub Actions will automatically build the APK
4. Download the APK from the **Actions → Artifacts** tab

> **Debug APK**: `Nlkkn-debug`  
> **Release APK**: `Nlkkn-release-unsigned`

## Hilt — Dependency Injection

- `MyApplication` is annotated with `@HiltAndroidApp` and declared in the manifest
- `di/AppModule` is where you add `@Provides` methods for injecting dependencies
- Add `@AndroidEntryPoint` to any Activity or Fragment that uses injection

## ⚠️ Room Database Warning

`fallbackToDestructiveMigration()` will **delete all data** when the schema changes.
Replace with proper migrations before shipping to production.

## Project Structure

```
Nlkkn/
├── app/src/main/
│   ├── java/com/tes/my/
│   │   ├── MainActivity.java
│   │   ├── MyApplication.java
│   │   ├── di/AppModule.java
│   │   ├── MyAdapter.java
│   │   ├── ApiClient + ApiService.java
│   │   ├── User + UserDao + AppDatabase.java
│   │   ├── MainViewModel.java
│   │   └── Home/Dashboard/ProfileFragment.java
│   └── res/layout/activity_main.xml
├── .github/workflows/build.yml
├── .gitignore
└── app/build.gradle
```
