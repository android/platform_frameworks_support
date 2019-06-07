/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method in a {@link Dao} annotated class as a delete method.
 * <p>
 * The implementation of the method will delete its parameters from the database.
 * <p>
 * All of the parameters of the Delete method must either be classes annotated with {@link Entity}
 * or collections/array of it. However if the target entity is specified via {@link #entity()} then
 * the method can contain a single parameter of a Pojo class or collection of a class that
 * will be interpreted as a partial entity.
 * <p>
 * Example:
 * <pre>
 * {@literal @}Dao
 * public interface MyDao {
 *     {@literal @}Delete
 *     public void deleteUsers(User... users);
 *
 *     {@literal @}Delete
 *     public void deleteAll(User user1, User user2);
 *
 *     {@literal @}Delete
 *     public void deleteWithFriends(User user, List&lt;User&gt; friends);
 *
 *     {@literal @}Delete(entity = User.class)
 *     public void deleteViaUsername(Username username);
 * }
 * </pre>
 *
 * @see Insert
 * @see Query
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Delete {

    /**
     * The target entity of the delete method.
     * <p>
     * When this is declared the delete method must only contain a single parameter. The Pojo class
     * of the parameter must contain a subset of the fields of the target entity. The fields value
     * will be used to find matching entities to delete.
     * <p>
     * By default the target entity is interpreted by the methods parameter.
     *
     * @return the target entity of the delete method or none if the method should use the
     *         parameter type entities.
     */
    Class entity() default Object.class;
}
