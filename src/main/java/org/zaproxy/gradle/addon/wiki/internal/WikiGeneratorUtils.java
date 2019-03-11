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
package org.zaproxy.gradle.addon.wiki.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public final class WikiGeneratorUtils {

    private static final Pattern HTML_EXTENSION =
            Pattern.compile("\\.html$", Pattern.CASE_INSENSITIVE);

    private WikiGeneratorUtils() {}

    public static String createWikiFileName(String filenamePrefix, String path) {
        return createWikiFileName(filenamePrefix, path, "");
    }

    public static String createWikiFileName(
            String filenamePrefix, String path, String newFileExtension) {
        StringBuilder strBuilderFileName = new StringBuilder();
        strBuilderFileName.append(filenamePrefix);
        String[] segments = path.split("/", -1);
        for (int i = 0; i < segments.length - 1; i++) {
            strBuilderFileName.append(StringUtils.capitalize(segments[i]));
        }
        String fileName = StringUtils.capitalize(segments[segments.length - 1]);
        strBuilderFileName.append(HTML_EXTENSION.matcher(fileName).replaceFirst(newFileExtension));

        return strBuilderFileName.toString();
    }

    public static URL createUrlFor(URL file, String path) {
        try {
            return new URL(file, path);
        } catch (MalformedURLException e) {
            throw new WikiGenerationException(
                    "Failed to create the URL with " + file + " and " + path, e);
        }
    }

    public static String normalisedPath(String baseDir, URL file, String path) {
        return normalisedPath(baseDir, createUrlFor(file, path));
    }

    public static String normalisedPath(String baseDir, URL url) {
        return normalisePath(baseDir, extractPath(url));
    }

    private static String normalisePath(String baseDir, String path) {
        if (!path.startsWith(baseDir)) {
            throw new WikiGenerationException("Path " + path + " not under base dir " + baseDir);
        }
        return path.substring(baseDir.length() + 1);
    }

    private static String extractPath(URL url) {
        String fileString = url.toString();
        return fileString.substring(fileString.indexOf("!/") + 2);
    }

    public static String normalisedImagePath(String baseDir, URL url) {
        String path = extractPath(url);
        if (path.startsWith(baseDir)) {
            path = normalisePath(baseDir, path);
            if (path.startsWith("images")) {
                path = path.substring("images".length() + 1);
            }
        } else {
            path = path.substring(path.lastIndexOf('/') + 1);
        }
        return path;
    }
}
