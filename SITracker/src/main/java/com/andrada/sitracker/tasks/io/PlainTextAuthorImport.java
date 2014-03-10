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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlainTextAuthorImport implements AuthorImportStrategy {
    @Override
    public List<String> extractAuthorsFromFile(String absoluteFilename) {
        List<String> authorLinks = new ArrayList<String>();

        BufferedReader data = null;
        try {
            File file = new File(absoluteFilename);
            data = new BufferedReader(new FileReader(file));
            String lRegex = "(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))";
            String line;
            while ((line = data.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && line.matches(lRegex)) {
                    authorLinks.add(line);
                }
            }
            data.close();
        } catch (Exception ignored) {

        } finally {
            try {
                if (data != null) {
                    data.close();
                }
            } catch (IOException ignored) {
            }

        }

        return authorLinks;
    }
}
