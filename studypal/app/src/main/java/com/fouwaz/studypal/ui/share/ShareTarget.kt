package com.fouwaz.studypal.ui.share

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class ShareTarget(
    val id: String,
    val displayName: String,
    val packageName: String?,
    val icon: ImageVector
) {
    object Notion : ShareTarget(
        id = "notion",
        displayName = "Notion",
        packageName = "notion.id",
        icon = Icons.Default.Description
    )

    object GoogleDocs : ShareTarget(
        id = "google_docs",
        displayName = "Google Docs",
        packageName = "com.google.android.apps.docs.editors.docs",
        icon = Icons.Default.Description
    )

    object GoogleKeep : ShareTarget(
        id = "google_keep",
        displayName = "Google Keep",
        packageName = "com.google.android.keep",
        icon = Icons.Default.Note
    )

    object CopyToClipboard : ShareTarget(
        id = "copy",
        displayName = "Copy to Clipboard",
        packageName = null, // Special action, no package
        icon = Icons.Default.ContentCopy
    )

    object More : ShareTarget(
        id = "more",
        displayName = "More Options",
        packageName = null, // Triggers generic share sheet
        icon = Icons.Default.MoreHoriz
    )

    companion object {
        fun getAllTargets() = listOf(
            Notion,
            GoogleDocs,
            GoogleKeep,
            CopyToClipboard,
            More
        )

        fun getAppTargets() = listOf(
            Notion,
            GoogleDocs,
            GoogleKeep
        )
    }
}
