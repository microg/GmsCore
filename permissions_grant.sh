#!/system/bin/sh
# Grant required permissions to Google Messages for RCS
pm grant com.google.android.apps.messaging com.google.android.gms.rcs.permission.RCS_PROVISIONING
pm grant com.google.android.apps.messaging com.google.android.gms.carrier.permission.CARRIER_PROVISIONING
# Also grant vital permissions (if missing)
pm grant com.google.android.apps.messaging android.permission.READ_PHONE_STATE
pm grant com.google.android.apps.messaging android.permission.INTERNET
pm grant com.google.android.apps.messaging com.google.android.gms.permission.ACTIVITY_RECOGNITION
echo "Permissions granted. Please restart Google Messages."