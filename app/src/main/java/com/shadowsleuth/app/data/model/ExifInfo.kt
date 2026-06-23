package com.shadowsleuth.app.data.model

/**
 * 图片 EXIF 信息数据模型
 */
data class ExifInfo(
    val hasExif: Boolean,
    val make: String?,
    val model: String?,
    val dateTimeOriginal: String?,
    val dateTimeDigitized: String?,
    val gpsLatitude: String?,
    val gpsLongitude: String?,
    val gpsAltitude: String?,
    val aperture: String?,
    val exposureTime: String?,
    val iso: String?,
    val focalLength: String?,
    val flash: String?,
    val whiteBalance: String?,
    val orientation: String?,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val mimeType: String,
    val dateAdded: Long
) {
    val formattedDateTime: String?
        get() = dateTimeOriginal ?: dateTimeDigitized
}
