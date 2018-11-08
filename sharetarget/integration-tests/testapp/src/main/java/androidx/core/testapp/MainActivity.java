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

package androidx.sharetarget.testapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.Person;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

class MainActivity extends AppCompatActivity {

    private static final String CATEGORY_TEXT_SHARE_TARGET =
            "androidx.sharetarget.testapp.category.TEXT_SHARE_TARGET";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.push_targets).setOnClickListener(mOnClickListener);
        findViewById(R.id.remove_targets).setOnClickListener(mOnClickListener);
        findViewById(R.id.parse_share_targets).setOnClickListener(mOnClickListener);
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.push_targets:
                    pushDirectShareTargets();
                    break;
                case R.id.remove_targets:
                    removeAllDirectShareTargets();
                    break;
                case R.id.parse_share_targets:
                    // can not do this from the app anymore
                    break;
            }
        }
    };

    private void pushDirectShareTargets() {
        ShortcutInfoCompat.Builder b = new ShortcutInfoCompat.Builder(this, "myid");
        b.setLongLived();

        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");

        Set<String> categories = new HashSet<>();
        categories.add(CATEGORY_TEXT_SHARE_TARGET);

        ArrayList<ShortcutInfoCompat> shortcuts = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            shortcuts.add(new ShortcutInfoCompat.Builder(this, "Person_" + (i + 1))
                    .setShortLabel("Person_" + (i + 1))
                    .setIcon(IconCompat.createWithResource(this, R.mipmap.logo_avatar))
                    .setIntent(intent)
                    .setLongLived()
                    .setPerson(new Person.Builder().build())
                    .setCategories(categories)
                    .build());
        }

        ShortcutManagerCompat.addDynamicShortcuts(this, shortcuts); // adding
    }

    private void removeAllDirectShareTargets() {
        System.out.println("METTT: removing all dynamic shortcuts");
        ShortcutManagerCompat.removeAllDynamicShortcuts(this);
    }


}
