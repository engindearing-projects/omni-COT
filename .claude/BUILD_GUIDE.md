# OmniCOT Plugin Build Guide

**PERMANENT REFERENCE** - Read this every time we work on this project!

## Critical Configuration

### Build Requirements
- **Java**: Temurin JDK 17 (Eclipse Adoptium)
  - Download: https://adoptium.net/temurin/releases/?version=17
  - **NOT JDK 23** - Must be Java 17!
- **Gradle**: Wrapper included (8.13)
- **ATAK SDK**: 5.4.0.25 located at `C:\Users\jwyli\Downloads\ATAK-CIV-5.4.0.25-SDK`

### Version Configuration (app/build.gradle)
```gradle
ext.PLUGIN_VERSION = "0.1"
ext.ATAK_VERSION = "5.4.0"      // MUST be 5.4.0 for Play Store compatibility
def takdevVersion = '3.+'        // MUST be 3.+ for TAK pipeline compatibility
```

**NEVER change takdevVersion to 2.+ - the pipeline requires 3.+!**

## Local Build Setup

### 1. Create local.properties
```properties
sdk.dir=C:/Users/jwyli/Downloads/ATAK-CIV-5.4.0.25-SDK/ATAK-CIV-5.4.0.25-SDK
takrepo.url=https://artifacts.tak.gov/artifactory/maven-release
takrepo.user=<your tak.gov username>
takrepo.password=<your tak.gov password>
takdev.plugin=.

# Keystore for local signing
keystore.file=C:/Users/jwyli/Downloads/ATAK-CIV-5.4.0.25-SDK/ATAK-CIV-5.4.0.25-SDK/android_keystore
keystore.password=atakatak
key.alias=androiddebugkey
key.password=atakatak
```

**IMPORTANT**: `local.properties` is in .gitignore and should NEVER be committed!

### 2. Set Java Environment
```bash
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.x.x"
export PATH="$JAVA_HOME/bin:$PATH"
```

Verify: `java -version` should show "Temurin-17"

### 3. Build Commands

#### Test Local Build (civDebug)
```bash
cd ~/Desktop/omni-COT-source/omni-COT
./gradlew assembleCivDebug
```

Output: `app/build/outputs/apk/civ/debug/ATAK-Plugin-omni-COT-0.1-*-5.4.0-civ-debug.apk`

#### Release Build (civRelease) - Locally Signed
```bash
cd ~/Desktop/omni-COT-source/omni-COT
./gradlew bundleCivRelease
```

Output:
- `app/build/outputs/bundle/civRelease/*.aab`
- `app/build/outputs/apk/civRelease/*-civ-release.apk`

#### Clean Build
```bash
./gradlew clean
```

## Pipeline Submission Workflow

### 1. Prepare Source Zip for TAK Pipeline

**CRITICAL**: The pipeline zip must NOT contain:
- ❌ `local.properties` (contains your credentials!)
- ❌ `build/` directories
- ❌ `.gradle/` directories
- ❌ Signed APKs/AABs

```bash
# From the parent directory
cd ~/Desktop/omni-COT-source

# Create clean zip (this is already in .gitignore)
powershell.exe -Command "Compress-Archive -Path 'C:\Users\jwyli\Desktop\omni-COT-source\omni-COT' -DestinationPath 'C:\Users\jwyli\Desktop\omni-COT-pipeline-submission.zip' -Force"
```

**What gets included**:
- ✅ Source code (.java files)
- ✅ `app/build.gradle` (with ATAK_VERSION and takdevVersion)
- ✅ `template.local.properties` (template only, no credentials)
- ✅ Resources, assets, manifest
- ✅ Gradle wrapper files
- ✅ `.gitignore`

**Pipeline handles**:
- Signing with TAK's official keystore
- Building release APK and AAB
- Security scanning (Fortify, OWASP Dependency Check)

### 2. Upload to TAK Pipeline

1. Go to: https://tak.gov/third-party-plugins (or your pipeline URL)
2. Upload: `C:\Users\jwyli\Desktop\omni-COT-pipeline-submission.zip`
3. Wait for build artifacts (usually ~5-10 minutes)
4. Download artifacts containing:
   - Signed APK
   - Signed AAB
   - Security scan reports
   - Build logs

### 3. Quick Upload Helper Script

Save to `~/.bashrc` or create as `upload-to-desktop.sh`:

```bash
#!/bin/bash
# Quick zip and copy to Desktop for TAK upload

cd ~/Desktop/omni-COT-source
ZIP_NAME="omni-COT-pipeline-$(date +%Y%m%d-%H%M%S).zip"

powershell.exe -Command "Compress-Archive -Path 'C:\Users\jwyli\Desktop\omni-COT-source\omni-COT' -DestinationPath 'C:\Users\jwyli\Desktop\$ZIP_NAME' -Force"

echo "Created: ~/Desktop/$ZIP_NAME"
echo "Ready to upload to tak.gov!"
```

## Installation to Device

### Via ADB
```bash
cd ~/Desktop/omni-COT-source/omni-COT/app/build/outputs/apk/civ/debug
adb devices
adb install -r ATAK-Plugin-omni-COT-*.apk
```

## Troubleshooting

### Build Fails: "Unknown Task validateSigningCivRelease"
- This is NORMAL - the task is skipped by design
- Pipeline signing is commented out in build.gradle

### Build Fails: Java Version Error
- Check: `java -version` shows Temurin 17
- Fix: Set JAVA_HOME to Temurin 17 path

### Build Fails: "takdevVersion 2.+ not found"
- Fix: Change to `def takdevVersion = '3.+'` in app/build.gradle

### Pipeline Returns 0 Artifacts
- Check ATAK_VERSION is set correctly (5.4.0)
- Check takdevVersion is 3.+ (NOT 2.+)
- Ensure zip doesn't contain build artifacts or local.properties

### Gradle Daemon Issues
```bash
./gradlew --stop
./gradlew --no-daemon assembleCivDebug
```

## File Structure Reference

```
omni-COT/
├── app/
│   ├── build.gradle          # CRITICAL: ATAK_VERSION & takdevVersion here
│   ├── proguard-gradle.txt
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/engindearing/omnicot/
│   │   │   ├── res/
│   │   │   ├── assets/
│   │   │   └── AndroidManifest.xml
│   └── build/                # NOT included in pipeline zip
├── gradle/
├── .gitignore               # Excludes local.properties, build/
├── template.local.properties # Template only
├── local.properties         # YOUR FILE - never commit!
├── gradlew
└── .claude/
    └── BUILD_GUIDE.md       # This file
```

## Quick Command Reference

```bash
# Build locally
cd ~/Desktop/omni-COT-source/omni-COT
./gradlew assembleCivDebug

# Create pipeline zip
cd ~/Desktop/omni-COT-source
powershell.exe -Command "Compress-Archive -Path 'omni-COT' -DestinationPath '../omni-COT-pipeline.zip' -Force"

# Copy to Desktop
mv ~/Desktop/omni-COT-pipeline.zip ~/Desktop/

# Install to device
adb install -r app/build/outputs/apk/civ/debug/*.apk
```

## Git Workflow

```bash
# Commit changes
cd ~/Desktop/omni-COT-source/omni-COT
git add .
git commit -m "Description of changes"
git push origin main

# NEVER commit local.properties!
```

---

**Last Updated**: 2025-11-03
**Project**: OmniCOT ATAK Plugin
**GitHub**: https://github.com/engindearing-projects/omni-COT
