/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.action.management.api.model;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class models the object typed action property values
 */
public class InputStreamWrapper {

    private final InputStream value;
    private int length = 0;

    public InputStreamWrapper(InputStream value) {

        this.value = value;
    }

    public InputStream getValue() {

        return value;
    }

    public int getLength() throws IOException {

        this.length = value.available();
        return length;
    }
}
