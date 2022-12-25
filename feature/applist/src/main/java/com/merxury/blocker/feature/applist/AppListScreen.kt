/*
 * Copyright 2022 Blocker
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.merxury.blocker.core.designsystem.component.BlockerLoadingWheel
import com.merxury.blocker.core.ui.data.ErrorMessage
import com.merxury.blocker.feature.applist.R.string
import com.merxury.blocker.feature.applist.component.AppListItem
import com.merxury.blocker.feature.applist.component.TopAppBarMoreMenu
import com.merxury.blocker.feature.applist.component.TopAppBarSortMenu

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun AppListRoute(
    navigateToAppDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AppListScreen(
        uiState = uiState,
        onAppItemClick = navigateToAppDetail,
        isRefreshing = uiState is AppListUiState.Loading,
        onRefresh = viewModel::loadData,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    uiState: AppListUiState,
    onAppItemClick: (String) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Blocker")
                },
                actions = {
                    TopAppBarSortMenu()
                    TopAppBarMoreMenu()
                },
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState) {
                AppListUiState.Loading -> {
                    Column(
                        modifier = modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        BlockerLoadingWheel(
                            modifier = modifier,
                            contentDesc = stringResource(id = string.loading),
                        )
                    }
                }

                is AppListUiState.Success -> {
                    AppListContent(
                        appList = uiState.appList,
                        onAppItemClick = onAppItemClick,
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = modifier
                    )
                }

                is AppListUiState.Error -> ErrorAppListScreen(uiState.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppListContent(
    appList: SnapshotStateList<AppItem>,
    onAppItemClick: (String) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listContent = remember { appList }
    val listState = rememberLazyListState()
    val refreshing by remember { mutableStateOf(isRefreshing) }
    val refreshingState = rememberPullRefreshState(refreshing, onRefresh)
    Box(modifier.pullRefresh(refreshingState)) {
        LazyColumn(
            modifier = modifier,
            state = listState
        ) {
            items(listContent, key = { it.packageName }) {
                AppListItem(
                    label = it.label,
                    packageName = it.packageName,
                    versionName = it.versionName,
                    packageInfo = it.packageInfo,
                    appServiceStatus = it.appServiceStatus,
                    onClick = onAppItemClick,
                )
            }
        }
        PullRefreshIndicator(
            refreshing = refreshing,
            state = refreshingState,
            modifier = Modifier.align(Alignment.TopCenter),
            scale = true
        )
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect {
                // TODO show service info
            }
    }
}

@Composable
fun ErrorAppListScreen(message: ErrorMessage) {
    Text(text = message.message)
}
