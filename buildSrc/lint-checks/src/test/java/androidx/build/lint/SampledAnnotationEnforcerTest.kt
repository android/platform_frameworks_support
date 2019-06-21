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

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.ProjectDescription
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

/**
 * Test for [SampledAnnotationEnforcer]
 *
 * This tests (with Parameterized) the two following module setups:
 *
 * Module 'foo', which lives in foo
 * Module 'foo:integration-tests:samples', which lives in foo/integration-tests/samples,
 * and depends on 'foo'
 *
 * Module 'foo:foo', which lives in foo/foo
 * Module 'foo:integration-tests:samples', which lives in foo/integration-tests/samples,
 * and depends on 'foo:foo'
 */
@RunWith(Parameterized::class)
class SampledAnnotationEnforcerTest {

    companion object {
        @JvmStatic
        @Parameters(name = "sourceModule={0}")
        fun moduleNames(): Array<String> {
            return arrayOf("foo", "foo:foo")
        }
    }

    // At runtime this contains one of the values listed in moduleNames()
    @Parameter lateinit var fooModuleName: String

    private val sampleModuleName = "foo:integration-tests:samples"

    private val missingSampleFile = kotlin("""
            package foo.samples
        """)

    private val unannotatedSampleFile = kotlin("""
            package foo.samples

            fun sampleBar() {}
        """)

    private val correctlyAnnotatedSampleFile = kotlin("""
            package foo.samples

            @Sampled
            fun sampleBar() {}
        """)

    private fun checkKotlin(fooFile: TestFile? = null, sampleFile: TestFile): TestLintResult {
        val fooProject = ProjectDescription().apply {
            name = fooModuleName
            fooFile?.let { files = arrayOf(fooFile) }
        }
        val sampleProject = ProjectDescription().apply {
            name = sampleModuleName
            files = arrayOf(sampleFile)
            dependsOn(fooProject)
        }
        return lint()
            .projects(fooProject, sampleProject)
            .allowMissingSdk(true)
            .issues(
                SampledAnnotationEnforcer.MISSING_SAMPLED_ANNOTATION,
                SampledAnnotationEnforcer.OBSOLETE_SAMPLED_ANNOTATION
            )
            .run()
    }

    @Test
    fun orphanedSampleFunction() {
        val sampleFile = correctlyAnnotatedSampleFile

        val path = if (fooModuleName == moduleNames()[0]) { "" } else { "foo" }

        val expected =
"$path:integration-tests:samples/src/foo/samples/test.kt:5: Error: sampleBar is annotated with" +
""" @Sampled, but is not linked to from a @sample tag. [EnforceSampledAnnotation]
            fun sampleBar() {}
                ~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun missingSampleFunction_Function() {
        val fooFile = kotlin("""
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              fun bar() {}
            }
        """)

        val sampleFile = missingSampleFile

        val expectedException =
            "Message: Couldn't find a valid function matching foo.samples.sampleBar"

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expectMatches(expectedException)
    }

    @Test
    fun unannotatedSampleFunction_Function() {
        val fooFile = kotlin("""
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              fun bar() {}
            }
        """)

        val sampleFile = unannotatedSampleFile

        val path = if (fooModuleName == moduleNames()[0]) { "" } else { "foo:foo/" }

        val expected =
"${path}src/foo/Bar.kt:6: Error: sampleBar is not annotated with @Sampled, but is linked to from" +
""" the KDoc of bar [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun correctlyAnnotatedSampleFunction_Function() {
        val fooFile = kotlin("""
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              fun bar() {}
            }
        """)

        val sampleFile = correctlyAnnotatedSampleFile

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expectClean()
    }

    @Test
    fun missingSampleFunction_Class() {
        val fooFile = kotlin("""
            package foo

            /**
             * @sample foo.samples.sampleBar
             */
            class Bar {}
        """)

        val sampleFile = missingSampleFile

        val expectedException =
            "Message: Couldn't find a valid function matching foo.samples.sampleBar"

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expectMatches(expectedException)
    }

    @Test
    fun unannotatedSampleFunction_Class() {
        val fooFile = kotlin("""
            package foo

            /**
             * @sample foo.samples.sampleBar
             */
            class Bar {}
        """)

        val sampleFile = unannotatedSampleFile

        val path = if (fooModuleName == moduleNames()[0]) { "" } else { "foo:foo/" }

        val expected =
"${path}src/foo/Bar.kt:5: Error: sampleBar is not annotated with @Sampled, but is linked to from" +
""" the KDoc of Bar [EnforceSampledAnnotation]
             * @sample foo.samples.sampleBar
               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun correctlyAnnotatedSampleFunction_Class() {
        val fooFile = kotlin("""
            package foo

            /**
             * @sample foo.samples.sampleBar
             */
            class Bar {}
        """)

        val sampleFile = correctlyAnnotatedSampleFile

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expectClean()
    }

    @Test
    fun missingSampleFunction_Field() {
        val fooFile = kotlin("""
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              const val bar = 0
            }
        """)

        val sampleFile = missingSampleFile

        val expectedException =
            "Message: Couldn't find a valid function matching foo.samples.sampleBar"

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expectMatches(expectedException)
    }

    @Test
    fun unannotatedSampleFunction_Field() {
        val fooFile = kotlin("""
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              const val bar = 0
            }
        """)

        val sampleFile = unannotatedSampleFile

        val path = if (fooModuleName == moduleNames()[0]) { "" } else { "foo:foo/" }

        val expected =
"${path}src/foo/Bar.kt:6: Error: sampleBar is not annotated with @Sampled, but is linked to from" +
""" the KDoc of bar [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun correctlyAnnotatedSampleFunction_Field() {
        val fooFile = kotlin("""
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              const val bar = 0
            }
        """)

        val sampleFile = correctlyAnnotatedSampleFile

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expectClean()
    }
}
