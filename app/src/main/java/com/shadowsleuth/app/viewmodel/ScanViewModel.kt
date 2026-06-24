package com.shadowsleuth.app.viewmodel

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shadowsleuth.app.data.DHashCalculator
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

/**
 * dHash 扫描状态
 */
sealed class DHashScanState {
    data object Idle : DHashScanState()
    data class Computing(val computed: Int, val total: Int) : DHashScanState()
    data class Complete(val groups: List<DuplicateGroup>) : DHashScanState()
    data class Error(val message: String) : DHashScanState()
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = ImageScanner(application)
    private val finder = DuplicateFinder()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _searchState = MutableStateFlow<SearchState>(SearchState.NoSample)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _dHashScanState = MutableStateFlow<DHashScanState>(DHashScanState.Idle)
    val dHashScanState: StateFlow<DHashScanState> = _dHashScanState.asStateFlow()

    private val _minSizeKb = MutableStateFlow(50)
    val minSizeKb: StateFlow<Int> = _minSizeKb.asStateFlow()

    private val _matchByFilename = MutableStateFlow(true)
    val matchByFilename: StateFlow<Boolean> = _matchByFilename.asStateFlow()

    private val _matchBySize = MutableStateFlow(true)
    val matchBySize: StateFlow<Boolean> = _matchBySize.asStateFlow()

    private val _scrollToTopResults = MutableSharedFlow<Unit>()
    val scrollToTopResults: SharedFlow<Unit> = _scrollToTopResults.asSharedFlow()

    private val _pendingDeleteRequest = MutableSharedFlow<IntentSender>()
    val pendingDeleteRequest: SharedFlow<IntentSender> = _pendingDeleteRequest.asSharedFlow()

    private var pendingDeleteImage: ImageMetadata? = null
    private var pendingDeleteCallbacks: DeleteCallbacks? = null

    private data class DeleteCallbacks(
        val onSuccess: () -> Unit,
        val onError: (String) -> Unit
    )

    private var allImages: List<ImageMetadata> = emptyList()

    /** 已计算好的 dHash 缓存：图片 id → dHash */
    private var dHashCache: MutableMap<Long, Long> = mutableMapOf()

    /** 对外暴露缓存条目数，用于 UI 展示 */
    private val _dHashCacheSize = MutableStateFlow(0)
    val dHashCacheSize: StateFlow<Int> = _dHashCacheSize.asStateFlow()

    /** 缓存占用内存估算（字节） */
    private val _dHashCacheBytes = MutableStateFlow(0L)
    val dHashCacheBytes: StateFlow<Long> = _dHashCacheBytes.asStateFlow()

    /** 缓存创建时间（毫秒时间戳），0 表示未创建 */
    private val _dHashCacheCreatedAt = MutableStateFlow(0L)
    val dHashCacheCreatedAt: StateFlow<Long> = _dHashCacheCreatedAt.asStateFlow()

    /** 清空 dHash 缓存，同时重置 dHash 扫描状态 */
    fun clearDHashCache() {
        dHashCache.clear()
        _dHashCacheSize.value = 0
        _dHashCacheBytes.value = 0L
        _dHashCacheCreatedAt.value = 0L
        _dHashScanState.value = DHashScanState.Idle
    }

    fun setMinSize(kb: Int) {
        _minSizeKb.value = kb
    }

    fun setMatchOptions(filename: Boolean, size: Boolean) {
        _matchByFilename.value = filename
        _matchBySize.value = size
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
                    matchBySize = matchBySize.value
                )
                _scanState.value = ScanState.Complete(allImages, groups)
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "扫描失败")
            }
        }
    }

    /**
     * dHash 全量扫描：计算所有图片的 dHash 并找出相似组
     * 相似组追加到当前 ScanState.Complete 的 groups 中
     */
    fun startDHashScan() {
        viewModelScope.launch {
            _dHashScanState.value = DHashScanState.Computing(0, 0)
            try {
                // 确保已扫描
                if (allImages.isEmpty()) {
                    allImages = scanner.scanAllImages(minSizeBytes = minSizeKb.value * 1024L)
                }
                val context = getApplication<Application>()
                val total = allImages.size
                val hashMap = mutableMapOf<Long, Long>()

                allImages.forEachIndexed { index, img ->
                    val hash = DHashCalculator.compute(context, img.uri)
                    if (hash != null) hashMap[img.id] = hash
                    if ((index + 1) % 20 == 0 || index == total - 1) {
                        _dHashScanState.value = DHashScanState.Computing(index + 1, total)
                    }
                }

                dHashCache = hashMap
                _dHashCacheSize.value = hashMap.size
                _dHashCacheBytes.value = hashMap.size * 48L
                _dHashCacheCreatedAt.value = System.currentTimeMillis()
                val dHashGroups = finder.findDuplicatesByDHash(hashMap, allImages)
                _dHashScanState.value = DHashScanState.Complete(dHashGroups)

                // 将 dHash 结果追加到 ScanState.Complete
                val currentScan = _scanState.value
                if (currentScan is ScanState.Complete) {
                    val existingIds = currentScan.groups.map { it.id }.toSet()
                    val merged = currentScan.groups +
                        dHashGroups.filter { it.id !in existingIds }
                    _scanState.value = ScanState.Complete(currentScan.images, merged)
                } else {
                    val groups = finder.findDuplicates(
                        allImages,
                        matchByFilename = matchByFilename.value,
                        matchBySize = matchBySize.value
                    ) + dHashGroups
                    _scanState.value = ScanState.Complete(allImages, groups)
                }
            } catch (e: Exception) {
                _dHashScanState.value = DHashScanState.Error(e.message ?: "dHash 扫描失败")
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
                    matchBySize = matchBySize.value
                )
                _searchState.value = SearchState.Ready(sample, groups)
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(e.message ?: "搜索失败")
            }
        }
    }

    /**
     * dHash 单图搜索：在所有图片中找出与样本相似的图片
     * 结果追加到当前 SearchState.Ready 的 groups 中
     */
    fun searchSampleByDHash(sample: ImageMetadata) {
        viewModelScope.launch {
            try {
                if (allImages.isEmpty()) {
                    allImages = scanner.scanAllImages(minSizeBytes = minSizeKb.value * 1024L)
                }
                val context = getApplication<Application>()

                // 优先从缓存读取样本 dHash
                val sampleHash: Long = dHashCache[sample.id]
                    ?: DHashCalculator.compute(context, sample.uri)
                    ?: run {
                        _searchState.value = SearchState.Error("无法计算该图片的 dHash，可能格式不支持")
                        return@launch
                    }
                // 样本 dHash 未缓存则补入
                if (!dHashCache.containsKey(sample.id)) {
                    dHashCache[sample.id] = sampleHash
                }

                // 批量补全缺失的 dHash
                allImages.forEach { img ->
                    if (!dHashCache.containsKey(img.id)) {
                        val h = DHashCalculator.compute(context, img.uri)
                        if (h != null) dHashCache[img.id] = h
                    }
                }
                _dHashCacheSize.value = dHashCache.size
                _dHashCacheBytes.value = dHashCache.size * 48L
                if (_dHashCacheCreatedAt.value == 0L) {
                    _dHashCacheCreatedAt.value = System.currentTimeMillis()
                }

                val dHashGroups = finder.findMatchesByDHash(sampleHash, sample, dHashCache, allImages)

                // 与现有结果合并
                val currentSearch = _searchState.value
                val existingGroups = if (currentSearch is SearchState.Ready) currentSearch.groups else emptyList()
                val existingIds = existingGroups.map { it.id }.toSet()
                val merged = existingGroups + dHashGroups.filter { it.id !in existingIds }
                _searchState.value = SearchState.Ready(sample, merged)
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(e.message ?: "dHash 搜索失败")
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
            } catch (e: RecoverableSecurityException) {
                pendingDeleteImage = image
                pendingDeleteCallbacks = DeleteCallbacks(onSuccess, onError)
                _pendingDeleteRequest.emit(e.userAction.actionIntent.intentSender)
            } catch (e: SecurityException) {
                onError("需要存储写入权限才能删除此图片，请检查权限设置")
            } catch (e: Exception) {
                onError(e.message ?: "删除图片失败")
            }
        }
    }

    /**
     * 用户授权删除后重试之前失败的删除请求
     */
    fun retryPendingDelete() {
        val image = pendingDeleteImage ?: return
        val callbacks = pendingDeleteCallbacks ?: return
        pendingDeleteImage = null
        pendingDeleteCallbacks = null
        deleteImage(image, callbacks.onSuccess, callbacks.onError)
    }

    fun clearPendingDelete() {
        pendingDeleteImage = null
        pendingDeleteCallbacks = null
    }

    private fun removeImageFromState(image: ImageMetadata) {
        allImages = allImages.filter { it.id != image.id }
        dHashCache.remove(image.id)

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

        /**
         * Android 9 及以下删除文件需要 WRITE_EXTERNAL_STORAGE 权限。
         * 在 Android 10+ 上该权限已无法直接删除共享目录图片，但仍请求它作为辅助。
         */
        fun getWritePermission(): String {
            return android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

        fun needWritePermissionForDelete(): Boolean {
            return Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
        }

        fun hasPermission(context: Context): Boolean {
            return context.checkSelfPermission(getRequiredPermission()) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        fun hasWritePermission(context: Context): Boolean {
            return context.checkSelfPermission(getWritePermission()) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        /**
         * Android 10+ 上需要 MANAGE_EXTERNAL_STORAGE 才能避免每次删除都弹系统授权框。
         */
        fun hasManageStoragePermission(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else {
                false
            }
        }

        fun getAllRequiredPermissions(): Array<String> {
            val permissions = mutableListOf(getRequiredPermission())
            // 大胆请求 WRITE_EXTERNAL_STORAGE，即使 Android 10+ 仅作辅助
            permissions.add(getWritePermission())
            return permissions.toTypedArray()
        }
    }
}
