# OmniCOT + OmniTAK Integration Guide

This guide explains how to use **OmniCOT** (ATAK plugin) together with **OmniTAK** (TAK Server Aggregator) to create a powerful tactical data management solution.

## Overview

**OmniCOT** and **OmniTAK** are complementary tools that work with the same TAK servers:

### OmniCOT (This Plugin)
- **Platform**: Android (ATAK plugin)
- **Purpose**: Enhance ATAK with marker affiliation management and Area of Interest detection
- **CoT Features**:
  - Visual affiliation markers (Friendly, Neutral, Hostile, Unknown)
  - Real-time Remote ID integration via Bluetooth
  - Automatic affiliation federation across TAK servers
- **Connection**: Uses ATAK's built-in TAK server connections

### OmniTAK
- **Platform**: Server/Desktop (Rust application)
- **Purpose**: Aggregate CoT messages from multiple TAK servers
- **Key Features**:
  - Connect to multiple TAK servers simultaneously
  - REST API for programmatic access
  - Web UI for management
  - Advanced message filtering and routing
  - Real-time metrics and monitoring
- **Repository**: https://github.com/engindearing-projects/omni-TAK

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    TAK Server Network                    │
│                                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ TAK Server 1 │  │ TAK Server 2 │  │ TAK Server 3 │  │
│  │  Port 8089   │  │  Port 8089   │  │  Port 8089   │  │
│  └───────┬──────┘  └──────┬───────┘  └──────┬───────┘  │
│          │                 │                 │          │
└──────────┼─────────────────┼─────────────────┼──────────┘
           │                 │                 │
     ┌─────┴─────┐     ┌─────┴─────┐     ┌─────┴─────┐
     │           │     │           │     │           │
┌────▼─────┐ ┌──▼────────────┐ ┌──▼────────────┐ ┌──▼───────┐
│  ATAK    │ │    OmniTAK     │ │  ATAK Device  │ │  WinTAK  │
│  Device  │ │  Aggregator    │ │               │ │          │
│          │ │                │ │               │ │          │
│ OmniCOT  │ │  - REST API    │ │   OmniCOT     │ │          │
│  Plugin  │ │  - Web UI      │ │   Plugin      │ │          │
│          │ │  - Filtering   │ │               │ │          │
│  Sends:  │ │  - Metrics     │ │   Sends:      │ │          │
│  • CoT   │ │                │ │   • CoT       │ │          │
│  • Affil │ │  Aggregates    │ │   • Affil     │ │          │
│  • RemID │ │  all CoT data  │ │   • RemID     │ │          │
└──────────┘ └────────────────┘ └───────────────┘ └──────────┘
```

## Use Cases

### 1. Command & Control Dashboard

**Scenario**: Operations center needs to monitor all field units across multiple TAK servers.

**Setup**:
- Field units run ATAK with OmniCOT plugin
- Operations center runs OmniTAK to aggregate data
- Web dashboard displays real-time tactical picture

**Benefits**:
- Unified view of all TAK traffic
- Filter by affiliation, geography, or team
- REST API for custom integrations
- Real-time metrics and alerts

### 2. Multi-Domain Operations

**Scenario**: Different units operate on separate TAK servers (e.g., ground, air, maritime).

**Setup**:
- Each domain has its own TAK server
- OmniTAK connects to all servers
- Filters route relevant data to each domain

**Benefits**:
- Cross-domain situational awareness
- Maintain security boundaries with filtering
- Centralized CoT message management

### 3. Development & Testing

**Scenario**: Developers need to test ATAK plugins against recorded or simulated traffic.

**Setup**:
- OmniTAK records CoT traffic from production
- Development ATAK instances connect to OmniTAK
- Playback and analyze CoT scenarios

**Benefits**:
- Safe testing environment
- Reproducible test scenarios
- API access for automated testing

## Getting Started

### Prerequisites

1. **OmniCOT Plugin Installed**
   - This ATAK plugin should be installed and configured
   - Connected to at least one TAK server
   - Certificates configured in ATAK

2. **OmniTAK Installed**
   - Clone from: https://github.com/engindearing-projects/omni-TAK
   - Built and ready to run (Rust 1.90+)

### Step 1: Extract Certificates from ATAK

OmniTAK needs the same certificates as ATAK to connect to your TAK servers.

**Option A: Automatic Extraction (Recommended)**

```bash
# Clone OmniTAK
git clone https://github.com/engindearing-projects/omni-TAK
cd omni-TAK

# Connect ATAK device via USB and enable USB Debugging

# Run automated setup
./scripts/setup-tak-connection.sh --adb
```

This will:
- Extract certificates from your ATAK device
- Convert them to the correct format
- Generate a ready-to-use configuration

**Option B: Manual Certificate Transfer**

1. In ATAK, navigate to the certificate location
2. Export or copy the certificate files
3. Transfer to your computer (email, cloud, USB)
4. Convert certificates:
   ```bash
   cd omni-TAK
   ./scripts/convert-p12-to-pem.sh /path/to/cert.p12 certs
   ```

### Step 2: Configure OmniTAK

**Use an example configuration:**

```bash
cd omni-TAK

# For single TAK server
cp config/examples/single-tak-server.yaml config/config.yaml

# Edit the configuration
nano config/config.yaml
```

Update the server address to match your TAK server:

```yaml
servers:
  - id: tak-server-main
    address: "YOUR_TAK_SERVER:8089"  # Match ATAK's server address
    protocol: tls
    auto_reconnect: true
    tls:
      cert_path: "certs/client.pem"
      key_path: "certs/client.key"
      ca_path: "certs/ca.pem"
      validate_certs: true
```

### Step 3: Start OmniTAK

```bash
cargo run --release -- \
  --config config/config.yaml \
  --admin-password your_secure_password
```

**Look for successful connection:**
```
INFO omnitak: Successfully connected to tak-server-main
INFO omnitak: Server listening address=0.0.0.0:9443
```

### Step 4: Verify Integration

**On ATAK (with OmniCOT):**
1. Change a marker's affiliation (e.g., set a unit to "Hostile")
2. The affiliation should propagate through the TAK server

**On OmniTAK:**
1. Open Web UI: http://localhost:9443
2. Login with: `admin` / `your_secure_password`
3. You should see the CoT message with affiliation data

**Check logs:**
```bash
# In OmniTAK
# You should see messages being received
INFO omnitak_pool: Message received server=tak-server-main
```

## Common Configurations

### Configuration 1: Single TAK Server

Both OmniCOT and OmniTAK connect to the same server.

```yaml
# OmniTAK config
servers:
  - id: production-server
    address: "takserver.example.com:8089"
    protocol: tls
    tls:
      cert_path: "certs/client.pem"
      key_path: "certs/client.key"
      ca_path: "certs/ca.pem"
```

### Configuration 2: Multiple TAK Servers

OmniCOT devices on different servers, OmniTAK aggregates all.

```yaml
# OmniTAK config
servers:
  - id: tak-east
    address: "tak-east.example.com:8089"
    protocol: tls
    tls:
      cert_path: "certs/client.pem"
      key_path: "certs/client.key"
      ca_path: "certs/ca.pem"

  - id: tak-west
    address: "tak-west.example.com:8089"
    protocol: tls
    tls:
      cert_path: "certs/client.pem"
      key_path: "certs/client.key"
      ca_path: "certs/ca.pem"
```

### Configuration 3: Filtered Distribution

Filter CoT messages based on affiliation.

```yaml
# OmniTAK config
filters:
  mode: whitelist
  rules:
    # Only friendly forces to backup server
    - id: friendly-backup
      type: affiliation
      allow: [friend, assumedfriend]
      destinations: [tak-backup]

    # All entities to primary
    - id: all-primary
      type: affiliation
      allow: [friend, assumedfriend, neutral, hostile, unknown]
      destinations: [tak-primary]
```

## OmniCOT Features Available in OmniTAK

When OmniTAK receives CoT messages from ATAK devices running OmniCOT:

### 1. Affiliation Data
- **CoT Field**: `<detail><affiliation>`
- **Values**: `friend`, `assumedfriend`, `neutral`, `hostile`, `suspect`, `unknown`
- **Use**: Filter and route based on affiliation
- **Example Filter**:
  ```yaml
  - type: affiliation
    allow: [hostile, suspect]
    destinations: [threat-server]
  ```

### 2. Affiliation Metadata
- **CoT Fields**:
  - `<detail><omnicot><marked_by>` - Who set the affiliation
  - `<detail><omnicot><server>` - Source TAK server
  - `<detail><omnicot><timestamp>` - When it was marked
  - `<detail><omnicot><notes>` - Optional notes

### 3. Remote ID Integration
- **CoT Field**: `<detail><remote_id>`
- **Data**: Bluetooth-detected drone information
- **Use**: Track detected drones via OmniTAK API

## API Integration

OmniTAK provides a REST API to access CoT data programmatically.

### Authentication

```bash
# Get access token
TOKEN=$(curl -s -X POST http://localhost:9443/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"your_password"}' | \
  jq -r '.access_token')
```

### Query Connections

```bash
# List all TAK server connections
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:9443/api/v1/connections | jq

# Get connection details
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:9443/api/v1/connections/tak-server-main | jq
```

### Stream CoT Messages

```bash
# WebSocket streaming (real-time)
websocat ws://localhost:9443/api/v1/stream

# Or use the Web UI
open http://localhost:9443
```

### Query by Affiliation

Use filters to extract specific affiliations:

```bash
# Configure filter via API
curl -X POST http://localhost:9443/api/v1/filters \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "hostile-only",
    "type": "affiliation",
    "allow": ["hostile", "suspect"],
    "destinations": ["threat-analysis"]
  }'
```

## Monitoring & Metrics

OmniTAK provides Prometheus-compatible metrics:

```bash
# Metrics endpoint
curl http://localhost:9443/api/v1/metrics
```

**Key Metrics**:
- `omnitak_messages_received_total` - Total CoT messages
- `omnitak_messages_sent_total` - Messages forwarded
- `omnitak_connection_status` - Connection health
- `omnitak_filter_matches_total` - Filter statistics

**Grafana Integration**:
1. Configure Prometheus to scrape OmniTAK
2. Import OmniTAK Grafana dashboard
3. Visualize CoT traffic and affiliations

## Troubleshooting

### OmniTAK Can't Connect to TAK Server

**Symptom**: Connection refused or timeout errors

**Solutions**:
1. Verify server address matches ATAK configuration
2. Check certificates are correctly converted
3. Ensure firewall allows outbound connection
4. Test with: `telnet your-server 8089`

### No CoT Messages Received

**Symptom**: OmniTAK connects but no messages appear

**Solutions**:
1. Verify ATAK devices are sending position updates
2. Check OmniTAK filters aren't blocking all messages
3. Enable debug logging:
   ```yaml
   logging:
     level: "debug"
   ```
4. Verify TAK server allows this client certificate

### Certificate Format Issues

**Symptom**: "Failed to load private key" errors

**Solution**: Private key must be in traditional RSA format:
```bash
# Check format
head -n 1 certs/client.key
# Should show: -----BEGIN RSA PRIVATE KEY-----

# Convert if needed
openssl rsa -in certs/client.key -out certs/client-rsa.key -traditional
mv certs/client-rsa.key certs/client.key
```

### Affiliation Data Not Visible

**Symptom**: Messages received but affiliation not shown

**Possible Causes**:
1. OmniCOT plugin not installed on sending device
2. Affiliation data in different CoT field
3. API client not parsing `<detail><affiliation>` field

**Solution**: Check raw CoT XML in OmniTAK debug logs

## Advanced Topics

### Custom Filtering

Create sophisticated filters based on OmniCOT data:

```yaml
filters:
  mode: whitelist
  rules:
    # Hostile entities with Remote ID data
    - id: hostile-drones
      type: composite
      operator: and
      conditions:
        - type: affiliation
          allow: [hostile, suspect]
        - type: field_exists
          field: "detail.remote_id"
      destinations: [threat-server]
```

### Data Export

Export OmniCOT affiliation data for analysis:

```bash
# Stream to file
websocat ws://localhost:9443/api/v1/stream > cot_data.jsonl

# Parse affiliation data
cat cot_data.jsonl | jq 'select(.detail.affiliation != null)'
```

### Integration with GIS

Use OmniTAK API to feed CoT data to GIS systems:

```python
import requests
import json

# Get access token
auth = requests.post('http://localhost:9443/api/v1/auth/login',
                     json={'username': 'admin', 'password': 'password'})
token = auth.json()['access_token']

# Stream CoT messages
# (Use websocket library for real-time streaming)
```

## Additional Resources

**OmniTAK Documentation**:
- [TAK Server Setup Guide](https://github.com/engindearing-projects/omni-TAK/blob/main/docs/TAK_SERVER_SETUP.md)
- [ADB Certificate Setup](https://github.com/engindearing-projects/omni-TAK/blob/main/docs/ADB_SETUP.md)
- [Configuration Examples](https://github.com/engindearing-projects/omni-TAK/blob/main/config/examples/README.md)
- [Filtering Guide](https://github.com/engindearing-projects/omni-TAK/blob/main/docs/FILTERING.md)

**OmniCOT Documentation**:
- [Plugin Summary](../PLUGIN_SUMMARY.md)
- [Remote ID Integration](../REMOTE_ID_INTEGRATION.md)
- [README](../README.md)

**TAK Resources**:
- [TAK.gov](https://tak.gov) - Official TAK documentation
- [ATAK CIV](https://tak.gov/products/atak-civ) - Civilian ATAK
- [TAK Server](https://tak.gov/products/tak-server) - Official TAK Server

## Support

For issues with:
- **OmniCOT Plugin**: Open issue at [omni-COT GitHub](https://github.com/engindearing-projects/omni-COT/issues)
- **OmniTAK Aggregator**: Open issue at [omni-TAK GitHub](https://github.com/engindearing-projects/omni-TAK/issues)
- **TAK Servers**: Contact your TAK server administrator or visit [TAK.gov](https://tak.gov)

---

**Together, OmniCOT and OmniTAK provide a complete solution for tactical data management!** 🚀
