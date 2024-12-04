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

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughSubscriptExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Arrays;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/** A task that converts markdown to HTML. */
public class ConvertMarkdownToHtml extends DefaultTask {

    private final RegularFileProperty markdown;
    private final RegularFileProperty html;

    public ConvertMarkdownToHtml() {
        ObjectFactory objects = getProject().getObjects();
        markdown = objects.fileProperty();
        html = objects.fileProperty();
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public RegularFileProperty getMarkdown() {
        return markdown;
    }

    @OutputFile
    public RegularFileProperty getHtml() {
        return html;
    }

    @TaskAction
    public void convert() throws IOException {
        MutableDataSet options =
                new MutableDataSet()
                        .set(TablesExtension.COLUMN_SPANS, false)
                        .set(TablesExtension.APPEND_MISSING_COLUMNS, true)
                        .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
                        .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true)
                        .set(
                                Parser.EXTENSIONS,
                                Arrays.asList(
                                        StrikethroughSubscriptExtension.create(),
                                        TablesExtension.create(),
                                        TaskListExtension.create()));

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        String changes = Files.readString(markdown.get().getAsFile().toPath());
        try (Writer writer = Files.newBufferedWriter(html.get().getAsFile().toPath())) {
            renderer.render(parser.parse(changes), writer);
        }
    }
}
