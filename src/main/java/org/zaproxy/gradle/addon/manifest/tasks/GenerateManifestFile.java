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
package org.zaproxy.gradle.addon.manifest.tasks;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.zaproxy.gradle.addon.AddOnStatus;
import org.zaproxy.gradle.addon.internal.Constants;
import org.zaproxy.gradle.addon.internal.DefaultIndenter;
import org.zaproxy.gradle.addon.manifest.Bundle;
import org.zaproxy.gradle.addon.manifest.BundledLibs;
import org.zaproxy.gradle.addon.manifest.Classnames;
import org.zaproxy.gradle.addon.manifest.Dependencies;
import org.zaproxy.gradle.addon.manifest.Extension;
import org.zaproxy.gradle.addon.manifest.HelpSet;
import org.zaproxy.gradle.addon.manifest.ScanRule;

/** A task that generates the manifest ({@code ZapAddOn.xml}) for a given add-on. */
@CacheableTask
public class GenerateManifestFile extends DefaultTask {

    private final Property<String> addOnName;
    private final Property<String> version;
    private final Property<String> semVer;
    private final Property<AddOnStatus> status;
    private final Property<String> addOnDescription;
    private final Property<String> author;
    private final Property<String> url;
    private final Property<String> changes;
    private final RegularFileProperty changesFile;
    private final Property<String> repo;
    private final Property<Dependencies> dependencies;
    private final Property<BundledLibs> bundledLibs;
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
    private final ConfigurableFileCollection compileClasspath;

    private final DirectoryProperty outputDir;
    private final Provider<RegularFile> manifest;

    public GenerateManifestFile() {
        ObjectFactory objects = getProject().getObjects();
        this.addOnName = objects.property(String.class);
        this.version = objects.property(String.class);
        this.version.set(getProject().provider(() -> getProject().getVersion().toString()));
        this.semVer = objects.property(String.class);
        this.status = objects.property(AddOnStatus.class).value(AddOnStatus.ALPHA);
        this.addOnDescription = objects.property(String.class);
        this.addOnDescription.set(getProject().provider(getProject()::getDescription));
        this.author = objects.property(String.class);
        this.url = objects.property(String.class);
        this.changes = objects.property(String.class);
        this.changesFile = objects.fileProperty();
        this.repo = objects.property(String.class);
        this.dependencies = objects.property(Dependencies.class);
        this.bundledLibs = objects.property(BundledLibs.class);
        this.bundle = objects.property(Bundle.class);
        this.helpSet = objects.property(HelpSet.class);
        this.classnames = objects.property(Classnames.class);
        this.extensions =
                getProject()
                        .container(
                                Extension.class,
                                classname -> new Extension(classname, getProject()));
        this.ascanrules = getProject().container(ScanRule.class, ScanRule::new);
        this.pscanrules = getProject().container(ScanRule.class, ScanRule::new);
        this.files = getProject().files();
        this.notBeforeVersion = objects.property(String.class);
        this.notFromVersion = objects.property(String.class);
        this.classpath = getProject().files();
        this.compileClasspath = getProject().files();
        this.outputDir = objects.directoryProperty();
        this.manifest = outputDir.map(dir -> dir.file(Constants.ADD_ON_MANIFEST_FILE_NAME));
    }

    @Input
    public Property<String> getAddOnName() {
        return addOnName;
    }

    @Input
    public Property<String> getVersion() {
        return version;
    }

    @Input
    @Optional
    public Property<String> getSemVer() {
        return semVer;
    }

    @Input
    public Property<AddOnStatus> getStatus() {
        return status;
    }

    @Input
    @Optional
    public Property<String> getAddOnDescription() {
        return addOnDescription;
    }

    @Input
    @Optional
    public Property<String> getAuthor() {
        return author;
    }

    @Input
    @Optional
    public Property<String> getUrl() {
        return url;
    }

    @Input
    @Optional
    public Property<String> getChanges() {
        return changes;
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    @Optional
    public RegularFileProperty getChangesFile() {
        return changesFile;
    }

    @Input
    @Optional
    public Property<String> getRepo() {
        return repo;
    }

    @Nested
    @Optional
    public Property<Dependencies> getDependencies() {
        return dependencies;
    }

    @Nested
    @Optional
    public Property<BundledLibs> getBundledLibs() {
        return bundledLibs;
    }

    @Nested
    @Optional
    public Property<Bundle> getBundle() {
        return bundle;
    }

    @Nested
    @Optional
    public Property<HelpSet> getHelpSet() {
        return helpSet;
    }

    @Nested
    @Optional
    public Property<Classnames> getClassnames() {
        return classnames;
    }

    @Nested
    @Optional
    public Iterable<Extension> getAddOnExtensions() {
        return new ArrayList<>(extensions);
    }

    public void setExtensions(NamedDomainObjectContainer<Extension> extensions) {
        this.extensions = extensions;
    }

    public void extensions(Action<? super NamedDomainObjectContainer<Extension>> action) {
        action.execute(extensions);
    }

    @Nested
    @Optional
    public Iterable<ScanRule> getAscanrules() {
        return new ArrayList<>(ascanrules);
    }

    public void setAscanrules(NamedDomainObjectContainer<ScanRule> ascanrules) {
        this.ascanrules = ascanrules;
    }

    public void ascanrules(Action<? super NamedDomainObjectContainer<ScanRule>> action) {
        action.execute(ascanrules);
    }

    @Nested
    @Optional
    public Iterable<ScanRule> getPscanrules() {
        return new ArrayList<>(pscanrules);
    }

    public void setPscanrules(NamedDomainObjectContainer<ScanRule> pscanrules) {
        this.pscanrules = pscanrules;
    }

    public void pscanrules(Action<? super NamedDomainObjectContainer<ScanRule>> action) {
        action.execute(pscanrules);
    }

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getFiles() {
        return files;
    }

    @Input
    @Optional
    public Property<String> getNotBeforeVersion() {
        return notBeforeVersion;
    }

    @Input
    @Optional
    public Property<String> getNotFromVersion() {
        return notFromVersion;
    }

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getClasspath() {
        return classpath;
    }

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getCompileClasspath() {
        return compileClasspath;
    }

    public void dependencies(Action<? super Dependencies> action) {
        if (!dependencies.isPresent()) {
            dependencies.set(
                    getProject().getObjects().newInstance(Dependencies.class, getProject()));
        }
        action.execute(dependencies.get());
    }

    public void bundledLibs(Action<? super BundledLibs> action) {
        if (!bundledLibs.isPresent()) {
            bundledLibs.set(
                    getProject()
                            .getObjects()
                            .newInstance(BundledLibs.class, getProject().getObjects()));
        }
        action.execute(bundledLibs.get());
    }

    public void bundle(Action<? super Bundle> action) {
        if (!bundle.isPresent()) {
            bundle.set(
                    getProject().getObjects().newInstance(Bundle.class, getProject().getObjects()));
        }
        action.execute(bundle.get());
    }

    public void helpSet(Action<? super HelpSet> action) {
        if (!helpSet.isPresent()) {
            helpSet.set(
                    getProject()
                            .getObjects()
                            .newInstance(HelpSet.class, getProject().getObjects()));
        }
        action.execute(helpSet.get());
    }

    public void classnames(Action<? super Classnames> action) {
        if (!classnames.isPresent()) {
            classnames.set(
                    getProject()
                            .getObjects()
                            .newInstance(Classnames.class, getProject().getObjects()));
        }
        action.execute(classnames.get());
    }

    @Internal
    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    @OutputFile
    public Provider<RegularFile> getManifest() {
        return manifest;
    }

    @TaskAction
    public void generate() throws IOException {
        if ("unspecified".equals(version.get())) {
            throw new InvalidUserDataException("No version specified for the add-on.");
        }

        if (getChangesFile().isPresent() && getChanges().isPresent()) {
            throw new InvalidUserDataException("Only one type of changes property must be set.");
        }

        try (Writer w = Files.newBufferedWriter(getManifest().get().getAsFile().toPath())) {
            DefaultXmlPrettyPrinter.Indenter indenter = new DefaultIndenter();
            DefaultXmlPrettyPrinter printer = new DefaultXmlPrettyPrinter();
            printer.indentObjectsWith(indenter);
            printer.indentArraysWith(indenter);

            XmlMapper mapper = new XmlMapper();
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            mapper.writer(printer).writeValue(w, createManifest());
        }
    }

    private org.zaproxy.gradle.addon.internal.model.Manifest createManifest() throws IOException {
        org.zaproxy.gradle.addon.internal.model.Manifest manifest =
                new org.zaproxy.gradle.addon.internal.model.Manifest();
        manifest.name = getAddOnName().get();
        manifest.version = getVersion().get();
        manifest.semver = getSemVer().getOrNull();
        manifest.status = getStatus().get().toString();
        manifest.description = getAddOnDescription().getOrNull();
        manifest.author = getAuthor().getOrNull();
        manifest.url = getUrl().getOrNull();
        manifest.changes =
                getChangesFile().isPresent()
                        ? readContents(getChangesFile().getAsFile().get().toPath())
                        : getChanges().getOrNull();
        manifest.repo = getRepo().getOrNull();

        if (getDependencies().isPresent()) {
            Dependencies dep = getDependencies().get();
            manifest.dependencies = new org.zaproxy.gradle.addon.internal.model.Dependencies();
            if (dep.getJavaVersion().isPresent()) {
                manifest.dependencies.javaversion = dep.getJavaVersion().get();
            }
            if (!dep.getAddOns().isEmpty()) {
                manifest.dependencies.addOns = new ArrayList<>();
                dep.getAddOns()
                        .forEach(
                                e -> {
                                    org.zaproxy.gradle.addon.internal.model.AddOn addOnSer =
                                            new org.zaproxy.gradle.addon.internal.model.AddOn();
                                    addOnSer.id = e.getId();
                                    addOnSer.version = e.getVersion().getOrNull();
                                    addOnSer.semver = e.getSemVer().getOrNull();
                                    if (e.getNotBeforeVersion().isPresent()) {
                                        int value = e.getNotBeforeVersion().get();
                                        if (value != 0) {
                                            addOnSer.notBeforeVersion = value;
                                        }
                                    }
                                    if (e.getNotFromVersion().isPresent()) {
                                        int value = e.getNotFromVersion().get();
                                        if (value != 0) {
                                            addOnSer.notFromVersion = value;
                                        }
                                    }
                                    manifest.dependencies.addOns.add(addOnSer);
                                });
            }
        }

        if (getBundledLibs().isPresent()) {
            BundledLibs bundledLibs1 = getBundledLibs().get();
            ConfigurableFileCollection libs = bundledLibs1.getLibs();
            if (!libs.isEmpty()) {
                String dirName = bundledLibs1.getDirName().get();
                manifest.libs =
                        libs.getFiles().stream()
                                .filter(File::isFile)
                                .map(File::getName)
                                .sorted()
                                .map(name -> dirName + "/" + name)
                                .collect(Collectors.toList());
            }
        }

        if (getClassnames().isPresent()) {
            Classnames names = getClassnames().get();
            List<String> allowed = names.getAllowed().getOrElse(Collections.emptyList());
            List<String> restricted = names.getRestricted().getOrElse(Collections.emptyList());
            if (!allowed.isEmpty() || !restricted.isEmpty()) {
                manifest.classnames = new org.zaproxy.gradle.addon.internal.model.Classnames();
                manifest.classnames.allowed = allowed;
                manifest.classnames.restricted = restricted;
            }
        }

        if (getBundle().isPresent()) {
            Bundle bundle1 = getBundle().get();
            manifest.bundle = new org.zaproxy.gradle.addon.internal.model.Bundle();
            manifest.bundle.bundle = bundle1.getBaseName().get();
            manifest.bundle.prefix = bundle1.getPrefix().getOrNull();
        }

        if (getHelpSet().isPresent()) {
            HelpSet helpSet1 = getHelpSet().get();
            manifest.helpset = new org.zaproxy.gradle.addon.internal.model.Helpset();
            manifest.helpset.helpset = helpSet1.getBaseName().get();
            manifest.helpset.localetoken = helpSet1.getLocaleToken().getOrNull();
        }

        final List<String> extensionsClasspath = new ArrayList<>();
        final List<String> ascanrulesClasspath = new ArrayList<>();
        final List<String> pscanrulesClasspath = new ArrayList<>();
        if (!classpath.isEmpty()) {
            Set<File> targetClasses = new HashSet<>(classpath.getFiles());
            Set<File> allClasses = new HashSet<>(targetClasses);
            allClasses.addAll(compileClasspath.getFiles());
            try (ScanResult scanResult =
                    new ClassGraph().overrideClasspath(allClasses).enableAllInfo().scan()) {
                ClassInfoList addOnClasses =
                        scanResult
                                .getAllStandardClasses()
                                .filter(ci -> targetClasses.contains(ci.getClasspathElementFile()));
                addClassesAssignableTo(
                        extensionsClasspath,
                        addOnClasses,
                        scanResult.getClassInfo("org.parosproxy.paros.extension.Extension"));
                addClassesAssignableTo(
                        ascanrulesClasspath,
                        addOnClasses,
                        scanResult.getClassInfo("org.parosproxy.paros.core.scanner.Plugin"));
                addClassesAssignableTo(
                        pscanrulesClasspath,
                        addOnClasses,
                        scanResult.getClassInfo(
                                "org.zaproxy.zap.extension.pscan.PluginPassiveScanner"));
            }
        }

        if (!extensions.isEmpty() || !extensionsClasspath.isEmpty()) {
            manifest.extensions = new ArrayList<>();
            extensions.forEach(
                    e -> {
                        extensionsClasspath.remove(e.getClassname());
                        org.zaproxy.gradle.addon.internal.model.Extension extSer =
                                new org.zaproxy.gradle.addon.internal.model.Extension(
                                        e.getClassname());
                        if (e.getClassnames().isPresent()) {
                            Classnames names = e.getClassnames().get();
                            List<String> allowed =
                                    names.getAllowed().getOrElse(Collections.emptyList());
                            List<String> restricted =
                                    names.getRestricted().getOrElse(Collections.emptyList());
                            if (!allowed.isEmpty() || !restricted.isEmpty()) {
                                extSer.classnames =
                                        new org.zaproxy.gradle.addon.internal.model.Classnames();
                                extSer.classnames.allowed = allowed;
                                extSer.classnames.restricted = restricted;
                                extSer.version = "1";
                            }
                        }
                        if (e.getDependencies().isPresent()) {
                            Extension.Dependencies dep = e.getDependencies().get();
                            if (!dep.getAddOns().isEmpty()) {
                                extSer.dependencies =
                                        new org.zaproxy.gradle.addon.internal.model
                                                .DependenciesExt();
                                extSer.dependencies.addOns = new ArrayList<>();
                                dep.getAddOns()
                                        .forEach(
                                                addOn -> {
                                                    org.zaproxy.gradle.addon.internal.model.AddOn
                                                            addOnSer =
                                                                    new org.zaproxy.gradle.addon
                                                                            .internal.model.AddOn();
                                                    addOnSer.id = addOn.getId();
                                                    if (addOn.getVersion().isPresent()) {
                                                        addOnSer.version = addOn.getVersion().get();
                                                    }
                                                    if (addOn.getSemVer().isPresent()) {
                                                        addOnSer.semver = addOn.getSemVer().get();
                                                    }
                                                    if (addOn.getNotBeforeVersion().isPresent()) {
                                                        int value =
                                                                addOn.getNotBeforeVersion().get();
                                                        if (value != 0) {
                                                            addOnSer.notBeforeVersion = value;
                                                        }
                                                    }
                                                    if (addOn.getNotFromVersion().isPresent()) {
                                                        int value = addOn.getNotFromVersion().get();
                                                        if (value != 0) {
                                                            addOnSer.notFromVersion = value;
                                                        }
                                                    }
                                                    extSer.dependencies.addOns.add(addOnSer);
                                                });
                                extSer.version = "1";
                            }
                            if (!dep.getExtensions().isEmpty()) {
                                if (extSer.dependencies == null) {
                                    extSer.dependencies =
                                            new org.zaproxy.gradle.addon.internal.model
                                                    .DependenciesExt();
                                    extSer.version = "1";
                                }
                                extSer.dependencies.extensions = new ArrayList<>();
                                dep.getExtensions()
                                        .forEach(
                                                extension -> {
                                                    org.zaproxy.gradle.addon.internal.model
                                                                    .ExtensionNoDeps
                                                            depExtSer =
                                                                    new org.zaproxy.gradle.addon
                                                                            .internal.model
                                                                            .ExtensionNoDeps();
                                                    depExtSer.extension = extension.getClassname();
                                                    extSer.dependencies.extensions.add(depExtSer);
                                                });
                            }
                        }
                        manifest.extensions.add(extSer);
                    });
            extensionsClasspath.forEach(
                    classname ->
                            manifest.extensions.add(
                                    new org.zaproxy.gradle.addon.internal.model.Extension(
                                            classname)));
            manifest.extensions.sort(Comparator.comparing(e -> e.classname));
        }

        if (!ascanrules.isEmpty() || !ascanrulesClasspath.isEmpty()) {
            manifest.ascanrules = new ArrayList<>();
            ascanrules.forEach(e -> manifest.ascanrules.add(e.getClassname()));
            ascanrulesClasspath.stream()
                    .filter(e -> !manifest.ascanrules.contains(e))
                    .forEach(manifest.ascanrules::add);
            Collections.sort(manifest.ascanrules);
        }

        if (!pscanrules.isEmpty() || !pscanrulesClasspath.isEmpty()) {
            manifest.pscanrules = new ArrayList<>();
            pscanrules.forEach(e -> manifest.pscanrules.add(e.getClassname()));
            pscanrulesClasspath.stream()
                    .filter(e -> !manifest.pscanrules.contains(e))
                    .forEach(manifest.pscanrules::add);
            Collections.sort(manifest.pscanrules);
        }

        if (!getFiles().isEmpty()) {
            List<String> paths = new ArrayList<>();
            getFiles()
                    .getAsFileTree()
                    .visit(
                            details -> {
                                if (!details.isDirectory()) {
                                    paths.add(details.getRelativePath().toString());
                                }
                            });
            Collections.sort(paths);
            manifest.files = paths;
        }

        if (getNotBeforeVersion().isPresent()) {
            manifest.notBeforeVersion = getNotBeforeVersion().get();
        }
        if (getNotFromVersion().isPresent()) {
            manifest.notFromVersion = getNotFromVersion().get();
        }
        return manifest;
    }

    private void addClassesAssignableTo(
            List<String> list, ClassInfoList addOnClasses, ClassInfo baseClass) {
        addOnClasses
                .getAssignableTo(baseClass)
                .filter(
                        ci ->
                                ci.isPublic()
                                        && !ci.isAbstract()
                                        && !ci.getDeclaredConstructorInfo()
                                                .filter(MethodInfo::isConstructor)
                                                .filter(MethodInfo::isPublic)
                                                .filter(mi -> mi.getParameterInfo().length == 0)
                                                .isEmpty())
                .stream()
                .map(ClassInfo::getName)
                .collect(() -> list, List::add, List::addAll);
    }

    private static String readContents(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
