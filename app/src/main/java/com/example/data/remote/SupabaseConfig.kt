package com.example.data.remote

import com.example.BuildConfig

object SupabaseConfig {
    val rawUrl: String = BuildConfig.SUPABASE_URL
    val anonKey: String = BuildConfig.SUPABASE_ANON_KEY

    val isConfigured: Boolean
        get() {
            val url = rawUrl.trim()
            val key = anonKey.trim()
            return url.isNotEmpty() &&
                    !url.contains("MY_SUPABASE_URL", ignoreCase = true) &&
                    (url.startsWith("http://") || url.startsWith("https://")) &&
                    key.isNotEmpty() &&
                    !key.contains("MY_SUPABASE_ANON_KEY", ignoreCase = true)
        }

    fun getSafeBaseUrl(): String {
        val trimmed = rawUrl.trim()
        if (isConfigured) {
            return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        }
        return "https://placeholder.supabase.co/"
    }
}
