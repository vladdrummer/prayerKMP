package org.example.prayerkmp.feature.mainmenu.view_model

import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.DrawableResource

data class MainMenuItem (
    val id: Int,
    val title: String,
    val resId: DrawableResource
)