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
        SIZE,
        DHASH
    }

    val title: String
        get() = when (matchType) {
            MatchType.FILENAME -> images.firstOrNull()?.displayName ?: "未知文件"
            MatchType.SIZE -> images.firstOrNull()?.formattedSize ?: "未知大小"
            MatchType.DHASH -> images.firstOrNull()?.displayName ?: "相似图片"
        }

    val subtitle: String
        get() = when (matchType) {
            MatchType.FILENAME -> "名称相同 · ${images.size} 张"
            MatchType.SIZE -> "容量相同 · ${images.size} 张"
            MatchType.DHASH -> "dHash 相似 · ${images.size} 张"
        }
}
