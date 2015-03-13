GmsLib
======
This library is a compatibility implementation of the often used play-services library.

It will try to use the Play Services when installed on the target device. If this is not possible, a basic fallback implementation might be used.

WIP
---
This is still work in progress, and most applications will not build. 
However feel free to try it out and create issues for missing method calls (please include an application to test it).

Example: DashClock
------------------
[DashClock](https://code.google.com/p/dashclock/) is an open source clock widget with enhanced features. 
However it uses play services as location backend and thus requires proprietary libraries to compile it. 

However, it is possible to build DashClock using GmsLib, supporting all it's location features, with or without play services installed.
To do this, download and build GmsLib plus its submodules and install it to the local gradle repository:

	$ git clone https://github.com/microg/android_external_GmsLib.git GmsLib
	$ cd GmsLib
	$ git submodule update --init --recursive
	$ gradle install

Then update the main/build.gradle to point to non-google gms in local maven:

	 repositories {
	+    maven {   url "${System.env.HOME}/.m2/repository" } // This can be mavenLocal() since Gradle 2.0
	     mavenCentral()
	     flatDir {
	         dirs '../local_aars'
	     }
	 }
	 
	 dependencies {
	     compile 'com.android.support:support-v13:22.0.0'
	-    compile 'com.google.android.gms:play-services:4.0.30'
	+    compile 'org.microg.gms:play-services:1.0-SNAPSHOT'
	     //compile 'com.mobeta.android.dslv:drag-sort-listview:0.6.1-SNAPSHOT-AAR'
	     compile 'com.mobeta.android.dslv:drag-sort-listview:0.6.1-SNAPSHOT-AAR@aar'
	     compile project(':api')
	 }

Afterwards you can compile dashclock the usual way:

	$ gradle :main:assembleDebug
