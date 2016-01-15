/*
 * Copyright 2016 Gleb Godonoga.
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

package com.andrada.sitracker.tasks.io;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AuthorFileImportContext {

    @NotNull
    public List<String> getAuthorListFromFile(@NotNull String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1);
        }
        AuthorImportStrategy importStrategy = null;

        if (extension.equals("txt") ||
                extension.equals("rtf") ||
                extension.equals("doc") ||
                extension.equals("")) {
            importStrategy = new PlainTextAuthorImport();
        } else if (extension.equals("xml")) {
            importStrategy = new SIInformerXMLAuthorImport();
        }

        if (importStrategy != null) {
            return importStrategy.extractAuthorsFromFile(fileName);
        }

        return new ArrayList<String>();
    }
}
