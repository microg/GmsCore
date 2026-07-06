# RCS Support for microG

This patch adds the necessary permissions and a stub RCS service to microG (GmsCore) to enable Google Messages to set up RCS on devices with a locked bootloader, without requiring root.

## Installation

1. Apply the patch to the GmsCore source code:
   ```
   git apply patches/0001-enable-rcs-support.patch
   ```
2. Build and install the modified GmsCore.
3. Grant the following permissions via ADB if not automatically granted:
   ```
   adb shell appops set com.google.android.gms READ_PRIVILEGED_PHONE_STATE allow
   adb shell appops set com.google.android.gms CARRIER_CONFIG allow
   adb shell appops set com.google.android.gms CONNECTIVITY_INTERNAL allow
   ```

## How it works

Google Messages requires the presence of certain system services and permissions for RCS. By adding `READ_PRIVILEGED_PHONE_STATE` and `CARRIER_CONFIG` permissions, along with a service that responds positively to RCS queries, the app proceeds with setup without requiring a full attestation check. This mirrors the approach used in GrapheneOS's Play services sandbox.

## Limitations

- This is a minimal stub; actual RCS messaging may not work if Google Messages requires additional deep integrations.
- Tested with Google Messages versions from 2023 onward.
- For full functionality, further reverse engineering of the RCS protocol may be needed.
