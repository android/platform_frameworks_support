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
package androidx.compose.benchmark.realworld4

import androidx.compose.Model
import androidx.ui.graphics.Color
import java.util.Random

/**
 * RealWorld4 is a performance test that attempts to simulate a real-world application of reasonably
 * large scale (eg. gmail-sized application).
 */
@Model
class RealWorld4_DataModel_09() {
    var f0: RealWorld4_DataModel_10 = RealWorld4_DataModel_10()
    var f1: Int = Random().nextInt()
    var f2: RealWorld4_DataModel_10 = RealWorld4_DataModel_10()
    var f3: Color = Color(red=Random().nextInt(255), green=Random().nextInt(255), blue=Random().nextInt(255))
    var f4: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f5: Int = Random().nextInt()
    var f6: Color = Color(red=Random().nextInt(255), green=Random().nextInt(255), blue=Random().nextInt(255))
    var f7: Int = Random().nextInt()
    var f8: Color = Color(red=Random().nextInt(255), green=Random().nextInt(255), blue=Random().nextInt(255))
    var f9: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f10: String = smallRange().map { createSomeText() }.joinToString("\n")
 }

@Model
class RealWorld4_DataModel_10() {
    var f0: Int = Random().nextInt()
    var f1: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f2: Int = Random().nextInt()
    var f3: Int = Random().nextInt()
 }

@Model
class RealWorld4_DataModel_06() {
    var f0: RealWorld4_DataModel_07 = RealWorld4_DataModel_07()
    var f1: Boolean = Random().nextInt(1) > 0
    var f2: Int = Random().nextInt()
    var f3: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f4: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f5: Int = Random().nextInt()
    var f6: Color = Color(red=Random().nextInt(255), green=Random().nextInt(255), blue=Random().nextInt(255))
    var f7: Boolean = Random().nextInt(1) > 0
    var f8: Int = Random().nextInt()
    var f9: Int = Random().nextInt()
    var f10: RealWorld4_DataModel_07 = RealWorld4_DataModel_07()
    var f11: Int = Random().nextInt()
    var f12: Color = Color(red=Random().nextInt(255), green=Random().nextInt(255), blue=Random().nextInt(255))
    var f13: Color = Color(red=Random().nextInt(255), green=Random().nextInt(255), blue=Random().nextInt(255))
 }

@Model
class RealWorld4_DataModel_07() {
    var f0: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f1: Boolean = Random().nextInt(1) > 0
    var f2: Boolean = Random().nextInt(1) > 0
    var f3: Boolean = Random().nextInt(1) > 0
    var f4: Int = Random().nextInt()
    var f5: RealWorld4_DataModel_08 = RealWorld4_DataModel_08()
    var f6: Int = Random().nextInt()
    var f7: RealWorld4_DataModel_08 = RealWorld4_DataModel_08()
    var f8: Color = Color(red=Random().nextInt(255), green=Random().nextInt(255), blue=Random().nextInt(255))
 }

@Model
class RealWorld4_DataModel_08() {
    var f0: RealWorld4_DataModel_09 = RealWorld4_DataModel_09()
    var f1: Int = Random().nextInt()
    var f2: Int = Random().nextInt()
    var f3: RealWorld4_DataModel_09 = RealWorld4_DataModel_09()
    var f4: Color = Color(red=Random().nextInt(255), green=Random().nextInt(255), blue=Random().nextInt(255))
    var f5: Boolean = Random().nextInt(1) > 0
 }

@Model
class RealWorld4_DataModel_05() {
    var f0: Boolean = Random().nextInt(1) > 0
    var f1: Int = Random().nextInt()
    var f2: Boolean = Random().nextInt(1) > 0
    var f3: RealWorld4_DataModel_06 = RealWorld4_DataModel_06()
    var f4: RealWorld4_DataModel_06 = RealWorld4_DataModel_06()
    var f5: String = smallRange().map { createSomeText() }.joinToString("\n")
 }

@Model
class RealWorld4_DataModel_00() {
    var f0: Boolean = Random().nextInt(1) > 0
    var f1: Int = Random().nextInt()
    var f2: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f3: Boolean = Random().nextInt(1) > 0
    var f4: Color = Color(red=Random().nextInt(255), green=Random().nextInt(255), blue=Random().nextInt(255))
    var f5: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f6: RealWorld4_DataModel_01 = RealWorld4_DataModel_01()
    var f7: Boolean = Random().nextInt(1) > 0
    var f8: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f9: Color = Color(red=Random().nextInt(255), green=Random().nextInt(255), blue=Random().nextInt(255))
    var f10: Boolean = Random().nextInt(1) > 0
    var f11: Int = Random().nextInt()
    var f12: RealWorld4_DataModel_01 = RealWorld4_DataModel_01()
    var f13: String = smallRange().map { createSomeText() }.joinToString("\n")
 }

@Model
class RealWorld4_DataModel_02() {
    var f0: Int = Random().nextInt()
    var f1: Color = Color(red=Random().nextInt(255), green=Random().nextInt(255), blue=Random().nextInt(255))
    var f2: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f3: Int = Random().nextInt()
    var f4: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f5: Boolean = Random().nextInt(1) > 0
    var f6: Color = Color(red=Random().nextInt(255), green=Random().nextInt(255), blue=Random().nextInt(255))
    var f7: RealWorld4_DataModel_03 = RealWorld4_DataModel_03()
    var f8: Int = Random().nextInt()
    var f9: Boolean = Random().nextInt(1) > 0
    var f10: Boolean = Random().nextInt(1) > 0
    var f11: RealWorld4_DataModel_03 = RealWorld4_DataModel_03()
    var f12: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f13: Boolean = Random().nextInt(1) > 0
    var f14: Color = Color(red=Random().nextInt(255), green=Random().nextInt(255), blue=Random().nextInt(255))
    var f15: Boolean = Random().nextInt(1) > 0
 }

@Model
class RealWorld4_DataModel_04() {
    var f0: RealWorld4_DataModel_05 = RealWorld4_DataModel_05()
    var f1: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f2: Boolean = Random().nextInt(1) > 0
    var f3: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f4: Int = Random().nextInt()
    var f5: Boolean = Random().nextInt(1) > 0
    var f6: Int = Random().nextInt()
    var f7: Int = Random().nextInt()
    var f8: Boolean = Random().nextInt(1) > 0
    var f9: Int = Random().nextInt()
    var f10: Int = Random().nextInt()
    var f11: RealWorld4_DataModel_05 = RealWorld4_DataModel_05()
    var f12: Int = Random().nextInt()
    var f13: Color = Color(red=Random().nextInt(255), green=Random().nextInt(255), blue=Random().nextInt(255))
 }

@Model
class RealWorld4_DataModel_01() {
    var f0: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f1: Boolean = Random().nextInt(1) > 0
    var f2: RealWorld4_DataModel_02 = RealWorld4_DataModel_02()
    var f3: RealWorld4_DataModel_02 = RealWorld4_DataModel_02()
    var f4: Int = Random().nextInt()
    var f5: Int = Random().nextInt()
 }

@Model
class RealWorld4_DataModel_03() {
    var f0: Color = Color(red=Random().nextInt(255), green=Random().nextInt(255), blue=Random().nextInt(255))
    var f1: RealWorld4_DataModel_04 = RealWorld4_DataModel_04()
    var f2: Boolean = Random().nextInt(1) > 0
    var f3: RealWorld4_DataModel_04 = RealWorld4_DataModel_04()
    var f4: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f5: Color = Color(red=Random().nextInt(255), green=Random().nextInt(255), blue=Random().nextInt(255))
 }
