package org.microg.gms.common;

import android.content.Intent;

interface IIntentMessenger {
    oneway void sendIntent(in Intent intent);
}