package com.shadowsleuth.app.data.model

/**
 * 重复图片分组模型
 */
data class DuplicateGroup(
    val id: String,
    val matchType: MatchType,
    val images: List<ImageMetadata>
) {
    enum class MatchType {
        FILENAME,
        SIZE
    }

    val title: String
        get() = when (matchType) {
            MatchType.FILENAME -> images.firstOrNull()?.displayName ?: "未知文件"
            MatchType.SIZE -> images.firstOrNull()?.formattedSize ?: "未知大小"
        }

    val subtitle: String
        get() = when (matchType) {
            MatchType.FILENAME -> "文件名相同 · ${images.size} 张"
            MatchType.SIZE -> "文件大小相同 · ${images.size} 张"
        }
}
