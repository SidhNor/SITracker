/*
 * Copyright 2013 Gleb Godonoga.
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

package com.andrada.sitracker.contracts;

import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SiteProvider {

    /**
     * Get base link to particular site implementation
     *
     * @return base link for particular site provider implementation
     */
    @NotNull
    String baseSiteLink();

    /**
     * Returns parsed out author instance. If author name or update date is not found, returns null.
     * {@link com.andrada.sitracker.db.beans.Author} instance is not persisted in this method.
     *
     * @param page raw source of author page
     * @return an instance of {@link com.andrada.sitracker.db.beans.Author} with populated data either null
     */
    @NotNull
    Author getAuthor(String page);

    /**
     * Creates a list of {@link com.andrada.sitracker.db.beans.Publication} instances that are
     * associated with the specified author. Publications are not persisted.
     *
     * @param page   raw source of author page that contains the list of publications
     * @param author instance of {@link com.andrada.sitracker.db.beans.Author} that will be associated with all parsed publications
     * @return a list of {@link com.andrada.sitracker.db.beans.Publication} that are associated with the specified author
     */
    @NotNull
    List<Publication> getPublications(String page, Author author);

}
