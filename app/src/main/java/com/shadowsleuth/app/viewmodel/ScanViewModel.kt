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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    data class Error(val message: String) : SearchState()
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = ImageScanner(application)
    private val finder = DuplicateFinder()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _searchState = MutableStateFlow<SearchState>(SearchState.NoSample)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _minSizeKb = MutableStateFlow(50)
    val minSizeKb: StateFlow<Int> = _minSizeKb.asStateFlow()

    private val _matchByFilename = MutableStateFlow(true)
    val matchByFilename: StateFlow<Boolean> = _matchByFilename.asStateFlow()

    private val _matchBySize = MutableStateFlow(true)
    val matchBySize: StateFlow<Boolean> = _matchBySize.asStateFlow()

    private val _matchByDimensions = MutableStateFlow(true)
    val matchByDimensions: StateFlow<Boolean> = _matchByDimensions.asStateFlow()

    private val _scrollToTopResults = MutableSharedFlow<Unit>()
    val scrollToTopResults: SharedFlow<Unit> = _scrollToTopResults.asSharedFlow()

    private var allImages: List<ImageMetadata> = emptyList()

    fun setMinSize(kb: Int) {
        _minSizeKb.value = kb
    }

    fun setMatchOptions(filename: Boolean, size: Boolean, dimensions: Boolean) {
        _matchByFilename.value = filename
        _matchBySize.value = size
        _matchByDimensions.value = dimensions
    }

    fun startScan() {
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning(0)
            try {
                allImages = scanner.scanAllImages(
                    minSizeBytes = minSizeKb.value * 1024L,
                    onProgress = { count ->
                        _scanState.value = ScanState.Scanning(count, "已扫描 $count 张图片…")
                    }
                )
                val groups = finder.findDuplicates(
                    allImages,
                    matchByFilename = matchByFilename.value,
                    matchBySize = matchBySize.value,
                    matchByDimensions = matchByDimensions.value
                )
                _scanState.value = ScanState.Complete(allImages, groups)
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "扫描失败")
            }
        }
    }

    fun searchSample(sample: ImageMetadata) {
        viewModelScope.launch {
            try {
                if (allImages.isEmpty()) {
                    allImages = scanner.scanAllImages(minSizeBytes = minSizeKb.value * 1024L)
                }
                val groups = finder.findMatches(
                    sample,
                    allImages,
                    matchByFilename = matchByFilename.value,
                    matchBySize = matchBySize.value,
                    matchByDimensions = matchByDimensions.value
                )
                _searchState.value = SearchState.Ready(sample, groups)
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(e.message ?: "搜索失败")
            }
        }
    }

    fun scrollToTopResults() {
        viewModelScope.launch {
            _scrollToTopResults.emit(Unit)
        }
    }

    fun clearSearchSample() {
        _searchState.value = SearchState.NoSample
    }

    fun deleteImage(image: ImageMetadata, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val resolver = getApplication<Application>().contentResolver
                val deletedRows = resolver.delete(image.uri, null, null)
                if (deletedRows > 0) {
                    removeImageFromState(image)
                    onSuccess()
                } else {
                    onError("删除失败：系统未删除该图片")
                }
            } catch (e: SecurityException) {
                onError("需要更高权限才能删除此图片，请手动在系统相册中删除")
            } catch (e: Exception) {
                onError(e.message ?: "删除图片失败")
            }
        }
    }

    private fun removeImageFromState(image: ImageMetadata) {
        allImages = allImages.filter { it.id != image.id }

        val currentScan = _scanState.value
        if (currentScan is ScanState.Complete) {
            val newImages = currentScan.images.filter { it.id != image.id }
            val newGroups = currentScan.groups
                .map { group -> group.copy(images = group.images.filter { it.id != image.id }) }
                .filter { it.images.size >= 2 }
            _scanState.value = ScanState.Complete(newImages, newGroups)
        }

        val currentSearch = _searchState.value
        if (currentSearch is SearchState.Ready) {
            val newSample = currentSearch.sample.takeIf { it.id != image.id }
            val newGroups = currentSearch.groups
                .map { group -> group.copy(images = group.images.filter { it.id != image.id }) }
                .filter { it.images.size >= 2 }
            _searchState.value = if (newSample != null) {
                SearchState.Ready(newSample, newGroups)
            } else {
                SearchState.NoSample
            }
        }
    }

    fun setSearchError(message: String) {
        _searchState.value = SearchState.Error(message)
    }

    fun findImageById(id: Long): ImageMetadata? {
        val searchState = _searchState.value
        if (searchState is SearchState.Ready) {
            if (searchState.sample.id == id) return searchState.sample
            searchState.groups.flatMap { it.images }.find { it.id == id }?.let { return it }
        }
        return allImages.find { it.id == id }
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
