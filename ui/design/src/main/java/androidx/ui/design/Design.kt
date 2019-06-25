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

package androidx.ui.design

import androidx.compose.Ambient
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.SlotTable
import androidx.compose.ambient
import androidx.compose.composer
import androidx.compose.unaryPlus
import java.util.Collections
import java.util.WeakHashMap

/**
 * Design mode ambient. True if the the composition is composed inside a Design component.
 */
val DesignMode = Ambient.of("Design Mode") { false }

/**
 * A wrapper for compositions in design mode. The composition below the Design child composition is
 * considered in design mode.
 */
@Composable
fun Design(@Children children: @Composable() () -> Unit) {
    composer.composer.collectKeySourceInformation()
    tables.add(composer.composer.slotTable)
    DesignMode.Provider(true) {
        children()
    }
}

val tables = Collections.newSetFromMap(WeakHashMap<SlotTable, Boolean>())

/**
 * A wrapper for design-only behavior. It will the children will only be composed if the composition
 * is in design mode.
 */
@Composable
fun InDesignModeOnly(@Children children: @Composable() () -> Unit) {
    if (+ambient(DesignMode)) {
        children()
    }
}
