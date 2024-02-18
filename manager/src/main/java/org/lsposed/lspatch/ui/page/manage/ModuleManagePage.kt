package org.lsposed.lspatch.ui.page.manage

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import kotlinx.coroutines.launch
import org.lsposed.lspatch.R
import org.lsposed.lspatch.config.ConfigManager
import org.lsposed.lspatch.ui.component.AnywhereDropdown
import org.lsposed.lspatch.ui.component.AppItem
import org.lsposed.lspatch.ui.page.SelectAppsResult
import org.lsposed.lspatch.ui.page.SelectAppsType
import org.lsposed.lspatch.ui.page.destinations.SelectAppsScreenDestination
import org.lsposed.lspatch.ui.viewmodel.manage.ModuleManageViewModel
import org.lsposed.lspatch.util.LSPPackageManager

private const val TAG = "ModuleManagePage"
@Composable
fun ModuleManageBody(
    navigator: DestinationsNavigator,
    resultRecipient: ResultRecipient<SelectAppsScreenDestination, SelectAppsResult>
) {
    val context = LocalContext.current
    val viewModel = viewModel<ModuleManageViewModel>()
    val scope = rememberCoroutineScope()
    var moduleApp by rememberSaveable { mutableStateOf("") }

    resultRecipient.onNavResult {
        if (it is NavResult.Value) {
            scope.launch {
                val result = it.value as SelectAppsResult.MultipleApps
                    ConfigManager.getAppForModule(moduleApp).forEach {
                        ConfigManager.deactivateModule(it, moduleApp)
                    }
                    result.selected.forEach {
                        Log.d(TAG, "Activate $moduleApp for ${it.app.packageName}")
                        ConfigManager.activateModule(it.app.packageName, moduleApp)
                    }
            }
        }
    }

    if (viewModel.appList.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = run {
                    if (LSPPackageManager.appList.isEmpty()) stringResource(R.string.manage_loading)
                    else stringResource(R.string.manage_no_modules)
                },
                fontFamily = FontFamily.Serif,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    } else {
        LazyColumn(Modifier.fillMaxHeight()) {
            items(
                items = viewModel.appList,
                key = { it.first.app.packageName }
            ) {
                var expanded by remember { mutableStateOf(false) }
                val settingsIntent = remember { LSPPackageManager.getSettingsIntent(it.first.app.packageName) }
                AnywhereDropdown(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    onClick = { expanded = true },
                    onLongClick = { expanded = true },
                    surface = {
                        AppItem(
                            icon = LSPPackageManager.getIcon(it.first),
                            label = it.first.label,
                            packageName = it.first.app.packageName,
                            additionalContent = {
                                Text(
                                    text = it.second.description,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = buildAnnotatedString {
                                        append(AnnotatedString("API", SpanStyle(color = MaterialTheme.colorScheme.secondary)))
                                        append("  ")
                                        append(it.second.api.toString())
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Serif,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = it.first.label, color = MaterialTheme.colorScheme.primary) },
                        onClick = {}, enabled = false
                    )
                    if (settingsIntent != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.manage_module_settings)) },
                            onClick = { context.startActivity(settingsIntent) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.manage_app_info)) },
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", it.first.app.packageName, null)
                            )
                            context.startActivity(intent)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.manage_module_scope)) },
                        onClick = {
                            expanded = false
                            scope.launch {
                                moduleApp = it.first.app.packageName
                                val actived = ConfigManager.getAppForModule(moduleApp).mapTo(ArrayList()) { it }
                                navigator.navigate(SelectAppsScreenDestination(true, SelectAppsType.Managed, actived))
                            }
                        }
                    )
                }
            }
        }
    }
}
