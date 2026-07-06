package com.example.timeapk.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.timeapk.R
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val currentCategory = currentCategoryName?.let { name ->
        SettingsCategory.entries.firstOrNull { it.name == name }
    }
    val snackbarHostState = remember { SnackbarHostState() }

    // Intercept back press when in a sub-screen
    BackHandler(enabled = currentCategory != null) {
        currentCategoryName = null
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
                                currentCategoryName = null
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        SongLineIcon(
                            kind = SongLineIconKind.Back,
                            contentDescription = stringResource(R.string.nav_back),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
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
                    onCategoryClick = { currentCategoryName = it.name }
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
                SettingsCategory.MILESTONE -> MilestoneSettingsContent(
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
            }
        }
    }
}

@Composable
fun SettingsCategoryList(
    onCategoryClick: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.Top
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        SettingsCategory.entries.forEach { category ->
            SettingsCategoryRow(
                category = category,
                onClick = { onCategoryClick(category) },
                modifier = Modifier
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
        }
    }
}
