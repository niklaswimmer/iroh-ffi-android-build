/*
 * SPDX-FileCopyrightText: 2024 Block, Inc.
 * SPDX-FileCopyrightText: 2024 Niklas Wimmer
 * SPDX-License-Identifier: MIT AND LicenseRef-Block-MIT-CC
 *
 * See the following links for the source code this work was derived from (as of 2024-11-10):
 * https://github.com/proto-at-block/bitkey/blob/main/app/gradle/build-logic/src/main/kotlin/build/wallet/gradle/logic/rust/task/GenerateKotlinRustBindingsTask.kt
 */
package me.nwimmer.unofficial.iroh.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

internal abstract class GenerateUniffiBindingsTask : DefaultTask() {

    @get:InputDirectory
    abstract val cargoManifestDir: DirectoryProperty

    @get:InputFile
    abstract val libraryWithDebugSymbols: RegularFileProperty

    @get:Input
    abstract val binName: Property<String>

    @get:InputFile
    abstract val uniffiConfigFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        this.binName.convention("uniffi-bindgen")
        this.uniffiConfigFile.convention(this.cargoManifestDir.file("uniffi.toml"))
        this.outputDirectory.convention(this.project.layout.buildDirectory.dir("generated/uniffi/bindings"))
    }

    @TaskAction
    fun generate() {
        project.exec {
            workingDir(this@GenerateUniffiBindingsTask.cargoManifestDir)
            executable("cargo")
            args(
                "run",
                "--bin",
                this@GenerateUniffiBindingsTask.binName.get(),
                "--",
                "generate",
                "--language",
                "kotlin",
                /*
                 * TODO the command just hang with formatting enabled, so it is disabled for now.
                 *  Formatting would be nice for a sources JAR, the default output already looks
                 *  decent so this is not a hard requirement for publishing useful sources.
                 *  Maybe it is possible to enable this in CI only, if it turns out that it works
                 *  there.
                 */
                "--no-format",
                "--library",
                this@GenerateUniffiBindingsTask.libraryWithDebugSymbols.get().asFile.absolutePath,
                "--out-dir",
                this@GenerateUniffiBindingsTask.outputDirectory.get().asFile.absolutePath,
                "--config",
                this@GenerateUniffiBindingsTask.uniffiConfigFile.get().asFile.absolutePath
            )
        }.assertNormalExitValue()
    }
}
