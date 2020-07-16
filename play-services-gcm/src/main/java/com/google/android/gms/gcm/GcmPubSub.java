/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.gcm;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.android.gms.iid.InstanceID;

import java.io.IOException;
import java.util.regex.Pattern;

import static org.microg.gms.gcm.GcmConstants.EXTRA_TOPIC;

/**
 * GcmPubSub provides a publish-subscribe model for sending GCM topic messages.
 * <p/>
 * An app can subscribe to different topics defined by the
 * developer. The app server can then send messages to the subscribed devices
 * without having to maintain topic-subscribers mapping. Topics do not
 * need to be explicitly created before subscribing or publishing—they
 * are automatically created when publishing or subscribing.
 * <pre>
 * String topic = "/topics/myTopic";
 * String registrationToken = InstanceID.getInstance(context)
 *          .getToken(SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
 * GcmPubSub.getInstance(context).subscribe(registrationToken, topic, null);
 * // Messages published to the topic will be received as regular GCM messages
 * // with 'from' set to "/topics/myTopic"</pre>
 * To publish to a topic, see
 * <a href="https://developer.android.com/google/gcm/server.html">GCM server documentation</a>.
 */
public class GcmPubSub {

    private static final Pattern topicPattern = Pattern.compile("/topics/[a-zA-Z0-9-_.~%]{1,900}");
    private static GcmPubSub INSTANCE;

    private final InstanceID instanceId;

    public GcmPubSub(Context context) {
        this.instanceId = InstanceID.getInstance(context);
    }

    /**
     * Returns an instance of GCM PubSub.
     *
     * @return GcmPubSub instance
     */
    public static synchronized GcmPubSub getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new GcmPubSub(context);
        }
        return INSTANCE;
    }

    /**
     * Subscribes an app instance to a topic, enabling it to receive messages
     * sent to that topic.
     * <p/>
     * The topic sender must be authorized to send messages to the
     * app instance. To authorize it, call {@link com.google.android.gms.iid.InstanceID#getToken(java.lang.String, java.lang.String)}
     * with the sender ID and {@link com.google.android.gms.gcm.GoogleCloudMessaging#INSTANCE_ID_SCOPE}
     * <p/>
     * Do not call this function on the main thread.
     *
     * @param registrationToken {@link com.google.android.gms.iid.InstanceID} token that authorizes topic
     *                          sender to send messages to the app instance.
     * @param topic             developer defined topic name.
     *                          Must match the following regular expression:
     *                          "/topics/[a-zA-Z0-9-_.~%]{1,900}".
     * @param extras            (optional) additional information.
     * @throws IOException if the request fails.
     */
    public void subscribe(String registrationToken, String topic, Bundle extras) throws IOException {
        if (TextUtils.isEmpty(registrationToken))
            throw new IllegalArgumentException("No registration token!");
        if (TextUtils.isEmpty(topic) || !topicPattern.matcher(topic).matches())
            throw new IllegalArgumentException("Invalid topic: " + topic);

        if (extras == null) extras = new Bundle();
        extras.putString(EXTRA_TOPIC, topic);
        instanceId.getToken(registrationToken, topic, extras);
    }

    /**
     * Unsubscribes an app instance from a topic, stopping it from receiving
     * any further messages sent to that topic.
     * <p/>
     * Do not call this function on the main thread.
     *
     * @param registrationToken {@link com.google.android.gms.iid.InstanceID} token
     *                          for the same sender and scope that was previously
     *                          used for subscribing to the topic.
     * @param topic             from which to stop receiving messages.
     * @throws IOException if the request fails.
     */
    public void unsubscribe(String registrationToken, String topic) throws IOException {
        if (TextUtils.isEmpty(topic) || !topicPattern.matcher(topic).matches())
            throw new IllegalArgumentException("Invalid topic: " + topic);

        Bundle extras = new Bundle();
        extras.putString(EXTRA_TOPIC, topic);
        instanceId.deleteToken(registrationToken, topic, extras);
    }

}