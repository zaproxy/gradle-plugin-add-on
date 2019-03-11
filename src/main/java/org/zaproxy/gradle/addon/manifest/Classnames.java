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
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

public class Classnames {

    private final ListProperty<String> allowed;
    private final ListProperty<String> restricted;

    @Inject
    public Classnames(ObjectFactory objectFactory) {
        this.allowed = objectFactory.listProperty(String.class);
        this.restricted = objectFactory.listProperty(String.class);
    }

    @Input
    @Optional
    public ListProperty<String> getAllowed() {
        return allowed;
    }

    @Input
    @Optional
    public ListProperty<String> getRestricted() {
        return restricted;
    }
}
