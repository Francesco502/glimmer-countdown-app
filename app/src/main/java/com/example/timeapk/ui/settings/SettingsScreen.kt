package com.example.timeapk.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.timeapk.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentCategory by remember { mutableStateOf<SettingsCategory?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Intercept back press when in a sub-screen
    BackHandler(enabled = currentCategory != null) {
        currentCategory = null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (currentCategory != null) stringResource(currentCategory!!.titleRes) else stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentCategory != null) {
                                currentCategory = null
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        val baseModifier = modifier
            .padding(innerPadding)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)

        if (currentCategory == null) {
            Column(
                modifier = baseModifier.verticalScroll(rememberScrollState())
            ) {
                SettingsCategoryList(
                    onCategoryClick = { currentCategory = it }
                )
            }
        } else {
            when (currentCategory) {
                SettingsCategory.APPEARANCE -> AppearanceSettingsContent(
                    modifier = baseModifier
                )
                SettingsCategory.DISPLAY -> DisplaySettingsContent(
                    modifier = baseModifier
                )
                SettingsCategory.DATA -> DataSettingsContent(
                    snackbarHostState = snackbarHostState,
                    modifier = baseModifier
                )
                SettingsCategory.ABOUT -> AboutSettingsContent(
                    snackbarHostState = snackbarHostState,
                    modifier = baseModifier
                )
                else -> {}
            }
        }
    }
}

@Composable
fun SettingsCategoryList(
    onCategoryClick: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SettingsCategory.entries.forEach { category ->
            SettingsCategoryRow(
                category = category,
                onClick = { onCategoryClick(category) }
            )
            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
