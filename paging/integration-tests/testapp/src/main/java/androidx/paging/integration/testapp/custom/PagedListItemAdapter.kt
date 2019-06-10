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

package androidx.paging.integration.testapp.custom

import android.graphics.Color
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagedListAdapter
import androidx.paging.integration.testapp.R
import androidx.recyclerview.widget.RecyclerView

/**
 * Sample PagedList item Adapter, which uses a AsyncPagedListDiffer.
 */
internal class PagedListItemAdapter :
    PagedListAdapter<Item, RecyclerView.ViewHolder>(Item.DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val holder = object : RecyclerView.ViewHolder(TextView(parent.context)) {}
        holder.itemView.minimumHeight = 400
        holder.itemView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val textView = holder.itemView as TextView
        val item = getItem(position)
        when {
            item != null -> {
                textView.text = item.text
                textView.setBackgroundColor(item.bgColor)
            }
            else -> {
                textView.setText(R.string.loading)
                textView.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }
}
