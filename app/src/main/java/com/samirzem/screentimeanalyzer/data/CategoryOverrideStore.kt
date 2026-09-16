package com.samirzem.screentimeanalyzer.data

import android.content.Context

/**
 * Manual per-app category overrides, keyed by package name. Kept in SharedPreferences
 * rather than the Room cache: that DB is destructively rebuilt on every schema bump,
 * but a user's category choice isn't recomputable from UsageStatsManager so it needs
 * to survive that.
 */
class CategoryOverrideStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(packageName: String): String? = prefs.getString(packageName, null)

    fun set(packageName: String, category: String) {
        prefs.edit().putString(packageName, category).apply()
    }

    private companion object {
        const val PREFS_NAME = "category_overrides"
    }
}
