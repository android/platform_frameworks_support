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

package androidx.room.writer

import androidx.annotation.VisibleForTesting
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.T
import androidx.room.processor.Context
import androidx.room.solver.CodeGenScope
import androidx.room.vo.InsertionMethod
import androidx.room.vo.ShortcutQueryParameter
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/**
 * Class that knows how to write a shortcut method (insert/update/delete) body.
 */
class InsertMethodAdapter private constructor(
    val context: Context,
    val insertionType: InsertionMethod.Type
) {
    companion object {
        fun create(
            context: Context,
            returnType: TypeMirror,
            params: List<ShortcutQueryParameter>
        ): InsertMethodAdapter? {
            var insertionType = getInsertionType(returnType)
            if (insertionType != null && isInsertValid(insertionType, params)) {
                return InsertMethodAdapter(context, insertionType)
            }
            return null
        }

        private fun isInsertValid(
            insertionType: InsertionMethod.Type?,
            params: List<ShortcutQueryParameter>
        ): Boolean {
            if (insertionType == null) {
                return false
            }
            val acceptableTypes = acceptableTypes(params)
            return insertionType in acceptableTypes
        }

        @VisibleForTesting
        val VOID_SET by lazy { setOf(InsertionMethod.Type.INSERT_VOID) }
        @VisibleForTesting
        val SINGLE_ITEM_SET by lazy {
            setOf(InsertionMethod.Type.INSERT_VOID, InsertionMethod.Type.INSERT_SINGLE_ID)
        }
        @VisibleForTesting
        val MULTIPLE_ITEM_SET by lazy {
            setOf(InsertionMethod.Type.INSERT_VOID, InsertionMethod.Type.INSERT_ID_ARRAY,
                    InsertionMethod.Type.INSERT_ID_ARRAY_BOX,
                    InsertionMethod.Type.INSERT_ID_LIST)
        }

        private fun acceptableTypes(
            params: List<ShortcutQueryParameter>
        ): Set<InsertionMethod.Type> {
            if (params.isEmpty()) {
                return VOID_SET
            }
            if (params.size > 1) {
                return VOID_SET
            }
            if (params.first().isMultiple) {
                return MULTIPLE_ITEM_SET
            } else {
                return SINGLE_ITEM_SET
            }
        }

        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        private fun getInsertionType(returnType: TypeMirror): InsertionMethod.Type? {
            // TODO we need to support more types here.
            fun isLongPrimitiveType(typeMirror: TypeMirror) = typeMirror.kind == TypeKind.LONG

            fun isLongBoxType(typeMirror: TypeMirror) =
                    MoreTypes.isType(typeMirror) &&
                            MoreTypes.isTypeOf(java.lang.Long::class.java, typeMirror)

            fun isLongType(typeMirror: TypeMirror) =
                    isLongPrimitiveType(typeMirror) || isLongBoxType(typeMirror)

            return if (returnType.kind == TypeKind.VOID) {
                InsertionMethod.Type.INSERT_VOID
            } else if (returnType.kind == TypeKind.ARRAY) {
                val arrayType = MoreTypes.asArray(returnType)
                val param = arrayType.componentType
                if (isLongPrimitiveType(param)) {
                    InsertionMethod.Type.INSERT_ID_ARRAY
                } else if (isLongBoxType(param)) {
                    InsertionMethod.Type.INSERT_ID_ARRAY_BOX
                } else {
                    null
                }
            } else if (MoreTypes.isType(returnType) &&
                    MoreTypes.isTypeOf(List::class.java, returnType)) {
                val declared = MoreTypes.asDeclared(returnType)
                val param = declared.typeArguments.first()
                if (isLongBoxType(param)) {
                    InsertionMethod.Type.INSERT_ID_LIST
                } else {
                    null
                }
            } else if (isLongType(returnType)) {
                InsertionMethod.Type.INSERT_SINGLE_ID
            } else {
                null
            }
        }
    }

    fun createInsertionMethodBody(
        parameters: List<ShortcutQueryParameter>,
        insertionAdapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        scope: CodeGenScope
    ) {
        scope.builder().apply {
            // TODO assert thread
            // TODO collect results
            addStatement("$N.beginTransaction()", DaoWriter.dbField)
            val needsReturnType = insertionType != InsertionMethod.Type.INSERT_VOID
            val resultVar = if (needsReturnType) {
                scope.getTmpVar("_result")
            } else {
                null
            }

            beginControlFlow("try").apply {
                parameters.forEach { param ->
                    val insertionAdapter = insertionAdapters[param.name]?.first
                    if (needsReturnType) {
                        // if it has more than 1 parameter, we would've already printed the error
                        // so we don't care about re-declaring the variable here
                        addStatement("$T $L = $N.$L($L)",
                                insertionType.returnTypeName, resultVar,
                                insertionAdapter, insertionType.methodName,
                                param.name)
                    } else {
                        addStatement("$N.$L($L)", insertionAdapter, insertionType.methodName,
                                param.name)
                    }
                }
                addStatement("$N.setTransactionSuccessful()", DaoWriter.dbField)
                if (needsReturnType) {
                    addStatement("return $L", resultVar)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("$N.endTransaction()", DaoWriter.dbField)
            }
            endControlFlow()
        }
    }
}