# RCS Support for microG / GmsCore

This set of files adds stub implementations for RCS and Carrier services required by Google Messages to enable RCS chat functionality.

## Installation
1. Integrate the Java files into your GmsCore source tree under the respective packages.
2. Add the following service declarations to your AndroidManifest.xml of GmsCore:
   ```xml
   <service android:name="com.google.android.gms.rcs.RcsService"
       android:exported="true"
       android:permission="com.google.android.gms.rcs.permission.RCS_PROVISIONING">
       <intent-filter>
           <action android:name="com.google.android.gms.rcs.service.START" />
       </intent-filter>
   </service>
   <service android:name="com.google.android.gms.carrier.CarrierService"
       android:exported="true"
       android:permission="com.google.android.gms.carrier.permission.CARRIER_PROVISIONING">
       <intent-filter>
           <action android:name="com.google.android.gms.carrier.service.START" />
       </intent-filter>
   </service>
   ```
3. Also add the permissions:
   ```xml
   <uses-permission android:name="com.google.android.gms.rcs.permission.RCS_PROVISIONING" />
   <uses-permission android:name="com.google.android.gms.carrier.permission.CARRIER_PROVISIONING" />
   ```
4. Rebuild and install the updated GmsCore.
5. Run the provided script `permissions_grant.sh` as root (or adb shell) to grant necessary permissions to Google Messages.
6. Clear Google Messages data or restart the app. RCS setup should now succeed.

## Limitations
- This is a minimal stub that returns success immediately without actual carrier interaction. Real RCS messaging relies on carrier network support.
- Works on devices with locked bootloader as long as GmsCore is installed as a system app (or via microG installer).
- Tested with Google Messages versions from 2024 onward.

## Notes
microG must have proper signature spoofing enabled for Google Messages to recognize it as Google Play Services.