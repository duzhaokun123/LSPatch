package org.lsposed.lspatch.ui.viewmodel

import android.util.Base64
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.lsposed.lspatch.share.PatchConfig
import org.lsposed.lspatch.ui.viewmodel.manage.AppManageViewModel
import org.lsposed.lspatch.util.LSPPackageManager
import org.lsposed.lspatch.util.LSPPackageManager.AppInfo

class SelectAppsViewModel : ViewModel() {

    companion object {
        private const val TAG = "SelectAppViewModel"
    }

    init {
        Log.d(TAG, "SelectAppsViewModel ${toString().substringAfterLast('@')} construct")
    }

    var isRefreshing by mutableStateOf(false)
        private set

    var filteredList by mutableStateOf(listOf<AppInfo>())
        private set

    val multiSelected = mutableStateListOf<AppInfo>()

    fun filterAppList(refresh: Boolean, filter: (AppInfo) -> Boolean) {
        viewModelScope.launch {
            if (LSPPackageManager.appList.isEmpty() || refresh) {
                isRefreshing = true
                LSPPackageManager.fetchAppList()
                isRefreshing = false
            }
            filteredList = LSPPackageManager.appList.filter(filter)
            Log.d(TAG, "Filtered ${filteredList.size} apps")
        }
    }
}
