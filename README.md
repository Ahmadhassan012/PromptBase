# PromptBase

PromptBase is a prompt manager for Android. Designed for power users and AI enthusiasts, it allows you to store, organize, and transform your complex AI prompts into ready-to-copy templates with dynamic variables.

---

## Features

- **Dynamic Templating**: Use `{{ variable }}` or `{{ variable : default }}` syntax to create reusable prompt templates with live preview.
- **Markdown Rendering**: Custom parser supporting Headers, Code Blocks, Lists, and Paragraphs with variable highlighting.
- **Smart Organization**: Many-to-many tag system with category filter chips (All, by tag, Others for untagged).
- **Instant Search**: Real-time filtering across title and content.
- **Trash System**: Soft-delete with 30-day auto-expiry, restore, and empty trash.
- **Backup & Restore**: JSON export/import via system file picker with format validation and duplicate skipping.
- **The Magic Sheet**: Dedicated bottom-sheet UI for filling variables and previewing the final prompt before copying.
- **Premium Theme**: Material 3 with custom indigo-purple palette, full typography scale, light/dark mode.
- **Private and Offline**: 100% local storage using Room (SQLite). Your data never leaves your device.

---

## Technical Stack

- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose (Material 3)
- **Database**: Room Persistence Library v2.7.0 (Offline-first architecture)
- **Asynchrony**: Kotlin Coroutines and Flow
- **Serialization**: Moshi for JSON export/import
- **Architecture**: MVVM with Repository pattern
- **CI/CD**: GitHub Actions (Automated APK/AAB builds and releases)

---

## Getting Started

### Prerequisites
- Android Studio Ladybug or newer.
- Android SDK 36 (target), min SDK 24.
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

PromptBase follows MVVM with Repository pattern:
- **Data Layer**: Room Entities (Prompt, Tag, PromptTagCrossRef), DAOs, and PromptRepository.
- **Domain Logic**: VariableParser for template variable extraction and replacement.
- **Presentation Layer**: State-driven UI using ViewModels (StateFlow) and Jetpack Compose.
