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
     * 计算图片的 dHash
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
