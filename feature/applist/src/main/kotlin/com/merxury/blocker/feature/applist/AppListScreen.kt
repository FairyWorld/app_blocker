/*
 * Copyright 2023 Blocker
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

package com.merxury.blocker.feature.applist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.merxury.blocker.core.designsystem.component.BlockerErrorAlertDialog
import com.merxury.blocker.core.designsystem.component.BlockerTopAppBar
import com.merxury.blocker.core.designsystem.component.BlockerWarningAlertDialog
import com.merxury.blocker.core.designsystem.icon.BlockerIcons
import com.merxury.blocker.core.model.data.AppItem
import com.merxury.blocker.core.ui.TrackScreenViewEvent
import com.merxury.blocker.core.ui.applist.AppList
import com.merxury.blocker.core.ui.screen.EmptyScreen
import com.merxury.blocker.core.ui.screen.ErrorScreen
import com.merxury.blocker.core.ui.screen.InitializingScreen
import com.merxury.blocker.feature.applist.R.string
import com.merxury.blocker.feature.applist.component.TopAppBarMoreMenu

@Composable
fun AppListRoute(
    navigateToAppDetail: (String) -> Unit,
    navigateToSettings: () -> Unit,
    navigateToSupportAndFeedback: () -> Unit,
    navigateTooAppSortScreen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorState by viewModel.errorState.collectAsStateWithLifecycle()
    val warningState by viewModel.warningState.collectAsStateWithLifecycle()
    val appList = viewModel.appListFlow.collectAsState()
    AppListScreen(
        uiState = uiState,
        appList = appList.value,
        onAppItemClick = navigateToAppDetail,
        onClearCacheClick = viewModel::clearCache,
        onClearDataClick = viewModel::clearData,
        onForceStopClick = viewModel::forceStop,
        onUninstallClick = viewModel::uninstall,
        onEnableClick = viewModel::enable,
        onDisableClick = viewModel::disable,
        onServiceStateUpdate = viewModel::updateServiceStatus,
        navigateToSettings = navigateToSettings,
        navigateToSupportAndFeedback = navigateToSupportAndFeedback,
        navigateTooAppSortScreen = navigateTooAppSortScreen,
        modifier = modifier,
    )
    if (errorState != null) {
        BlockerErrorAlertDialog(
            title = errorState?.title.orEmpty(),
            text = errorState?.content.orEmpty(),
            onDismissRequest = viewModel::dismissErrorDialog,
        )
    }
    warningState?.let {
        BlockerWarningAlertDialog(
            title = it.title,
            text = stringResource(id = it.message),
            onDismissRequest = viewModel::dismissWarningDialog,
            onConfirmRequest = it.onPositiveButtonClicked,
        )
    }
}

@Composable
fun AppListScreen(
    uiState: AppListUiState,
    appList: List<AppItem>,
    modifier: Modifier = Modifier,
    onAppItemClick: (String) -> Unit = {},
    onClearCacheClick: (String) -> Unit = {},
    onClearDataClick: (String) -> Unit = {},
    onForceStopClick: (String) -> Unit = {},
    onUninstallClick: (String) -> Unit = {},
    onEnableClick: (String) -> Unit = {},
    onDisableClick: (String) -> Unit = {},
    onServiceStateUpdate: (String, Int) -> Unit = { _, _ -> },
    navigateTooAppSortScreen: () -> Unit = {},
    navigateToSettings: () -> Unit = {},
    navigateToSupportAndFeedback: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            BlockerTopAppBar(
                title = stringResource(id = string.feature_applist_app_name),
                actions = {
                    IconButton(onClick = navigateTooAppSortScreen) {
                        Icon(
                            imageVector = BlockerIcons.Sort,
                            contentDescription = stringResource(id = string.feature_applist_sort_menu),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    TopAppBarMoreMenu(
                        navigateToSettings = navigateToSettings,
                        navigateToFeedback = navigateToSupportAndFeedback,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal,
                    ),
                ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val appListTestTag = "appList:applicationList"
            when (uiState) {
                is AppListUiState.Initializing -> InitializingScreen(processingName = uiState.processingName)

                is AppListUiState.Success -> {
                    if (appList.isEmpty()) {
                        EmptyScreen(textRes = string.feature_applist_no_applications_to_display)
                    } else {
                        AppList(
                            appList = appList,
                            onAppItemClick = onAppItemClick,
                            onClearCacheClick = onClearCacheClick,
                            onClearDataClick = onClearDataClick,
                            onForceStopClick = onForceStopClick,
                            onUninstallClick = onUninstallClick,
                            onEnableClick = onEnableClick,
                            onDisableClick = onDisableClick,
                            onServiceStateUpdate = onServiceStateUpdate,
                            modifier = modifier.testTag(appListTestTag),
                        )
                    }
                }

                is AppListUiState.Error -> ErrorScreen(uiState.error)
            }
        }
    }
    TrackScreenViewEvent(screenName = "AppListScreen")
}