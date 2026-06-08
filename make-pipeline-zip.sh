#!/bin/bash
# OmniCOT Pipeline Submission Zip Creator
# Creates clean zips for TAK third-party pipeline submission
# Usage: ./make-pipeline-zip.sh
#
# This script creates submission zips for ATAK 5.6 + 5.7 (the supported
# range under the new ATAK 5.6+ build env). Plugins built against
# ATAK 5.4 or earlier still ship from releases/v0.6/ — the build env
# changed substantially at 5.6 (compileSdk 36, AGP 8.13, takdev 3.+).
#
# Versions produced:
# - ATAK 5.6.0 (current stable)
# - ATAK 5.7.0 (latest)

set -e

echo "=========================================================="
echo "OmniCOT Multi-Version Pipeline Submission Zip Creator"
echo "=========================================================="
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
    echo "It's recommended to commit your changes before creating submission zips"
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Store the current branch to return to it later
ORIGINAL_BRANCH=$(git branch --show-current)

# Create timestamp
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

# Function to create a zip for a specific ATAK version
create_version_zip() {
    local ATAK_VERSION=$1
    local VERSION_LABEL=$2
    local ZIP_NAME="omnicot-pipeline-atak${ATAK_VERSION}-${TIMESTAMP}.zip"
    local DEST_PATH="$SCRIPT_DIR/$ZIP_NAME"

    echo ""
    echo "=========================================="
    echo "Creating zip for ATAK ${ATAK_VERSION}"
    echo "=========================================="

    # Create a temporary branch for this version
    TEMP_BRANCH="temp-build-${ATAK_VERSION}-$$"
    git checkout -b "$TEMP_BRANCH" >/dev/null 2>&1

    # Modify build.gradle to set the ATAK version
    # Use sed without backup file on macOS
    sed -i '' "s/ext.ATAK_VERSION = \"[0-9.]*\"/ext.ATAK_VERSION = \"${ATAK_VERSION}\"/" app/build.gradle

    # Stage the change
    git add app/build.gradle >/dev/null 2>&1

    # Create a temporary commit (won't be pushed)
    # Use --allow-empty in case the version is already set correctly
    git commit --allow-empty -m "Temp: Set ATAK version to ${ATAK_VERSION} for pipeline submission" >/dev/null 2>&1

    # Create zip from this commit
    git archive --format=zip --prefix=omnicot/ --output="$DEST_PATH" HEAD

    # Verify the zip was created
    if [ ! -f "$DEST_PATH" ]; then
        echo "ERROR: Failed to create zip file for ATAK ${ATAK_VERSION}!"
        # Cleanup
        git checkout "$ORIGINAL_BRANCH" >/dev/null 2>&1
        git branch -D "$TEMP_BRANCH" >/dev/null 2>&1
        exit 1
    fi

    # Verify ATAK version in the zip
    echo "Verifying ATAK version in zip..."
    if unzip -p "$DEST_PATH" "omnicot/app/build.gradle" | grep -q "ext.ATAK_VERSION = \"${ATAK_VERSION}\""; then
        echo "✓ ATAK version ${ATAK_VERSION} correctly set in build.gradle"
    else
        echo "ERROR: ATAK version not correctly set in zip!"
        rm "$DEST_PATH"
        git checkout "$ORIGINAL_BRANCH" >/dev/null 2>&1
        git branch -D "$TEMP_BRANCH" >/dev/null 2>&1
        exit 1
    fi

    # Check that local.properties is NOT in the zip
    echo "Checking zip contents for local.properties..."
    if unzip -l "$DEST_PATH" 2>/dev/null | grep " local\.properties$"; then
        echo ""
        echo "ERROR: local.properties found in zip! This should not happen!"
        echo "Check your .gitignore file"
        rm "$DEST_PATH"
        git checkout "$ORIGINAL_BRANCH" >/dev/null 2>&1
        git branch -D "$TEMP_BRANCH" >/dev/null 2>&1
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

    echo ""
    echo "✅ SUCCESS: ATAK ${ATAK_VERSION} zip created"
    echo "   File: $ZIP_NAME"
    echo "   Size: $SIZE"

    # Cleanup: Go back to original branch and delete temp branch
    git checkout "$ORIGINAL_BRANCH" >/dev/null 2>&1
    git branch -D "$TEMP_BRANCH" >/dev/null 2>&1

    # Store info for summary
    echo "$ZIP_NAME|$SIZE|$ATAK_VERSION" >> /tmp/omnicot-zips-$$.txt
}

# Clean up any previous temp file
rm -f /tmp/omnicot-zips-$$.txt

# Create zips for ATAK 5.6 + 5.7
echo "Creating submission zips for ATAK 5.5, 5.6 and 5.7 (all from current branch)..."
echo ""

create_version_zip "5.5.0" "Supported"
create_version_zip "5.6.0" "Stable"
create_version_zip "5.7.0" "Latest"

# Print summary
echo ""
echo "=========================================================="
echo "✅ ALL SUBMISSION ZIPS CREATED SUCCESSFULLY!"
echo "=========================================================="
echo ""
echo "Created ${TIMESTAMP} submission package with 3 versions:"
echo ""

# Read the temp file and display results
while IFS='|' read -r filename size version; do
    echo "📦 ATAK ${version}"
    echo "   File: ${filename}"
    echo "   Size: ${size}"
    echo "   Target: https://tak.gov/third-party-plugins"
    echo ""
done < /tmp/omnicot-zips-$$.txt

rm -f /tmp/omnicot-zips-$$.txt

echo "What's included in each zip:"
echo "  ✅ Source code (.java files)"
echo "  ✅ Build configuration (build.gradle with respective ATAK version)"
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
echo "  2. Upload each zip for the corresponding ATAK version:"
echo "     - omnicot-pipeline-atak5.6.0-${TIMESTAMP}.zip → ATAK 5.6 pipeline"
echo "     - omnicot-pipeline-atak5.7.0-${TIMESTAMP}.zip → ATAK 5.7 pipeline"
echo "  3. Wait for build artifacts (~5-10 minutes per version)"
echo ""
echo "Build configuration:"
echo "  - takdevVersion: 3.+ (ATAK 5.6+ build env)"
echo "  - TAK's official signing keystore"
echo "  - All variants: CIV, MIL, GOV"
echo ""
echo "Note: Your working directory is unchanged."
echo "      All temporary branches were cleaned up."
echo ""
