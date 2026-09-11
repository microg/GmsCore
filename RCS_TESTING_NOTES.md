# RCS Testing Notes

## Environment
- Repository: microg/GmsCore fork
- Branch: rcs-support
- Integrated PRs:
  - PR #3359 Constellation Service
  - PR #3360 Asterism Service

## Build Attempt

Command:

./gradlew assembleDebug

Result:

FAILED

Known issue:

Java compiler toolchain configuration issue.

## Required Testing Device
Full RCS testing requires:

- Physical Android device
- Active SIM card
- Google Messages installed
- Locked bootloader
- microG GmsCore built and installed
- Required phone, SMS, contacts, and network permissions granted

## Suggested Logcat Commands

```bash
adb logcat | grep -iE "rcs|tachyon|constellation|asterism|provisioning"
adb logcat | grep -i "google.android.apps.messaging"
adb logcat | grep -iE "auth|verification|droidguard"