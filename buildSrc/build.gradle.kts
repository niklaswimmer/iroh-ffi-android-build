/*
 * SPDX-FileCopyrightText: 2024 Niklas Wimmer <mail@nwimmer.me>
 * SPDX-License-Identifier: MIT-0
 */
plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        create("uniffi") {
            id = "uniffi"
            implementationClass = "UniffiPlugin"
        }
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
    implementation(libs.buildsrc.agp)
}
