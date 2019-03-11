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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class WikiTocGenerator {

    private static final Pattern IMAGES_PATTERN = Pattern.compile("(?i).*\\.(png|jpg)$");

    private final String contentsDir;
    private final URL toc;
    private final URL map;
    private final String outputFilenamePrefix;
    private final Map<String, MapTarget> targetsMap;
    private final List<TocItem> tocItems;

    public WikiTocGenerator(URL toc, URL map, String contentsDir, String outputFilenamePrefix) {
        this.contentsDir = contentsDir;
        this.toc = toc;
        this.map = map;
        this.outputFilenamePrefix = outputFilenamePrefix;
        this.targetsMap = createTargetsMap();
        this.tocItems = createTocItems();
    }

    private Map<String, MapTarget> createTargetsMap() {
        Element rootMapElement = null;
        NodeList childNodes = createDocument(map).getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE
                    && "map".equals(childNode.getNodeName())) {
                rootMapElement = (Element) childNode;
                break;
            }
        }

        if (rootMapElement == null) {
            return Collections.emptyMap();
        }

        Map<String, MapTarget> targetToFile = new HashMap<>();
        childNodes = rootMapElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if ("mapID".equals(childNode.getNodeName())) {
                Element childTocItemElement = (Element) childNode;
                Attr attrTarget = childTocItemElement.getAttributeNode("target");
                if (attrTarget == null) {
                    throw new WikiGenerationException(
                            "MapID element with no attribute \"target\".");
                }
                String target = attrTarget.getValue();
                if (target.isEmpty()) {
                    throw new WikiGenerationException(
                            "MapID element with empty attribute \"target\".");
                }

                Attr attrUrl = childTocItemElement.getAttributeNode("url");
                if (attrUrl == null) {
                    throw new WikiGenerationException("MapID element with no attribute \"url\".");
                }
                String url = attrUrl.getValue();
                if (url.isEmpty()) {
                    throw new WikiGenerationException(
                            "MapID element with empty attribute \"url\".");
                }

                targetToFile.put(target, createMapTarget(url));
            }
        }

        if (targetToFile.isEmpty()) {
            return Collections.emptyMap();
        }

        return targetToFile;
    }

    public void writeTo(Path outputFile) {
        if (tocItems.isEmpty()) {
            return;
        }

        if (Files.exists(outputFile) && !Files.isWritable(outputFile)) {
            throw new WikiGenerationException(
                    "The output file [" + outputFile + "] is not writable.");
        }

        try (Writer writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writeTocItems(writer, tocItems, "");
        } catch (IOException e) {
            throw new WikiGenerationException("Failed to write the TOC.", e);
        }
    }

    public void createImages(Path dir) {
        Path imagesDir = dir.resolve("images");
        try {
            Files.createDirectories(imagesDir);
        } catch (IOException e) {
            throw new WikiGenerationException(
                    "Failed to create the directory for images: " + imagesDir, e);
        }

        for (MapTarget targeMap : targetsMap.values()) {
            targeMap.createImage(imagesDir);
        }
    }

    private static void writeTocItems(Writer writer, List<TocItem> tocItems, String padding)
            throws IOException {
        for (TocItem tocItem : tocItems) {
            writer.write(padding);
            writer.write("* ");

            if (tocItem.getImage() != null) {
                writer.write("![](");
                writer.write(tocItem.getImage());
                writer.write(") ");
            }

            String target = tocItem.getTarget();
            if (target != null) {
                writer.write('[');
            }

            writer.write(tocItem.getText());

            if (target != null) {
                writer.write("](");
                writer.write(target);
                writer.write(')');
            }

            writer.append('\n');

            writeTocItems(writer, tocItem.getChildren(), padding + "  ");
        }
    }

    private List<TocItem> createTocItems() {
        Element rootTocElement = null;
        NodeList childNodes = createDocument(toc).getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE
                    && "toc".equals(childNode.getNodeName())) {
                rootTocElement = (Element) childNode;
                break;
            }
        }

        if (rootTocElement == null) {
            return Collections.emptyList();
        }

        return createTocItemNodes(rootTocElement);
    }

    private List<TocItem> createTocItemNodes(Element tocItemElement) {
        ArrayList<TocItem> items = new ArrayList<>(tocItemElement.getChildNodes().getLength());

        NodeList childNodes = tocItemElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if ("tocitem".equals(childNode.getNodeName())) {
                Element childTocItemElement = (Element) childNode;
                Attr attrText = childTocItemElement.getAttributeNode("text");
                if (attrText == null) {
                    throw new WikiGenerationException("TOC item with no attribute \"text\".");
                }
                String text = attrText.getValue();
                if (text.isEmpty()) {
                    throw new WikiGenerationException("TOC item with empty attribute \"text\".");
                }

                String target = getNonEmptyAttribute(childTocItemElement, "target");
                if (target != null) {
                    MapTarget mapTarget = targetsMap.get(target);
                    if (mapTarget != null) {
                        if (mapTarget.isImage()) {
                            System.err.println(
                                    "TOC attribute \"target\" points to an image [text="
                                            + text
                                            + ", target="
                                            + target
                                            + "].");
                        }
                        target = mapTarget.getWikiLink();
                    } else {
                        System.err.println(
                                "No mapping for TOC entry with target attribute: " + target);
                    }
                }
                String image = getNonEmptyAttribute(childTocItemElement, "image");
                if (image != null) {
                    MapTarget mapTarget = targetsMap.get(image);
                    if (mapTarget == null) {
                        throw new WikiGenerationException(
                                "No mapping for TOC entry with image attribute: " + image);
                    }
                    if (!mapTarget.isImage()) {
                        throw new WikiGenerationException(
                                "TOC attribute \"image\" points to non image file [text="
                                        + text
                                        + ", image="
                                        + image
                                        + "].");
                    }
                    image = mapTarget.getWikiLink();
                }

                items.add(
                        new TocItem(text, target, image, createTocItemNodes(childTocItemElement)));
            }
        }

        items.trimToSize();
        return items;
    }

    private static String getNonEmptyAttribute(Element element, String attribute) {
        Attr attrLink = element.getAttributeNode(attribute);
        if (attrLink != null) {
            String linkValue = attrLink.getValue();
            if (!linkValue.isEmpty()) {
                return linkValue;
            }
        }
        return null;
    }

    private MapTarget createMapTarget(String url) {
        URL file = WikiGeneratorUtils.createUrlFor(map, url);

        if (IMAGES_PATTERN.matcher(url).matches()) {
            String path = WikiGeneratorUtils.normalisedImagePath(contentsDir, file);
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            return new MapTarget("images/" + fileName, file, path);
        }

        return new MapTarget(
                WikiGeneratorUtils.createWikiFileName(
                        outputFilenamePrefix,
                        WikiGeneratorUtils.normalisedPath(contentsDir, file),
                        ""));
    }

    public static final class TocItem {

        private final String text;
        private final String target;
        private final String image;
        private final List<TocItem> children;

        public TocItem(String text, String target, String image, List<TocItem> children) {
            this.text = text;
            this.target = target;
            this.image = image;

            if (children != null) {
                this.children = Collections.unmodifiableList(new ArrayList<>(children));
            } else {
                this.children = Collections.emptyList();
            }
        }

        public String getText() {
            return text;
        }

        public String getTarget() {
            return target;
        }

        public String getImage() {
            return image;
        }

        public List<TocItem> getChildren() {
            return children;
        }
    }

    private static class MapTarget {

        private final String wikiLink;
        private final boolean image;

        private final URL srcImageFile;
        private final String imagePath;

        public MapTarget(String wikiLink) {
            this(wikiLink, null, null);
        }

        private MapTarget(String wikiLink, URL srcImageFile, String imagePath) {
            this.wikiLink = wikiLink;
            this.image = srcImageFile != null;
            this.srcImageFile = srcImageFile;
            this.imagePath = imagePath;
        }

        public String getWikiLink() {
            return wikiLink;
        }

        public boolean isImage() {
            return image;
        }

        public void createImage(Path dir) {
            if (!image) {
                return;
            }
            Path target = dir.resolve(imagePath);
            try (InputStream inputStream = new BufferedInputStream(srcImageFile.openStream())) {
                Files.copy(inputStream, target);
            } catch (IOException e) {
                throw new WikiGenerationException(
                        "Failed to copy image from [" + srcImageFile + "] to [" + target + "]", e);
            }
        }
    }

    private static Document createDocument(URL url) {
        try (InputStream inputStream = new BufferedInputStream(url.openStream())) {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setValidating(false);
            builderFactory.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            builderFactory.setFeature(
                    "http://xml.org/sax/features/external-general-entities", false);
            builderFactory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            return builder.parse(inputStream);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            throw new WikiGenerationException("Failed to read file: " + url, e);
        }
    }
}
