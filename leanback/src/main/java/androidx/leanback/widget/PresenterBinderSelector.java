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
package androidx.leanback.widget;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A PresenterSelector is used to obtain a {@link Presenter} for a given Object.
 * Similar to {@link Presenter},  PresenterSelector is stateless.
 * A regularly used PresenterBinderSelector is {@link ClassPresenterBinderSelector}.
 *
 * @param <PresenterT> Type of associated Presenter.
 * @param <VH> Type of associated Presenter.ViewHolder.
 */
public abstract class PresenterBinderSelector<PresenterT extends Presenter,
        VH extends Presenter.ViewHolder> {
    /**
     * Returns a PresenterBinder for the given item.
     */
    public abstract @Nullable PresenterBinder<PresenterT, VH, ?> getPresenterBinder(
            @NonNull Object item);

}
