/*
 * SPDX-FileCopyrightText: 2024 Niklas Wimmer <mail@nwimmer.me>
 * SPDX-License-Identifier: MIT-0
 */
package me.nwimmer.unofficial.iroh.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * A simple copy task that copies all files in the directory specified by [from] to the location
 * specified by [into].
 *
 * If possible, use Gradle's own [Copy][org.gradle.api.tasks.Copy] task instead. This task only
 * exists because the `addGeneratedSourceDirectory` in AGP source sets only works with tasks that
 * use properties for configuration.
 */
@DisableCachingByDefault(because = "Copy is not worth caching")
abstract class CopyTask : DefaultTask() {

    @get:InputDirectory
    abstract val from: DirectoryProperty

    @get:OutputDirectory
    abstract val into: DirectoryProperty

    @TaskAction
    fun copy() {
        this.didWork = project.copy {
            from(this@CopyTask.from)
            into(this@CopyTask.into)
        }.didWork
    }
}
