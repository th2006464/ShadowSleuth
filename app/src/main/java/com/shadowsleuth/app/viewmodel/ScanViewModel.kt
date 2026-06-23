package com.shadowsleuth.app.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shadowsleuth.app.data.DuplicateFinder
import com.shadowsleuth.app.data.ImageScanner
import com.shadowsleuth.app.data.model.DuplicateGroup
import com.shadowsleuth.app.data.model.ImageMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 扫描状态
 */
sealed class ScanState {
    data object Idle : ScanState()
    data class Scanning(val scanned: Int, val message: String = "") : ScanState()
    data class Complete(val images: List<ImageMetadata>, val groups: List<DuplicateGroup>) : ScanState()
    data class Error(val message: String) : ScanState()
}

/**
 * 搜索状态
 */
sealed class SearchState {
    data object Idle : SearchState()
    data object NoSample : SearchState()
    data class Ready(val sample: ImageMetadata, val groups: List<DuplicateGroup>) : SearchState()
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = ImageScanner(application)
    private val finder = DuplicateFinder()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _searchState = MutableStateFlow<SearchState>(SearchState.NoSample)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private var allImages: List<ImageMetadata> = emptyList()

    var minSizeKb: Int = 50
        private set

    var matchByFilename: Boolean = true
        private set

    var matchBySize: Boolean = true
        private set

    fun setMinSize(kb: Int) {
        minSizeKb = kb
    }

    fun setMatchOptions(filename: Boolean, size: Boolean) {
        matchByFilename = filename
        matchBySize = size
    }

    fun startScan() {
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning(0)
            try {
                allImages = scanner.scanAllImages(
                    minSizeBytes = minSizeKb * 1024L,
                    onProgress = { count ->
                        _scanState.value = ScanState.Scanning(count, "已扫描 $count 张图片…")
                    }
                )
                val groups = finder.findDuplicates(
                    allImages,
                    matchByFilename = matchByFilename,
                    matchBySize = matchBySize
                )
                _scanState.value = ScanState.Complete(allImages, groups)
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "扫描失败")
            }
        }
    }

    fun searchSample(sample: ImageMetadata) {
        viewModelScope.launch {
            if (allImages.isEmpty()) {
                allImages = scanner.scanAllImages(minSizeBytes = minSizeKb * 1024L)
            }
            val groups = finder.findMatches(
                sample,
                allImages,
                matchByFilename = matchByFilename,
                matchBySize = matchBySize
            )
            _searchState.value = SearchState.Ready(sample, groups)
        }
    }

    fun clearSearchSample() {
        _searchState.value = SearchState.NoSample
    }

    companion object {
        fun getRequiredPermission(): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }

        fun hasPermission(context: Context): Boolean {
            return context.checkSelfPermission(getRequiredPermission()) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}
