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

package androidx.dynamicanimation.animation

import android.view.View

/**
 * Creates [FlingAnimation] for view.
 *
 * @param property View property to be animated.
 * @return [FlingAnimation]
 */
inline fun <K : View> K.flingAnimationOf(property: FloatPropertyCompat<K>): FlingAnimation {
    return FlingAnimation(this, property)
}

/**
 * Creates [SpringAnimation] for view.
 *
 * @param property View property to be animated.
 * @return [SpringAnimation]
 */
inline fun <K : View> K.springAnimationOf(property: FloatPropertyCompat<K>): SpringAnimation {
    return SpringAnimation(this, property)
}

/**
 * Creates [SpringAnimation] for view.
 *
 * @param property View property to be animated.
 * @param finalPosition [SpringForce.mFinalPosition] Final position of spring.
 * @return [SpringAnimation]
 */
inline fun <K : View> K.springAnimationOf(
    property: FloatPropertyCompat<K>,
    finalPosition: Float
): SpringAnimation {
    return SpringAnimation(this, property, finalPosition)
}

/**
 * Creates [SpringAnimation] for view along with [SpringForce]
 *
 * @param property View property to be animated.
 * @param func lambda with receiver on [SpringForce]
 * @return [SpringAnimation]
 */
inline fun <K : View> K.springAnimationOf(
    property: FloatPropertyCompat<K>,
    func: SpringForce.() -> Unit
): SpringAnimation {
    val springAnimation = SpringAnimation(this, property)
    val springForce = SpringForce()
    springForce.func()
    springAnimation.spring = springForce
    return springAnimation
}

/**
 * Creates [SpringAnimation] for view along with [SpringForce]
 *
 * @param property View property to be animated.
 * @param finalPosition [SpringForce.mFinalPosition] Final position of spring.
 * @param func lambda with receiver on [SpringForce]
 * @return [SpringAnimation]
 */
inline fun <K : View> K.springAnimationOf(
    property: FloatPropertyCompat<K>,
    finalPosition: Float,
    func: SpringForce.() -> Unit
): SpringAnimation {
    val springAnimation = SpringAnimation(this, property, finalPosition)
    springAnimation.spring.func()
    return springAnimation
}

/**
 * Updates or applies spring force properties like [SpringForce.mDampingRatio],
 * [SpringForce.mFinalPosition] and stiffness on SpringAnimation.
 *
 * If [SpringAnimation.mSpring] is null in case [SpringAnimation] is created without final position
 * it will be created and attached to [SpringAnimation]
 *
 * @param func lambda with receiver on [SpringForce]
 * @return [SpringAnimation]
 */
inline fun SpringAnimation.withSpringForceProperties(
    func: SpringForce.() -> Unit
): SpringAnimation {
    if (spring == null) {
        spring = SpringForce()
    }
    spring.func()
    return this
}