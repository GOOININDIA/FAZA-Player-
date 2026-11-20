package com.example.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

fun hasAllFilesAccessPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching { Environment.isExternalStorageManager() }.getOrDefault(false)
    } else {
        true
    }
}

@SuppressLint("InlinedApi")
fun buildAllFilesAccessSystemIntents(
    packageName: String,
    sdkInt: Int = Build.VERSION.SDK_INT
): List<Intent> {
    val packageUri = Uri.fromParts("package", packageName, null)
    return if (sdkInt >= Build.VERSION_CODES.R) {
        listOf(
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, packageUri),
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
        )
    } else {
        listOf(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
        )
    }
}

fun openSystemAllFilesAccessSettings(context: Context): Boolean {
    val intents = buildAllFilesAccessSystemIntents(context.packageName)
    for (baseIntent in intents) {
        val intent = Intent(baseIntent).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        val launched = runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
        if (launched) return true
    }
    return false
}
