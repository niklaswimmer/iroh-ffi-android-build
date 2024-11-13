/*
 * SPDX-FileCopyrightText: 2019-2021 Brainium Studios LLC
 * SPDX-FileCopyrightText: 2024 Block, Inc.
 * SPDX-FileCopyrightText: 2024 Niklas Wimmer
 * SPDX-License-Identifier: MIT AND LicenseRef-Block-MIT-CC
 *
 * See the following links for the source code this work was derived from (as of 2024-11-10):
 * https://github.com/BrainiumLLC/cargo-mobile/blob/38e48c3373b23e90ab0392a651925f67af48076e/templates/platforms/android-studio/buildSrc/src/main/kotlin/BuildTask.kt.hbs
 * https://github.com/proto-at-block/bitkey/blob/694c152387c1fdb2b6be01ba35e0a9c092a81879/app/gradle/build-logic/src/main/kotlin/build/wallet/gradle/logic/rust/task/CompileRustForJvmTask.kt
 * https://github.com/proto-at-block/bitkey/blob/694c152387c1fdb2b6be01ba35e0a9c092a81879/app/gradle/build-logic/src/main/kotlin/build/wallet/gradle/logic/rust/task/CompileRustForAndroidTask.kt
 */
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem

internal enum class CargoProfile(val cargoProfileName: String, val outputDirectoryName: String) {
    Debug("dev", "debug"),
    Release("release", "release"),
    ;

    val flavorName: String
        get() = this.name
}

internal abstract class CargoBuildTask : DefaultTask() {

    @get:Internal
    abstract val cargoManifestDir: DirectoryProperty

    @get:Input
    abstract val libraryName: Property<String>

    @get:Input
    abstract val profile: Property<CargoProfile>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    abstract val intermediateTargetRootDir: DirectoryProperty

    init {
        this.inputs.file(this.cargoManifestDir.file("Cargo.toml"))
        this.inputs.dir(this.cargoManifestDir.dir("src"))
    }

    @TaskAction
    fun build() {
        project.exec {
            workingDir(this@CargoBuildTask.cargoManifestDir)
            executable("cargo")
            args(
                "build",
                "--lib",
                "--profile",
                this@CargoBuildTask.profile.get().cargoProfileName,
                "--target-dir",
                this@CargoBuildTask.intermediateTargetRootDir.get().asFile.absolutePath
            )
            if (project.logger.isEnabled(LogLevel.DEBUG)) {
                args("-vv")
            } else if (project.logger.isEnabled(LogLevel.INFO)) {
                args("-v")
            }
        }.assertNormalExitValue()

        val lib = this.intermediateTargetRootDir.get()
            .dir(this.profile.get().outputDirectoryName)
            .file(OperatingSystem.current().getSharedLibraryName(this.libraryName.get()))

        project.copy {
            from(lib)
            into(this@CargoBuildTask.outputDirectory)
        }
    }
}

internal abstract class CargoNdkBuildTask : DefaultTask() {
    @get:Internal
    abstract val cargoManifestDir: DirectoryProperty

    @get:Input
    abstract val targetAbi: Property<String>

    @get:Input
    abstract val release: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        this.inputs.file(this.cargoManifestDir.file("Cargo.toml"))
        this.inputs.dir(this.cargoManifestDir.dir("src"))
    }

    @TaskAction
    fun build() {
        project.exec {
            workingDir(this@CargoNdkBuildTask.cargoManifestDir)
            executable("cargo")
            args(
                "ndk",
                "-t",
                this@CargoNdkBuildTask.targetAbi.get(),
                "-o",
                this@CargoNdkBuildTask.outputDirectory.get().asFile.absolutePath,
                "build",
            )
            if (project.logger.isEnabled(LogLevel.DEBUG)) {
                args("-vv")
            } else if (project.logger.isEnabled(LogLevel.INFO)) {
                args("-v")
            }
            if (this@CargoNdkBuildTask.release.get()) {
                args("--release")
            }
        }.assertNormalExitValue()
    }
}
