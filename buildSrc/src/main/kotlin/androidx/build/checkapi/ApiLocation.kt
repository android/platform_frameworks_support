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

package androidx.build.checkapi

import java.io.File

import androidx.build.Version

// An ApiLocation contains the filepath of a public API and restricted API of a library
data class ApiLocation (
    // file specifying the public API of the library
    val publicApiFile: File,
    // file specifying the restricted API (marked by the RestrictTo annotation) of the library
    val restrictedApiFile: File
) {

    fun files() = listOf(publicApiFile, restrictedApiFile)

    fun version(): Version? {
        val text = publicApiFile.name.removeSuffix(".txt")
        if (text == "current") {
            return null
        }
        return Version(text)
    }

    companion object {
        fun fromPublicApiFile(f: File): ApiLocation {
            return ApiLocation(f, File(f.parentFile, "restricted_" + f.name))
        }
    }
}

// An ApiTrackingStatus records the API of a library and possibly a list of known violations
// (to the api-tracking SemVer policy) that we want to ignore
data class ApiTrackingStatus (
    // specifies the API of the library
    val api: ApiLocation,
    // specifies known violations (to the api-tracking SemVer policy) that we want to ignore
    val exclusions: ApiLocation
) {

    fun files(): List<File> {
        return api.files() + exclusions.files()
    }

    companion object {
        fun fromApi(apiLocation: ApiLocation): ApiTrackingStatus {
            val publicExclusionsFile = File(apiLocation.publicApiFile.toString().removeSuffix(".txt") + ".ignore")
            val restrictedExclusionsFile = File(apiLocation.restrictedApiFile.parentFile.toString().removeSuffix(".txt") + ".ignore")
            val exclusions = ApiLocation(publicExclusionsFile, restrictedExclusionsFile)
            return ApiTrackingStatus(apiLocation, exclusions)
        }
    }

}
