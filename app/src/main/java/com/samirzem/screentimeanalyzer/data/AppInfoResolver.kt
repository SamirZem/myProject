package com.samirzem.screentimeanalyzer.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache

data class ResolvedAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val category: String,
)

/**
 * Looks up (and caches) the human-readable label, icon and category for a package
 * name reported by [UsageAnalyticsEngine]. Uninstalled apps that still show up in
 * historical usage data fall back to the raw package name.
 */
class AppInfoResolver(context: Context) {

    private val packageManager = context.applicationContext.packageManager
    private val overrides = CategoryOverrideStore(context)
    private val cache = LruCache<String, ResolvedAppInfo>(256)

    fun resolve(packageName: String): ResolvedAppInfo {
        cache.get(packageName)?.let { return it }

        val resolved = try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            ResolvedAppInfo(
                packageName = packageName,
                label = packageManager.getApplicationLabel(appInfo).toString(),
                icon = try {
                    packageManager.getApplicationIcon(appInfo)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                },
                category = overrides.get(packageName) ?: categoryLabel(appInfo),
            )
        } catch (e: PackageManager.NameNotFoundException) {
            ResolvedAppInfo(
                packageName = packageName,
                label = packageName.substringAfterLast('.'),
                icon = null,
                category = overrides.get(packageName) ?: "Autre",
            )
        }

        cache.put(packageName, resolved)
        return resolved
    }

    /** Manually reassigns [packageName]'s category, overriding whatever the system reports. */
    fun setCategoryOverride(packageName: String, category: String) {
        overrides.set(packageName, category)
        cache.remove(packageName)
    }

    private fun categoryLabel(appInfo: ApplicationInfo): String = when (appInfo.category) {
        ApplicationInfo.CATEGORY_GAME -> "Jeux"
        ApplicationInfo.CATEGORY_AUDIO -> "Musique et audio"
        ApplicationInfo.CATEGORY_VIDEO -> "Vidéo"
        ApplicationInfo.CATEGORY_IMAGE -> "Photo"
        ApplicationInfo.CATEGORY_SOCIAL -> "Réseaux sociaux"
        ApplicationInfo.CATEGORY_NEWS -> "Actualités"
        ApplicationInfo.CATEGORY_MAPS -> "Cartes et navigation"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivité"
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> "Accessibilité"
        else -> "Autre"
    }

    companion object {
        /** Choices offered when manually reassigning an app's category (system set + a few extras). */
        val CATEGORIES = listOf(
            "Réseaux sociaux",
            "Jeux",
            "Musique et audio",
            "Vidéo",
            "Photo",
            "Actualités",
            "Cartes et navigation",
            "Productivité",
            "Communication",
            "Achats",
            "Santé et fitness",
            "Finance",
            "Accessibilité",
            "Autre",
        )
    }
}
