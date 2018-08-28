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


package com.example.android.supportv4.widget;

import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android.supportv4.R;

/**
 * This activity demonstrates the use of nested scrolling in the v4 support library.
 * See the associated layout file for details.
 */
public class AppBarOverlayTest extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nested_scroll);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSupportActionBar().setHideOnContentScrollEnabled(true);
        }
    }
}
