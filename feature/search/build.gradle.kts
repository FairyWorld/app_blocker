/*
 * Copyright 2023 Blocker
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("blocker.android.feature")
    id("blocker.android.library.compose")
    id("blocker.android.library.jacoco")
}
android {
    namespace = "com.merxury.blocker.feature.search"
}
dependencies {
    implementation(project(":core:database"))
    implementation(libs.kotlinx.datetime)
}