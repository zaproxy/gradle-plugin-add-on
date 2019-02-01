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
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public class ManifestExtension {

    private final Property<String> semVer;
    private final Property<String> description;
    private final Property<String> author;
    private final Property<String> url;
    private final Property<String> changes;
    private final RegularFileProperty changesFile;
    private final Property<Dependencies> dependencies;
    private final Property<Bundle> bundle;
    private final Property<HelpSet> helpSet;
    private final Property<Classnames> classnames;
    private NamedDomainObjectContainer<Extension> extensions;
    private NamedDomainObjectContainer<ScanRule> ascanrules;
    private NamedDomainObjectContainer<ScanRule> pscanrules;
    private final ConfigurableFileCollection files;
    private final Property<String> notBeforeVersion;
    private final Property<String> notFromVersion;

    private final ConfigurableFileCollection classpath;

    private final DirectoryProperty outputDir;

    private final Project project;

    @Inject
    public ManifestExtension(Project project) {
        this.project = project;
        ObjectFactory objects = project.getObjects();

        this.semVer = objects.property(String.class);
        this.description = objects.property(String.class);
        this.description.set(project.provider(project::getDescription));
        this.author = objects.property(String.class);
        this.url = objects.property(String.class);
        this.changes = objects.property(String.class);
        this.changesFile = objects.fileProperty();
        this.dependencies = objects.property(Dependencies.class);
        this.bundle = objects.property(Bundle.class);
        this.helpSet = objects.property(HelpSet.class);
        this.classnames = objects.property(Classnames.class);
        this.extensions =
                project.container(Extension.class, classname -> new Extension(classname, project));
        this.ascanrules = project.container(ScanRule.class, ScanRule::new);
        this.pscanrules = project.container(ScanRule.class, ScanRule::new);
        this.files = project.files();
        this.notBeforeVersion = objects.property(String.class);
        this.notFromVersion = objects.property(String.class);
        this.classpath = project.files();
        this.outputDir = objects.directoryProperty();
    }

    public Property<String> getSemVer() {
        return semVer;
    }

    public Property<String> getDescription() {
        return description;
    }

    public Property<String> getAuthor() {
        return author;
    }

    public Property<String> getUrl() {
        return url;
    }

    public Property<String> getChanges() {
        return changes;
    }

    public RegularFileProperty getChangesFile() {
        return changesFile;
    }

    public Property<Dependencies> getDependencies() {
        return dependencies;
    }

    public void dependencies(Action<? super Dependencies> action) {
        if (!dependencies.isPresent()) {
            dependencies.set(project.getObjects().newInstance(Dependencies.class, project));
        }
        action.execute(dependencies.get());
    }

    public Property<Bundle> getBundle() {
        return bundle;
    }

    public void bundle(Action<? super Bundle> action) {
        if (!bundle.isPresent()) {
            bundle.set(project.getObjects().newInstance(Bundle.class, project.getObjects()));
        }
        action.execute(bundle.get());
    }

    public Property<HelpSet> getHelpSet() {
        return helpSet;
    }

    public void helpSet(Action<? super HelpSet> action) {
        if (!helpSet.isPresent()) {
            helpSet.set(project.getObjects().newInstance(HelpSet.class, project.getObjects()));
        }
        action.execute(helpSet.get());
    }

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

    public NamedDomainObjectContainer<Extension> getAddOnExtensions() {
        return extensions;
    }

    public void extensions(Action<? super NamedDomainObjectContainer<Extension>> action) {
        action.execute(extensions);
    }

    public NamedDomainObjectContainer<ScanRule> getAscanrules() {
        return ascanrules;
    }

    public void ascanrules(Action<? super NamedDomainObjectContainer<ScanRule>> action) {
        action.execute(ascanrules);
    }

    public NamedDomainObjectContainer<ScanRule> getPscanrules() {
        return pscanrules;
    }

    public void pscanrules(Action<? super NamedDomainObjectContainer<ScanRule>> action) {
        action.execute(pscanrules);
    }

    public ConfigurableFileCollection getFiles() {
        return files;
    }

    public Property<String> getNotBeforeVersion() {
        return notBeforeVersion;
    }

    public Property<String> getNotFromVersion() {
        return notFromVersion;
    }

    public ConfigurableFileCollection getClasspath() {
        return classpath;
    }

    public DirectoryProperty getOutputDir() {
        return outputDir;
    }
}
