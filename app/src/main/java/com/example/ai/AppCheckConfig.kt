package com.example.ai

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"

const val DEFAULT_FIREBASE_APPCHECK_DEBUG_TOKEN = "e55f2c3d-a060-47e7-8a66-523c0ce67774"

internal fun resolveAppCheckDebugToken(context: Context, intent: Intent?): String {
    val intentToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)?.trim()
    if (!intentToken.isNullOrEmpty()) {
        return intentToken
    }

    val configuredToken = runCatching {
        val resId = context.resources.getIdentifier(
            "firebase_appcheck_debug_token",
            "string",
            context.packageName
        )
        if (resId != 0) context.getString(resId).trim() else null
    }.getOrNull()
    if (!configuredToken.isNullOrEmpty()) {
        return configuredToken
    }

    return DEFAULT_FIREBASE_APPCHECK_DEBUG_TOKEN
}

@SuppressLint("ApplySharedPref")
fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val debugToken = resolveAppCheckDebugToken(context, intent)
            val persistenceKey = FirebaseApp.getInstance().persistenceKey
            val prefs = context.getSharedPreferences(
                "$DEBUG_STORE_PREFIX$persistenceKey",
                Context.MODE_PRIVATE
            )
            // Synchronous commit ensures DebugAppCheckProviderFactory reads the secret immediately.
            prefs.edit()
                .putString(DEBUG_SECRET_KEY, debugToken)
                .commit()
            Log.d(TAG, "App Check debug secret configured successfully.")

            FirebaseAppCheck.getInstance().apply {
                installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
                setTokenAutoRefreshEnabled(true)
            }
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val siteKey = runCatching {
                context.getString(R.string.recaptcha_site_key)
            }.getOrNull().orEmpty()
            if (siteKey.isNotBlank()) {
                FirebaseAppCheck.getInstance().apply {
                    installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    setTokenAutoRefreshEnabled(true)
                }
                Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
            } else {
                Log.e(TAG, "recaptcha_site_key resource string is blank or missing.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}
