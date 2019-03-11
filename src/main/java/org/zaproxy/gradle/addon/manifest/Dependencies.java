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

import groovy.lang.Closure;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;

public class Dependencies {

    private final Property<String> javaVersion;
    private final NamedDomainObjectContainer<AddOn> addOns;

    @Inject
    public Dependencies(Project project) {
        this.javaVersion = project.getObjects().property(String.class);
        this.addOns = project.container(AddOn.class, id -> new AddOn(id, project.getObjects()));
    }

    @Input
    @Optional
    public Property<String> getJavaVersion() {
        return javaVersion;
    }

    @Nested
    public Iterable<AddOn> getAddOnsIterable() {
        return getAddOns();
    }

    @Internal
    public NamedDomainObjectContainer<AddOn> getAddOns() {
        return addOns;
    }

    public void addOns(Action<? super NamedDomainObjectContainer<AddOn>> action) {
        action.execute(addOns);
    }

    public void addOns(Closure<?> action) {
        addOns.configure(action);
    }
}
