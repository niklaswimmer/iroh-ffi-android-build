/*
 * SPDX-FileCopyrightText: 2024 Niklas Wimmer <mail@nwimmer.me>
 * SPDX-License-Identifier: MIT-0
 */

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
