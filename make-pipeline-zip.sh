#!/bin/bash
# OmniCOT Pipeline Submission Zip Creator
# Creates a clean zip for TAK third-party pipeline submission
# Usage: ./make-pipeline-zip.sh

set -e

echo "=================================================="
echo "OmniCOT Pipeline Submission Zip Creator"
echo "=================================================="
echo ""

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Check if we're in a git repo
if [ ! -d ".git" ]; then
    echo "ERROR: Not in a git repository!"
    echo "This script must be run from the omni-COT root directory"
    exit 1
fi

# Check for uncommitted changes
if ! git diff-index --quiet HEAD --; then
    echo "WARNING: You have uncommitted changes!"
    echo "It's recommended to commit your changes before creating submission zip"
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Create timestamp
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
ZIP_NAME="omni-COT-pipeline-$TIMESTAMP.zip"
DEST_PATH="$HOME/Desktop/$ZIP_NAME"

echo "Creating pipeline submission zip..."
echo "  Source: $SCRIPT_DIR"
echo "  Output: $DEST_PATH"
echo ""

# Use git archive to create clean zip (respects .gitignore)
# Create zip with omni-COT root folder as required by TAK pipeline
git archive --format=zip --prefix=omni-COT/ --output="$DEST_PATH" HEAD

# Verify the zip was created
if [ ! -f "$DEST_PATH" ]; then
    echo "ERROR: Failed to create zip file!"
    exit 1
fi

# Check that local.properties is NOT in the zip (but template.local.properties should be)
echo "Checking zip contents for local.properties..."
if unzip -l "$DEST_PATH" 2>/dev/null | grep " local\.properties$"; then
    echo ""
    echo "ERROR: local.properties found in zip! This should not happen!"
    echo "Check your .gitignore file"
    echo ""
    echo "All files with 'local' in name:"
    unzip -l "$DEST_PATH" 2>/dev/null | grep "local"
    rm "$DEST_PATH"
    exit 1
else
    echo "✓ local.properties correctly excluded"
fi

# Verify template.local.properties IS in the zip
if ! unzip -l "$DEST_PATH" 2>/dev/null | grep -q "template.local.properties"; then
    echo "WARNING: template.local.properties not found in zip"
fi

# Get file size
SIZE=$(du -h "$DEST_PATH" | cut -f1)

echo "=================================================="
echo "✅ SUCCESS!"
echo "=================================================="
echo ""
echo "Created: $ZIP_NAME"
echo "Location: ~/Desktop/$ZIP_NAME"
echo "Size: $SIZE"
echo ""
echo "What's included:"
echo "  ✅ Source code (.java files)"
echo "  ✅ Build configuration (build.gradle with ATAK 5.4.0)"
echo "  ✅ Resources and assets"
echo "  ✅ Documentation"
echo ""
echo "What's excluded:"
echo "  ❌ local.properties (your credentials)"
echo "  ❌ build/ directories"
echo "  ❌ .gradle/ cache"
echo "  ❌ Signed APK/AAB files"
echo ""
echo "Next steps:"
echo "  1. Go to https://tak.gov/third-party-plugins"
echo "  2. Upload: ~/Desktop/$ZIP_NAME"
echo "  3. Wait for build artifacts (~5-10 minutes)"
echo ""
echo "Build will use:"
echo "  - ATAK Version: 5.4.0 (Play Store compatible)"
echo "  - takdevVersion: 2.+ (Pipeline compatible - required for ATAK 4.2+)"
echo "  - TAK's official signing keystore"
echo ""
