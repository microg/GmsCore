/*
 * Copyright (C) 2019 microG Project Team
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

package org.microg.tools;

import android.util.Log;

import static android.os.Build.VERSION.SDK_INT;

public class CondLog
{

	private CondLog()
	{
	}

	public static boolean isLoggable(String tag, final int level)
	{
		// Note: Log.isLoggable() will only return true for levels of INFO and above by
		// default. To override this behavior, set system properties as described in the
		// documentation for Log.isLoggable(), or add appropriate filtering code here.

		// Log.isLoggable() will throw an exception if the length of the tag is greater than
		// 23 characters, on API Level 23 or prior, see:
		// https://developer.android.com/reference/android/util/Log.html#isLoggable(java.lang.String,%20int)
		// so trim it if necessary to avoid the exception.
		if (tag.length() > 23 && SDK_INT < 24)
		{
			tag = tag.substring(0, 22);
		}

		return Log.isLoggable(tag, level);
	}

	/**
	 * Call Log.*(tag, msg, tr) if loggable
	 * 
	 * @param tag
	 *            Used to identify the source of a log message. It usually identifies the class
	 *            or activity where the log call occurs.
	 * @param level
	 *            Requested log level
	 * @param msg
	 *            The message you would like logged.
	 * @param tr
	 */
	public static void print(final String tag, final int level, final String msg, final Throwable tr)
	{
		if (isLoggable(tag, level))
		{
			switch (level) {
				case Log.ASSERT:
					Log.wtf(tag, msg, tr);
					break;
				case Log.VERBOSE:
					Log.v(tag, msg, tr);
					break;
				case Log.DEBUG:
					Log.d(tag, msg, tr);
					break;
				case Log.INFO:
					Log.i(tag, msg, tr);
					break;
				case Log.WARN:
					Log.w(tag, msg, tr);
					break;
				case Log.ERROR:
					Log.e(tag, msg, tr);
					break;
			}
		}
	}

	/**
	 * Call Log.*(tag, msg) if loggable
	 * 
	 * @param tag
	 *            Used to identify the source of a log message. It usually identifies the class
	 *            or activity where the log call occurs.
	 * @param level
	 *            Requested log level
	 * @param msg
	 *            The message you would like logged.
	 */
	public static void print(final String tag, final int level, final String msg)
	{
		if (isLoggable(tag, level))
		{
			switch (level) {
				case Log.ASSERT:
					Log.wtf(tag, msg);
					break;
				case Log.VERBOSE:
					Log.v(tag, msg);
					break;
				case Log.DEBUG:
					Log.d(tag, msg);
					break;
				case Log.INFO:
					Log.i(tag, msg);
					break;
				case Log.WARN:
					Log.w(tag, msg);
					break;
				case Log.ERROR:
					Log.e(tag, msg);
					break;
			}
		}
	}
}
