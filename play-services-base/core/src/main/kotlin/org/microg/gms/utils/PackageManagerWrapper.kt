/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.utils

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.*
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.UserHandle
import androidx.annotation.RequiresApi

open class PackageManagerWrapper(private val wrapped: PackageManager) : PackageManager() {
    override fun getPackageInfo(packageName: String, flags: Int): PackageInfo {
        return wrapped.getPackageInfo(packageName, flags)
    }

    @TargetApi(26)
    override fun getPackageInfo(versionedPackage: VersionedPackage, flags: Int): PackageInfo {
        return wrapped.getPackageInfo(versionedPackage, flags)
    }

    override fun currentToCanonicalPackageNames(packageNames: Array<out String>): Array<String> {
        return wrapped.currentToCanonicalPackageNames(packageNames)
    }

    override fun canonicalToCurrentPackageNames(packageNames: Array<out String>): Array<String> {
        return wrapped.canonicalToCurrentPackageNames(packageNames)
    }

    override fun getLaunchIntentForPackage(packageName: String): Intent? {
        return wrapped.getLaunchIntentForPackage(packageName)
    }

    @TargetApi(21)
    override fun getLeanbackLaunchIntentForPackage(packageName: String): Intent? {
        return wrapped.getLeanbackLaunchIntentForPackage(packageName)
    }

    override fun getPackageGids(packageName: String): IntArray {
        return wrapped.getPackageGids(packageName)
    }

    @TargetApi(24)
    override fun getPackageGids(packageName: String, flags: Int): IntArray {
        return wrapped.getPackageGids(packageName, flags)
    }

    @TargetApi(24)
    override fun getPackageUid(packageName: String, flags: Int): Int {
        return wrapped.getPackageUid(packageName, flags)
    }

    override fun getPermissionInfo(permName: String, flags: Int): PermissionInfo {
        return wrapped.getPermissionInfo(permName, flags)
    }

    override fun queryPermissionsByGroup(permissionGroup: String?, flags: Int): MutableList<PermissionInfo> {
        return wrapped.queryPermissionsByGroup(permissionGroup, flags)
    }

    override fun getPermissionGroupInfo(permName: String, flags: Int): PermissionGroupInfo {
        return wrapped.getPermissionGroupInfo(permName, flags)
    }

    override fun getAllPermissionGroups(flags: Int): MutableList<PermissionGroupInfo> {
        return wrapped.getAllPermissionGroups(flags)
    }

    override fun getApplicationInfo(packageName: String, flags: Int): ApplicationInfo {
        return wrapped.getApplicationInfo(packageName, flags)
    }

    override fun getActivityInfo(component: ComponentName, flags: Int): ActivityInfo {
        return wrapped.getActivityInfo(component, flags)
    }

    override fun getReceiverInfo(component: ComponentName, flags: Int): ActivityInfo {
        return wrapped.getReceiverInfo(component, flags)
    }

    override fun getServiceInfo(component: ComponentName, flags: Int): ServiceInfo {
        return wrapped.getServiceInfo(component, flags)
    }

    override fun getProviderInfo(component: ComponentName, flags: Int): ProviderInfo {
        return wrapped.getProviderInfo(component, flags)
    }

    @RequiresApi(29)
    override fun getInstalledModules(flags: Int): MutableList<ModuleInfo> {
        return wrapped.getInstalledModules(flags)
    }

    override fun getInstalledPackages(flags: Int): MutableList<PackageInfo> {
        return wrapped.getInstalledPackages(flags)
    }

    @TargetApi(18)
    override fun getPackagesHoldingPermissions(permissions: Array<out String>, flags: Int): MutableList<PackageInfo> {
        return wrapped.getPackagesHoldingPermissions(permissions, flags)
    }

    override fun checkPermission(permName: String, packageName: String): Int {
        return wrapped.checkPermission(permName, packageName)
    }

    @TargetApi(23)
    override fun isPermissionRevokedByPolicy(permName: String, packageName: String): Boolean {
        return wrapped.isPermissionRevokedByPolicy(permName, packageName)
    }

    override fun addPermission(info: PermissionInfo): Boolean {
        return wrapped.addPermission(info)
    }

    override fun addPermissionAsync(info: PermissionInfo): Boolean {
        return wrapped.addPermissionAsync(info)
    }

    override fun removePermission(permName: String) {
        return wrapped.removePermission(permName)
    }

    override fun checkSignatures(packageName1: String, packageName2: String): Int {
        return wrapped.checkSignatures(packageName1, packageName2)
    }

    override fun checkSignatures(uid1: Int, uid2: Int): Int {
        return wrapped.checkSignatures(uid1, uid2)
    }

    override fun getPackagesForUid(uid: Int): Array<String>? {
        return wrapped.getPackagesForUid(uid)
    }

    override fun getNameForUid(uid: Int): String? {
        return wrapped.getNameForUid(uid)
    }

    override fun getInstalledApplications(flags: Int): MutableList<ApplicationInfo> {
        return wrapped.getInstalledApplications(flags)
    }

    @TargetApi(26)
    override fun isInstantApp(): Boolean {
        return wrapped.isInstantApp
    }

    @TargetApi(26)
    override fun isInstantApp(packageName: String): Boolean {
        return wrapped.isInstantApp(packageName)
    }

    @TargetApi(26)
    override fun getInstantAppCookieMaxBytes(): Int {
        return wrapped.instantAppCookieMaxBytes
    }

    @TargetApi(26)
    override fun getInstantAppCookie(): ByteArray {
        return wrapped.instantAppCookie
    }

    @TargetApi(26)
    override fun clearInstantAppCookie() {
        return wrapped.clearInstantAppCookie()
    }

    @TargetApi(26)
    override fun updateInstantAppCookie(cookie: ByteArray?) {
        return wrapped.updateInstantAppCookie(cookie)
    }

    @TargetApi(26)
    override fun getSystemSharedLibraryNames(): Array<String>? {
        return wrapped.systemSharedLibraryNames
    }

    @TargetApi(26)
    override fun getSharedLibraries(flags: Int): MutableList<SharedLibraryInfo> {
        return wrapped.getSharedLibraries(flags)
    }

    @TargetApi(26)
    override fun getChangedPackages(sequenceNumber: Int): ChangedPackages? {
        return wrapped.getChangedPackages(sequenceNumber)
    }

    override fun getSystemAvailableFeatures(): Array<FeatureInfo> {
        return wrapped.systemAvailableFeatures
    }

    override fun hasSystemFeature(featureName: String): Boolean {
        return wrapped.hasSystemFeature(featureName)
    }

    @TargetApi(24)
    override fun hasSystemFeature(featureName: String, version: Int): Boolean {
        return wrapped.hasSystemFeature(featureName, version)
    }

    override fun resolveActivity(intent: Intent, flags: Int): ResolveInfo? {
        return wrapped.resolveActivity(intent, flags)
    }

    override fun queryIntentActivities(intent: Intent, flags: Int): MutableList<ResolveInfo> {
        return wrapped.queryIntentActivities(intent, flags)
    }

    override fun queryIntentActivityOptions(caller: ComponentName?, specifics: Array<out Intent>?, intent: Intent, flags: Int): MutableList<ResolveInfo> {
        return wrapped.queryIntentActivityOptions(caller, specifics, intent, flags)
    }

    override fun queryBroadcastReceivers(intent: Intent, flags: Int): MutableList<ResolveInfo> {
        return wrapped.queryBroadcastReceivers(intent, flags)
    }

    override fun resolveService(intent: Intent, flags: Int): ResolveInfo? {
        return wrapped.resolveService(intent, flags)
    }

    override fun queryIntentServices(intent: Intent, flags: Int): MutableList<ResolveInfo> {
        return wrapped.queryIntentServices(intent, flags)
    }

    @TargetApi(19)
    override fun queryIntentContentProviders(intent: Intent, flags: Int): MutableList<ResolveInfo> {
        return wrapped.queryIntentContentProviders(intent, flags)
    }

    override fun resolveContentProvider(authority: String, flags: Int): ProviderInfo? {
        return wrapped.resolveContentProvider(authority, flags)
    }

    override fun queryContentProviders(processName: String?, uid: Int, flags: Int): MutableList<ProviderInfo> {
        return wrapped.queryContentProviders(processName, uid, flags)
    }

    override fun getInstrumentationInfo(className: ComponentName, flags: Int): InstrumentationInfo {
        return wrapped.getInstrumentationInfo(className, flags)
    }

    override fun queryInstrumentation(targetPackage: String, flags: Int): MutableList<InstrumentationInfo> {
        return wrapped.queryInstrumentation(targetPackage, flags)
    }

    override fun getDrawable(packageName: String, resid: Int, appInfo: ApplicationInfo?): Drawable? {
        return wrapped.getDrawable(packageName, resid, appInfo)
    }

    override fun getActivityIcon(activityName: ComponentName): Drawable {
        return wrapped.getActivityIcon(activityName)
    }

    override fun getActivityIcon(intent: Intent): Drawable {
        return wrapped.getActivityIcon(intent)
    }

    @TargetApi(20)
    override fun getActivityBanner(activityName: ComponentName): Drawable? {
        return wrapped.getActivityBanner(activityName)
    }

    @TargetApi(20)
    override fun getActivityBanner(intent: Intent): Drawable? {
        return wrapped.getActivityBanner(intent)
    }

    override fun getDefaultActivityIcon(): Drawable {
        return wrapped.defaultActivityIcon
    }

    override fun getApplicationIcon(info: ApplicationInfo): Drawable {
        return wrapped.getApplicationIcon(info)
    }

    override fun getApplicationIcon(packageName: String): Drawable {
        return wrapped.getApplicationIcon(packageName)
    }

    @TargetApi(20)
    override fun getApplicationBanner(info: ApplicationInfo): Drawable? {
        return wrapped.getApplicationBanner(info)
    }

    @TargetApi(20)
    override fun getApplicationBanner(packageName: String): Drawable? {
        return wrapped.getApplicationBanner(packageName)
    }

    override fun getActivityLogo(activityName: ComponentName): Drawable? {
        return wrapped.getActivityLogo(activityName)
    }

    override fun getActivityLogo(intent: Intent): Drawable? {
        return wrapped.getActivityLogo(intent)
    }

    override fun getApplicationLogo(info: ApplicationInfo): Drawable? {
        return wrapped.getApplicationLogo(info)
    }

    override fun getApplicationLogo(packageName: String): Drawable? {
        return wrapped.getApplicationLogo(packageName)
    }

    @TargetApi(21)
    override fun getUserBadgedIcon(drawable: Drawable, user: UserHandle): Drawable {
        return wrapped.getUserBadgedIcon(drawable, user)
    }

    @TargetApi(21)
    override fun getUserBadgedDrawableForDensity(drawable: Drawable, user: UserHandle, badgeLocation: Rect?, badgeDensity: Int): Drawable {
        return wrapped.getUserBadgedDrawableForDensity(drawable, user, badgeLocation, badgeDensity)
    }

    @TargetApi(21)
    override fun getUserBadgedLabel(label: CharSequence, user: UserHandle): CharSequence {
        return wrapped.getUserBadgedLabel(label, user)
    }

    override fun getText(packageName: String, resid: Int, appInfo: ApplicationInfo?): CharSequence? {
        return wrapped.getText(packageName, resid, appInfo)
    }

    override fun getXml(packageName: String, resid: Int, appInfo: ApplicationInfo?): XmlResourceParser? {
        return wrapped.getXml(packageName, resid, appInfo)
    }

    override fun getApplicationLabel(info: ApplicationInfo): CharSequence {
        return wrapped.getApplicationLabel(info)
    }

    override fun getResourcesForActivity(activityName: ComponentName): Resources {
        return wrapped.getResourcesForActivity(activityName)
    }

    override fun getResourcesForApplication(app: ApplicationInfo): Resources {
        return wrapped.getResourcesForApplication(app)
    }

    override fun getResourcesForApplication(packageName: String): Resources {
        return wrapped.getResourcesForApplication(packageName)
    }

    override fun verifyPendingInstall(id: Int, verificationCode: Int) {
        return wrapped.verifyPendingInstall(id, verificationCode)
    }

    @TargetApi(17)
    override fun extendVerificationTimeout(id: Int, verificationCodeAtTimeout: Int, millisecondsToDelay: Long) {
        return wrapped.extendVerificationTimeout(id, verificationCodeAtTimeout, millisecondsToDelay)
    }

    override fun setInstallerPackageName(targetPackage: String, installerPackageName: String?) {
        return wrapped.setInstallerPackageName(targetPackage, installerPackageName)
    }

    override fun getInstallerPackageName(packageName: String): String? {
        return wrapped.getInstallerPackageName(packageName)
    }

    override fun addPackageToPreferred(packageName: String) {
        return wrapped.addPackageToPreferred(packageName)
    }

    override fun removePackageFromPreferred(packageName: String) {
        return wrapped.removePackageFromPreferred(packageName)
    }

    override fun getPreferredPackages(flags: Int): MutableList<PackageInfo> {
        return wrapped.getPreferredPackages(flags)
    }

    override fun addPreferredActivity(filter: IntentFilter, match: Int, set: Array<out ComponentName>?, activity: ComponentName) {
        return wrapped.addPreferredActivity(filter, match, set, activity)
    }

    override fun clearPackagePreferredActivities(packageName: String) {
        return wrapped.clearPackagePreferredActivities(packageName)
    }

    override fun getPreferredActivities(outFilters: MutableList<IntentFilter>, outActivities: MutableList<ComponentName>, packageName: String?): Int {
        return wrapped.getPreferredActivities(outFilters, outActivities, packageName)
    }

    override fun setComponentEnabledSetting(componentName: ComponentName, newState: Int, flags: Int) {
        return wrapped.setComponentEnabledSetting(componentName, newState, flags)
    }

    override fun getComponentEnabledSetting(componentName: ComponentName): Int {
        return wrapped.getComponentEnabledSetting(componentName)
    }

    override fun setApplicationEnabledSetting(packageName: String, newState: Int, flags: Int) {
        return wrapped.setApplicationEnabledSetting(packageName, newState, flags)
    }

    override fun getApplicationEnabledSetting(packageName: String): Int {
        return wrapped.getApplicationEnabledSetting(packageName)
    }

    override fun isSafeMode(): Boolean {
        return wrapped.isSafeMode
    }

    @TargetApi(26)
    override fun setApplicationCategoryHint(packageName: String, categoryHint: Int) {
        return wrapped.setApplicationCategoryHint(packageName, categoryHint)
    }

    @TargetApi(21)
    override fun getPackageInstaller(): PackageInstaller {
        return wrapped.packageInstaller
    }

    @TargetApi(26)
    override fun canRequestPackageInstalls(): Boolean {
        return wrapped.canRequestPackageInstalls()
    }


    @TargetApi(29)
    override fun addWhitelistedRestrictedPermission(packageName: String, permName: String, whitelistFlags: Int): Boolean {
        return wrapped.addWhitelistedRestrictedPermission(packageName, permName, whitelistFlags)
    }

    @TargetApi(30)
    override fun getBackgroundPermissionOptionLabel(): CharSequence {
        return wrapped.getBackgroundPermissionOptionLabel()
    }

    @TargetApi(30)
    override fun getInstallSourceInfo(packageName: String): InstallSourceInfo {
        return wrapped.getInstallSourceInfo(packageName)
    }

    @TargetApi(30)
    override fun getMimeGroup(mimeGroup: String): MutableSet<String> {
        return wrapped.getMimeGroup(mimeGroup)
    }

    @TargetApi(29)
    override fun getModuleInfo(packageName: String, flags: Int): ModuleInfo {
        return wrapped.getModuleInfo(packageName, flags)
    }

    override fun getPackageArchiveInfo(archiveFilePath: String, flags: Int): PackageInfo? {
        return wrapped.getPackageArchiveInfo(archiveFilePath, flags)
    }

    @TargetApi(28)
    override fun getSuspendedPackageAppExtras(): Bundle? {
        return wrapped.suspendedPackageAppExtras
    }

    @TargetApi(29)
    override fun getSyntheticAppDetailsActivityEnabled(packageName: String): Boolean {
        return wrapped.getSyntheticAppDetailsActivityEnabled(packageName)
    }

    @TargetApi(29)
    override fun getWhitelistedRestrictedPermissions(packageName: String, whitelistFlag: Int): MutableSet<String> {
        return wrapped.getWhitelistedRestrictedPermissions(packageName, whitelistFlag)
    }

    @TargetApi(28)
    override fun hasSigningCertificate(packageName: String, certificate: ByteArray, type: Int): Boolean {
        return wrapped.hasSigningCertificate(packageName, certificate, type)
    }

    @TargetApi(28)
    override fun hasSigningCertificate(uid: Int, certificate: ByteArray, type: Int): Boolean {
        return wrapped.hasSigningCertificate(uid, certificate, type)
    }

    @TargetApi(30)
    override fun isAutoRevokeWhitelisted(): Boolean {
        return wrapped.isAutoRevokeWhitelisted
    }

    @TargetApi(30)
    override fun isAutoRevokeWhitelisted(packageName: String): Boolean {
        return wrapped.isAutoRevokeWhitelisted(packageName)
    }

    @TargetApi(30)
    override fun isDefaultApplicationIcon(drawable: Drawable): Boolean {
        return wrapped.isDefaultApplicationIcon(drawable)
    }

    @TargetApi(29)
    override fun isDeviceUpgrading(): Boolean {
        return wrapped.isDeviceUpgrading
    }

    @TargetApi(28)
    override fun isPackageSuspended(): Boolean {
        return wrapped.isPackageSuspended
    }

    @TargetApi(29)
    override fun isPackageSuspended(packageName: String): Boolean {
        return wrapped.isPackageSuspended(packageName)
    }

    @TargetApi(29)
    override fun removeWhitelistedRestrictedPermission(packageName: String, permName: String, whitelistFlags: Int): Boolean {
        return wrapped.removeWhitelistedRestrictedPermission(packageName, permName, whitelistFlags)
    }

    @TargetApi(30)
    override fun setAutoRevokeWhitelisted(packageName: String, whitelisted: Boolean): Boolean {
        return wrapped.setAutoRevokeWhitelisted(packageName, whitelisted)
    }

    @TargetApi(30)
    override fun setMimeGroup(mimeGroup: String, mimeTypes: MutableSet<String>) {
        return wrapped.setMimeGroup(mimeGroup, mimeTypes)
    }
}
