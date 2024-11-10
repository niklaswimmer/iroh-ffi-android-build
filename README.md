<!--
SPDX-FileCopyrightText: 2024 Niklas Wimmer <mail@nwimmer.me>
SPDX-License-Identifier: MIT-0
-->
# Android Build for Iroh-FFI

This project publishes Gradle build logic that can be used to build the [Iroh-FFI](https://github.com/n0-computer/iroh-ffi) library and Kotlin bindings for Android as part of your normal Gradle build process. This allows you to run `./gradlew build` and have Gradle automatically build the Iroh-FFI native library using [`cargo-ndk`](https://github.com/bbqsrc/cargo-ndk) and generate the appropiate Kotlin bindings for use in Android projects.

**This project in an intermediate solution, until we have a solution for publishing Iroh-FFI libraries upstream. Best case scenario is if this project becomes obselete.**

### Usage

You basically have two options, you can either copy the full repository and use `includeBuild` in your root Gradle project or you can copy just the `buildSrc` source and integrate them directly into your build system. Note that the build logic does not take care of downloading the `iroh-ffi` repository, it expects to be already present locally (this project contains the Iroh-FFI repository as a submodule, but depending on how you copy this project into yours you may still have to take care of that yourself).

The easiest way to make use of this project is to add it as a Git submodule to your Git repository:

```bash
git submodule add https://github.com/niklaswimmer/iroh-ffi-android-build iroh-ffi
```

You can then modify your root `settings.gradle.kts` file to include the following line:

```kotlin
includeBuild("iroh-ffi/")
```

For more information on `includeBuild` please consult the [Gradle documentation](https://docs.gradle.org/current/userguide/composite_builds.html).

Lastly, you need to declare a dependency on the `iroh-ffi` library in your application's `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("me.nwimmer.unofficial.iroh:iroh:0.28.1")
}
```

Or, alternatively, in your version catalog file (`libs.versions.toml` or similar):

```toml
[versions]
iroh = "0.28.1"

[libraries]
iroh = { module = "me.nwimmer.unofficial.iroh:iroh", version.ref = "iroh" }
```

### License

All files include a header specifying their copyright and the licenses they are distributed under. Copies of all licenses used by this project are available in the [`LICENSES`](./LICENSES) folder. This project is compliant with version 3.2 of the [REUSE](https://reuse.software) Specification.

What follows is a summary of which license is used where, please note however that the license(s) specified in the source files take precedence in case of any discrepancies.

- Files provided by [Gradle](https://gradle.org) are licensed under [Apache-2.0](./LICENSES/Apache-2.0.txt).
- Most other files are licensed under [MIT-0](./LICENSES/MIT-0.txt). Note that this includes file which are most likely not copyrightable (such as `.gitmodule` and other configuration), in line with the [recommendation by REUSE](https://reuse.software/faq/#uncopyrightable) I still included license headers for these. Since it is MIT-0, you are not required to keep these headers if you do not want them.
- Much of the actual build logic is derived work from [`cargo-mobile`'s Android Studio template](https://github.com/BrainiumLLC/cargo-mobile/tree/38e48c3373b23e90ab0392a651925f67af48076e/templates/platforms/android-studio) and [Bitkey's application's build logic](https://github.com/proto-at-block/bitkey/tree/694c152387c1fdb2b6be01ba35e0a9c092a81879/app/gradle/build-logic/src/main/kotlin/build/wallet/gradle/logic/rust), which are licensed under "[MIT](./LICENSES/MIT.txt) OR Apache-2.0" and [LicenseRef-Block-MIT-CC](./LICENSES/LicenseRef-Block-MIT-CC.txt) respectively. Since "MIT-0 AND MIT" does not make much sense in my eyes, I decided to just go with MIT in those cases. In practice this means that some files of the `buildSrc` source are licensed under MIT-0, other files under MIT and yet other files under "MIT AND LicenseRef-Block-MIT-CC", depending on the license of the original work.

Please note that the [Commons Clause](https://commonsclause.com/) in LicenseRef-Block-MIT-CC is for "Bitkey mobile application and server code" and **not** for this work. Since this work is only derived from 2-3 files (out of over 48000 at the time of writing) of the "Bitkey mobile application and server code" it hardly counts as substantial and you may therefore use this work just fine in software you intend to sell (side note, I am not a lawyer..). Please also see the [Commons Clause FAQ](https://commonsclause.com/) for more information and/or ask your lawyer about the specifics.

### Contact

If you have any questions regarding this project, the licensing situation or just want to discuss the implementation, please feel free to open an issue/discussion, contact me via email (included in each source file) or ping me on the Iroh discord (nickname: Nikx).
