/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2026 The ZAP Development Team
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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.io.TempDir;

public abstract class FunctionalTest {

    @TempDir protected Path projectDir;

    protected static void createFile(String content, Path file) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    protected void buildFile(String content) throws Exception {
        createFile(content, projectDir.resolve("build.gradle.kts"));
    }

    protected BuildResult build(String... arguments) throws Exception {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(arguments)
                .withPluginClasspath()
                .build();
    }

    protected BuildResult buildAndFail(String... arguments) throws Exception {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(arguments)
                .withPluginClasspath()
                .buildAndFail();
    }

    protected static void assertTaskSuccess(BuildResult result, String taskName) {
        assertTaskOutcome(result, taskName, TaskOutcome.SUCCESS);
    }

    protected static void assertTaskFailed(BuildResult result, String taskName) {
        assertTaskOutcome(result, taskName, TaskOutcome.FAILED);
    }

    private static void assertTaskOutcome(
            BuildResult result, String taskName, TaskOutcome outcome) {
        assertThat(result.task(taskName)).extracting(BuildTask::getOutcome).isEqualTo(outcome);
    }
}
