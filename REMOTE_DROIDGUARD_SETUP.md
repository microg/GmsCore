# Remote DroidGuard for Play Integrity

## Overview

This document describes how to set up and use remote DroidGuard functionality for Play Integrity attestation. This allows devices that cannot pass Play Integrity locally to use a remote device (such as an old stock phone) to obtain valid integrity tokens.

## Background

Play Integrity uses a multi-step DroidGuard process, which was not previously supported by the remote DroidGuard implementation in microG. This fix extends the remote DroidGuard to support multi-step processes, enabling Play Integrity attestation over a remote connection.

## Architecture

```
┌─────────────────┐         ┌─────────────────┐
│  Client Device  │         │  Server Device  │
│  (microG)       │         │  (Stock ROM)    │
│                 │         │                 │
│  App Request    │         │  Play Integrity │
│       │         │         │  Service        │
│       ▼         │         │       ▲         │
│  Remote DG      │◄───────►│  Remote DG      │
│  Client         │  HTTP   │  Server         │
│                 │         │                 │
└─────────────────┘         └─────────────────┘
```

## Server Setup

### Requirements

- A device with stock Android ROM (or custom ROM that passes Play Integrity)
- Android 8.0 or higher
- Google Play Services installed
- Network connectivity (WiFi or mobile data)
- microG with remote DroidGuard support

### Steps

1. **Prepare the Server Device**
   - Use a device that passes Play Integrity (check at https://play.google.com/store/apps/details?id=com.google.android.play.integrity.test)
   - Ensure the device has a stable IP address or domain name
   - Install microG with remote DroidGuard server support

2. **Configure the Server**
   - Open microG settings
   - Navigate to "Device Registration"
   - Enable "Remote DroidGuard Server"
   - Configure port (default: 8765)
   - Set authentication token

3. **Firewall/Network**
   - Open port 8765 on your router/firewall
   - Optionally set up port forwarding for remote access
   - For local network only: ensure client devices are on same network

## Client Setup

### Requirements

- Device running microG 0.3.6 or higher
- LineageOS for microG or similar custom ROM
- Network access to server device

### Steps

1. **Configure Remote DroidGuard**
   - Open microG settings
   - Navigate to "Device Registration"
   - Enable "Use remote DroidGuard"
   - Enter server address (IP:port or domain:port)
   - Enter authentication token

2. **Test Connection**
   - Use the Play Integrity test app
   - Verify that MEETS_DEVICE integrity is achieved

## Security Considerations

1. **Authentication**: Always use a strong authentication token
2. **Encryption**: Use HTTPS for remote connections (not on local network)
3. **Rate Limiting**: Play Integrity has request limits per device
4. **Access Control**: Restrict server access to known clients

## Troubleshooting

### Connection Failed

- Verify server is running and accessible
- Check firewall settings
- Ensure correct IP address and port

### Integrity Check Fails

- Verify server device passes Play Integrity
- Check that multi-step DroidGuard is enabled
- Review server logs for errors

### Timeout Errors

- Increase timeout settings in client configuration
- Check network latency between client and server
- Ensure server has sufficient resources

## Implementation Details

### Multi-step DroidGuard Support

The key fix enables the remote DroidGuard to handle multi-step processes:

1. **State Management**: Server maintains state across multiple requests
2. **Session IDs**: Each multi-step process uses a unique session identifier
3. **Response Chaining**: Responses from earlier steps are used in subsequent steps

### API Endpoints

- `POST /dg/init` - Initialize DroidGuard session
- `POST /dg/step` - Execute a step in the process
- `POST /dg/complete` - Complete the multi-step process
- `GET /dg/status` - Check session status

## Testing

To verify the setup works:

1. Install Play Integrity Test app on client device
2. Run the test
3. Verify "MEETS_DEVICE" or better is reported
4. Check server logs for successful request handling

## Commercial Use

This setup enables commercial integrity attestation services where providers can offer Play Integrity tokens as a service. Consider:

- Pricing models (per-request or subscription)
- SLA requirements
- Scaling infrastructure
- Legal compliance

## References

- Issue #2851: https://github.com/microg/GmsCore/issues/2851
- microG Documentation: https://microg.org/
- Play Integrity API: https://developer.android.com/google/play/integrity

## License

This documentation is part of microG and follows the same license terms.
