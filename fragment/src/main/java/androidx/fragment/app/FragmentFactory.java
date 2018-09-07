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

package androidx.fragment.app;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SimpleArrayMap;

import java.lang.reflect.InvocationTargetException;

/**
 * Interface used to control the instantiation of {@link Fragment} instances.
 * Implementations can be registered with a {@link FragmentManager} via
 * {@link FragmentManager#setFragmentFactory(FragmentFactory)}.
 *
 * @see FragmentManager#setFragmentFactory(FragmentFactory)
 */
public class FragmentFactory {
    private static final SimpleArrayMap<String, Class<?>> sClassMap = new SimpleArrayMap<>();

    /**
     * Determine if the given fragment name is a support library fragment class.
     *
     * @param classLoader The default classloader to use for loading the Class
     * @param className Class name of the fragment to load
     * @return Returns the parsed Class
     */
    @NonNull
    private static Class<?> loadClass(@NonNull ClassLoader classLoader,
            @NonNull String className) throws ClassNotFoundException {
        Class<?> clazz = sClassMap.get(className);
        if (clazz == null) {
            // Class not found in the cache, see if it's real, and try to add it
            clazz = classLoader.loadClass(className);
            sClassMap.put(className, clazz);
        }
        return clazz;
    }

    /**
     * Determine if the given fragment name is a valid Fragment class.
     *
     * @param classLoader The default classloader to use for loading the Class
     * @param className Class name of the fragment to test
     * @return true if <code>className</code> is <code>androidx.fragment.app.Fragment</code>
     *         or a subclass, false otherwise.
     */
    static boolean isFragmentClass(@NonNull ClassLoader classLoader,
            @NonNull String className) {
        try {
            Class<?> clazz = loadClass(classLoader, className);
            return Fragment.class.isAssignableFrom(clazz);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Parse a Fragment Class from the given class name. The resulting Class is kept in a global
     * cache, bypassing the {@link ClassLoader#loadClass(String)} calls when passed the same
     * class name again.
     *
     * @param classLoader The default classloader to use for loading the Class
     * @param className The class name of the fragment to parse.
     * @return Returns the parsed Fragment Class
     * @throws Fragment.InstantiationException If there is a failure in parsing
     * the given fragment class.  This is a runtime exception; it is not
     * normally expected to happen.
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public static Class<? extends Fragment> loadFragmentClass(@NonNull ClassLoader classLoader,
            @NonNull String className) {
        try {
            Class<?> clazz = loadClass(classLoader, className);
            return (Class<? extends Fragment>) clazz;
        } catch (ClassNotFoundException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className
                    + ": make sure class name exists", e);
        } catch (ClassCastException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className
                    + ": make sure class is a valid subclass of Fragment", e);
        }
    }

    /**
     * Create a new instance of a Fragment with the given class name. This uses
     * {@link #loadFragmentClass(ClassLoader, String)} and the empty
     * constructor of the resulting Class by default.
     * <p>
     * Note that this method is <strong>not</strong> responsible for calling
     * {@link Fragment#setArguments(Bundle)} on the returned Fragment instance.
     *
     * @param classLoader The default classloader to use for instantiation
     * @param className The class name of the fragment to instantiate.
     * @param args The Fragment's arguments, if any. This will be <code>null</code> when
     *             inflating the Fragment from XML.
     * @return Returns a new fragment instance.
     * @throws InstantiationException If there is a failure in instantiating
     * the given fragment class.  This is a runtime exception; it is not
     * normally expected to happen.
     */
    @NonNull
    public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className,
                                @SuppressWarnings("unused") @Nullable Bundle args) {
        try {
            Class<? extends Fragment> cls = loadFragmentClass(classLoader, className);
            return cls.getConstructor().newInstance();
        } catch (java.lang.InstantiationException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        } catch (IllegalAccessException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        } catch (NoSuchMethodException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className
                    + ": could not find Fragment constructor", e);
        } catch (InvocationTargetException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className
                    + ": calling Fragment constructor caused an exception", e);
        }
    }

    /**
     * Provide a {@link FragmentFactory} for child Fragments. When a
     * {@link Fragment#getChildFragmentManager() child FragmentManager} does not have an explicitly
     * set {@link FragmentFactory}, this method is called to give the parent's FragmentFactory
     * (this object) the ability to provide a specific FragmentFactory.
     * <p>
     * By default, this method returns this FragmentFactory instance.
     *
     * @param parent The parent Fragment
     * @return A FragmentFactory suitable for constructing child Fragments for the given parent.
     */
    @NonNull
    public FragmentFactory provideChildFragmentFactory(@NonNull Fragment parent) {
        return this;
    }
}
