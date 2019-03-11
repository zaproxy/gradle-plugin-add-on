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

import com.overzealous.remark.Options;
import com.overzealous.remark.Remark;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class WikiGenerator {

    private static final Remark MARKDOWN_CONVERTER;

    static {
        Options options = Options.github();
        options.tables = Options.Tables.LEAVE_AS_HTML;
        options.preserveRelativeLinks = true;
        MARKDOWN_CONVERTER = new Remark(options);
    }

    private static final String ABSOLUTE_SCHEME = "//";
    private static final String HTTP_SCHEME = "http://";
    private static final String HTTPS_SCHEME = "https://";
    private static final String MAILTO_SCHEME = "mailto:";

    private static final String MARKDOWN_EXTENSION = ".md";

    private final Set<SourceFile> srcFiles;
    private final String contentsDir;
    private final String filenamePrefix;
    private final Set<SourceImage> images;

    public WikiGenerator(Set<SourceFile> srcFiles, String contentsDir, String filenamePrefix) {
        this.srcFiles = srcFiles;
        this.contentsDir = contentsDir;
        this.filenamePrefix = filenamePrefix;
        this.images = new HashSet<>();
    }

    public WikiGenerationResults generateWikiPages(Path destDir) {
        SortedMap<String, URL> generatedFiles = new TreeMap<>();
        SortedSet<String> wikiLinks = new TreeSet<>();
        SortedSet<Path> generatedWikiFiles = new TreeSet<>();

        LinkConversionHandler linksHandler = new LinkConversionHandler(wikiLinks);
        for (SourceFile srcFile : srcFiles) {
            String wikiFileName =
                    WikiGeneratorUtils.createWikiFileName(
                            filenamePrefix, srcFile.getRelativePath(), "");
            URL path = generatedFiles.get(wikiFileName);
            if (path != null) {
                throw new WikiGenerationException(
                        "Duplicated output file name ["
                                + wikiFileName
                                + "] for ["
                                + path
                                + "] and ["
                                + srcFile.getPath()
                                + "]");
            }
            generatedFiles.put(wikiFileName, srcFile.getPath());

            Path destFile = destDir.resolve(wikiFileName + MARKDOWN_EXTENSION);

            linksHandler.setCurrentFile(srcFile.getPath());
            createWikiPage(srcFile, destFile, linksHandler);

            generatedWikiFiles.add(destFile);
        }

        Path imagesDir = destDir.resolve("images");
        for (SourceImage image : images) {
            Path imageFile = imagesDir.resolve(image.getPath());
            try {
                Files.createDirectories(imageFile.getParent());
            } catch (IOException e) {
                throw new WikiGenerationException(
                        "Failed to create the directory for image: " + imageFile, e);
            }

            try (InputStream inputStream = new BufferedInputStream(image.getUrl().openStream())) {
                Files.copy(inputStream, imageFile);
            } catch (IOException e) {
                throw new WikiGenerationException(
                        "Failed to copy image from ["
                                + image.getPath()
                                + "] to ["
                                + imageFile
                                + "]",
                        e);
            }
        }

        if (!generatedFiles.isEmpty()) {
            wikiLinks.removeAll(generatedFiles.keySet());
            if (!wikiLinks.isEmpty()) {
                System.err.println(
                        "The following wiki links that do not have corresponding wiki page: "
                                + wikiLinks.toString());
            }
        }

        return new WikiGenerationResults(generatedFiles.keySet(), generatedWikiFiles);
    }

    public static final class WikiGenerationResults {
        private final SortedSet<String> wikiFileNames;
        private final SortedSet<Path> generatedFiles;

        private WikiGenerationResults(Set<String> wikiFileNames, SortedSet<Path> generatedFiles) {
            this.wikiFileNames = Collections.unmodifiableSortedSet(new TreeSet<>(wikiFileNames));
            this.generatedFiles = Collections.unmodifiableSortedSet(new TreeSet<>(generatedFiles));
        }

        public SortedSet<String> getWikiFileNames() {
            return wikiFileNames;
        }

        public SortedSet<Path> getGeneratedFiles() {
            return generatedFiles;
        }
    }

    private static void createWikiPage(
            SourceFile sourceFile, Path destWikiFile, LinkConversionHandler linkConversionHandler) {
        try (Writer writer = Files.newBufferedWriter(destWikiFile, StandardCharsets.UTF_8);
                InputStream is = sourceFile.getPath().openStream()) {
            Document doc = Jsoup.parse(is, "UTF-8", "http://example.com");
            convertLinks(doc, linkConversionHandler);

            MARKDOWN_CONVERTER.withWriter(writer).convert(doc);
        } catch (IOException e) {
            throw new WikiGenerationException(
                    "Failed to convert file: " + sourceFile.getRelativePath(), e);
        }
    }

    private static void convertLinks(Document doc, LinkConversionHandler linksHandler) {
        for (Element a : doc.getElementsByTag("a")) {
            String href = a.attr("href");
            if (href != null && !href.isEmpty()) {
                a.attr("href", linksHandler.convertHyperLink(href));
            }
        }

        for (Element a : doc.getElementsByTag("img")) {
            String src = a.attr("src");
            if (src != null && !src.isEmpty()) {
                a.attr("src", linksHandler.convertImg(src));
            }
        }
    }

    private class LinkConversionHandler {

        private final SortedSet<String> wikiLinks;
        private URL currentFile;

        public LinkConversionHandler(SortedSet<String> wikiLinks) {
            this.wikiLinks = wikiLinks;
        }

        public void setCurrentFile(URL currentFile) {
            this.currentFile = currentFile;
        }

        public String convertHyperLink(String href) {
            if (isExternalLink(href)) {
                return href;
            }

            String normalisedHref = href;
            String anchor = null;
            int idx = href.indexOf('#');
            if (idx != -1) {
                anchor = href.substring(idx);
                normalisedHref = href.substring(0, idx);
                if (normalisedHref.isEmpty()) {
                    normalisedHref = currentFile.toString();
                }
            }

            String path =
                    WikiGeneratorUtils.normalisedPath(contentsDir, currentFile, normalisedHref);
            String wikiLink = WikiGeneratorUtils.createWikiFileName(filenamePrefix, path, "");
            wikiLinks.add(wikiLink);

            if (anchor != null) {
                wikiLink += anchor;
            }

            return wikiLink;
        }

        public String convertImg(String src) {
            if (isExternalLink(src)) {
                return src;
            }

            URL url = WikiGeneratorUtils.createUrlFor(currentFile, src);
            String path = WikiGeneratorUtils.normalisedImagePath(contentsDir, url);
            images.add(new SourceImage(url, path));

            return "images/" + path;
        }

        private boolean isExternalLink(String href) {
            return StringUtils.startsWithIgnoreCase(href, HTTP_SCHEME)
                    || StringUtils.startsWithIgnoreCase(href, HTTPS_SCHEME)
                    || StringUtils.startsWithIgnoreCase(href, ABSOLUTE_SCHEME)
                    || StringUtils.startsWithIgnoreCase(href, MAILTO_SCHEME);
        }
    }

    private static class SourceImage {
        private final URL url;
        private final String path;

        public SourceImage(URL url, String path) {
            this.url = url;
            this.path = path;
        }

        public URL getUrl() {
            return url;
        }

        public String getPath() {
            return path;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(path);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            return path.equals(((SourceImage) obj).path);
        }
    }
}
