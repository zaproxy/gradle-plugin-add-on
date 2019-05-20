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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * A task that prepares the release of an add-on.
 *
 * <p>Replaces the Unreleased section and adds the release link to the changelog.
 *
 * <p>The release link can have tokens to refer to current and previous versions, which are replaced
 * when adding the link.
 */
public class PrepareAddOnRelease extends DefaultTask {

    public static final String CURRENT_VERSION_TOKEN = "@CURRENT_VERSION@";
    public static final String PREVIOUS_VERSION_TOKEN = "@PREVIOUS_VERSION@";

    private static final Pattern UNRELEASED_VERSION_PATTERN =
            Pattern.compile("^## \\[?Unreleased]?");
    private static final Pattern VERSION_SECTION_PATTERN = Pattern.compile("^## \\[?(.+?)]? -");
    private static final Pattern VERSION_LINK_PATTERN = Pattern.compile("^\\[.+]:");

    private final Property<String> currentVersion;
    private final Property<String> currentVersionToken;
    private final Property<String> previousVersionToken;
    private final Property<String> releaseLink;
    private final Property<String> releaseDate;
    private final RegularFileProperty changelog;
    private String previousVersion;

    public PrepareAddOnRelease() {
        ObjectFactory objects = getProject().getObjects();
        this.currentVersion = objects.property(String.class);
        this.currentVersionToken = objects.property(String.class).value(CURRENT_VERSION_TOKEN);
        this.previousVersionToken = objects.property(String.class).value(PREVIOUS_VERSION_TOKEN);
        this.releaseLink = objects.property(String.class);
        this.releaseDate = objects.property(String.class).value(LocalDate.now().toString());
        this.changelog = objects.fileProperty();

        setGroup("ZAP Add-On Misc");
        setDescription("Prepares the release of the add-on.");
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
    public Property<String> getPreviousVersionToken() {
        return previousVersionToken;
    }

    @Input
    public Property<String> getReleaseLink() {
        return releaseLink;
    }

    @Input
    public Property<String> getReleaseDate() {
        return releaseDate;
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public RegularFileProperty getChangelog() {
        return changelog;
    }

    @TaskAction
    public void prepare() throws IOException {
        Path changelogPath = changelog.getAsFile().get().toPath();
        Path updatedChangelog =
                getTemporaryDir().toPath().resolve("updated-" + changelogPath.getFileName());

        boolean replaceUnreleased = true;

        try (BufferedReader reader = Files.newBufferedReader(changelogPath);
                BufferedWriter writer = Files.newBufferedWriter(updatedChangelog)) {
            boolean lastLineEmpty = false;
            boolean insertLink = true;
            boolean extractPreviousVersion = releaseLink.get().contains(previousVersionToken.get());
            String line;
            while ((line = reader.readLine()) != null) {
                if (extractPreviousVersion) {
                    Matcher version = VERSION_SECTION_PATTERN.matcher(line);
                    if (version.find()) {
                        previousVersion = version.group(1).trim();
                        extractPreviousVersion = false;
                    }
                }
                if (insertLink && VERSION_LINK_PATTERN.matcher(line).find()) {
                    if (line.startsWith("[Unreleased]:")) {
                        line = buildReleaseLink();
                    } else {
                        writeReleaseLink(writer);
                    }
                    insertLink = false;
                } else if (replaceUnreleased && UNRELEASED_VERSION_PATTERN.matcher(line).find()) {
                    line = "## [" + currentVersion.get() + "] - " + releaseDate.get();
                    replaceUnreleased = false;
                }
                writer.write(line);
                writer.write("\n");
                lastLineEmpty = line.isEmpty();
            }

            if (insertLink) {
                if (!lastLineEmpty) {
                    writer.write("\n");
                }
                writeReleaseLink(writer);
            }
        }

        if (replaceUnreleased) {
            throw new InvalidUserDataException("Changelog does not have the unreleased section.");
        }

        Files.copy(updatedChangelog, changelogPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private String buildReleaseLink() {
        return "[" + currentVersion.get() + "]: " + replaceVersionTokens(releaseLink.get());
    }

    private void writeReleaseLink(Writer writer) throws IOException {
        writer.write(buildReleaseLink() + "\n");
    }

    private String replaceVersionTokens(String link) {
        String replacedLink = link.replace(currentVersionToken.get(), currentVersion.get());
        if (link.contains(previousVersionToken.get())) {
            if (previousVersion == null) {
                throw new InvalidUserDataException(
                        "The changelog does not contain a released version to add to the release link: "
                                + releaseLink.get());
            }
            replacedLink = replacedLink.replace(previousVersionToken.get(), previousVersion);
        }
        return replacedLink;
    }
}
