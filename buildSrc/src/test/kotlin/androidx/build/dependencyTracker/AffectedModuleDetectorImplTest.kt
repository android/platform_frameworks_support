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

package androidx.build.dependencyTracker

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class AffectedModuleDetectorImplTest {
    @Rule
    @JvmField
    val attachLogsRule = AttachLogsTestRule()
    private val logger = attachLogsRule.logger
    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

    private lateinit var root: Project
    private lateinit var p1: Project
    private lateinit var p2: Project

    @Before
    fun init() {
        val tmpDir = tmpFolder.root

        root = ProjectBuilder.builder()
                .withProjectDir(tmpDir)
                .withName("root")
                .build()
        p1 = ProjectBuilder.builder()
                .withProjectDir(tmpDir.resolve("p1"))
                .withName("p1")
                .withParent(root)
                .build()
        p2 = ProjectBuilder.builder()
                .withProjectDir(tmpDir.resolve("p2"))
                .withName("p2")
                .withParent(root)
                .build()
    }

    @Test
    fun noChangeCLs() {
        val detector = AffectedModuleDetectorImpl(
                rootProject = root,
                logger = logger,
                ignoreUnknownProjects = false,
                injectedGitClient = MockGitClient(
                        lastMergeSha = "foo",
                        changedFiles = emptyList())
        )
        MatcherAssert.assertThat(detector.affectedProjects, CoreMatchers.`is`(
                setOf(p1, p2)
        ))
    }

    @Test
    fun changeInOne() {
        val detector = AffectedModuleDetectorImpl(
                rootProject = root,
                logger = logger,
                ignoreUnknownProjects = false,
                injectedGitClient = MockGitClient(
                        lastMergeSha = "foo",
                        changedFiles = listOf(convertToFilePath("p1", "foo.java")))
        )
        MatcherAssert.assertThat(detector.affectedProjects, CoreMatchers.`is`(
                setOf(p1)
        ))
    }

    @Test
    fun changeInBoth() {
        val detector = AffectedModuleDetectorImpl(
                rootProject = root,
                logger = logger,
                ignoreUnknownProjects = false,
                injectedGitClient = MockGitClient(
                        lastMergeSha = "foo",
                        changedFiles = listOf(
                                convertToFilePath("p1", "foo.java"),
                                convertToFilePath("p2", "bar.java")))
        )
        MatcherAssert.assertThat(detector.affectedProjects, CoreMatchers.`is`(
                setOf(p1, p2)
        ))
    }

    @Test
    fun changeInRoot() {
        val detector = AffectedModuleDetectorImpl(
                rootProject = root,
                logger = logger,
                ignoreUnknownProjects = false,
                injectedGitClient = MockGitClient(
                        lastMergeSha = "foo",
                        changedFiles = listOf("foo.java"))
        )
        MatcherAssert.assertThat(detector.affectedProjects, CoreMatchers.`is`(
                setOf(p1, p2)
        ))
    }

    @Test(expected = IllegalStateException::class)
    fun changeInCobuiltOnlyChangedMissingCobuilt() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            cobuiltTestPaths = setOf(setOf("cobuilt1", "cobuilt2", "cobuilt3")),
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(convertToFilePath(
                    "p8", "foo.java")))
        )
        // This should trigger IllegalStateException due to missing cobuilt3
        detector.affectedProjects
    }

    @Test
    fun changeInCobuiltOnlyChangedAllCobuiltsMissing() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            cobuiltTestPaths = setOf(setOf("cobuilt3", "cobuilt4", "cobuilt5")),
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(convertToFilePath(
                    "p8", "foo.java")))
        )
        // There should be no exception thrown here because *all* cobuilts are missing.
        MatcherAssert.assertThat(detector.affectedProjects, CoreMatchers.`is`(
            setOf(p8, p10)
        ))
    }

    // For both Linux/Windows
    fun convertToFilePath(vararg list: String): String {
        return list.toList().joinToString(File.separator)
    }

    private class MockGitClient(
        val lastMergeSha: String?,
        val changedFiles: List<String>
    ) : GitClient {
        override fun findChangedFilesSince(
            sha: String,
            top: String,
            includeUncommitted: Boolean
        ) = changedFiles

        override fun findPreviousMergeCL() = lastMergeSha
    }
}
