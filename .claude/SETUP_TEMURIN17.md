# Temurin 17 Setup for OmniCOT

## Current Status
- ❌ Temurin 17: NOT INSTALLED
- ✅ JDK 23: Installed (but wrong version for this project!)

## Why Temurin 17?
ATAK plugins MUST be built with Java 17 for compatibility. JDK 23 will cause build failures.

## Installation Steps

### 1. Download Temurin 17
Go to: https://adoptium.net/temurin/releases/?version=17

**Select**:
- Version: 17 - LTS
- Operating System: Windows
- Architecture: x64
- Package Type: JDK
- Format: .msi installer

**Direct Link**: https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_windows_hotspot_17.0.13_11.msi

### 2. Install
1. Run the downloaded `.msi` file
2. **IMPORTANT**: Check "Add to PATH" during installation
3. Install to default location: `C:\Program Files\Eclipse Adoptium\jdk-17.x.x`
4. Restart your terminal after installation

### 3. Verify Installation
Open a NEW terminal and run:

```bash
java -version
```

You should see:
```
openjdk version "17.0.13" 2025-01-21 LTS
OpenJDK Runtime Environment Temurin-17.0.13+11 (build 17.0.13+11-LTS)
OpenJDK 64-Bit Server VM Temurin-17.0.13+11 (build 17.0.13+11-LTS, mixed mode, sharing)
```

### 4. Set JAVA_HOME (if needed)
If Java 23 is still showing, manually set JAVA_HOME:

**Windows PowerShell**:
```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.13+11"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
```

**Git Bash**:
```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.13+11"
export PATH="$JAVA_HOME/bin:$PATH"
```

**Permanent (Windows Environment Variables)**:
1. Search "Environment Variables" in Windows
2. Click "Environment Variables"
3. Under "System variables":
   - New: `JAVA_HOME` = `C:\Program Files\Eclipse Adoptium\jdk-17.0.13+11`
   - Edit `Path`: Add `%JAVA_HOME%\bin` to the TOP of the list
4. Restart terminal

### 5. Verify Again
```bash
java -version
javac -version
```

Both should show version 17.

## Quick Setup Script

Save as `setup-java17.sh`:

```bash
#!/bin/bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.13+11"
export PATH="$JAVA_HOME/bin:$PATH"
echo "Java setup complete!"
java -version
```

Then run:
```bash
source setup-java17.sh
```

## Troubleshooting

### "java: command not found"
- Temurin not installed or not in PATH
- Solution: Reinstall with "Add to PATH" checked

### Still shows Java 23
- JAVA_HOME pointing to wrong JDK
- Solution: Check environment variables, ensure Temurin path is BEFORE Java 23 in PATH

### Build fails with "unsupported class file version"
- Wrong Java version being used
- Solution: Run `java -version` before building, ensure it shows 17

---

**After installation, you can build the project!**
```bash
cd ~/Desktop/omni-COT-source/omni-COT
./gradlew assembleCivDebug
```
