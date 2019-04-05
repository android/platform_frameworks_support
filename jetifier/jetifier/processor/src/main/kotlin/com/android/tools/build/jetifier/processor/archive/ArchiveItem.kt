/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.build.jetifier.processor.archive

import java.io.OutputStream
import java.nio.file.Path

/**
 * Abstraction to represent archive and its files as a one thing before and after transformation
 * together with information if any changes happened during the transformation.
 */
interface ArchiveItem {

    /**
     * Relative path of the item according to its location in the archive.
     *
     * Files in a nested archive have a path relative to that archive not to the parent of
     * the archive. The root archive has the file system path set as its relative path.
     */
    val relativePath: Path

    /**
     * Name of the file.
     */
    val fileName: String

    /**
     * Whether the item's content or its children were changed by Jetifier. This determines
     * whether the parent archive is going to be marked as changed thus having a dependency on
     * support.
     */
    val wasChanged: Boolean

    /**
     * Accepts visitor.
     */
    fun accept(visitor: ArchiveItemVisitor)

    /**
     * Writes its internal data (or other nested files) into the given output stream.
     */
    fun writeSelfTo(outputStream: OutputStream)

    fun isPomFile() = fileName.equals("pom.xml", ignoreCase = true)
            || fileName.endsWith(".pom", ignoreCase = true)

    fun isClassFile() = fileName.endsWith(".class", ignoreCase = true)

    fun isXmlFile() = fileName.endsWith(".xml", ignoreCase = true)

<<<<<<< HEAD   (69f76e Merge "Merge empty history for sparse-5425228-L6310000028962)
    fun isProGuardFile() = fileName.equals("proguard.txt", ignoreCase = true)
}

/**
 * Aggregated result of all the files that were found.
 *
 * @see ArchiveItem.findAllFiles
 */
class FileSearchResult {

    val all = mutableSetOf<ArchiveFile>()

    fun addFile(file: ArchiveFile) {
        all.add(file)
    }
=======
    fun isProGuardFile () = fileName.equals("proguard.txt", ignoreCase = true)
>>>>>>> BRANCH (bf79df Merge "Merge cherrypicks of [940699] into sparse-5433600-L95)
}