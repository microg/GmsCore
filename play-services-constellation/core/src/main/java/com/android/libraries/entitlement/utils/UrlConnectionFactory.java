/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.libraries.entitlement.utils;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Factory for creating {@link URLConnections}.
 */
public interface UrlConnectionFactory {

  /**
   * Returns a {@link URLConnection} instance that represents a connection to
   * the remote object referred to by the {@code URL}.
   *
   * @param url the URL to which the connection will be made.
   */
  public abstract URLConnection openConnection(URL url) throws IOException;
}
