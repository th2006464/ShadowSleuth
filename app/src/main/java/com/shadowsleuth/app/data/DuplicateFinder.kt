package com.shadowsleuth.app.data

import com.shadowsleuth.app.data.model.DuplicateGroup
import com.shadowsleuth.app.data.model.DuplicateGroup.MatchType
import com.shadowsleuth.app.data.model.ImageMetadata
import java.util.UUID

/**
 * 重复图片查找器
 */
class DuplicateFinder {

    /**
     * 按文件名、文件大小和分辨率查找重复图片
     * @param images 图片列表
     * @param matchByFilename 是否按文件名匹配
     * @param matchBySize 是否按文件大小匹配
     * @param matchByDimensions 是否按分辨率匹配
     */
    fun findDuplicates(
        images: List<ImageMetadata>,
        matchByFilename: Boolean = true,
        matchBySize: Boolean = true,
        matchByDimensions: Boolean = true
    ): List<DuplicateGroup> {
        val groups = mutableListOf<DuplicateGroup>()

        if (matchByFilename) {
            val byName = images.groupBy { it.displayName.lowercase() }
            byName.values
                .filter { it.size >= 2 }
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

        if (matchByDimensions) {
            val byDimensions = images.groupBy { "${it.width}x${it.height}" }
            byDimensions.values
                .filter { it.size >= 2 }
                .forEach { groupImages ->
                    groups.add(
                        DuplicateGroup(
                            id = UUID.randomUUID().toString(),
                            matchType = MatchType.DIMENSIONS,
                            images = groupImages.sortedByDescending { it.dateAdded }
                        )
                    )
                }
        }

        return groups.sortedByDescending { it.images.size }
    }

    /**
     * 单图搜索：找出与样本图片同名、同大小或同分辨率的图片
     */
    fun findMatches(
        sample: ImageMetadata,
        images: List<ImageMetadata>,
        matchByFilename: Boolean = true,
        matchBySize: Boolean = true,
        matchByDimensions: Boolean = true
    ): List<DuplicateGroup> {
        val groups = mutableListOf<DuplicateGroup>()
        val others = images.filter { it.id != sample.id }

        if (matchByFilename) {
            val matches = others.filter { it.displayName.equals(sample.displayName, ignoreCase = true) }
            if (matches.isNotEmpty()) {
                groups.add(
                    DuplicateGroup(
                        id = UUID.randomUUID().toString(),
                        matchType = MatchType.FILENAME,
                        images = (listOf(sample) + matches).sortedByDescending { it.dateAdded }
                    )
                )
            }
        }

        if (matchBySize) {
            val matches = others.filter { it.sizeBytes == sample.sizeBytes }
            if (matches.isNotEmpty()) {
                groups.add(
                    DuplicateGroup(
                        id = UUID.randomUUID().toString(),
                        matchType = MatchType.SIZE,
                        images = (listOf(sample) + matches).sortedByDescending { it.dateAdded }
                    )
                )
            }
        }

        if (matchByDimensions) {
            val matches = others.filter { it.width == sample.width && it.height == sample.height }
            if (matches.isNotEmpty()) {
                groups.add(
                    DuplicateGroup(
                        id = UUID.randomUUID().toString(),
                        matchType = MatchType.DIMENSIONS,
                        images = (listOf(sample) + matches).sortedByDescending { it.dateAdded }
                    )
                )
            }
        }

        return groups
    }
}
