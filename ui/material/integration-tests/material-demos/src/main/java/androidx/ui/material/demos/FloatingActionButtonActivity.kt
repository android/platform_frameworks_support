<<<<<<< HEAD   (810747 Merge "Merge empty history for sparse-5626174-L1780000033228)
=======
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

package androidx.ui.material.demos

import android.app.Activity
import android.os.Bundle
import androidx.ui.painting.imageFromResource
import androidx.compose.setContent
import androidx.compose.composer

class FloatingActionButtonActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val icon = imageFromResource(resources, R.drawable.ic_favorite)
        setContent { FloatingActionButtonDemo(icon = icon) }
    }
}
>>>>>>> BRANCH (2c954e Merge "Merge cherrypicks of [988730] into sparse-5676727-L53)
