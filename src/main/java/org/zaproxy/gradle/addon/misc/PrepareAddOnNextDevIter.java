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
package org.zaproxy.gradle.addon.misc;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * A task that prepares the next development iteration of an add-on.
 *
 * <p>Adds the Unreleased section and (optionally) the unreleased link to the changelog and bumps
 * the version in the build file.
 *
 * <p>The unreleased link can have a token to refer to current version, which is replaced when
 * adding the link.
 */
public class PrepareAddOnNextDevIter extends DefaultTask {

    public static final String CURRENT_VERSION_TOKEN = "@CURRENT_VERSION@";

    private static final String UNRELEASED_SECTION = "## Unreleased";
    private static final String UNRELEASED_SECTION_LINK = "## [Unreleased]";
    private static final Pattern VERSION_SECTION_PATTERN = Pattern.compile("^## \\[?.+]?.*");
    private static final Pattern VERSION_LINK_PATTERN = Pattern.compile("^\\[.+]:");

    private final Property<String> currentVersion;
    private final Property<String> currentVersionToken;
    private final Property<String> unreleasedLink;
    private final RegularFileProperty buildFile;
    private final RegularFileProperty changelog;

    public PrepareAddOnNextDevIter() {
        ObjectFactory objects = getProject().getObjects();
        this.currentVersion = objects.property(String.class);
        this.currentVersionToken = objects.property(String.class).value(CURRENT_VERSION_TOKEN);
        this.unreleasedLink = objects.property(String.class);
        this.buildFile = objects.fileProperty();
        this.changelog = objects.fileProperty();

        setGroup("ZAP Add-On Misc");
        setDescription("Prepares the next development iteration of the add-on.");
    }

    @Input
    public Property<String> getCurrentVersion() {
        return currentVersion;
    }

    @Input
    public Property<String> getCurrentVersionToken() {
        return currentVersionToken;
    }

    @Input
    @Optional
    public Property<String> getUnreleasedLink() {
        return unreleasedLink;
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public RegularFileProperty getBuildFile() {
        return buildFile;
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public RegularFileProperty getChangelog() {
        return changelog;
    }

    @TaskAction
    public void prepare() throws IOException {
        Path updatedChangelog = updateChangelog();
        Path updatedBuildFile = updateBuildFile();

        Files.copy(
                updatedChangelog,
                changelog.getAsFile().get().toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(
                updatedBuildFile,
                buildFile.getAsFile().get().toPath(),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private Path updateChangelog() throws IOException {
        Path changelogPath = changelog.getAsFile().get().toPath();
        Path updatedChangelog =
                getTemporaryDir().toPath().resolve("updated-" + changelogPath.getFileName());

        boolean insertUnreleased = true;
        boolean insertUnreleasedLink = unreleasedLink.isPresent();

        try (BufferedReader reader = Files.newBufferedReader(changelogPath);
                BufferedWriter writer = Files.newBufferedWriter(updatedChangelog)) {
            boolean lastLineEmpty = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (insertUnreleased) {
                    if (line.startsWith(UNRELEASED_SECTION)
                            || line.startsWith(UNRELEASED_SECTION_LINK)) {
                        throw new InvalidUserDataException(
                                "The changelog already contains the unreleased section.");
                    }

                    if (VERSION_SECTION_PATTERN.matcher(line).find()) {
                        writer.write(
                                insertUnreleasedLink
                                        ? UNRELEASED_SECTION_LINK
                                        : UNRELEASED_SECTION);
                        writer.write("\n\n\n");
                        insertUnreleased = false;
                    }
                }
                if (insertUnreleasedLink && VERSION_LINK_PATTERN.matcher(line).find()) {
                    writeUnreleaseLink(writer);
                    insertUnreleasedLink = false;
                }

                writer.write(line);
                writer.write("\n");
                lastLineEmpty = line.isEmpty();
            }

            if (insertUnreleasedLink) {
                if (!lastLineEmpty) {
                    writer.write("\n");
                }
                writeUnreleaseLink(writer);
            }
        }

        if (insertUnreleased) {
            throw new InvalidUserDataException(
                    "Failed to insert the unreleased section, no version section found.");
        }

        return updatedChangelog;
    }

    private Path updateBuildFile() throws IOException {
        Path buildFilePath = buildFile.getAsFile().get().toPath();
        Path updatedBuildFile =
                getTemporaryDir().toPath().resolve("updated-" + buildFilePath.getFileName());

        String currentVersionLine = versionLine(currentVersion.get());
        String newVersion = bumpVersion(currentVersion.get());

        boolean updateVersion = true;
        try (BufferedReader reader = Files.newBufferedReader(buildFilePath);
                BufferedWriter writer = Files.newBufferedWriter(updatedBuildFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (updateVersion && currentVersionLine.equals(line)) {
                    line = versionLine(newVersion);
                    updateVersion = false;
                }
                writer.write(line);
                writer.write("\n");
            }
        }

        if (updateVersion) {
            throw new InvalidUserDataException(
                    "Failed to update the version, current version line not found: "
                            + currentVersionLine);
        }

        return updatedBuildFile;
    }

    private static String versionLine(String version) {
        return "version = \"" + version + "\"";
    }

    private static String bumpVersion(String version) {
        try {
            int currentVersion = Integer.parseInt(version);
            return Integer.toString(++currentVersion);
        } catch (NumberFormatException e) {
            // Ignore, not an integer version.
        }

        try {
            return Version.valueOf(version).incrementMinorVersion().toString();
        } catch (IllegalArgumentException | ParseException e) {
            throw new InvalidUserDataException(
                    "Failed to parse the current version: " + version, e);
        }
    }

    private void writeUnreleaseLink(Writer writer) throws IOException {
        String link = unreleasedLink.get().replace(currentVersionToken.get(), currentVersion.get());
        writer.write("[Unreleased]: " + link + "\n");
    }
}
