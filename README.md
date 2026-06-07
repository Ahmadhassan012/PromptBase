# PromptBase

PromptBase is a prompt manager for Android. Designed for power users and AI enthusiasts, it allows you to store, organize, and transform your complex AI prompts into ready-to-copy templates with dynamic variables.

---

## Features

- **Dynamic Templating**: Use `{{ variable }}` or `{{ variable : default }}` syntax to create reusable prompt templates with live preview.
- **Markdown Rendering**: Custom parser supporting Headers, Code Blocks, Lists, and Paragraphs with variable highlighting.
- **Smart Organization**: Many-to-many tag system with category filter chips (All, by tag, Others for untagged).
- **Instant Search**: Real-time filtering across title and content via SQL-backed queries.
- **Trash System**: Soft-delete with 30-day auto-expiry, restore, and empty trash.
- **Backup & Restore**: JSON export/import via system file picker with format validation and duplicate skipping.
- **Magic Fill Sheet**: Dedicated bottom-sheet UI for filling variables and previewing the final prompt before copying.
- **Undo Snackbar**: Archiving a prompt shows an undo action via Snackbar.
- **Premium Theme**: Material 3 with custom indigo-purple palette, full typography scale, light/dark mode.
- **Private and Offline**: 100% local storage using Room (SQLite). Your data never leaves your device.

---

## Technical Stack

- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose (Material 3) with Jetpack Navigation (NavHost)
- **Database**: Room Persistence Library v2.7.0 (Offline-first architecture)
- **Dependency Injection**: Hilt 2.59.1
- **Asynchrony**: Kotlin Coroutines and Flow
- **Serialization**: Moshi for JSON export/import
- **Architecture**: MVVM with Repository pattern
- **Testing**: JUnit 4, MockK, Turbine, Room in-memory testing
- **CI/CD**: GitHub Actions (Automated APK/AAB builds and releases)
- **Build System**: Gradle 9.3.1 with AGP 9.1.1

---

## Project Structure

```
app/src/main/java/com/promptbase/app/
├── MainActivity.kt              # Entry point + NavHost container
├── PromptBaseApplication.kt     # @HiltAndroidApp
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt       # Room DB v2 with MIGRATION_1_2
│   │   ├── DatabaseModule.kt    # Hilt module (DB + DAO + seed)
│   │   └── PromptDao.kt         # DAOs with SQL-backed filtering
│   ├── model/
│   │   └── Prompt.kt            # Entities: Prompt, Tag, PromptTagCrossRef
│   └── repository/
│       └── PromptRepository.kt  # @Singleton repository
├── ui/
│   ├── PromptViewModel.kt       # @HiltViewModel with StateFlow
│   ├── HomeScreen.kt            # Prompt list + search + filters
│   ├── EditorScreen.kt          # Create/edit prompt with tags
│   ├── PromptCard.kt            # Individual prompt card composable
│   ├── MagicFillSheet.kt        # Bottom-sheet variable filler
│   ├── MarkdownRenderer.kt      # Markdown parser + renderer
│   ├── ProfileScreen.kt         # Export/import/trash screen
│   ├── TrashScreen.kt           # Trash management screen
│   └── theme/                   # Material 3 theme (Color, Theme, Type)
└── util/
    ├── VariableParser.kt        # Regex variable extraction/replacement
    └── ExportImport.kt          # Moshi-based JSON export/import
```

---

## Getting Started

### Prerequisites
- Android Studio Otter or newer.
- Android SDK 36 (target), min SDK 24.
- JDK 21.

### Local Setup
1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/promptbase.git
   cd promptbase/promptbase
   ```
2. **Open in Android Studio**: Select the `promptbase` folder.
3. **Build the project**: Allow Gradle to sync and download dependencies.
4. **Run**: Deploy to an emulator or physical device.

### Codespace / Headless Build
```bash
# Install Android SDK
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q commandlinetools-linux-11076708_latest.zip
mkdir -p android-sdk/cmdline-tools
mv cmdline-tools android-sdk/cmdline-tools/latest
export ANDROID_HOME=$PWD/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
yes | sdkmanager --licenses
sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools"

# Install JDK 21 (if not available)
wget -q https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.11+10/OpenJDK21U-jdk_x64_linux_hotspot_21.0.11_10.tar.gz
tar -xzf OpenJDK21U-jdk_x64_linux_hotspot_21.0.11_10.tar.gz
export JAVA_HOME=$PWD/jdk-21.0.11+10

# Build
./gradlew assembleDebug
```

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

PromptBase follows MVVM with Repository pattern and Hilt DI:

- **Data Layer**: Room Entities (Prompt, Tag, PromptTagCrossRef), DAOs, and PromptRepository. Filtering is SQL-backed via `flatMapLatest` in the ViewModel.
- **Domain Logic**: VariableParser for template variable extraction and replacement.
- **Presentation Layer**: State-driven UI using `@HiltViewModel` (StateFlow) and Jetpack Compose with NavHost for navigation.

---

## Testing

Unit tests are located under `app/src/test/java/com/promptbase/app/`:

- **VariableParserTest** (17 cases): Regex extraction and replacement
- **PromptDaoTest** (14 cases): Room DAO queries via in-memory database
- **PromptViewModelTest** (16 cases): ViewModel state flows with MockK + Turbine

Run tests:
```bash
./gradlew test
```
