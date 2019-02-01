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
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

public class AddOn implements Named {

    private final String id;

    private final Property<String> version;
    private final Property<String> semVer;
    private final Property<Integer> notBeforeVersion;
    private final Property<Integer> notFromVersion;

    @Inject
    public AddOn(String id, ObjectFactory objectFactory) {
        this.id = id;
        this.version = objectFactory.property(String.class);
        this.semVer = objectFactory.property(String.class);
        this.notBeforeVersion = objectFactory.property(Integer.class);
        this.notFromVersion = objectFactory.property(Integer.class);
    }

    @Internal
    @Override
    public String getName() {
        return getId();
    }

    @Input
    public String getId() {
        return id;
    }

    @Input
    @Optional
    public Property<String> getVersion() {
        return version;
    }

    @Input
    @Optional
    public Property<String> getSemVer() {
        return semVer;
    }

    @Input
    @Optional
    public Property<Integer> getNotBeforeVersion() {
        return notBeforeVersion;
    }

    @Input
    @Optional
    public Property<Integer> getNotFromVersion() {
        return notFromVersion;
    }
}
