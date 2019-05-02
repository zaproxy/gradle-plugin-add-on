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
package org.zaproxy.gradle.addon;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.zaproxy.gradle.addon.apigen.ApiClientGenExtension;
import org.zaproxy.gradle.addon.apigen.tasks.GenerateApiClientFiles;
import org.zaproxy.gradle.addon.internal.Constants;
import org.zaproxy.gradle.addon.jh.tasks.JavaHelpIndexer;
import org.zaproxy.gradle.addon.manifest.ManifestExtension;
import org.zaproxy.gradle.addon.manifest.tasks.GenerateManifestFile;
import org.zaproxy.gradle.addon.misc.CopyAddOn;
import org.zaproxy.gradle.addon.misc.DeployAddOn;
import org.zaproxy.gradle.addon.misc.InstallAddOn;
import org.zaproxy.gradle.addon.misc.UninstallAddOn;
import org.zaproxy.gradle.addon.wiki.WikiGenExtension;
import org.zaproxy.gradle.addon.wiki.tasks.GenerateWiki;

/** The plugin to help build ZAP add-ons. */
public class AddOnPlugin implements Plugin<Project> {

    /** The name of the extension to configure the ZAP add-on. */
    public static final String MAIN_EXTENSION_NAME = "zapAddOn";

    /**
     * The name of the extension to configure the add-on manifest.
     *
     * <p>Accessible through the {@value #MAIN_EXTENSION_NAME} extension.
     */
    public static final String MANIFEST_EXTENSION_NAME = "manifest";

    /**
     * The name of the extension to configure the generation of wiki files.
     *
     * <p>Accessible through the {@value #MAIN_EXTENSION_NAME} extension.
     */
    public static final String WIKI_GEN_EXTENSION_NAME = "wikiGen";

    /**
     * The name of the extension to configure the generation of API client files.
     *
     * <p>Accessible through the {@value #MAIN_EXTENSION_NAME} extension.
     */
    public static final String API_CLIENT_GEN_EXTENSION_NAME = "apiClientGen";

    /** The name of the ZAP configuration. */
    public static final String ZAP_CONFIGURATION_NAME = "zap";

    /** The name of the JavaHelp configuration. */
    public static final String JAVA_HELP_CONFIGURATION_NAME = "javahelp";

    /** The name of the task that assembles the add-on. */
    public static final String JAR_ZAP_ADD_ON_TASK_NAME = "jarZapAddOn";

    static final String JAR_ZAP_ADD_ON_TASK_DESC = "Assembles the ZAP add-on.";

    /**
     * The name of the task that copies the add-on to zaproxy project.
     *
     * @see org.zaproxy.gradle.addon.misc.CopyAddOn
     */
    public static final String COPY_ADD_ON_TASK_NAME = "copyZapAddOn";

    static final String COPY_ADD_ON_TASK_DESC =
            "Copies the add-on to zaproxy project (defaults to \"$rootDir/../zaproxy/src/plugin/\").";

    /**
     * The name of the task that deploys the add-on and its home files to ZAP home dir.
     *
     * @see org.zaproxy.gradle.addon.misc.DeployAddOn
     */
    public static final String DEPLOY_ADD_ON_TASK_NAME = "deployZapAddOn";

    static final String DEPLOY_ADD_ON_TASK_DESC =
            "Deploys the add-on and its home files to ZAP home dir.\n"
                    + "Defaults to dev home dir if not specified through the command line nor\n"
                    + "through the system property \"zap.home.dir\". The command line argument\n"
                    + "takes precedence over the system property.\n"
                    + "By default the existing home files are deleted before deploying the new\n"
                    + "files, to prevent stale files. This behaviour can be changed through the\n"
                    + "command line.";

    /**
     * The name of the task that uninstalls the add-on from ZAP, using the ZAP API.
     *
     * @see org.zaproxy.gradle.addon.misc.UninstallAddOn
     */
    public static final String UNINSTALL_ADD_ON_TASK_NAME = "uninstallZapAddOn";

    static final String UNINSTALL_ADD_ON_TASK_DESC =
            "Uninstalls the add-on from ZAP, listening on 8080 by default.";

    /**
     * The name of the task that installs the add-on into ZAP, using the ZAP API.
     *
     * @see org.zaproxy.gradle.addon.misc.InstallAddOn
     */
    public static final String INSTALL_ADD_ON_TASK_NAME = "installZapAddOn";

    static final String INSTALL_ADD_ON_TASK_DESC =
            "Installs the add-on into ZAP, listening on 8080 by default.";

    /**
     * The name of the task that generates the add-on manifest ({@code ZapAddOn.xml}).
     *
     * @see org.zaproxy.gradle.addon.manifest.tasks.GenerateManifestFile
     */
    public static final String GENERATE_MANIFEST_TASK_NAME = "generateZapAddOnManifest";

    static final String GENERATE_MANIFEST_TASK_DESC =
            "Generates the manifest (ZapAddOn.xml) for the ZAP add-on.";

    /**
     * The name of the task that generates the API client files for the ZAP add-on.
     *
     * @see org.zaproxy.gradle.addon.apigen.tasks.GenerateApiClientFiles
     */
    public static final String GENERATE_API_CLIENT_TASK_NAME = "generateZapApiClientFiles";

    static final String GENERATE_API_CLIENT_TASK_DESC =
            "Generates the API client files for the ZAP add-on.";

    private static final String ZAP_TASK_GROUP_NAME = "ZAP Add-On Misc";

    private static final String ZAP_GROUP_ARTIFACT = "org.zaproxy:zap:";

    private static final String JAVA_HELP_DEFAULT_DEPENDENCY = "javax.help:javahelp:2.0.05";

    private static final String MAIN_OUTPUT_DIR = "zapAddOn";

    @Override
    public void apply(Project project) {
        project.getPlugins()
                .withType(
                        JavaPlugin.class,
                        jp -> {
                            AddOnPluginExtension extension =
                                    project.getExtensions()
                                            .create(
                                                    MAIN_EXTENSION_NAME,
                                                    AddOnPluginExtension.class,
                                                    project);

                            ConfigurationContainer confs = project.getConfigurations();

                            NamedDomainObjectProvider<Configuration> zapConfig =
                                    confs.register(
                                            ZAP_CONFIGURATION_NAME,
                                            config -> {
                                                config.setVisible(false)
                                                        .setDescription(
                                                                "The ZAP artifact, automatically added to compileOnly and testImplementation.");

                                                config.defaultDependencies(
                                                        deps -> {
                                                            boolean depNotAdded = true;
                                                            Property<String> zapVersion =
                                                                    extension.getZapVersion();
                                                            if (zapVersion.isPresent()) {
                                                                String version = zapVersion.get();
                                                                if (!version.isEmpty()) {
                                                                    deps.add(
                                                                            project.getDependencies()
                                                                                    .create(
                                                                                            ZAP_GROUP_ARTIFACT
                                                                                                    + version));
                                                                    depNotAdded = false;
                                                                }
                                                            }

                                                            if (depNotAdded) {
                                                                project.getLogger()
                                                                        .warn(
                                                                                "Not adding ZAP dependency to {}, no ZAP version defined for the add-on.",
                                                                                project);
                                                            }
                                                        });
                                            });

                            confs.named(
                                    JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
                                    e -> e.extendsFrom(zapConfig.get()));
                            confs.named(
                                    JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME,
                                    e -> e.extendsFrom(zapConfig.get()));

                            DirectoryProperty zapAddOnBuildDir =
                                    project.getObjects()
                                            .directoryProperty()
                                            .convention(
                                                    project.getLayout()
                                                            .getBuildDirectory()
                                                            .dir(MAIN_OUTPUT_DIR));

                            setUpManifest(project, extension, zapAddOnBuildDir);
                            setUpAddOnFiles(project, extension);
                            setUpAddOn(project, extension, zapAddOnBuildDir);
                            setUpJavaHelp(project, extension, zapAddOnBuildDir);
                            setUpMiscTasks(project, extension);
                            setUpApiClientGen(project, extension);
                        });
    }

    private static void setUpManifest(
            Project project, AddOnPluginExtension extension, DirectoryProperty zapAddOnBuildDir) {
        ManifestExtension manifestExtension =
                ((ExtensionAware) extension)
                        .getExtensions()
                        .create(MANIFEST_EXTENSION_NAME, ManifestExtension.class, project);
        manifestExtension.getNotBeforeVersion().set(extension.getZapVersion());
        manifestExtension
                .getClasspath()
                .from(
                        project.getConvention()
                                .getPlugin(JavaPluginConvention.class)
                                .getSourceSets()
                                .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                                .getOutput()
                                .getClassesDirs());
        manifestExtension.getOutputDir().set(zapAddOnBuildDir);

        TaskProvider<GenerateManifestFile> generateTaskProvider =
                project.getTasks()
                        .register(
                                GENERATE_MANIFEST_TASK_NAME,
                                GenerateManifestFile.class,
                                t -> {
                                    t.setDescription(GENERATE_MANIFEST_TASK_DESC);
                                    t.setGroup(LifecycleBasePlugin.BUILD_GROUP);

                                    t.getAddOnName().set(extension.getAddOnName());
                                    t.getVersion().set(extension.getAddOnVersion());
                                    t.getSemVer().set(manifestExtension.getSemVer());
                                    t.getStatus().set(extension.getAddOnStatus());
                                    t.getAddOnDescription().set(manifestExtension.getDescription());
                                    t.getAuthor().set(manifestExtension.getAuthor());
                                    t.getUrl().set(manifestExtension.getUrl());
                                    t.getChanges().set(manifestExtension.getChanges());
                                    t.getChangesFile().set(manifestExtension.getChangesFile());
                                    t.getDependencies().set(manifestExtension.getDependencies());
                                    t.getBundle().set(manifestExtension.getBundle());
                                    t.getHelpSet().set(manifestExtension.getHelpSet());
                                    t.getClassnames().set(manifestExtension.getClassnames());

                                    t.setExtensions(manifestExtension.getAddOnExtensions());
                                    t.setAscanrules(manifestExtension.getAscanrules());
                                    t.setPscanrules(manifestExtension.getPscanrules());

                                    t.getFiles().from(manifestExtension.getFiles());
                                    t.getNotBeforeVersion()
                                            .set(manifestExtension.getNotBeforeVersion());
                                    t.getNotFromVersion()
                                            .set(manifestExtension.getNotFromVersion());
                                    t.getClasspath().from(manifestExtension.getClasspath());

                                    t.getOutputDir().set(manifestExtension.getOutputDir());
                                });
        project.getTasks()
                .named(JavaPlugin.JAR_TASK_NAME, Jar.class, jar -> jar.from(generateTaskProvider));
    }

    private static void setUpAddOnFiles(Project project, AddOnPluginExtension extension) {
        DirectoryProperty srcDir = project.getObjects().directoryProperty();
        srcDir.set(project.file("src/main/zapHomeFiles"));

        ((ExtensionAware) extension)
                .getExtensions()
                .getByType(ManifestExtension.class)
                .getFiles()
                .from(srcDir);

        JavaPluginConvention javaConvention =
                project.getConvention().getPlugin(JavaPluginConvention.class);
        javaConvention
                .getSourceSets()
                .named(
                        SourceSet.MAIN_SOURCE_SET_NAME,
                        sourceSet -> sourceSet.getResources().srcDir(srcDir));
    }

    private static void setUpAddOn(
            Project project, AddOnPluginExtension extension, DirectoryProperty zapAddOnBuildDir) {
        TaskProvider<Jar> jarAddOn =
                project.getTasks()
                        .register(
                                JAR_ZAP_ADD_ON_TASK_NAME,
                                Jar.class,
                                t -> {
                                    t.setDescription(JAR_ZAP_ADD_ON_TASK_DESC);
                                    t.setGroup(LifecycleBasePlugin.BUILD_GROUP);

                                    t.getArchiveBaseName().set(extension.getAddOnId());
                                    t.getArchiveAppendix()
                                            .set(
                                                    extension
                                                            .getAddOnStatus()
                                                            .map(AddOnStatus::toString));
                                    t.getArchiveVersion().set(extension.getAddOnVersion());
                                    t.getArchiveExtension().set(Constants.ADD_ON_FILE_EXTENSION);
                                    t.getDestinationDirectory().set(zapAddOnBuildDir.dir("bin"));

                                    t.getOutputs()
                                            .upToDateWhen(
                                                    task -> {
                                                        Path dir =
                                                                t.getDestinationDirectory()
                                                                        .getAsFile()
                                                                        .get()
                                                                        .toPath();
                                                        if (!Files.exists(dir)) {
                                                            return true;
                                                        }
                                                        try (Stream<Path> stream =
                                                                Files.find(
                                                                        dir,
                                                                        1,
                                                                        (p, a) ->
                                                                                p.getFileName()
                                                                                        .toString()
                                                                                        .endsWith(
                                                                                                Constants
                                                                                                        .ADD_ON_FILE_EXTENSION))) {
                                                            return stream.count() == 1;
                                                        } catch (IOException e) {
                                                            throw new UncheckedIOException(e);
                                                        }
                                                    });
                                    // Do not use a lambda, not supported by up-to-date checks.
                                    t.doFirst(
                                            new Action<Task>() {

                                                @Override
                                                public void execute(Task task) {
                                                    project.delete(
                                                            project.fileTree(
                                                                            t
                                                                                    .getDestinationDirectory())
                                                                    .getFiles());
                                                }
                                            });

                                    t.setPreserveFileTimestamps(false);
                                    t.setReproducibleFileOrder(true);

                                    Jar jar =
                                            project.getTasks()
                                                    .named(JavaPlugin.JAR_TASK_NAME, Jar.class)
                                                    .get();
                                    t.with(jar);
                                    t.getManifest().from(jar.getManifest());

                                    NamedDomainObjectProvider<Configuration> runtimeClasspath =
                                            project.getConfigurations()
                                                    .named(
                                                            JavaPlugin
                                                                    .RUNTIME_CLASSPATH_CONFIGURATION_NAME);
                                    t.dependsOn(runtimeClasspath);
                                    t.from(
                                                    project.provider(
                                                            () ->
                                                                    runtimeClasspath.get()
                                                                            .getFiles().stream()
                                                                            .map(
                                                                                    e ->
                                                                                            e
                                                                                                            .isDirectory()
                                                                                                    ? e
                                                                                                    : project
                                                                                                            .zipTree(
                                                                                                                    e))
                                                                            .collect(
                                                                                    Collectors
                                                                                            .toList())))
                                            .exclude(
                                                    "META-INF/*.SF",
                                                    "META-INF/*.DSA",
                                                    "META-INF/*.RSA");
                                });
        project.getTasks()
                .named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME, t -> t.dependsOn(jarAddOn));
    }

    private static void setUpJavaHelp(
            Project project, AddOnPluginExtension extension, DirectoryProperty zapAddOnBuildDir) {
        NamedDomainObjectProvider<Configuration> javaHelpConfig =
                project.getConfigurations()
                        .register(
                                JAVA_HELP_CONFIGURATION_NAME,
                                conf ->
                                        conf.setVisible(false)
                                                .setDescription(
                                                        "The dependencies for JavaHelp related tasks.")
                                                .defaultDependencies(
                                                        deps ->
                                                                deps.add(
                                                                        project.getDependencies()
                                                                                .create(
                                                                                        JAVA_HELP_DEFAULT_DEPENDENCY))));

        project.getTasks()
                .withType(JavaHelpIndexer.class)
                .configureEach(jhi -> jhi.getClasspath().from(javaHelpConfig));

        DirectoryProperty srcDir = project.getObjects().directoryProperty();
        File srcDirFile = project.file("src/main/javahelp");
        srcDir.set(srcDirFile);

        TaskProvider<Jar> addOnTask =
                project.getTasks().named(JAR_ZAP_ADD_ON_TASK_NAME, Jar.class, t -> t.from(srcDir));

        Directory mainJhiDestDir = zapAddOnBuildDir.dir("jhindexes").get();

        Set<File> helpsets =
                project.fileTree(srcDir)
                        .filter(e -> e.getName().endsWith(Constants.HELPSET_FILE_EXTENSION))
                        .getFiles();
        helpsets.forEach(
                helpset -> {
                    String name = helpset.getParentFile().getName();
                    Directory dir = mainJhiDestDir.dir(name);

                    TaskProvider<JavaHelpIndexer> jhi =
                            project.getTasks()
                                    .register(
                                            "jhindexer-" + name,
                                            JavaHelpIndexer.class,
                                            t -> {
                                                t.setDescription(
                                                        "Generates the JavaHelp indexes for "
                                                                + name
                                                                + " dir.");
                                                t.setGroup(LifecycleBasePlugin.BUILD_GROUP);

                                                t.getHelpset().set(helpset);
                                                t.getOutputPrefix()
                                                        .set(
                                                                srcDir.getAsFile()
                                                                        .get()
                                                                        .toPath()
                                                                        .relativize(
                                                                                helpset.toPath()
                                                                                        .getParent())
                                                                        .toString());
                                                t.setSource(helpset.getParentFile());
                                                t.getDestinationDir().set(dir);
                                            });
                    addOnTask.configure(t -> t.from(jhi));
                });

        Directory mainWikiDestDir = zapAddOnBuildDir.dir("wiki").get();

        TaskProvider<Jar> jarWikiHelpTask =
                project.getTasks()
                        .register(
                                "jarJavaHelpDataForWiki",
                                Jar.class,
                                t -> {
                                    t.setDescription(
                                            "Assembles the JAR used for the wiki generation from the JavaHelp data.");
                                    t.setGroup(LifecycleBasePlugin.BUILD_GROUP);

                                    t.from(
                                            srcDir,
                                            project.getConvention()
                                                    .getPlugin(JavaPluginConvention.class)
                                                    .getSourceSets()
                                                    .named(SourceSet.MAIN_SOURCE_SET_NAME)
                                                    .map(SourceSet::getResources));
                                    t.getArchiveBaseName().set("javaHelpData");
                                    t.getDestinationDirectory().set(mainWikiDestDir);
                                    t.getArchiveVersion().set("");
                                    t.setPreserveFileTimestamps(false);
                                    t.setReproducibleFileOrder(true);
                                });

        WikiGenExtension wikiGenExtension =
                ((ExtensionAware) extension)
                        .getExtensions()
                        .create(WIKI_GEN_EXTENSION_NAME, WikiGenExtension.class, project);

        helpsets.forEach(
                helpset -> {
                    File helpDir = helpset.getParentFile();
                    String name = helpDir.getName();

                    Directory wikiDir = mainWikiDestDir.dir(name);
                    TaskProvider<GenerateWiki> wikiTask =
                            project.getTasks()
                                    .register(
                                            "generateWiki-" + name,
                                            GenerateWiki.class,
                                            t -> {
                                                t.setDescription(
                                                        "Generates the wiki files from "
                                                                + name
                                                                + " dir.");
                                                t.setGroup(LifecycleBasePlugin.BUILD_GROUP);

                                                t.getJavaHelpData()
                                                        .set(
                                                                jarWikiHelpTask.flatMap(
                                                                        Jar::getArchiveFile));

                                                t.getContentsDir()
                                                        .set(
                                                                srcDirFile
                                                                        .toPath()
                                                                        .relativize(
                                                                                helpDir.toPath()
                                                                                        .resolve(
                                                                                                "contents"))
                                                                        .toString());

                                                t.getToc()
                                                        .set(
                                                                srcDirFile
                                                                        .toPath()
                                                                        .relativize(
                                                                                helpDir.toPath()
                                                                                        .resolve(
                                                                                                "toc.xml"))
                                                                        .toString());
                                                t.getMap()
                                                        .set(
                                                                srcDirFile
                                                                        .toPath()
                                                                        .relativize(
                                                                                helpDir.toPath()
                                                                                        .resolve(
                                                                                                "map.jhm"))
                                                                        .toString());
                                                t.getWikiFilesPrefix()
                                                        .set(wikiGenExtension.getWikiFilesPrefix());

                                                t.getWikiToc().set(wikiDir.file("_Sidebar.md"));
                                                t.getOutputDir().set(wikiDir.dir("contents"));
                                            });

                    project.getTasks()
                            .register(
                                    "copyWiki-" + name,
                                    Copy.class,
                                    t -> {
                                        t.setDescription(
                                                "Copies the wiki files of "
                                                        + name
                                                        + " dir to the wiki dir.");
                                        t.setGroup(LifecycleBasePlugin.BUILD_GROUP);
                                        t.from(wikiTask.map(GenerateWiki::getOutputDir));
                                        t.into(wikiGenExtension.getWikiDir());
                                    });
                });
    }

    private static void setUpMiscTasks(Project project, AddOnPluginExtension extension) {
        project.getTasks()
                .withType(CopyAddOn.class, t -> t.getAddOnId().set(extension.getAddOnId()));

        TaskProvider<Jar> jarZapAddOn =
                project.getTasks().named(JAR_ZAP_ADD_ON_TASK_NAME, Jar.class);
        Provider<RegularFile> jarFile = jarZapAddOn.flatMap(Jar::getArchiveFile);

        project.getTasks()
                .register(
                        COPY_ADD_ON_TASK_NAME,
                        CopyAddOn.class,
                        t -> {
                            t.setDescription(COPY_ADD_ON_TASK_DESC);
                            t.setGroup(ZAP_TASK_GROUP_NAME);
                        });

        project.getTasks()
                .register(
                        DEPLOY_ADD_ON_TASK_NAME,
                        DeployAddOn.class,
                        t -> {
                            t.setDescription(DEPLOY_ADD_ON_TASK_DESC);
                            t.setGroup(ZAP_TASK_GROUP_NAME);

                            t.getAddOn().set(jarFile);
                            t.getFiles()
                                    .from(
                                            ((ExtensionAware) extension)
                                                    .getExtensions()
                                                    .getByType(ManifestExtension.class)
                                                    .getFiles());
                        });

        TaskProvider<UninstallAddOn> uninstallAddOn =
                project.getTasks()
                        .register(
                                UNINSTALL_ADD_ON_TASK_NAME,
                                UninstallAddOn.class,
                                t -> {
                                    t.setDescription(UNINSTALL_ADD_ON_TASK_DESC);
                                    t.setGroup(ZAP_TASK_GROUP_NAME);
                                    t.getAddOnId().set(extension.getAddOnId());
                                });
        jarZapAddOn.configure(t -> t.mustRunAfter(uninstallAddOn));

        project.getTasks()
                .register(
                        INSTALL_ADD_ON_TASK_NAME,
                        InstallAddOn.class,
                        t -> {
                            t.setDescription(INSTALL_ADD_ON_TASK_DESC);
                            t.setGroup(ZAP_TASK_GROUP_NAME);

                            t.dependsOn(uninstallAddOn);
                            t.getAddOn().set(jarFile);
                        });
    }

    private static void setUpApiClientGen(Project project, AddOnPluginExtension extension) {
        ApiClientGenExtension apiClientGenExtension =
                ((ExtensionAware) extension)
                        .getExtensions()
                        .create(
                                API_CLIENT_GEN_EXTENSION_NAME,
                                ApiClientGenExtension.class,
                                project);
        apiClientGenExtension
                .getClasspath()
                .from(
                        project.getConfigurations()
                                .named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME))
                .from(project.getTasks().named(JavaPlugin.JAR_TASK_NAME));

        project.getTasks()
                .register(
                        GENERATE_API_CLIENT_TASK_NAME,
                        GenerateApiClientFiles.class,
                        t -> {
                            t.setDescription(GENERATE_API_CLIENT_TASK_DESC);
                            t.setGroup(LifecycleBasePlugin.BUILD_GROUP);

                            t.getApi().set(apiClientGenExtension.getApi());
                            t.getOptions().set(apiClientGenExtension.getOptions());
                            t.getMessages().set(apiClientGenExtension.getMessages());
                            t.getBaseDir().set(apiClientGenExtension.getBaseDir());
                            t.getClasspath().setFrom(apiClientGenExtension.getClasspath());
                        });
    }
}
