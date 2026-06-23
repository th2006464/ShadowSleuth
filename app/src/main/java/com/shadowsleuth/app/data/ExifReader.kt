package com.shadowsleuth.app.data

import android.content.Context
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import com.shadowsleuth.app.data.model.ExifInfo
import com.shadowsleuth.app.data.model.ImageMetadata

/**
 * 读取图片 EXIF 元信息
 */
object ExifReader {

    fun read(context: Context, image: ImageMetadata): ExifInfo {
        var inputStream = context.contentResolver.openInputStream(image.uri)
        val exif = inputStream?.use { ExifInterface(it) }
        inputStream = context.contentResolver.openInputStream(image.uri)
        val options = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        inputStream?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) }

        val hasExif = exif?.getAttribute(ExifInterface.TAG_MAKE) != null

        return ExifInfo(
            hasExif = hasExif,
            make = exif?.getAttribute(ExifInterface.TAG_MAKE),
            model = exif?.getAttribute(ExifInterface.TAG_MODEL),
            dateTimeOriginal = exif?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
            dateTimeDigitized = exif?.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED),
            gpsLatitude = exif?.latLong?.takeIf { it.size >= 2 }?.get(0)?.toString(),
            gpsLongitude = exif?.latLong?.takeIf { it.size >= 2 }?.get(1)?.toString(),
            gpsAltitude = exif?.getAttribute(ExifInterface.TAG_GPS_ALTITUDE),
            aperture = exif?.getAttribute(ExifInterface.TAG_F_NUMBER),
            exposureTime = exif?.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
            iso = exif?.getAttribute(ExifInterface.TAG_ISO_SPEED),
            focalLength = exif?.getAttribute(ExifInterface.TAG_FOCAL_LENGTH),
            flash = exif?.getAttribute(ExifInterface.TAG_FLASH),
            whiteBalance = exif?.getAttribute(ExifInterface.TAG_WHITE_BALANCE),
            orientation = (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                ?: ExifInterface.ORIENTATION_NORMAL)
                .let { getOrientation(it) },
            width = image.width.takeIf { it > 0 } ?: options.outWidth,
            height = image.height.takeIf { it > 0 } ?: options.outHeight,
            sizeBytes = image.sizeBytes,
            mimeType = image.mimeType.ifBlank { options.outMimeType ?: "image/*" },
            dateAdded = image.dateAdded
        )
    }

    private fun getOrientation(orientation: Int): String {
        return when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> "正常"
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "水平翻转"
            ExifInterface.ORIENTATION_ROTATE_180 -> "旋转 180°"
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> "垂直翻转"
            ExifInterface.ORIENTATION_TRANSPOSE -> "转置"
            ExifInterface.ORIENTATION_ROTATE_90 -> "顺时针 90°"
            ExifInterface.ORIENTATION_TRANSVERSE -> "反向转置"
            ExifInterface.ORIENTATION_ROTATE_270 -> "顺时针 270°"
            else -> "未知"
        }
    }
}
