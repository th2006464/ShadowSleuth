package com.shadowsleuth.app.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shadowsleuth.app.R
import com.shadowsleuth.app.data.model.ExifInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExifDetailDialog(
    exifInfo: ExifInfo,
    imageUri: android.net.Uri,
    displayName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateAddedText = rememberFormattedDate(exifInfo.dateAdded)
    val exifDateText = exifInfo.formattedDateTime

    SsDialog(
        onDismiss = onDismiss,
        title = "图片详细信息",
        dismissText = stringResource(R.string.close)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = displayName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            DetailRow(label = stringResource(R.string.name), value = displayName)
            DetailRow(
                label = stringResource(R.string.size),
                value = Formatter.formatFileSize(context, exifInfo.sizeBytes)
            )
            DetailRow(
                label = stringResource(R.string.dimensions),
                value = "${exifInfo.width} × ${exifInfo.height}"
            )
            DetailRow(label = stringResource(R.string.format), value = exifInfo.mimeType)
            DetailRow(label = "添加到相册时间", value = dateAddedText)
            if (!exifDateText.isNullOrBlank()) {
                DetailRow(label = "EXIF 拍摄时间", value = exifDateText)
            }
            if (!exifInfo.make.isNullOrBlank()) {
                DetailRow(label = "设备厂商", value = exifInfo.make)
            }
            if (!exifInfo.model.isNullOrBlank()) {
                DetailRow(label = "设备型号", value = exifInfo.model)
            }
            if (!exifInfo.gpsLatitude.isNullOrBlank() && !exifInfo.gpsLongitude.isNullOrBlank()) {
                DetailRow(
                    label = "GPS 位置",
                    value = "纬度 ${exifInfo.gpsLatitude}, 经度 ${exifInfo.gpsLongitude}"
                )
            }
            if (!exifInfo.aperture.isNullOrBlank()) {
                DetailRow(label = "光圈", value = "f/${exifInfo.aperture}")
            }
            if (!exifInfo.exposureTime.isNullOrBlank()) {
                DetailRow(label = "曝光时间", value = exifInfo.exposureTime)
            }
            if (!exifInfo.iso.isNullOrBlank()) {
                DetailRow(label = "ISO", value = exifInfo.iso)
            }
            if (!exifInfo.focalLength.isNullOrBlank()) {
                DetailRow(label = "焦距", value = "${exifInfo.focalLength} mm")
            }
            if (!exifInfo.flash.isNullOrBlank()) {
                DetailRow(label = "闪光灯", value = exifInfo.flash)
            }
            if (!exifInfo.whiteBalance.isNullOrBlank()) {
                DetailRow(label = "白平衡", value = exifInfo.whiteBalance)
            }
            if (!exifInfo.orientation.isNullOrBlank()) {
                DetailRow(label = "方向", value = exifInfo.orientation)
            }
            if (!exifInfo.hasExif) {
                Text(
                    text = "该图片未包含 EXIF 元信息，仅显示文件基础信息。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun rememberFormattedDate(timestamp: Long): String {
    return if (timestamp > 0) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(timestamp))
    } else {
        "未知"
    }
}
