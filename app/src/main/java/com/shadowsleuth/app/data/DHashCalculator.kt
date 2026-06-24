package com.shadowsleuth.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * dHash（差分哈希）计算器
 *
 * 算法步骤：
 * 1. 将图片缩放到 9×8（宽9像素，高8像素）
 * 2. 转换为灰度图
 * 3. 计算每行相邻像素的差分（右-左），得到 8×8 = 64 个 bit
 * 4. 将 64 个 bit 编码为 Long 类型
 *
 * 相似性判断：两张图片 dHash 的汉明距离（bit 差异数）≤ SIMILARITY_THRESHOLD 时视为相似
 */
object DHashCalculator {

    /** 汉明距离阈值，≤ 此值视为相似图片 */
    const val SIMILARITY_THRESHOLD = 10

    /** 缩放目标宽度（差分哈希需要多一列） */
    private const val HASH_WIDTH = 9

    /** 缩放目标高度 */
    private const val HASH_HEIGHT = 8

    /**
     * 计算图片的 dHash（标准方法，加载完整图片）
     * @param context Android 上下文
     * @param uri 图片 URI
     * @return 64-bit dHash 值，计算失败时返回 null
     */
    suspend fun compute(context: Context, uri: Uri): Long? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            original ?: return@withContext null
            computeFromBitmap(original).also { original.recycle() }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 快速计算图片的 dHash（使用 inSampleSize 加载缩小图，速度更快）
     *
     * 对于 15000 张照片，比 [compute] 快 5-10 倍。
     * 原理：先用 inJustDecodeBounds 获取图片尺寸，
     * 再用 inSampleSize 直接加载缩小图（如 1/64 尺寸），
     * 最后缩放到 9×8 计算哈希。
     *
     * @param context Android 上下文
     * @param uri 图片 URI
     * @param targetPreviewSize 加载时的目标大致尺寸（像素），默认 64，
     *                           越小越快但哈希精度略降（推荐 48~128）
     * @return 64-bit dHash 值，计算失败时返回 null
     */
    suspend fun computeFast(
        context: Context,
        uri: Uri,
        targetPreviewSize: Int = 64
    ): Long? = withContext(Dispatchers.IO) {
        try {
            // Step 1: 获取图片原始尺寸
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            }
            val (imgWidth, imgHeight) = boundsOptions.outWidth to boundsOptions.outHeight
            if (imgWidth <= 0 || imgHeight <= 0) return@withContext null

            // Step 2: 计算 inSampleSize，使加载后的图片接近 targetPreviewSize
            val inSampleSize = calculateInSampleSize(imgWidth, imgHeight, targetPreviewSize, targetPreviewSize)

            // Step 3: 用 inSampleSize 加载缩小图
            val loadOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val bitmap = BitmapFactory.decodeStream(inputStream, null, loadOptions)
            inputStream.close()
            bitmap ?: return@withContext null

            computeFromBitmap(bitmap).also { bitmap.recycle() }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 计算 inSampleSize，使解码后的图片尺寸尽量接近（但不少于）reqWidth × reqHeight。
     *
     * 这是 Android 官方推荐算法（参见 BitmapFactory.Options 文档）。
     */
    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * 从 Bitmap 计算 dHash（同步，调用方负责 recycle）
     */
    fun computeFromBitmap(bitmap: Bitmap): Long {
        // 1. 缩放到 9×8
        val scaled = Bitmap.createScaledBitmap(bitmap, HASH_WIDTH, HASH_HEIGHT, true)

        // 2. 读取灰度值并计算差分哈希
        var hash = 0L
        for (row in 0 until HASH_HEIGHT) {
            for (col in 0 until (HASH_WIDTH - 1)) {
                val left = toGray(scaled.getPixel(col, row))
                val right = toGray(scaled.getPixel(col + 1, row))
                hash = hash shl 1
                if (right > left) hash = hash or 1L
            }
        }

        if (scaled != bitmap) scaled.recycle()
        return hash
    }

    /**
     * 计算两个 dHash 之间的汉明距离（不同 bit 位的数量）
     */
    fun hammingDistance(hash1: Long, hash2: Long): Int {
        return java.lang.Long.bitCount(hash1 xor hash2)
    }

    /**
     * 判断两个 dHash 是否视为相似（汉明距离 ≤ SIMILARITY_THRESHOLD）
     */
    fun isSimilar(hash1: Long, hash2: Long): Boolean {
        return hammingDistance(hash1, hash2) <= SIMILARITY_THRESHOLD
    }

    /**
     * ARGB 像素转灰度值（0-255）
     */
    private fun toGray(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        // 标准亮度系数
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }
}
