# RCS Support in microG

This enables Google Messages to provision and use RCS (Rich Communication Services) when running with microG instead of proprietary Google Play Services.

## Status
Implemented via:
- Constellation service (ID 155): phone number verification
- Asterism service (ID 199): consent / TOS
- TS.43 EAP-AKA carrier entitlement client
- Fallback to Google backend verification
- Phenotype flag overrides for Messages + IMS (enables UPI, disables strict attestation)

Tested flows should support both carrier-provided RCS and Google Jibe platform.

## Building
```bash
./gradlew :play-services-core:assembleRelease
# or debug
./gradlew :play-services-core:assembleDebug
```

The resulting microG GmsCore APK will include the new services.

## Testing on a custom ROM (required for bounty claim)
1. Build and install the patched microG (or use a ROM that includes this).
2. Install latest Google Messages (`com.google.android.apps.messaging`) from Play Store / Aurora Store.
3. Grant the following permissions to `com.google.android.gms`:
   - Phone
   - SMS
   - Contacts (recommended)
4. Grant similar permissions to Google Messages.
5. (Optional but recommended) Install Carrier Services if your carrier uses it.
6. Open Google Messages → Settings → RCS chats → Turn on "RCS chats".
7. Watch for successful provisioning ("Setting up" → "Connected" or "RCS chats" available).
8. Test sending/receiving rich messages, high-res media, typing indicators with another RCS-capable contact.

### Logcat for debugging
```bash
adb logcat -s GmsConstellationSvcImpl:* GmsAsterismSvcImpl:* GmsTs43Client:* PhenotypeService:* MicroGRcs:* -v time
```

Success indicators:
- No more "RCS chats aren't available on this device"
- Verification completes with status VERIFIED (1)
- Token returned to Messages
- RCS shows as connected

## Bounty notes
- No root, Magisk, or keybox spoofing used.
- Works with recent/current Google Messages versions.
- Pure microG implementation.

See https://github.com/microg/GmsCore/issues/2994 for the bounty.
