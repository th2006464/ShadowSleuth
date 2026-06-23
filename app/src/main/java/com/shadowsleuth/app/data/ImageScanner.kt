package com.shadowsleuth.app.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.shadowsleuth.app.data.model.DuplicateGroup
import com.shadowsleuth.app.data.model.ImageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * 本地图片扫描器
 */
class ImageScanner(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * 扫描全部图片，返回元数据列表
     * @param minSizeBytes 忽略小于该大小的图片
     */
    suspend fun scanAllImages(
        minSizeBytes: Long = DEFAULT_MIN_SIZE,
        onProgress: (Int) -> Unit = {}
    ): List<ImageMetadata> = withContext(Dispatchers.IO) {
        val images = mutableListOf<ImageMetadata>()
        val projection = buildProjection()

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Images.Media.SIZE} > 0"
        } else null

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val columnIndexes = ColumnIndexes(cursor)
            var count = 0
            while (cursor.moveToNext() && isActive) {
                val size = cursor.getLong(columnIndexes.size)
                if (size < minSizeBytes) continue

                val id = cursor.getLong(columnIndexes.id)
                val width = cursor.getInt(columnIndexes.width)
                val height = cursor.getInt(columnIndexes.height)

                // 过滤无效或损坏的图片元数据：宽或高为 0 的通常不是真实图片
                if (width <= 0 || height <= 0) continue

                images.add(
                    ImageMetadata(
                        id = id,
                        uri = Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        ),
                        path = if (columnIndexes.data >= 0) cursor.getString(columnIndexes.data) ?: "" else "",
                        displayName = cursor.getString(columnIndexes.displayName) ?: "",
                        sizeBytes = size,
                        dateAdded = cursor.getLong(columnIndexes.dateAdded) * 1000,
                        width = width,
                        height = height,
                        mimeType = cursor.getString(columnIndexes.mimeType) ?: "image/*"
                    )
                )
                count++
                if (count % 50 == 0) {
                    onProgress(count)
                }
            }
        }

        images
    }

    private fun buildProjection(): Array<String> {
        val base = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            base.add(MediaStore.Images.Media.DATA)
        }
        return base.toTypedArray()
    }

    private class ColumnIndexes(cursor: android.database.Cursor) {
        val id = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val displayName = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val size = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        val dateAdded = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        val width = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
        val height = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
        val mimeType = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
        val data = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
    }

    companion object {
        const val DEFAULT_MIN_SIZE = 50 * 1024L // 50KB
    }
}
