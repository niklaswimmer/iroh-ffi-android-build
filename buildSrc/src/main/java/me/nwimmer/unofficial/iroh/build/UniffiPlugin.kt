/*
 * SPDX-FileCopyrightText: 2019-2021 Brainium Studios LLC
 * SPDX-FileCopyrightText: 2024 Niklas Wimmer
 * SPDX-License-Identifier: MIT
 *
 * See the following links for the source code this work was derived from (as of 2024-11-10):
 * https://github.com/BrainiumLLC/cargo-mobile/blob/38e48c3373b23e90ab0392a651925f67af48076e/templates/platforms/android-studio/buildSrc/src/main/kotlin/RustPlugin.kt.hbs
 */
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import me.nwimmer.unofficial.iroh.build.CopyTask
import me.nwimmer.unofficial.iroh.build.GenerateUniffiBindingsTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.configure

const val TASK_GROUP = "uniffi"

enum class Target(
    val abi: String,
    val arch: String,
) {
    ARM64("arm64-v8a", "arm64"),
    ARM("armeabi-v7a", "arm"),
    X86("x86", "x86"),
    X86_64("x86_64", "x86_64"),
}

interface UniffiPluginExtension {
    /**
     * The directory containing the Cargo manifest file (`Cargo.toml`) for the Rust crate that
     * should be build.
     *
     * The plugin does not support Cargo workspaces, the manifest must be for a single package.
     * Additionally, the package must provide a binary configuration as described in
     * [Uniffi's documentation](https://mozilla.github.io/uniffi-rs/latest/tutorial/foreign_language_bindings.html#creating-the-bindgen-binary).
     * The name of this binary must be `uniffi-bindgen`.
     */
    val cargoManifestDir: DirectoryProperty

    /**
     * Path to the `uniffi.toml` that should be used when generating the Kotlin bindings. This file
     * needs to at least declare `bindings.kotlin.android = true`, otherwise the generated code will
     * not compile.
     */
    val uniffiConfigFile: RegularFileProperty

    /**
     * The name of the library as specified in the `[lib]` section of the `Cargo.toml`. The plugin
     * needs this information for finding the correct shared library on the filesystem after it has
     * been build by Cargo.
     */
    val libraryName: Property<String>

    /**
     * List of Android ABIs that should be used as build targets for the native library.
     *
     * By default, a native library for all four ABIs will be built.
     *
     * See also the [Android developer documentation on available target ABIs](https://developer.android.com/ndk/guides/abis).
     */
    val targets: ListProperty<Target>
}

/**
 * A Gradle plugin that can automatically build a Rust crate for configured Android ABI targets and
 * generate the appropiate Kotlin bindings using [UniFFI](https://mozilla.github.io/uniffi-rs/latest/).
 */
open class UniffiPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        val ext = extensions.create("uniffi", UniffiPluginExtension::class.java)

        ext.targets.convention(Target.values().asIterable())

        val buildHostLibraryTask = tasks.register(
            "buildHostLibraryWithDebugSymbols",
            CargoBuildTask::class.java
        ) {
            group = TASK_GROUP
            description =
                "Build native library in debug mode for the host target for consumption by UniFFI"

            cargoManifestDir.set(ext.cargoManifestDir)
            libraryName.set(ext.libraryName)
            profile.set(CargoProfile.Debug)

            intermediateTargetRootDir.set(ext.cargoManifestDir.dir("target/"))

            outputDirectory.set(layout.buildDirectory.dir("uniffi/target/"))
        }
        val generateBindingsTask = tasks.register(
            "generateUniffiKotlinBindings",
            GenerateUniffiBindingsTask::class.java
        ) {
            group = TASK_GROUP
            description = "Generate bindings to the native library using UniFFI"

            cargoManifestDir.set(ext.cargoManifestDir)
            uniffiConfigFile.set(ext.uniffiConfigFile)

            libraryWithDebugSymbols.set(buildHostLibraryTask.flatMap { task ->
                task.outputDirectory.file(ext.libraryName.map {
                    OperatingSystem.current().getSharedLibraryName(it)
                })
            })
        }

        val debugNativeBuildTasks = registerNativeBuildTasks(ext, "debug")
        val releaseNativeBuildTasks = registerNativeBuildTasks(ext, "release")

        extensions.configure<LibraryAndroidComponentsExtension> {
            onVariants { variant ->
                variant.sources.java?.addGeneratedSourceDirectory(
                    tasks.register(
                        "copyBindings${variant.name.capitalized()}",
                        CopyTask::class.java
                    ) {
                        from.set(generateBindingsTask.flatMap(GenerateUniffiBindingsTask::outputDirectory))
                    },
                    CopyTask::into,
                )


                val nativeBuildTasks = when (variant.buildType) {
                    "release" -> releaseNativeBuildTasks
                    else -> debugNativeBuildTasks
                }

                nativeBuildTasks.entries.forEach {
                    variant.sources.jniLibs?.addGeneratedSourceDirectory(
                        tasks.register(
                            "copyNativeLibrary${it.key.arch.capitalized()}${variant.name.capitalized()}",
                            CopyTask::class.java
                        ) {
                            from.set(it.value.flatMap(CargoNdkBuildTask::outputDirectory))
                        },
                        CopyTask::into,
                    )
                }
            }
        }
    }

    private fun Project.registerNativeBuildTasks(
        ext: UniffiPluginExtension,
        profile: String
    ): Map<Target, TaskProvider<CargoNdkBuildTask>> =
        ext.targets.get()
            .associateWith { target ->
                val variant = "${target.arch}${profile.capitalized()}"
                tasks.register(
                    "buildNative${variant.capitalized()}",
                    CargoNdkBuildTask::class.java
                ) {
                    group = TASK_GROUP
                    description = "Build native library in $profile mode for ${target.arch}"

                    cargoManifestDir.set(ext.cargoManifestDir)
                    targetAbi.set(target.abi)
                    release.set(profile == "release")

                    outputDirectory.set(layout.buildDirectory.dir("generated/uniffi/native/${variant}"))
                }
            }
}
