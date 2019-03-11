/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2019 The ZAP Development Team
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
package org.zaproxy.gradle.addon.manifest;

import javax.inject.Inject;
import org.gradle.api.Named;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

public class ScanRule implements Named {

    private final String classname;

    @Inject
    public ScanRule(String classname) {
        this.classname = classname;
    }

    @Internal
    @Override
    public String getName() {
        return getClassname();
    }

    @Input
    public String getClassname() {
        return classname;
    }
}
