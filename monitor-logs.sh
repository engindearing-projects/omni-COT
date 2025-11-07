#!/bin/bash
# OmniCOT Plugin Log Monitor
# Monitors all relevant logs for debugging Remote ID and Bluetooth issues

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo "========================================"
echo "OmniCOT Plugin Log Monitor"
echo "========================================"
echo ""
echo "Monitoring for:"
echo "  - Bluetooth connection events"
echo "  - Remote ID data reception"
echo "  - Drone detections and CoT dispatching"
echo "  - Errors and exceptions"
echo ""
echo "Press Ctrl+C to stop"
echo "========================================"
echo ""

# Clear logcat buffer
adb logcat -c

# Monitor logcat with colored output
adb logcat -v time | while read line; do
    # Highlight different types of messages with colors
    if echo "$line" | grep -iq "error\|exception\|fatal\|crash"; then
        echo -e "${RED}$line${NC}"
    elif echo "$line" | grep -iq "warning\|warn"; then
        echo -e "${YELLOW}$line${NC}"
    elif echo "$line" | grep -iq "bluetooth.*connect\|started reading"; then
        echo -e "${GREEN}$line${NC}"
    elif echo "$line" | grep -iq "json\|remoteid.*data\|drone detected"; then
        echo -e "${CYAN}$line${NC}"
    elif echo "$line" | grep -iq "cot.*dispatch\|displayed on map"; then
        echo -e "${MAGENTA}$line${NC}"
    elif echo "$line" | grep -iq "omnicot\|bluetoothmanager\|remoteid"; then
        echo -e "${BLUE}$line${NC}"
    fi
done | grep -iE "omnicot|bluetooth|remoteid|gyb|drone|cot|socket|exception|error|json"
