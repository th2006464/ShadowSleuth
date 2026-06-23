package com.shadowsleuth.app.data.model

import android.net.Uri

/**
 * 图片元数据模型
 */
data class ImageMetadata(
    val id: Long,
    val uri: Uri,
    val path: String,
    val displayName: String,
    val sizeBytes: Long,
    val dateAdded: Long,
    val width: Int,
    val height: Int,
    val mimeType: String
) {
    val formattedSize: String
        get() = formatBytes(sizeBytes)

    val formattedDimensions: String
        get() = "${width} × ${height}"

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024) return "%.1f KB".format(kb)
            val mb = kb / 1024.0
            if (mb < 1024) return "%.1f MB".format(mb)
            val gb = mb / 1024.0
            return "%.2f GB".format(gb)
        }
    }
}
