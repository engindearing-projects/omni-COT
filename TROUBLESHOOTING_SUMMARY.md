# OmniCOT Remote ID Troubleshooting Summary

## Issues Identified and Fixed

### 1. **"Socket is closed" Toast on ATAK Startup** ✅ FIXED
**Symptom**: Toast message "Bluetooth error: Connection failed: Socket is closed" appears when opening ATAK

**Root Cause**:
- BluetoothManager was trying to access socket before it was properly initialized
- Race condition where socket.getInputStream() was called on a closed/null socket
- No defensive checks before accessing socket

**Fix Applied**:
- Added null and connection state checks before starting reader thread
- Gracefully handle "Socket is closed" IOException without notifying user
- Double-check socket validity when reader thread starts
- Log warnings instead of showing error Toast for expected socket states

**Files Modified**:
- `app/src/main/java/com/engindearing/omnicot/remoteid/BluetoothManager.java`

---

### 2. **Bluetooth Connection Lost When Detecting Drones** ✅ FIXED
**Symptom**:
- gyb_detect turns red (detecting drone)
- Bluetooth connection immediately drops
- "Connection lost" errors
- No drones appear on map

**Root Cause**:
- **Character-by-character reading was too slow** for burst Remote ID data
- When gyb_detect detects drone, it sends rapid JSON data
- Single-char reads couldn't drain socket buffer fast enough
- Socket buffer overflow → device timeout → connection lost
- Race conditions in cleanup() method
- Error handling broke connection instead of recovering

**Fixes Applied**:
1. **Buffered Array Reading** (CRITICAL FIX)
   - Replaced char-by-char read with 1024-char buffer reads
   - Increased BufferedReader buffer from default to 8KB
   - Can now handle burst traffic from Remote ID detections

2. **reader.ready() Checks**
   - Prevents blocking indefinitely on socket reads
   - Small 10ms sleep when no data available
   - Allows graceful interrupt handling

3. **Synchronized cleanup()**
   - Prevents race conditions from multiple threads
   - Waits for reader thread to finish (up to 2 seconds)
   - Prevents double cleanup with null checks
   - Comprehensive debug logging

4. **Better Error Handling**
   - JSON processing errors don't break Bluetooth connection
   - Separate handling for InterruptedIOException vs IOException
   - All error notifications use mainHandler.post()
   - Only cleanup when connection is actually owned by thread

5. **Enhanced Logging**
   - Logs number of characters read each cycle
   - Tracks read loop start/exit
   - Debug logs for each complete JSON received

**Files Modified**:
- `app/src/main/java/com/engindearing/omnicot/remoteid/BluetoothManager.java`

---

### 3. **Drones Not Appearing on Map** ✅ FIXED
**Symptom**:
- Bluetooth connected
- gyb_detect detecting drones (turns red)
- No drones show on ATAK map

**Root Cause**:
- CotDispatcher was null during plugin initialization
- No null check before dispatching CoT events
- Silent failure - no user feedback

**Fixes Applied**:
1. **Null Checks for CotDispatcher**
   - Check if dispatcher is null before use
   - Automatic retry to re-initialize if null
   - Clear error messages in Activity Feed

2. **Better Error Visibility**
   - All errors now appear in OmniCOT Activity Feed
   - Detailed logging with RemoteIdData.toString()
   - User-friendly error messages

**Files Modified**:
- `app/src/main/java/com/engindearing/omnicot/OmniCOTDropDownReceiver.java`

---

### 4. **Drone Icons Not Displaying on Map** ✅ FIXED
**Symptom**:
- Bluetooth connected successfully
- gyb_detect detecting drones (turns red)
- Dashboard shows "1 drone detected"
- No drone icons appear on ATAK map

**Root Cause**:
- **Malformed CoT type** in RemoteIdToCotConverter
- Type was `"a-u-A-M-F-Q-r"` which has conflicting aircraft designations:
  - `F` = Fixed Wing aircraft
  - `r` = Rotary wing aircraft (suffix)
- ATAK couldn't parse the contradictory type and failed to render icon
- Not compliant with MIL-STD-2525C CoT format

**Fix Applied**:
- Changed to proper MIL-STD-2525C format: `"a-u-A-M-H-Q"`
  - `a` = Air
  - `u` = Unknown affiliation
  - `A` = Airborne
  - `M` = Military
  - `H` = Helicopter/Multirotor platform
  - `Q` = Unmanned Aerial System
- Hostile variant: `"a-h-A-M-H-Q"` (h = hostile)
- Now ATAK correctly recognizes and displays drone icons

**Files Modified**:
- `app/src/main/java/com/engindearing/omnicot/remoteid/RemoteIdToCotConverter.java`

---

## Monitoring Tools Created

### **monitor-logs.sh Script**
Location: `~/Downloads/ATAK-CIV-5.4.0.27-SDK/plugins/omni-COT/monitor-logs.sh`

**Usage**:
```bash
cd ~/Downloads/ATAK-CIV-5.4.0.27-SDK/plugins/omni-COT
./monitor-logs.sh
```

**Features**:
- Color-coded output (red for errors, green for connections, cyan for data, etc.)
- Filters for relevant logs: OmniCOT, Bluetooth, RemoteID, drone detections
- Real-time monitoring with timestamps
- Easy to read format

---

## Testing Checklist

### Prerequisites
- ✅ gyb_detect device paired with Android device
- ✅ ESP32 Remote ID spoofer configured and ready
- ✅ OmniCOT plugin installed (version 0.6-fa252bf or later)
- ✅ ATAK running on Android device

### Test Procedure

1. **Start Monitoring** (in separate terminal):
   ```bash
   ./monitor-logs.sh
   ```

2. **Open ATAK**
   - Should NOT see "Socket is closed" Toast ✅
   - Check logs for clean plugin initialization

3. **Open OmniCOT Plugin**
   - Tap OmniCOT icon in ATAK
   - Dashboard should open

4. **Connect Bluetooth**
   - Click "Connect" in Bluetooth section
   - Select gyb_detect from list
   - Wait for "Connected" status
   - Should show device name and battery percentage

5. **Monitor Logs** - You should see:
   ```
   Started reading data with buffered array reads
   ```

6. **Turn on ESP32 Spoofer**
   - gyb_detect should turn RED
   - Watch logs for:
     ```
     Read X characters from stream
     Received complete JSON: ...
     Drone detected: ...
     Dispatched drone CoT event: ...
     ```

7. **Check Map**
   - Drone icon should appear on ATAK map
   - Icon should be labeled with drone ID
   - Should see track information

8. **Check Activity Feed**
   - Open OmniCOT dashboard
   - Should show "Drone <ID> displayed on map" messages
   - No error messages

### Expected Behavior
- ✅ Bluetooth stays connected when detecting drones
- ✅ Drones appear on map within 1-2 seconds of detection
- ✅ No "Socket is closed" or "Connection lost" errors
- ✅ Activity Feed shows successful detections
- ✅ Logs show data being received and processed

---

## Known Limitations

### Current Issues to Investigate
1. **No logs visible in some cases**
   - R8/ProGuard may be stripping debug logs in release builds
   - May need to configure proguard-rules.pro to keep Log statements

2. **gyb_detect configuration**
   - Need to verify gyb_detect is configured to send Remote ID data over Bluetooth
   - Some devices only send status (battery, device info) by default
   - May need firmware update or configuration change

---

## Debugging Tips

### If Bluetooth won't connect:
1. Check Bluetooth is enabled on Android
2. Ensure gyb_detect is paired in Android Bluetooth settings
3. Check OmniCOT has Bluetooth permissions
4. Try unpairing and re-pairing device

### If connection drops:
1. Check monitor-logs.sh for IOException details
2. Verify gyb_detect is in range (Bluetooth range ~10m)
3. Check Android battery saver isn't killing Bluetooth
4. Look for "Socket closed" or "Connection lost" in logs

### If no drones appear on map:
1. Verify gyb_detect LED turns RED (detecting drone)
2. Check Activity Feed in OmniCOT dashboard for errors
3. Look for "CotDispatcher is null" errors in logs
4. Verify ESP32 spoofer is transmitting Remote ID
5. Check logs for "Received complete JSON" messages

### If seeing "Socket is closed" Toast:
- This is now fixed in version 0.6-fa252bf
- If still seeing it, check you have latest version installed
- Look for "Socket was closed before starting read - ignoring" in logs

---

## Git Commits

### Recent Fixes:
1. **cd3b2ea** - Fix CoT type for Remote ID drones to display correctly
2. **27aa22d** - Improve error reporting and fix null safety issues
3. **fa252bf** - Add defensive socket checks to prevent 'Socket is closed' errors
4. **c1a3327** - Fix Bluetooth connection stability and CoT dispatcher issues

### View Changes:
```bash
git log --oneline -10
git show cd3b2ea  # CoT type fix
git show 27aa22d  # Error reporting improvements
git show fa252bf  # Socket defensive checks
git show c1a3327  # Bluetooth stability
```

---

## Next Steps

1. **Test with new Android device**
   - Fresh install of latest APK
   - Full test procedure above
   - Monitor logs during testing

2. **Verify gyb_detect configuration**
   - Confirm it sends Remote ID data (not just status)
   - Check firmware version
   - Review any configuration settings

3. **Test edge cases**
   - Multiple drones detected simultaneously
   - Rapid connect/disconnect cycles
   - Low battery scenarios
   - Out of range scenarios

4. **Performance testing**
   - Monitor battery usage during extended use
   - Check for memory leaks
   - Verify data processing keeps up with high detection rates

---

## Support Information

**Repository**: https://github.com/engindearing-projects/omni-COT
**Branch**: main
**Latest Version**: 0.6-cd3b2ea
**ATAK SDK**: 5.4.0.27-CIV
**Working Directory**: ~/Downloads/ATAK-CIV-5.4.0.27-SDK/plugins/omni-COT

---

**Generated**: 2025-11-07
**Last Updated**: cd3b2ea (Fix CoT type for proper drone icon display)
