/*
 * Copyright 2024 Blocker
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

package com.merxury.blocker.core.utils

import android.annotation.SuppressLint
import android.content.Context

object ContextUtils {
    /**
     * If SDK >= 28, bypassing non-SDK interface restrictions by HiddenApiBypass
     */
    @SuppressLint("DiscouragedPrivateApi", "PrivateApi")
    fun getUserIdHidden(context: Context): Int {
        return Context::class.java
            .getDeclaredMethod("getUserId")
            .invoke(context) as Int
    }

    val Context.userId get() = getUserIdHidden(this)
}
