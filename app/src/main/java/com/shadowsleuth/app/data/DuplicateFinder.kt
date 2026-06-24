package com.shadowsleuth.app.data

import com.shadowsleuth.app.data.model.DuplicateGroup
import com.shadowsleuth.app.data.model.DuplicateGroup.MatchType
import com.shadowsleuth.app.data.model.ImageMetadata
import java.util.UUID

/**
 * 重复图片查找器
 *
 * 匹配规则：
 * - 文件名相同（忽略大小写）
 * - 文件字节大小相同
 * - dHash 差分哈希相似（汉明距离 ≤ DHashCalculator.SIMILARITY_THRESHOLD）
 *
 * 排除规则：
 * - 组内所有图片的保存时间（MediaStore DATE_ADDED，精确到秒）完全一致时，
 *   不视为重复组，避免误报。
 * - 宽或高为 0 的无效图片已在扫描阶段过滤，不再进入匹配。
 */
class DuplicateFinder {

    /**
     * 按文件名和文件大小查找重复图片
     * @param images 图片列表
     * @param matchByFilename 是否按文件名匹配
     * @param matchBySize 是否按文件大小匹配
     */
    fun findDuplicates(
        images: List<ImageMetadata>,
        matchByFilename: Boolean = true,
        matchBySize: Boolean = true
    ): List<DuplicateGroup> {
        val groups = mutableListOf<DuplicateGroup>()

        if (matchByFilename) {
            val byName = images.groupBy { it.displayName.lowercase() }
            byName.values
                .filter { it.size >= 2 }
                .filter { !allSameSaveTime(it) }
                .forEach { groupImages ->
                    groups.add(
                        DuplicateGroup(
                            id = UUID.randomUUID().toString(),
                            matchType = MatchType.FILENAME,
                            images = groupImages.sortedByDescending { it.dateAdded }
                        )
                    )
                }
        }

        if (matchBySize) {
            val bySize = images.groupBy { it.sizeBytes }
            bySize.values
                .filter { it.size >= 2 }
                .filter { !allSameSaveTime(it) }
                .forEach { groupImages ->
                    groups.add(
                        DuplicateGroup(
                            id = UUID.randomUUID().toString(),
                            matchType = MatchType.SIZE,
                            images = groupImages.sortedByDescending { it.dateAdded }
                        )
                    )
                }
        }

        return groups.sortedByDescending { it.images.size }
    }

    /**
     * 单图搜索：找出与样本图片同名或同大小的图片
     */
    fun findMatches(
        sample: ImageMetadata,
        images: List<ImageMetadata>,
        matchByFilename: Boolean = true,
        matchBySize: Boolean = true
    ): List<DuplicateGroup> {
        val groups = mutableListOf<DuplicateGroup>()
        val others = images.filter { it.id != sample.id }

        if (matchByFilename) {
            val matches = others.filter { it.displayName.equals(sample.displayName, ignoreCase = true) }
            val groupImages = (listOf(sample) + matches)
            if (matches.isNotEmpty() && !allSameSaveTime(groupImages)) {
                groups.add(
                    DuplicateGroup(
                        id = UUID.randomUUID().toString(),
                        matchType = MatchType.FILENAME,
                        images = groupImages.sortedByDescending { it.dateAdded }
                    )
                )
            }
        }

        if (matchBySize) {
            val matches = others.filter { it.sizeBytes == sample.sizeBytes }
            val groupImages = (listOf(sample) + matches)
            if (matches.isNotEmpty() && !allSameSaveTime(groupImages)) {
                groups.add(
                    DuplicateGroup(
                        id = UUID.randomUUID().toString(),
                        matchType = MatchType.SIZE,
                        images = groupImages.sortedByDescending { it.dateAdded }
                    )
                )
            }
        }

        return groups
    }

    /**
     * 判断一组图片是否保存时间完全一致（精确到秒）
     */
    private fun allSameSaveTime(images: List<ImageMetadata>): Boolean {
        if (images.size < 2) return false
        val firstSecond = images.first().dateAdded / 1000
        return images.all { it.dateAdded / 1000 == firstSecond }
    }

    /**
     * 全量 dHash 扫描：在已预计算的哈希表中查找相似组。
     *
     * @param hashMap 图片 id → dHash 映射（由 ViewModel 在 IO 线程预计算）
     * @param images  对应图片元数据列表
     * @return 相似图片分组列表
     */
    fun findDuplicatesByDHash(
        hashMap: Map<Long, Long>,
        images: List<ImageMetadata>
    ): List<DuplicateGroup> {
        val groups = mutableListOf<DuplicateGroup>()
        val imageList = images.filter { hashMap.containsKey(it.id) }
        val used = mutableSetOf<Long>()

        for (i in imageList.indices) {
            val imgA = imageList[i]
            if (imgA.id in used) continue
            val hashA = hashMap[imgA.id] ?: continue

            val similar = mutableListOf(imgA)
            for (j in (i + 1) until imageList.size) {
                val imgB = imageList[j]
                if (imgB.id in used) continue
                val hashB = hashMap[imgB.id] ?: continue
                if (DHashCalculator.isSimilar(hashA, hashB)) {
                    similar.add(imgB)
                }
            }

            if (similar.size >= 2 && !allSameSaveTime(similar)) {
                similar.forEach { used.add(it.id) }
                groups.add(
                    DuplicateGroup(
                        id = UUID.randomUUID().toString(),
                        matchType = MatchType.DHASH,
                        images = similar.sortedByDescending { it.dateAdded },
                        dHashValue = hashA
                    )
                )
            }
        }

        return groups.sortedByDescending { it.images.size }
    }

    /**
     * 单图 dHash 搜索：找出与样本图片 dHash 相似的图片。
     *
     * @param sampleHash 样本图片的 dHash 值
     * @param sample     样本图片元数据
     * @param hashMap    图片 id → dHash 映射
     * @param images     全量图片元数据列表
     * @return 相似图片分组（最多 1 组）
     */
    fun findMatchesByDHash(
        sampleHash: Long,
        sample: ImageMetadata,
        hashMap: Map<Long, Long>,
        images: List<ImageMetadata>
    ): List<DuplicateGroup> {
        val others = images.filter { it.id != sample.id }
        val matches = others.filter { img ->
            val hash = hashMap[img.id] ?: return@filter false
            DHashCalculator.isSimilar(sampleHash, hash)
        }
        val groupImages = listOf(sample) + matches
        if (matches.isEmpty() || allSameSaveTime(groupImages)) return emptyList()
        return listOf(
            DuplicateGroup(
                id = UUID.randomUUID().toString(),
                matchType = MatchType.DHASH,
                images = groupImages.sortedByDescending { it.dateAdded },
                dHashValue = sampleHash
            )
        )
    }
}
