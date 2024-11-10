/*
 * SPDX-FileCopyrightText: 2024 Niklas Wimmer <mail@nwimmer.me>
 * SPDX-License-Identifier: MIT-0
 */
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)

    id("uniffi")
}

/*
 * You can change the group to whatever you like, just make sure to also adjust your dependency
 * declaration if you do.
 */
group = "me.nwimmer.unofficial.iroh"
version = "0.28.1"

uniffi {
    cargoManifestDir.set(layout.projectDirectory.dir("iroh-ffi"))
    /*
     * Can not reuse upstream config because it does not include `bindings.kotlin.android = true`.
     */
    uniffiConfigFile.set(layout.projectDirectory.file("uniffi.toml"))
    libraryName.set("iroh_ffi")
    /*
     * This is a compromise between reducing the time it takes to build the library and the out of
     * the box experience when including this build script without modifying it.
     * Chances are your personal mobile phone's CPU supports 64-bit (in fact, it may *not* have
     * support for 32-bit applications if it is a recent one) and your development machine supports
     * 64-bit as well. Therefore, the library is only compiled for 64-bit targets by default under
     * the assumption that this will cause no problem for most people. The benefit is that build
     * times are almost halved.
     * If you want maximum compatibility or just have other ABI requirements, you will need to
     * modify this line (or remove it to build for all four targets).
     */
    targets.set(listOf(Target.ARM64, Target.X86_64))
}

java {
    toolchain {
        /*
         * Java 21 toolchains have only deprecated support for a targeting Java 8, to get rid of
         * that warning force the toolchain to be the latest LTS version that still has full support
         * for Java 8.
         */
        languageVersion = JavaLanguageVersion.of(JavaVersion.VERSION_17.majorVersion)
    }
}

android {
    namespace = "${project.group}"

    compileSdk = 35

    defaultConfig {
        /*
         * By increasing this you may be able to also increase the target Java version and disable
         * desugaring. Consult Android documentation for more info or just try what builds and what
         * fails with an error.
         */
        minSdk = 24
        version = project.version
    }

    compileOptions {
        // see comment on kotlinOptions.jvmTarget
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8

        // parts of the generated code need to be desugared to be compatible with our minSdk
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        /*
         * Android SDK version 24 (i.e. Android 7 "Nougat"), the current minSdk, only supports
         * Java 8.
         */
        jvmTarget = "${JavaVersion.VERSION_1_8}"
    }
}

dependencies {
    /*
     * These three libraries are required by UniFFI.
     */
    implementation(libs.jna) {
        artifact {
            name = "jna"
            type = "aar"
        }
    }
    implementation(libs.kotlinx.coroutines)
    implementation(libs.androidx.annotation)

    coreLibraryDesugaring(libs.android.desugar)
}
