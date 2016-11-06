GmsLib
======
This library is a compatibility implementation of the often used play-services library.

It will try to use the Play Services when installed on the target device. If this is not possible, a basic fallback implementation might be used.

WIP
---
This is still work in progress, and most applications will not build. 
However feel free to try it out and create issues for missing method calls (please include an application to test it).

Developer Notes
---------------

Replace all compile includes to com.google.android.gms with org.microg master-SNAPSHOT includes

	sed -i 's/compile [\'"]com.google.android.gms:\([^:]*\):[^\']*[\'"]/compile \'org.microg:\1:master-SNAPSHOT\'/g' build.gradle

