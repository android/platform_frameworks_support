/***
 * Calling {@code includeProject(name, filePath)} is shorthand for:
 * <pre>
 *   include(name)
 *   project(name).projectDir = filePath
 * </pre>
 * <p>
 * Note that {@code <name>} directly controls the Gradle project name and must be unique within
 * AndroidX. This parameter also indirectly sets:
 * <ul>
 * <li>the project name in the IDE
 * <li>the Maven artifactId
 * </ul>
 */
fun includeProject(artifactId: String, filePath: File) {
    val name = ":$artifactId"
    include(name)
    project(name).projectDir = filePath
}

/**
 * Includes a project within the AndroidX project root.
 *
 * @see includeProject(String, File)
 */
fun includeProject(artifactId: String, filePath: String) {
    includeProject(artifactId, File(filePath))
}

// Jetpack libraries
includeProject("activity", "activity")
includeProject("activity-ktx", "activity/ktx")
includeProject("activity:integration-tests:testapp",
    "activity/integration-tests/testapp")
includeProject("annotation", "annotations")
includeProject("animation", "animation")
includeProject("animation:testing", "animation/testing")
includeProject("animation:integration-tests:testapp",
    "animation/integration-tests/testapp")
includeProject("appcompat", "appcompat")
includeProject("appcompat:appcompat-resources", "appcompat/resources")
includeProject("arch:core-common", "arch/core-common")
includeProject("arch:core-testing", "arch/core-testing")
includeProject("arch:core-runtime", "arch/core-runtime")
includeProject("asynclayoutinflater", "asynclayoutinflater")
includeProject("benchmark", "benchmark")
includeProject("biometric", "biometric")
includeProject("browser", "browser")
includeProject("car", "car/core")
includeProject("car-cluster", "car/cluster")
includeProject("car-moderator", "car/moderator")
includeProject("cardview", "cardview")
includeProject("collection", "collection")
includeProject("collection-ktx", "collection/ktx")
includeProject("concurrent:concurrent-futures", "concurrent/futures")
includeProject("contentpager", "content")
includeProject("coordinatorlayout", "coordinatorlayout")
includeProject("core", "core")
includeProject("core-ktx", "core/ktx")
includeProject("cursoradapter", "cursoradapter")
includeProject("customview", "customview")
includeProject("documentfile", "documentfile")
includeProject("drawerlayout", "drawerlayout")
includeProject("dynamicanimation", "dynamic-animation")
includeProject("dynamicanimation-ktx", "dynamic-animation/ktx")
includeProject("emoji", "emoji/core")
includeProject("emoji-bundled", "emoji/bundled")
includeProject("emoji-appcompat", "emoji/appcompat")
includeProject("enterprise-feedback", "enterprise/feedback")
includeProject("exifinterface", "exifinterface")
includeProject("fragment", "fragment")
includeProject("fragment-ktx", "fragment/ktx")
includeProject("fragment-testing", "fragment/testing")
includeProject("gridlayout", "gridlayout")
includeProject("heifwriter", "heifwriter")
includeProject("interpolator", "interpolator")
includeProject("jetifier-core", "jetifier/jetifier/core")
includeProject("jetifier-processor", "jetifier/jetifier/processor")
includeProject("jetifier-gradle-plugin", "jetifier/jetifier/gradle-plugin")
includeProject("jetifier-standalone", "jetifier/jetifier/standalone")
includeProject("jetifier-preprocessor", "jetifier/jetifier/preprocessor")
includeProject("leanback", "leanback")
includeProject("leanback-preference", "leanback-preference")
includeProject("lifecycle:integration-tests:testapp",
    "lifecycle/integration-tests/testapp")
includeProject("lifecycle:lifecycle-common", "lifecycle/common")
includeProject("lifecycle:lifecycle-common-java8", "lifecycle/common-java8")
includeProject("lifecycle:lifecycle-compiler", "lifecycle/compiler")
includeProject("lifecycle:lifecycle-extensions", "lifecycle/extensions")
includeProject("lifecycle:lifecycle-livedata-core", "lifecycle/livedata-core")
includeProject("lifecycle:lifecycle-livedata-core-ktx", "lifecycle/livedata-core/ktx")
includeProject("lifecycle:lifecycle-livedata", "lifecycle/livedata")
includeProject("lifecycle:lifecycle-livedata-ktx", "lifecycle/livedata/ktx")
includeProject("lifecycle:lifecycle-process", "lifecycle/process")
includeProject("lifecycle:lifecycle-reactivestreams", "lifecycle/reactivestreams")
includeProject("lifecycle:lifecycle-reactivestreams-ktx",
    "lifecycle/reactivestreams/ktx")
includeProject("lifecycle:lifecycle-runtime", "lifecycle/runtime")
includeProject("lifecycle:lifecycle-service", "lifecycle/service")
includeProject("lifecycle:lifecycle-viewmodel", "lifecycle/viewmodel")
includeProject("lifecycle:lifecycle-viewmodel-ktx", "lifecycle/viewmodel/ktx")
includeProject("lifecycle:lifecycle-viewmodel-savedstate",
    "lifecycle/viewmodel-savedstate")
includeProject("loader", "loader")
includeProject("localbroadcastmanager", "localbroadcastmanager")
includeProject("media", "media")
includeProject("media2", "media2")
includeProject("media2-exoplayer", "media2/media2-exoplayer")
includeProject("media2-widget", "media2-widget")
includeProject("mediarouter", "mediarouter")
includeProject("navigation:navigation-benchmark", "navigation/benchmark")
includeProject("navigation:navigation-common", "navigation/common")
includeProject("navigation:navigation-common-ktx", "navigation/common/ktx")
includeProject("navigation:navigation-runtime", "navigation/runtime/")
includeProject("navigation:navigation-runtime-ktx", "navigation/runtime/ktx")
includeProject("navigation:navigation-testing", "navigation/testing")
includeProject("navigation:navigation-fragment", "navigation/fragment")
includeProject("navigation:navigation-fragment-ktx", "navigation/fragment/ktx")
includeProject("navigation:navigation-ui", "navigation/ui")
includeProject("navigation:navigation-ui-ktx", "navigation/ui/ktx")
includeProject("navigation:navigation-integration-tests:testapp",
    "navigation/integration-tests/testapp")
includeProject("navigation:navigation-safe-args-generator",
    "navigation/safe-args-generator")
includeProject("navigation:navigation-safe-args-gradle-plugin",
    "navigation/safe-args-gradle-plugin")
includeProject("paging:integration-tests:testapp", "paging/integration-tests/testapp")
includeProject("paging:paging-common", "paging/common")
includeProject("paging:paging-common-ktx", "paging/common/ktx")
includeProject("paging:paging-runtime", "paging/runtime")
includeProject("paging:paging-runtime-ktx", "paging/runtime/ktx")
includeProject("paging:paging-rxjava2", "paging/rxjava2")
includeProject("paging:paging-rxjava2-ktx", "paging/rxjava2/ktx")
includeProject("palette", "palette")
includeProject("palette-ktx", "palette/ktx")
includeProject("percentlayout", "percent")
includeProject("preference", "preference")
includeProject("preference-ktx", "preference/ktx")
includeProject("print", "print")
includeProject("recommendation", "recommendation")
includeProject("recyclerview", "recyclerview/recyclerview")
includeProject("recyclerview:recyclerview-benchmark", "recyclerview/benchmark")
includeProject("recyclerview-selection", "recyclerview/selection")
includeProject("room:integration-tests:room-testapp-noappcompat",
    "room/integration-tests/noappcompattestapp")
includeProject("room:integration-tests:room-testapp-autovalue",
    "room/integration-tests/autovaluetestapp")
includeProject("room:integration-tests:room-testapp",
    "room/integration-tests/testapp")
includeProject("room:integration-tests:room-testapp-kotlin",
    "room/integration-tests/kotlintestapp")
includeProject("room:room-benchmark", "room/benchmark")
includeProject("room:room-common", "room/common")
includeProject("room:room-compiler", "room/compiler")
includeProject("room:room-coroutines", "room/coroutines")
includeProject("room:room-guava", "room/guava")
includeProject("room:room-migration", "room/migration")
includeProject("room:room-runtime", "room/runtime")
includeProject("room:room-rxjava2", "room/rxjava2")
includeProject("room:room-testing", "room/testing")
includeProject("remotecallback-processor", "remotecallback/processor")
includeProject("remotecallback", "remotecallback")
includeProject("versionedparcelable-annotation", "versionedparcelable/annotation")
includeProject("versionedparcelable", "versionedparcelable")
includeProject("savedstate:savedstate-common", "savedstate/common")
includeProject("savedstate:savedstate-bundle", "savedstate/bundle")
includeProject("sharetarget", "sharetarget")
includeProject("sharetarget:integration-tests:testapp",
    "sharetarget/integration-tests/testapp")
includeProject("slice-core", "slices/core")
includeProject("slice-view", "slices/view")
includeProject("slice-builders", "slices/builders")
includeProject("slice-test", "slices/test")
includeProject("slice-builders-ktx", "slices/builders/ktx")
includeProject("slice-benchmark", "slices/benchmark")
includeProject("slidingpanelayout", "slidingpanelayout")
includeProject("sqlite:sqlite", "persistence/db")
includeProject("sqlite:sqlite-ktx", "persistence/db/ktx")
includeProject("sqlite:sqlite-framework", "persistence/db-framework")
includeProject("swiperefreshlayout", "swiperefreshlayout")
includeProject("textclassifier", "textclassifier")
includeProject("textclassifier:integration-tests:testapp",
    "textclassifier/integration-tests/testapp")
includeProject("transition", "transition")
includeProject("tvprovider", "tv-provider")
includeProject("vectordrawable", "graphics/drawable/static")
includeProject("vectordrawable-animated", "graphics/drawable/animated")
includeProject("viewpager", "viewpager")
includeProject("viewpager2", "viewpager2")
includeProject("wear", "wear")
includeProject("webkit", "webkit")
includeProject("webkit:integration-tests:testapp", "webkit/integration-tests/testapp")
includeProject("work:work-runtime", "work/workmanager")
includeProject("work:work-runtime-ktx", "work/workmanager-ktx")
includeProject("work:work-rxjava2", "work/workmanager-rxjava2")
includeProject("work:work-testing", "work/workmanager-testing")
includeProject("work:integration-tests:testapp", "work/integration-tests/testapp")

// Legacy libraries (only used for dejetify)
includeProject("legacy-support-core-ui", "legacy/core-ui")
includeProject("legacy-support-core-utils", "legacy/core-utils")
includeProject("legacy-support-v13", "legacy/v13")

// Legacy sample apps
// Note: don't add new samples/ apps. Instead, Create
// <module>/integration-tests/testapp in the "Libraries" section above.
val samplesRoot = File(rootDir, "samples")
includeProject("support-animation-demos", File(samplesRoot, "SupportAnimationDemos"))
includeProject("support-app-navigation", File(samplesRoot, "SupportAppNavigation"))
includeProject("support-biometric-demos", File(samplesRoot, "BiometricDemos"))
includeProject("support-car-demos", File(samplesRoot, "SupportCarDemos"))
includeProject("support-content-demos", File(samplesRoot, "SupportContentDemos"))
includeProject("support-design-demos", File(samplesRoot, "SupportDesignDemos"))
includeProject("support-emoji-demos", File(samplesRoot, "SupportEmojiDemos"))
includeProject("support-leanback-demos", File(samplesRoot, "SupportLeanbackDemos"))
includeProject("support-media-demos", File(samplesRoot, "SupportMediaDemos"))
includeProject("support-percent-demos", File(samplesRoot, "SupportPercentDemos"))
includeProject("support-preference-demos", File(samplesRoot, "SupportPreferenceDemos"))
includeProject("support-remotecallback-demos", File(samplesRoot, "SupportRemoteCallbackDemos"))
includeProject("support-slices-demos", File(samplesRoot, "SupportSliceDemos"))
includeProject("support-transition-demos", File(samplesRoot, "SupportTransitionDemos"))
includeProject("support-vector-drawable-demos", File(samplesRoot, "SupportVectorDrawableDemos"))
includeProject("support-v4-demos", File(samplesRoot, "Support4Demos"))
includeProject("support-v7-demos", File(samplesRoot, "Support7Demos"))
includeProject("support-v13-demos", File(samplesRoot, "Support13Demos"))
includeProject("support-wear-demos", File(samplesRoot, "SupportWearDemos"))
includeProject("viewpager2-demos", File(samplesRoot, "ViewPager2Demos"))

// Testing libraries
includeProject("internal-testutils", "testutils")
includeProject("internal-testutils-ktx", "testutils-ktx")

// Applications and libraries for tests
includeProject("support-media-compat-test-client",
    "media/version-compat-tests/current/client")
includeProject("support-media-compat-test-client-previous",
    "media/version-compat-tests/previous/client")
includeProject("support-media-compat-test-service",
    "media/version-compat-tests/current/service")
includeProject("support-media-compat-test-service-previous",
    "media/version-compat-tests/previous/service")
includeProject("support-media-compat-test-lib", "media/version-compat-tests/lib")
includeProject("support-media2-test-client",
    "media2/version-compat-tests/current/client")
includeProject("support-media2-test-service",
    "media2/version-compat-tests/current/service")
includeProject("support-media2-test-common", "media2/version-compat-tests/common")

// External projects
apply(from = "include-composite-deps.gradle")
val externalRoot = File(rootDir, "../../external")
includeProject("noto-emoji-compat", File(externalRoot, "noto-fonts/emoji-compat"))
includeProject("webview-support-interfaces", File(externalRoot, "webview_support_interfaces"))

// Fake project which is used for docs generation from prebuilts. We need a real Android project to
// generate the R.java, aidl, etc files referenced in source documentation.
if (!startParameter.projectProperties.containsKey("android.injected.invoked.from.ide")) {
    // We don't need it in ide, so we don't configure it there.
    includeProject("docs-fake", "docs-fake")
}

// Dumb test project that has a test for each size to ensure that at least one test is run
// for each size and test runner is happy when there is nothing to test.
includeProject("dumb-tests", "dumb-tests")
