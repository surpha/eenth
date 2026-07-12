package com.eenth.blocker

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class AppGroup(
    val id: String,
    val name: String,
    val emoji: String,
    val packages: Set<String>,
    val isPreset: Boolean,
    var isSelected: Boolean = false
)

object GroupManager {

    private const val KEY_GROUPS = "app_groups"
    private const val KEY_SELECTED_GROUPS = "selected_groups"

    // Pre-defined groups with common package names
    val presetGroups = listOf(
        AppGroup(
            id = "social_media",
            name = "Social Media",
            emoji = "📱",
            isPreset = true,
            packages = setOf(
                "com.instagram.android",
                "com.facebook.katana",
                "com.facebook.lite",
                "com.twitter.android",
                "com.twitter.android.lite",
                "com.zhiliaoapp.musically",  // TikTok
                "com.snapchat.android",
                "com.reddit.frontpage",
                "com.linkedin.android",
                "com.pinterest",
                "com.instagram.threads",
                "com.tumblr",
                "com.bsky.android"
            )
        ),
        AppGroup(
            id = "video_streaming",
            name = "Streaming",
            emoji = "🎬",
            isPreset = true,
            packages = setOf(
                "com.google.android.youtube",
                "com.netflix.mediaclient",
                "com.disney.disneyplus",
                "com.amazon.avod.thirdpartyclient",
                "com.hbo.hbonow",
                "in.startv.hotstar",  // Hotstar/JioCinema
                "com.jio.media.ondemand",
                "com.voot.android",
                "com.sonyliv",
                "com.mxtech.videoplayer.ad",
                "com.apple.atve.androidtv.appletv",
                "com.peacocktv.peacockandroid",
                "com.crunchyroll.crunchyroid"
            )
        ),
        AppGroup(
            id = "messaging",
            name = "Messaging",
            emoji = "💬",
            isPreset = true,
            packages = setOf(
                "com.whatsapp",
                "com.whatsapp.w4b",
                "org.telegram.messenger",
                "org.thoughtcrime.securesms",  // Signal
                "com.facebook.orca",  // Messenger
                "com.discord",
                "com.slack",
                "com.Slack",
                "com.google.android.apps.messaging",
                "com.samsung.android.messaging",
                "com.viber.voip",
                "jp.naver.line.android"
            )
        ),
        AppGroup(
            id = "games",
            name = "Games",
            emoji = "🎮",
            isPreset = true,
            packages = setOf(
                "com.supercell.clashofclans",
                "com.supercell.clashroyale",
                "com.supercell.brawlstars",
                "com.king.candycrushsaga",
                "com.activision.callofduty.shooter",
                "com.garena.game.codm",
                "com.tencent.ig",  // PUBG Mobile
                "com.pubg.krmobile",
                "com.dts.freefireth",
                "com.mojang.minecraftpe",
                "com.roblox.client",
                "com.innersloth.spacemafia",  // Among Us
                "com.epicgames.fortnite",
                "com.mobile.legends",
                "com.ea.game.pvzfree_row",
                "com.halfbrick.fruitninjafree",
                "com.imangi.templerun2",
                "com.miniclip.eightballpool",
                "com.rovio.baba"  // Angry Birds
            )
        ),
        AppGroup(
            id = "shopping",
            name = "Shopping",
            emoji = "🛒",
            isPreset = true,
            packages = setOf(
                "com.amazon.mShop.android.shopping",
                "com.flipkart.android",
                "com.myntra.android",
                "com.ajio.android",
                "club.cred",
                "in.swiggy.android",
                "com.application.zomato",
                "com.ubercab.eats",
                "com.blinkit.android",
                "com.zepto.android",
                "com.nykaa.android",
                "com.meesho.supply",
                "com.jio.jiomart"
            )
        )
    )

    fun loadGroups(prefs: SharedPreferences, installedPackages: Set<String>): List<AppGroup> {
        val selectedIds = prefs.getStringSet(KEY_SELECTED_GROUPS, emptySet()) ?: emptySet()
        val customGroupsJson = prefs.getString(KEY_GROUPS, null)

        // Load preset groups - use customized packages if saved, else defaults
        val presets = presetGroups.map { group ->
            val customPkgs = prefs.getStringSet("group_pkgs_${group.id}", null)
            val packages = if (customPkgs != null) {
                customPkgs.filter { installedPackages.contains(it) }.toSet()
            } else {
                group.packages.intersect(installedPackages)
            }
            group.copy(
                packages = packages,
                isSelected = selectedIds.contains(group.id)
            )
        }.filter { it.packages.isNotEmpty() }  // Only show presets that have installed apps

        // Load custom groups
        val customs = mutableListOf<AppGroup>()
        if (customGroupsJson != null) {
            val arr = JSONArray(customGroupsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val groupId = obj.getString("id")
                val customPkgs = prefs.getStringSet("group_pkgs_$groupId", null)
                val pkgs = if (customPkgs != null) {
                    customPkgs.filter { installedPackages.contains(it) }.toSet()
                } else {
                    val pkgArr = obj.getJSONArray("packages")
                    val fromJson = mutableSetOf<String>()
                    for (j in 0 until pkgArr.length()) {
                        val pkg = pkgArr.getString(j)
                        if (installedPackages.contains(pkg)) fromJson.add(pkg)
                    }
                    fromJson
                }
                if (pkgs.isNotEmpty()) {
                    customs.add(
                        AppGroup(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            emoji = obj.getString("emoji"),
                            packages = pkgs,
                            isPreset = false,
                            isSelected = selectedIds.contains(obj.getString("id"))
                        )
                    )
                }
            }
        }

        return presets + customs
    }

    fun saveCustomGroup(prefs: SharedPreferences, group: AppGroup) {
        val existing = prefs.getString(KEY_GROUPS, null)
        val arr = if (existing != null) JSONArray(existing) else JSONArray()

        val obj = JSONObject().apply {
            put("id", group.id)
            put("name", group.name)
            put("emoji", group.emoji)
            put("packages", JSONArray(group.packages.toList()))
        }
        arr.put(obj)
        prefs.edit().putString(KEY_GROUPS, arr.toString()).apply()
    }

    fun deleteCustomGroup(prefs: SharedPreferences, groupId: String) {
        val existing = prefs.getString(KEY_GROUPS, null) ?: return
        val arr = JSONArray(existing)
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.getString("id") != groupId) newArr.put(obj)
        }
        prefs.edit().putString(KEY_GROUPS, newArr.toString()).apply()

        // Also remove from selected
        val selected = prefs.getStringSet(KEY_SELECTED_GROUPS, emptySet())?.toMutableSet() ?: mutableSetOf()
        selected.remove(groupId)
        prefs.edit().putStringSet(KEY_SELECTED_GROUPS, selected).apply()
    }

    fun toggleGroup(prefs: SharedPreferences, groupId: String, selected: Boolean) {
        val current = prefs.getStringSet(KEY_SELECTED_GROUPS, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (selected) current.add(groupId) else current.remove(groupId)
        prefs.edit().putStringSet(KEY_SELECTED_GROUPS, current).apply()
    }

    /** Get all blocked packages from selected groups */
    fun getGroupBlockedPackages(prefs: SharedPreferences): Set<String> {
        val selectedIds = prefs.getStringSet(KEY_SELECTED_GROUPS, emptySet()) ?: emptySet()
        if (selectedIds.isEmpty()) return emptySet()

        val packages = mutableSetOf<String>()

        // Check presets
        for (group in presetGroups) {
            if (selectedIds.contains(group.id)) {
                val customPkgs = prefs.getStringSet("group_pkgs_${group.id}", null)
                packages.addAll(customPkgs ?: group.packages)
            }
        }

        // Check custom groups
        val customJson = prefs.getString(KEY_GROUPS, null)
        if (customJson != null) {
            val arr = JSONArray(customJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val groupId = obj.getString("id")
                if (selectedIds.contains(groupId)) {
                    val customPkgs = prefs.getStringSet("group_pkgs_$groupId", null)
                    if (customPkgs != null) {
                        packages.addAll(customPkgs)
                    } else {
                        val pkgArr = obj.getJSONArray("packages")
                        for (j in 0 until pkgArr.length()) {
                            packages.add(pkgArr.getString(j))
                        }
                    }
                }
            }
        }

        return packages
    }

    /** Get packages for a specific group */
    fun getGroupPackages(prefs: SharedPreferences, groupId: String, installedPackages: Set<String>): Set<String> {
        val customPkgs = prefs.getStringSet("group_pkgs_$groupId", null)
        if (customPkgs != null) return customPkgs.filter { installedPackages.contains(it) }.toSet()

        val preset = presetGroups.find { it.id == groupId }
        if (preset != null) return preset.packages.intersect(installedPackages)

        val customJson = prefs.getString(KEY_GROUPS, null) ?: return emptySet()
        val arr = JSONArray(customJson)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.getString("id") == groupId) {
                val pkgArr = obj.getJSONArray("packages")
                val pkgs = mutableSetOf<String>()
                for (j in 0 until pkgArr.length()) {
                    val pkg = pkgArr.getString(j)
                    if (installedPackages.contains(pkg)) pkgs.add(pkg)
                }
                return pkgs
            }
        }
        return emptySet()
    }

    fun addToGroup(prefs: SharedPreferences, groupId: String, packageName: String, installedPackages: Set<String>) {
        val current = getGroupPackages(prefs, groupId, installedPackages).toMutableSet()
        current.add(packageName)
        prefs.edit().putStringSet("group_pkgs_$groupId", current).apply()
    }

    fun removeFromGroup(prefs: SharedPreferences, groupId: String, packageName: String, installedPackages: Set<String>) {
        val current = getGroupPackages(prefs, groupId, installedPackages).toMutableSet()
        current.remove(packageName)
        prefs.edit().putStringSet("group_pkgs_$groupId", current).apply()
    }
}
