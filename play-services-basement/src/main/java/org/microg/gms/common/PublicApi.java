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

package org.microg.gms.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An class, method or field is named public, if it can be used with the original play services
 * client library.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface PublicApi {
    /**
     * @return the first version that contains the given class, method or field
     */
    String since() default "0";

    /**
     * @return the last version that contains the given class, method or field
     */
    String until() default "latest";

    /**
     * Used on a method or field to exclude it from the public api if the corresponding class was
     * marked as public api.
     *
     * @return true if the method or field is not part of the public api
     */
    boolean exclude() default false;
}
