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

package androidx.room.integration.autovaluetestapp.vo;

import androidx.room.DatabaseView;
import androidx.room.Embedded;

import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

@AutoValue
@DatabaseView("SELECT Dog.*, Person.* FROM Dog INNER JOIN Person ON Dog.ownerId = Person.id")
public abstract class DogWithOwner {

    @CopyAnnotations
    @Embedded
    public abstract Pet.Dog getDog();

    @CopyAnnotations
    @Embedded
    public abstract Person getOwner();

    public static DogWithOwner create(Pet.Dog dog, Person owner) {
        return new AutoValue_DogWithOwner(dog, owner);
    }
}
