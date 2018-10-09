<!-- Please check the following:

* you are using newest microg version (ale5000 installer is updated and should be very helpful)
* you deactivated battery optimization for microg
* you activated google device registration
* you get all the green checkmarks in microg diagnostics
* you made sure nothing is blocking network traffic to mtalk.google.com (e.g. afwall, adaway, ...)
* you deleted the app you have troubles with and installed it fresh after updating to microg 0.2.6-13280
* you have not used backup solutions, like oandbackup, titaniumbackup, ... to restore app data
* you can also use the push notification tester to check. I just tried it and it works.

To verify incoming messages, look in you logs and search for "GmsGcmMcsInput".
You should see the heartbeat and Incoming messages.
In the incoming messages there should be a category stating your apps package.
This should be messages your receive via push. -->
