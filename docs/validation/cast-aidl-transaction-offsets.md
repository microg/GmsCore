# Cast AIDL transaction offsets

The [numbering question on PR #3845](https://github.com/microg/GmsCore/pull/3845#issuecomment-5967082644) compares explicit AIDL method IDs with Binder transaction codes. These are offset by one: the Java AIDL generator emits `IBinder.FIRST_CALL_TRANSACTION + methodId`, and `FIRST_CALL_TRANSACTION` is 1.

The submitted declarations at `5de13aa4af21295e61f2e19298b9bbd151894046` therefore produce these codes:

| Interface | Method | AIDL ID | Binder transaction |
|---|---|---:|---:|
| Controller | `disconnect` | 0 | 1 |
| Controller | `sendMessage` | 8 | 9 |
| Controller | `launchApplication` | 12 | 13 |
| Controller | `connect` | 16 | 17 |
| Controller | `addListener` | 17 | 18 |
| Controller | `removeListener` | 18 | 19 |
| Listener | `onDeviceStatusChanged(CastDeviceStatus)` | 12 | 13 |
| Listener | `onConnectedWithResult(int)` | 13 | 14 |

These generated codes agree with the SDK transaction numbers reported in that discussion. Incrementing the AIDL IDs would introduce an off-by-one error. No renumbering is needed for this point.

## Source references

- [Controller AIDL at the submitted commit](https://github.com/woahwhattheheck/GmsCore/blob/5de13aa4af21295e61f2e19298b9bbd151894046/play-services-cast/src/main/aidl/com/google/android/gms/cast/internal/ICastDeviceController.aidl), blob `39e55f00837f887c8de8fbd38ec3094d7560fef1`.
- [Listener AIDL at the same commit](https://github.com/woahwhattheheck/GmsCore/blob/5de13aa4af21295e61f2e19298b9bbd151894046/play-services-cast/src/main/aidl/com/google/android/gms/cast/internal/ICastDeviceControllerListener.aidl), blob `b480b7c6369e55b7aa9e4ebddd850623a858fff1`.
- [AOSP Java generator at `dd0d78f0`](https://android.googlesource.com/platform/system/tools/aidl/+/dd0d78f0ead984881caee291751226001f92587e/generate_java_binder.cpp): `item->GetId()` is passed to `generate_methods`, which adds the value to `android.os.IBinder.FIRST_CALL_TRANSACTION` when defining each transaction constant.
- [Android API reference for `FIRST_CALL_TRANSACTION`](https://developer.android.com/reference/android/os/IBinder#FIRST_CALL_TRANSACTION) defines its value as 1.

This is a source compatibility explanation, checked on 2026-10-04. It adds no build or device execution evidence. The [maintainer's physical-receiver proof-video requirement](https://github.com/microg/GmsCore/pull/3845#issuecomment-5947015322) remains outstanding.
