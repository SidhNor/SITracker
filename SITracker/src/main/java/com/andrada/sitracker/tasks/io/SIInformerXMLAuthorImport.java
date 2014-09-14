/*
 * Copyright 2014 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.tasks.io;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class SIInformerXMLAuthorImport implements AuthorImportStrategy {

    @NotNull
    @Override
    public List<String> extractAuthorsFromFile(@NotNull String absoluteFilename) {
        List<String> result = new ArrayList<String>();
        if (!absoluteFilename.endsWith(".xml")) {
            return result;
        }
        try {
            File file = new File(absoluteFilename);
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            if (doc.hasChildNodes()) {
                return readFeed(doc);
            }
        } catch (Exception ignored) {

        }

        return result;
    }

    @NotNull
    private List<String> readFeed(@NotNull Document doc) {
        List<String> entries = new ArrayList<String>();
        NodeList authorsList = doc.getElementsByTagName("Author");
        if (authorsList.getLength() > 0) {
            for (int i = 0, l = authorsList.getLength(); i < l; i++) {
                Node author = authorsList.item(i);
                if (author.hasChildNodes()) {
                    NodeList authorChildNodes = author.getChildNodes();
                    for (int j = 0, l1 = authorChildNodes.getLength(); j < l1; j++) {
                        if (authorChildNodes.item(j).getNodeName().equalsIgnoreCase("URL")) {
                            Node childValue = authorChildNodes.item(j).getFirstChild();
                            if (childValue != null) {
                                entries.add(childValue.getNodeValue());
                            }
                        }
                    }
                }
            }
        }
        return entries;
    }
}
