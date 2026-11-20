package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface FazaRoute {
    @Serializable
    data object Library : FazaRoute

    @Serializable
    data object Folders : FazaRoute

    @Serializable
    data class FolderDetail(val folderName: String, val folderUri: String? = null) : FazaRoute

    @Serializable
    data object Updates : FazaRoute

    @Serializable
    data object Profile : FazaRoute

    @Serializable
    data object Search : FazaRoute

    @Serializable
    data object Settings : FazaRoute

    @Serializable
    data class Player(val videoId: Long) : FazaRoute
}
