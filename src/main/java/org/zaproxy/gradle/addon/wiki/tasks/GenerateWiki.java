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
package org.zaproxy.gradle.addon.wiki.tasks;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;
import io.github.classgraph.ScanResult;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.zaproxy.gradle.addon.wiki.internal.SourceFile;
import org.zaproxy.gradle.addon.wiki.internal.WikiGenerationException;
import org.zaproxy.gradle.addon.wiki.internal.WikiGenerator;
import org.zaproxy.gradle.addon.wiki.internal.WikiGeneratorUtils;
import org.zaproxy.gradle.addon.wiki.internal.WikiTocGenerator;

/** A task to generate the wiki files from the help of the add-on. */
public class GenerateWiki extends DefaultTask {

    private final RegularFileProperty javaHelpData;

    private final Property<String> contentsDir;

    private final Property<String> wikiFilesPrefix;

    private final Property<String> toc;
    private final Property<String> map;
    private final RegularFileProperty wikiToc;
    private final DirectoryProperty outputDir;

    public GenerateWiki() {
        ObjectFactory objects = getProject().getObjects();
        javaHelpData = objects.fileProperty();
        contentsDir = objects.property(String.class);
        wikiFilesPrefix = objects.property(String.class);
        toc = objects.property(String.class);
        map = objects.property(String.class);
        wikiToc = objects.fileProperty();
        outputDir = objects.directoryProperty();
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public RegularFileProperty getJavaHelpData() {
        return javaHelpData;
    }

    @Input
    public Property<String> getContentsDir() {
        return contentsDir;
    }

    @Input
    public Property<String> getWikiFilesPrefix() {
        return wikiFilesPrefix;
    }

    @Input
    public Property<String> getToc() {
        return toc;
    }

    @Input
    public Property<String> getMap() {
        return map;
    }

    @OutputFile
    public RegularFileProperty getWikiToc() {
        return wikiToc;
    }

    @OutputDirectory
    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    @TaskAction
    public void generate() {
        getProject().delete(outputDir.get().getAsFile());
        getProject().mkdir(outputDir.get().getAsFile());

        Path destDir = outputDir.get().getAsFile().toPath();

        URL tocUrl;
        URL mapUrl;
        Set<SourceFile> sourceFiles;
        try (ScanResult scanResult =
                new ClassGraph().overrideClasspath(javaHelpData.get().getAsFile()).scan()) {
            tocUrl = getUrl(scanResult, toc.get());
            mapUrl = getUrl(scanResult, map.get());
            sourceFiles = createSourceFiles(contentsDir.get(), scanResult);
        }

        WikiGenerator wikiGenerator =
                new WikiGenerator(sourceFiles, contentsDir.get(), wikiFilesPrefix.get());
        wikiGenerator.generateWikiPages(destDir);

        WikiTocGenerator wikiTocGenerator =
                new WikiTocGenerator(tocUrl, mapUrl, contentsDir.get(), wikiFilesPrefix.get());
        wikiTocGenerator.writeTo(wikiToc.get().getAsFile().toPath());
        wikiTocGenerator.createImages(destDir);
    }

    private static URL getUrl(ScanResult scanResult, String path) {
        String normalisedPath = WikiGeneratorUtils.normaliseFileSystemPath(path);
        ResourceList resources = scanResult.getResourcesWithPath(normalisedPath);
        if (resources.isEmpty()) {
            throw new WikiGenerationException("File not found in provided JAR: " + normalisedPath);
        }
        return resources.get(0).getURL();
    }

    private static Set<SourceFile> createSourceFiles(String contentsDir, ScanResult scanResult) {
        String normalisedContentsDir = WikiGeneratorUtils.normaliseFileSystemPath(contentsDir);
        Set<SourceFile> sourceFiles = new HashSet<>();
        for (Resource file :
                scanResult.getResourcesMatchingPattern(
                        Pattern.compile(Pattern.quote(normalisedContentsDir) + ".*\\.html"))) {
            sourceFiles.add(
                    new SourceFile(
                            file.getURL(),
                            file.getPath().substring(normalisedContentsDir.length() + 1)));
        }
        return sourceFiles;
    }
}
