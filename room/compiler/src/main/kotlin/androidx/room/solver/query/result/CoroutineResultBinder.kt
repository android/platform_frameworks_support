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

package androidx.room.solver.query.result

import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomCoroutineTypeNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import androidx.room.writer.DaoWriter
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeMirror

class CoroutineResultBinder(
    val typeArg: TypeMirror,
    private val continuationParamName: String,
    adapter: QueryResultAdapter?
) : QueryResultBinder(adapter) {

    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val callableImpl = createCallable(
            roomSQLiteQueryVar,
            canReleaseQuery,
            dbField,
            inTransaction,
            scope)

        scope.builder().apply {
            addStatement(
                "return $T.execute($N, $L, $N)",
                RoomCoroutineTypeNames.COROUTINE_ROOM,
                DaoWriter.dbField,
                callableImpl,
                continuationParamName)
        }
    }

    private fun createCallable(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ): TypeSpec {
        return TypeSpec.anonymousClassBuilder("").apply {
            superclass(
                ParameterizedTypeName.get(java.util.concurrent.Callable::class.typeName(),
                    typeArg.typeName()))
            addMethod(
                MethodSpec.methodBuilder("call").apply {
                    // public T call() throws Exception {}
                    returns(typeArg.typeName())
                    addAnnotation(Override::class.typeName())
                    addModifiers(Modifier.PUBLIC)
                    addException(Exception::class.typeName())

                    // Body.
                    createRunQueryAndReturnStatements(
                        builder = this,
                        roomSQLiteQueryVar = roomSQLiteQueryVar,
                        canReleaseQuery = canReleaseQuery,
                        dbField = dbField,
                        inTransaction = inTransaction,
                        scope = scope)
                }.build())
        }.build()
    }

    private fun createRunQueryAndReturnStatements(
        builder: MethodSpec.Builder,
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val transactionWrapper = if (inTransaction) {
            builder.transactionWrapper(dbField)
        } else {
            null
        }
        val shouldCopyCursor = adapter?.shouldCopyCursor() == true
        val outVar = scope.getTmpVar("_result")
        val cursorVar = scope.getTmpVar("_cursor")
        transactionWrapper?.beginTransactionWithControlFlow()
        builder.apply {
            addStatement("final $T $L = $T.query($N, $L, $L)",
                AndroidTypeNames.CURSOR,
                cursorVar,
                RoomTypeNames.DB_UTIL,
                DaoWriter.dbField,
                roomSQLiteQueryVar,
                if (shouldCopyCursor) "true" else "false")
            beginControlFlow("try").apply {
                val adapterScope = scope.fork()
                adapter?.convert(outVar, cursorVar, adapterScope)
                addCode(adapterScope.builder().build())
                transactionWrapper?.commitTransaction()
                addStatement("return $L", outVar)
            }
            nextControlFlow("finally").apply {
                addStatement("$L.close()", cursorVar)
                if (canReleaseQuery) {
                    addStatement("$L.release()", roomSQLiteQueryVar)
                }
            }
            endControlFlow()
        }
        transactionWrapper?.endTransactionWithControlFlow()
    }
}