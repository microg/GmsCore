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
To do this, download and build GmsLib plus its submodules and copy the three resulting .aar files (SafeParcel.aar, GmsApi.aar, GmsLib.aar) to `$DASHCLOCK_DIR/local_aars/`. 
Then replace `compile 'com.google.android.gms:play-services:4.0.30'` in `$DASHCLOCK_SRC/main/build.gradle` with 

	compile(name:'GmsLib', ext:'aar')
	compile(name:'GmsApi', ext:'aar')
	compile(name:'SafeParcel', ext:'aar')

and build as usual.
