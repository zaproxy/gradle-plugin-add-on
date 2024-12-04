/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2021 The ZAP Development Team
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
package org.zaproxy.gradle.addon.internal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.io.File;
import java.io.IOException;
import org.zaproxy.gradle.addon.internal.BuildException;

/** The release state computed from {@code gradle.properties} files. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(value = Include.NON_EMPTY)
public class ReleaseState {

    private static final ReleaseState NOT_NEW_RELEASE =
            new ReleaseState() {
                @Override
                public boolean isNewRelease() {
                    return false;
                }
            };

    private String previousVersion;

    private String currentVersion;

    private boolean previousRelease;

    private boolean currentRelease;

    public ReleaseState() {}

    @JsonIgnore
    public boolean isNewRelease() {
        return currentRelease && previousRelease != currentRelease;
    }

    public String getPreviousVersion() {
        return previousVersion;
    }

    public void setPreviousVersion(String previousVersion) {
        this.previousVersion = previousVersion;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public boolean isPreviousRelease() {
        return previousRelease;
    }

    public void setPreviousRelease(boolean previousRelease) {
        this.previousRelease = previousRelease;
    }

    public boolean isCurrentRelease() {
        return currentRelease;
    }

    public void setCurrentRelease(boolean currentRelease) {
        this.currentRelease = currentRelease;
    }

    /**
     * Writes this {@code ReleaseState} to the given file.
     *
     * @param file the file to write the release state.
     * @throws BuildException if an error occurred while writing the release state.
     */
    public void write(File file) {
        try {
            new ObjectMapper().writeValue(file, this);
        } catch (IOException e) {
            throw new BuildException("Failed to write the release state: " + e.getMessage(), e);
        }
    }

    /**
     * Reads a {@code ReleaseState} from the given file.
     *
     * @param file the file with the release state.
     * @return a new {@code ReleaseState} with the contents from the file.
     * @throws BuildException if an error occurred while reading the release state.
     */
    public static ReleaseState read(File file) {
        try {
            return new ObjectMapper().readValue(file, ReleaseState.class);
        } catch (IOException e) {
            throw new BuildException("Failed to read the release state: " + e.getMessage(), e);
        }
    }

    /**
     * Convenience method that reads a {@code ReleaseState} for the given project.
     *
     * <p>If the file does not exists it returns a "not a new release" state.
     *
     * @param projectInfo the info of the project.
     * @return a new {@code ReleaseState} with the contents from the file, or a "not a new release"
     *     state.
     * @throws BuildException if an error occurred while reading the release state.
     */
    public static ReleaseState read(ProjectInfo projectInfo) {
        File file = projectInfo.getOutputFile().getAsFile().get();
        if (!file.exists()) {
            return NOT_NEW_RELEASE;
        }
        return read(file);
    }
}
