package com.shadowsleuth.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shadowsleuth.app.data.model.DuplicateGroup
import com.shadowsleuth.app.data.model.ImageMetadata

@Composable
fun DuplicateGroupCard(
    group: DuplicateGroup,
    onImageClick: (ImageMetadata) -> Unit,
    onImageLongClick: (ImageMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        // Title + match type badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(10.dp))
            val badgeText = when (group.matchType) {
                DuplicateGroup.MatchType.FILENAME -> "文件名"
                DuplicateGroup.MatchType.SIZE -> "大小"
                DuplicateGroup.MatchType.DHASH -> "dHash"
            }
            val badgeColor = when (group.matchType) {
                DuplicateGroup.MatchType.FILENAME -> MaterialTheme.colorScheme.primary
                DuplicateGroup.MatchType.SIZE -> MaterialTheme.colorScheme.secondary
                DuplicateGroup.MatchType.DHASH -> MaterialTheme.colorScheme.tertiary
            }
            SsBadge(text = badgeText, color = badgeColor)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = group.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Image list
        group.images.forEachIndexed { index, image ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            ImageListItem(
                image = image,
                onClick = { onImageClick(image) },
                onLongClick = { onImageLongClick(image) }
            )
        }
    }
}
