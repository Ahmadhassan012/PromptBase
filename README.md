# PromptBase

PromptBase is a prompt manager for Android. Designed for power users and AI enthusiasts, it allows you to store, organize, and transform your complex AI prompts into ready-to-copy templates with dynamic variables.

---

## Features

- **Dynamic Templating**: Use `{{ variable }}` or `{{ variable : default }}` syntax to create reusable prompt templates.
- **Markdown Rendering**: High-performance rendering for structured prompts (Headers, Lists, Code Blocks) using the Inter typeface.
- **Smart Organization**: Many-to-many tag system to categorize your library precisely.
- **Instant Search**: Real-time indexed search across titles and prompt content.
- **Private and Offline**: 100% local storage using Room (SQLite). Your data never leaves your device.
- **The Magic Sheet**: A dedicated UI for filling variables and previewing the final prompt before copying.

---

## Technical Stack

- **Language**: Kotlin 2.1.0
- **UI Framework**: Jetpack Compose (Material 3)
- **Database**: Room Persistence Library (Offline-first architecture)
- **Asynchrony**: Kotlin Coroutines and Flow
- **Architecture**: MVVM and Clean Architecture
- **CI/CD**: GitHub Actions (Automated APK/AAB builds and releases)

---

## Getting Started

### Prerequisites
- Android Studio Ladybug or newer.
- Android SDK 36 (target).
- JDK 17.

### Local Setup
1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/promptbase.git
   cd promptbase/promptbase
   ```
2. **Open in Android Studio**: Select the `promptbase` folder.
3. **Build the project**: Allow Gradle to sync and download dependencies.
4. **Run**: Deploy to an emulator or physical device.

---

## Deployment and CI/CD

PromptBase is equipped with a GitHub Actions pipeline that automates the release process.

### Required GitHub Secrets
To enable automated builds, add the following to your repository secrets:
- `KEYSTORE_BASE64`: Your release `.jks` file encoded in Base64.
- `STORE_PASSWORD`: Password for your keystore.
- `KEY_PASSWORD`: Password for your key alias.

### Creating a Release
Simply push a version tag to trigger a GitHub Release with signed artifacts:
```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## Architecture

PromptBase follows Clean Architecture principles to ensure the codebase remains maintainable and "sync-ready" for future cloud features:
- **Data Layer**: Room Entities, DAOs, and the Repository pattern.
- **Domain Layer**: Business logic for variable parsing and text resolution.
- **Presentation Layer**: State-driven UI using ViewModels and Jetpack Compose.
