package com.fouwaz.studypal.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.fouwaz.studypal.ui.share.ShareTarget

object ShareTargetResolver {

    /**
     * Resolves which share targets are available on the device
     */
    fun resolveShareTargets(context: Context): List<ShareTarget> {
        val availableTargets = mutableListOf<ShareTarget>()

        // Check each app target
        ShareTarget.getAppTargets().forEach { target ->
            if (isPackageInstalled(context, target.packageName ?: "")) {
                availableTargets.add(target)
            }
        }

        // Always add Copy and More
        availableTargets.add(ShareTarget.CopyToClipboard)
        availableTargets.add(ShareTarget.More)

        return availableTargets
    }

    /**
     * Checks if a package is installed on the device
     */
    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Builds a share intent for a specific target
     */
    fun buildShareIntent(target: ShareTarget, title: String, content: String): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_SUBJECT, title)

            // Set package if targeting specific app
            target.packageName?.let { packageName ->
                setPackage(packageName)
            }
        }

        return intent
    }

    /**
     * Creates a generic share chooser
     */
    fun createShareChooser(title: String, content: String, chooserTitle: String = "Share with…"): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }

        return Intent.createChooser(intent, chooserTitle)
    }
}
