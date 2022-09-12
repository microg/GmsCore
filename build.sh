#!/usr/bin/env sh

set -eu

cd "$(dirname "$0")"

SRCS="
play-services-base-core-ui/src/main/AndroidManifest.xml
play-services-base-core-ui/src/main/res/drawable/ic_expand_apps.xml
play-services-base-core-ui/src/main/res/drawable/ic_info_outline.xml
play-services-base-core-ui/src/main/res/drawable/ic_open.xml
play-services-base-core-ui/src/main/res/drawable/ic_radio.xml
play-services-base-core-ui/src/main/res/drawable/ic_radio_checked.xml
play-services-base-core-ui/src/main/res/drawable/ic_radio_unchecked.xml
play-services-base-core-ui/src/main/res/layout/list_no_item.xml
play-services-base-core-ui/src/main/res/layout/preference_category_no_label.xml
play-services-base-core-ui/src/main/res/layout/preference_switch_bar.xml
play-services-base-core-ui/src/main/res/values-be/strings.xml
play-services-base-core-ui/src/main/res/values-de/strings.xml
play-services-base-core-ui/src/main/res/values-es/strings.xml
play-services-base-core-ui/src/main/res/values-fr/strings.xml
play-services-base-core-ui/src/main/res/values-it/strings.xml
play-services-base-core-ui/src/main/res/values-ja/strings.xml
play-services-base-core-ui/src/main/res/values-pl/strings.xml
play-services-base-core-ui/src/main/res/values-pt-rBR/strings.xml
play-services-base-core-ui/src/main/res/values-ru/strings.xml
play-services-base-core-ui/src/main/res/values-sr/strings.xml
play-services-base-core-ui/src/main/res/values-uk/strings.xml
play-services-base-core-ui/src/main/res/values-zh-rCN/strings.xml
play-services-base-core-ui/src/main/res/values-zh-rTW/strings.xml
play-services-base-core-ui/src/main/res/values/strings.xml
play-services-base-core-ui/src/main/res/values/themes.xml
play-services-base-core-ui/src/main/kotlin/org/microg/gms/ui/AppIconPreference.kt
play-services-base-core-ui/src/main/kotlin/org/microg/gms/ui/Constants.kt
play-services-base-core-ui/src/main/kotlin/org/microg/gms/ui/PreferenceSwitchBar.kt
play-services-base-core-ui/src/main/kotlin/org/microg/gms/ui/TextPreference.kt
play-services-base-core-ui/src/main/kotlin/org/microg/gms/ui/Utils.kt
play-services-base-core-ui/build.gradle
play-services-core/microg-ui-tools/build.gradle
play-services-core/microg-ui-tools/src/main/AndroidManifest.xml
play-services-core/microg-ui-tools/src/main/java/org/microg/tools/selfcheck/PermissionCheckGroup.java
play-services-core/microg-ui-tools/src/main/java/org/microg/tools/selfcheck/SelfCheckGroup.java
play-services-core/microg-ui-tools/src/main/java/org/microg/tools/ui/AbstractAboutFragment.java
play-services-core/microg-ui-tools/src/main/java/org/microg/tools/ui/AbstractDashboardActivity.java
play-services-core/microg-ui-tools/src/main/java/org/microg/tools/ui/AbstractSelfCheckFragment.java
play-services-core/microg-ui-tools/src/main/java/org/microg/tools/ui/AbstractSettingsActivity.java
play-services-core/microg-ui-tools/src/main/java/org/microg/tools/ui/AbstractSettingsFragment.java
play-services-core/microg-ui-tools/src/main/java/org/microg/tools/ui/Condition.java
play-services-core/microg-ui-tools/src/main/java/org/microg/tools/ui/DialogPreference.java
play-services-core/microg-ui-tools/src/main/java/org/microg/tools/ui/LongTextPreference.java
play-services-core/microg-ui-tools/src/main/java/org/microg/tools/ui/RadioButtonPreference.java
play-services-core/microg-ui-tools/src/main/java/org/microg/tools/ui/ResourceSettingsFragment.java
play-services-core/microg-ui-tools/src/main/java/org/microg/tools/ui/SwitchBar.java
play-services-core/microg-ui-tools/src/main/java/org/microg/tools/ui/SwitchBarResourceSettingsFragment.java
play-services-core/microg-ui-tools/src/main/java/org/microg/tools/ui/ToggleSwitch.java
play-services-core/microg-ui-tools/src/main/res/drawable-v21/switchbar_background.xml
play-services-core/microg-ui-tools/src/main/res/drawable/empty.xml
play-services-core/microg-ui-tools/src/main/res/drawable/ic_expand_less.xml
play-services-core/microg-ui-tools/src/main/res/drawable/ic_expand_more.xml
play-services-core/microg-ui-tools/src/main/res/drawable/self_check.xml
play-services-core/microg-ui-tools/src/main/res/drawable/switchbar_background.xml
play-services-core/microg-ui-tools/src/main/res/layout-v14/preference_category_dashboard.xml
play-services-core/microg-ui-tools/src/main/res/layout-v21/preference_material.xml
play-services-core/microg-ui-tools/src/main/res/layout/about_root.xml
play-services-core/microg-ui-tools/src/main/res/layout/app_bar.xml
play-services-core/microg-ui-tools/src/main/res/layout/condition_card.xml
play-services-core/microg-ui-tools/src/main/res/layout/dashboard_activity.xml
play-services-core/microg-ui-tools/src/main/res/layout/preference_widget_radiobutton.xml
play-services-core/microg-ui-tools/src/main/res/layout/self_check.xml
play-services-core/microg-ui-tools/src/main/res/layout/self_check_entry.xml
play-services-core/microg-ui-tools/src/main/res/layout/self_check_group.xml
play-services-core/microg-ui-tools/src/main/res/layout/settings_activity.xml
play-services-core/microg-ui-tools/src/main/res/layout/switch_bar.xml
play-services-core/microg-ui-tools/src/main/res/layout/toolbar.xml
play-services-core/microg-ui-tools/src/main/res/values-be/strings.xml
play-services-core/microg-ui-tools/src/main/res/values-de/strings.xml
play-services-core/microg-ui-tools/src/main/res/values-eo/strings.xml
play-services-core/microg-ui-tools/src/main/res/values-es/strings.xml
play-services-core/microg-ui-tools/src/main/res/values-fr/strings.xml
play-services-core/microg-ui-tools/src/main/res/values-it/strings.xml
play-services-core/microg-ui-tools/src/main/res/values-ja/strings.xml
play-services-core/microg-ui-tools/src/main/res/values-pl/strings.xml
play-services-core/microg-ui-tools/src/main/res/values-ro/strings.xml
play-services-core/microg-ui-tools/src/main/res/values-ru/strings.xml
play-services-core/microg-ui-tools/src/main/res/values-sr/strings.xml
play-services-core/microg-ui-tools/src/main/res/values-uk/strings.xml
play-services-core/microg-ui-tools/src/main/res/values-zh-rCN/strings.xml
play-services-core/microg-ui-tools/src/main/res/values-zh-rTW/strings.xml
play-services-core/microg-ui-tools/src/main/res/values/colors.xml
play-services-core/microg-ui-tools/src/main/res/values/strings.xml
play-services-core/multidex-keep.pro
play-services-core/src/main/java/org/microg/gms/ui/AboutFragment.java
play-services-core/src/main/java/org/microg/gms/ui/Conditions.java
play-services-core/src/main/java/org/microg/gms/ui/LocationSettingsActivity.java
play-services-core/src/main/java/org/microg/gms/ui/SettingsActivity.java
play-services-core/src/main/java/org/microg/gms/ui/SettingsDashboardActivity.java
play-services-core/src/main/java/org/microg/gms/ui/SelfCheckFragment.java
play-services-core/src/main/java/org/microg/tools/selfcheck/SystemChecks.java
play-services-core/src/main/kotlin/org/microg/gms/ui/SettingsFragment.kt
play-services-core/src/main/res/drawable-anydpi-v21/microg_light_color_24.xml
play-services-core/src/main/res/drawable-hdpi/ic_gamepad.png
play-services-core/src/main/res/drawable-hdpi/ic_generic_man.png
play-services-core/src/main/res/drawable-hdpi/ic_magnify.png
play-services-core/src/main/res/drawable-mdpi/ic_gamepad.png
play-services-core/src/main/res/drawable-mdpi/ic_generic_man.png
play-services-core/src/main/res/drawable-mdpi/ic_magnify.png
play-services-core/src/main/res/drawable-xhdpi/ic_gamepad.png
play-services-core/src/main/res/drawable-xhdpi/ic_generic_man.png
play-services-core/src/main/res/drawable-xhdpi/ic_magnify.png
play-services-core/src/main/res/drawable-xhdpi/proprietary_auth_gls_ic_google_minitab_selected.png
play-services-core/src/main/res/drawable-xhdpi/proprietary_auth_gls_ic_google_selected.png
play-services-core/src/main/res/drawable-xhdpi/proprietary_auth_ic_scope_icon_default.png
play-services-core/src/main/res/drawable-xxhdpi/ic_gamepad.png
play-services-core/src/main/res/drawable-xxhdpi/ic_generic_man.png
play-services-core/src/main/res/drawable-xxhdpi/ic_magnify.png
play-services-core/src/main/res/drawable-xxxhdpi/ic_magnify.png
play-services-core/src/main/res/drawable/circle_shape_background.xml
play-services-core/src/main/res/drawable/dots_horizontal.xml
play-services-core/src/main/res/drawable/ic_add_account.xml
play-services-core/src/main/res/drawable/ic_certificate.xml
play-services-core/src/main/res/drawable/ic_check_list.xml
play-services-core/src/main/res/drawable/ic_circle_check.xml
play-services-core/src/main/res/drawable/ic_circle_error.xml
play-services-core/src/main/res/drawable/ic_circle_pending.xml
play-services-core/src/main/res/drawable/ic_circle_warn.xml
play-services-core/src/main/res/drawable/ic_cloud_bell.xml
play-services-core/src/main/res/drawable/ic_device_login.xml
play-services-core/src/main/res/drawable/ic_map_marker.xml
play-services-core/src/main/res/drawable/ic_plusone_medium.xml
play-services-core/src/main/res/drawable/ic_plusone_small.xml
play-services-core/src/main/res/drawable/ic_plusone_standard.xml
play-services-core/src/main/res/drawable/ic_plusone_tall.xml
play-services-core/src/main/res/layout/settings_root_activity.xml
play-services-core/src/main/res/mipmap-anydpi-v26/ic_microg_settings.xml
play-services-core/src/main/res/mipmap-hdpi/ic_core_service_app.png
play-services-core/src/main/res/mipmap-hdpi/ic_microg_background.png
play-services-core/src/main/res/mipmap-hdpi/ic_microg_foreground.png
play-services-core/src/main/res/mipmap-hdpi/ic_microg_settings.png
play-services-core/src/main/res/mipmap-hdpi/ic_nlp_settings.png
play-services-core/src/main/res/mipmap-hdpi/ic_nlp_app.png
play-services-core/src/main/res/mipmap-mdpi/ic_core_service_app.png
play-services-core/src/main/res/mipmap-mdpi/ic_microg_background.png
play-services-core/src/main/res/mipmap-mdpi/ic_microg_foreground.png
play-services-core/src/main/res/mipmap-mdpi/ic_microg_settings.png
play-services-core/src/main/res/mipmap-mdpi/ic_nlp_settings.png
play-services-core/src/main/res/mipmap-mdpi/ic_nlp_app.png
play-services-core/src/main/res/mipmap-xhdpi/ic_core_service_app.png
play-services-core/src/main/res/mipmap-xhdpi/ic_microg_background.png
play-services-core/src/main/res/mipmap-xhdpi/ic_microg_foreground.png
play-services-core/src/main/res/mipmap-xhdpi/ic_microg_settings.png
play-services-core/src/main/res/mipmap-xhdpi/ic_nlp_settings.png
play-services-core/src/main/res/mipmap-xhdpi/ic_nlp_app.png
play-services-core/src/main/res/mipmap-xxhdpi/ic_core_service_app.png
play-services-core/src/main/res/mipmap-xxhdpi/ic_microg_background.png
play-services-core/src/main/res/mipmap-xxhdpi/ic_microg_foreground.png
play-services-core/src/main/res/mipmap-xxhdpi/ic_microg_settings.png
play-services-core/src/main/res/mipmap-xxhdpi/ic_nlp_settings.png
play-services-core/src/main/res/mipmap-xxhdpi/ic_nlp_app.png
play-services-core/src/main/res/mipmap-xxxhdpi/ic_core_service_app.png
play-services-core/src/main/res/mipmap-xxxhdpi/ic_microg_background.png
play-services-core/src/main/res/mipmap-xxxhdpi/ic_microg_foreground.png
play-services-core/src/main/res/mipmap-xxxhdpi/ic_microg_settings.png
play-services-core/src/main/res/mipmap-xxxhdpi/ic_nlp_settings.png
play-services-core/src/main/res/mipmap-xxxhdpi/ic_nlp_app.png
play-services-core/src/main/res/navigation/nav_settings.xml
play-services-core/src/main/res/values-be/permissions.xml
play-services-core/src/main/res/values-be/plurals.xml
play-services-core/src/main/res/values-be/strings.xml
play-services-core/src/main/res/values-de/permissions.xml
play-services-core/src/main/res/values-de/plurals.xml
play-services-core/src/main/res/values-de/strings.xml
play-services-core/src/main/res/values-eo/permissions.xml
play-services-core/src/main/res/values-eo/plurals.xml
play-services-core/src/main/res/values-eo/strings.xml
play-services-core/src/main/res/values-es/permissions.xml
play-services-core/src/main/res/values-es/plurals.xml
play-services-core/src/main/res/values-es/strings.xml
play-services-core/src/main/res/values-fr/permissions.xml
play-services-core/src/main/res/values-fr/plurals.xml
play-services-core/src/main/res/values-fr/strings.xml
play-services-core/src/main/res/values-it/permissions.xml
play-services-core/src/main/res/values-it/plurals.xml
play-services-core/src/main/res/values-it/strings.xml
play-services-core/src/main/res/values-ja/permissions.xml
play-services-core/src/main/res/values-ja/plurals.xml
play-services-core/src/main/res/values-ja/strings.xml
play-services-core/src/main/res/values-pl/permissions.xml
play-services-core/src/main/res/values-pl/plurals.xml
play-services-core/src/main/res/values-pl/strings.xml
play-services-core/src/main/res/values-pt-rBR/plurals.xml
play-services-core/src/main/res/values-pt-rBR/strings.xml
play-services-core/src/main/res/values-ro/permissions.xml
play-services-core/src/main/res/values-ro/plurals.xml
play-services-core/src/main/res/values-ro/strings.xml
play-services-core/src/main/res/values-ru/permissions.xml
play-services-core/src/main/res/values-ru/plurals.xml
play-services-core/src/main/res/values-ru/strings.xml
play-services-core/src/main/res/values-sr/permissions.xml
play-services-core/src/main/res/values-sr/plurals.xml
play-services-core/src/main/res/values-sr/strings.xml
play-services-core/src/main/res/values-uk/permissions.xml
play-services-core/src/main/res/values-uk/plurals.xml
play-services-core/src/main/res/values-uk/strings.xml
play-services-core/src/main/res/values-zh-rCN/permissions.xml
play-services-core/src/main/res/values-zh-rCN/plurals.xml
play-services-core/src/main/res/values-zh-rCN/strings.xml
play-services-core/src/main/res/values-zh-rTW/permissions.xml
play-services-core/src/main/res/values-zh-rTW/plurals.xml
play-services-core/src/main/res/values-zh-rTW/strings.xml
play-services-core/src/main/res/values/arrays.xml
play-services-core/src/main/res/values/bools.xml
play-services-core/src/main/res/values/colors.xml
play-services-core/src/main/res/values/dimens.xml
play-services-core/src/main/res/values/permissions.xml
play-services-core/src/main/res/values/signature.xml
play-services-core/src/main/res/values/themes.xml
play-services-core/src/main/res/values/plurals.xml
play-services-core/src/main/res/values/strings.xml
play-services-core/src/main/res/xml/preferences_start.xml
play-services-core/src/main/AndroidManifest.xml
play-services-core/build.gradle
build.gradle
settings.gradle
gradle.properties
"

TMPDIR="$(mktemp -d)"

trap 'rm -rf $TMPDIR; exit 1' INT

ret=0

{
    rm -rf play-services-core/build

    for file in $SRCS; do
        install -Dc "$file" "$TMPDIR/$file"
    done

    cp -R gradle "$TMPDIR/"

    (
        cd "$TMPDIR"

        echo 'org.gradle.jvmargs=-Xmx2048m -XX:+HeapDumpOnOutOfMemoryError' \
            >> gradle.properties
        gradle --no-daemon :play-services-core:build
    )
} || ret=$?

[ "$ret" = 0 ] && {
    for file in debug/play-services-core-debug.apk \
            release/play-services-core-release-unsigned.apk; do
        file="play-services-core/build/outputs/apk/$file"

        install -Dc "$TMPDIR/$file" "$PWD/$file"
        echo "Built $file"
    done
}

rm -rf "$TMPDIR"
exit "$ret"
