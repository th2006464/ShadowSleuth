package com.shadowsleuth.app.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shadowsleuth.app.R
import com.shadowsleuth.app.data.model.ImageMetadata
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Image Detail Dialog (replaces system AlertDialog) ─────────────────────────
@Composable
fun ImageDetailDialog(
    image: ImageMetadata,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateText = rememberFormattedDate(image.dateAdded)

    SsDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.image_details),
        dismissText = stringResource(R.string.close)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Thumbnail
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(image.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = image.displayName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp))
            )

            DetailRow(label = stringResource(R.string.name), value = image.displayName)
            DetailRow(label = stringResource(R.string.size), value = Formatter.formatFileSize(context, image.sizeBytes))
            DetailRow(label = stringResource(R.string.dimensions), value = image.formattedDimensions)
            DetailRow(label = stringResource(R.string.format), value = image.mimeType)
            DetailRow(label = stringResource(R.string.date), value = dateText)
            DetailRow(label = stringResource(R.string.path), value = image.path.ifBlank { "未知路径" })
        }
    }
}

// ── Delete Confirm Dialog (replaces system AlertDialog) ──────────────────────
@Composable
fun DeleteConfirmDialog(
    image: ImageMetadata,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    SsDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.delete_image),
        icon = Icons.Filled.Delete,
        iconTint = MaterialTheme.colorScheme.error,
        confirmText = stringResource(R.string.delete),
        onConfirm = onConfirm,
        confirmColor = MaterialTheme.colorScheme.error,
        dismissText = stringResource(R.string.cancel)
    ) {
        Text(
            text = stringResource(R.string.delete_confirm_message, image.displayName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Image Action Bottom Sheet (replaces ModalBottomSheet) ────────────────────
@Composable
fun ImageActionBottomSheet(
    image: ImageMetadata,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    SsActionSheet(onDismiss = onDismiss) {
        // Title
        Text(
            text = image.displayName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            maxLines = 1
        )
        // Action items
        SsActionSheetItem(
            icon = Icons.Filled.Info,
            text = stringResource(R.string.view_details),
            onClick = onViewDetails,
            iconTint = MaterialTheme.colorScheme.primary
        )
        SsActionSheetItem(
            icon = Icons.Filled.Delete,
            text = stringResource(R.string.delete),
            onClick = onDelete,
            iconTint = MaterialTheme.colorScheme.error,
            textColor = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

// ── Internal helpers ─────────────────────────────────────────────────────────

@Composable
internal fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun rememberFormattedDate(timestamp: Long): String {
    return remember(timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(timestamp))
    }
}
