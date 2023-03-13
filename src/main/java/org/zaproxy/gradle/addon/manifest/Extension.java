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
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;

public class Extension implements Named {

    private final String classname;
    private final Project project;
    private final Property<Classnames> classnames;
    private final Property<Dependencies> dependencies;

    @Inject
    public Extension(String classname, Project project) {
        this.classname = classname;
        this.project = project;

        ObjectFactory objectFactory = project.getObjects();
        this.classnames = objectFactory.property(Classnames.class);
        this.dependencies = objectFactory.property(Dependencies.class);
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

    @Nested
    @Optional
    public Property<Classnames> getClassnames() {
        return classnames;
    }

    public void classnames(Action<? super Classnames> action) {
        if (!classnames.isPresent()) {
            classnames.set(
                    project.getObjects().newInstance(Classnames.class, project.getObjects()));
        }
        action.execute(classnames.get());
    }

    public void classnames(Closure<? super Classnames> c) {
        if (!classnames.isPresent()) {
            classnames.set(
                    project.getObjects().newInstance(Classnames.class, project.getObjects()));
        }
        call(c, classnames.get());
    }

    private static <T> void call(Closure<? super T> c, T value) {
        c.setDelegate(value);
        c.call(value);
    }

    @Nested
    @Optional
    public Property<Dependencies> getDependencies() {
        return dependencies;
    }

    public void dependencies(Action<? super Dependencies> action) {
        if (!dependencies.isPresent()) {
            dependencies.set(project.getObjects().newInstance(Dependencies.class, project));
        }
        action.execute(dependencies.get());
    }

    public void dependencies(Closure<? super Dependencies> c) {
        if (!dependencies.isPresent()) {
            dependencies.set(project.getObjects().newInstance(Dependencies.class, project));
        }
        call(c, dependencies.get());
    }

    public static class Dependencies {

        private final NamedDomainObjectContainer<AddOn> addOns;
        private final NamedDomainObjectContainer<Extension> extensions;

        @Inject
        public Dependencies(Project project) {
            this.addOns = project.container(AddOn.class, id -> new AddOn(id, project.getObjects()));
            this.extensions =
                    project.container(
                            Extension.class, classname -> new Extension(classname, project));
        }

        @Nested
        public Iterable<AddOn> getAddOnsIterable() {
            return getAddOns();
        }

        @Nested
        public Iterable<Extension> getExtensionsIterable() {
            return getExtensions();
        }

        @Internal
        public NamedDomainObjectContainer<AddOn> getAddOns() {
            return addOns;
        }

        @Internal
        public NamedDomainObjectContainer<Extension> getExtensions() {
            return extensions;
        }

        public void addOns(Action<? super NamedDomainObjectContainer<AddOn>> action) {
            action.execute(addOns);
        }

        public void extensions(Action<? super NamedDomainObjectContainer<Extension>> action) {
            action.execute(extensions);
        }
    }
}
