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

package androidx.room.vo

import androidx.room.migration.bundle.DatabaseViewBundle
import androidx.room.parser.ParsedQuery
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

class DatabaseView(
    element: TypeElement,
    val viewName: String,
    val query: ParsedQuery,
    type: DeclaredType,
    fields: List<Field>,
    embeddedFields: List<EmbeddedField>,
    constructor: Constructor?
)
    : Pojo(element, type, fields, embeddedFields, emptyList(), constructor), HasSchemaIdentity {

    val createViewQuery by lazy {
        "CREATE VIEW IF NOT EXISTS `$viewName` AS ${query.original}"
    }

    /**
     * List of all the underlying tables including those that are indirectly referenced.
     *
     * This is populated by DatabaseProcessor. This cannot be a constructor parameter as it can
     * only be populated after all the other views are initialized and parsed.
     */
    val tables = mutableSetOf<String>()

    fun toBundle() = DatabaseViewBundle(viewName, createViewQuery)

    override fun getIdKey(): String {
        val identityKey = SchemaIdentityKey()
        identityKey.append(query.original)
        return identityKey.hash()
    }
}
