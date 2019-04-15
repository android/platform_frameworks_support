/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.room.ext.CallableTypeSpecBuilder
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomCoroutinesTypeNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.T
import androidx.room.ext.arrayTypeName
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import javax.lang.model.type.TypeMirror

/**
 * Binds the result of a of a Kotlin Coroutine Channel or Flow
 */
class CoroutineChannelFlowResultBinder(
    private val channelFlowType: Type,
    val typeArg: TypeMirror,
    val tableNames: Set<String>,
    adapter: QueryResultAdapter?
) : QueryResultBinder(adapter) {

    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val callableImpl = CallableTypeSpecBuilder(typeArg.typeName()) {
            createRunQueryAndReturnStatements(
                builder = this,
                roomSQLiteQueryVar = roomSQLiteQueryVar,
                canReleaseQuery = canReleaseQuery,
                dbField = dbField,
                inTransaction = inTransaction,
                scope = scope)
        }.build()

        val methodName = when (channelFlowType) {
            Type.CHANNEL -> "createChannel"
            Type.FLOW -> "createFlow"
        }
        scope.builder().apply {
            val tableNamesList = tableNames.joinToString(",") { "\"$it\"" }
            addStatement(
                "return $T.$methodName($N, $L, new $T{$L}, $L)",
                RoomCoroutinesTypeNames.COROUTINES_ROOM,
                dbField,
                if (inTransaction) "true" else "false",
                String::class.arrayTypeName(),
                tableNamesList,
                callableImpl)
        }
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
                dbField,
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

    enum class Type(val className: ClassName) {
        CHANNEL(KotlinTypeNames.RECEIVE_CHANNEL),
        FLOW(KotlinTypeNames.FLOW)
    }
}