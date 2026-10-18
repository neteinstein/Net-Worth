package com.neteinstein.networth.core.designsystem.ui.icons

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The line icons drawn on the artboards (24×24, round stroke), so the app does not need the large
 * material-icons-extended artifact. Tint them like any icon; the stroke colour is replaced by tint.
 */
object NetWorthIcons {
    val Sliders: ImageVector by lazy {
        strokeIcon(
            "Sliders",
            "M4 6h10",
            "M18 6h2",
            "M16 4a2 2 0 1 0 0 4a2 2 0 1 0 0-4z",
            "M4 12h4",
            "M12 12h8",
            "M10 10a2 2 0 1 0 0 4a2 2 0 1 0 0-4z",
            "M4 18h12",
            "M18 16a2 2 0 1 0 0 4a2 2 0 1 0 0-4z",
        )
    }
    val ChevronDown: ImageVector by lazy { strokeIcon("ChevronDown", "M6 9l6 6 6-6") }
    val ChevronUp: ImageVector by lazy { strokeIcon("ChevronUp", "M6 15l6-6 6 6") }
    val ChevronRight: ImageVector by lazy { strokeIcon("ChevronRight", "M9 6l6 6-6 6") }
    val ChevronLeft: ImageVector by lazy { strokeIcon("ChevronLeft", "M15 6l-6 6 6 6") }
    val TrendUp: ImageVector by lazy { strokeIcon("TrendUp", "M3 17l6-6 4 4 8-8", "M15 7h6v6") }
    val Home: ImageVector by lazy { strokeIcon("Home", "M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z") }
    val Bars: ImageVector by lazy { strokeIcon("Bars", "M5 20V10", "M12 20V4", "M19 20v-7") }
    val Download: ImageVector by lazy { strokeIcon("Download", "M12 4v11", "M7 10l5 5 5-5", "M5 20h14") }
    val Bell: ImageVector by lazy { strokeIcon("Bell", "M6 16v-5a6 6 0 1 1 12 0v5l2 2H4z", "M10 21h4") }
    val Refresh: ImageVector by lazy { strokeIcon("Refresh", "M20 11a8 8 0 1 0-2.3 5.7", "M20 4v7h-7") }
    val Settings: ImageVector by lazy {
        strokeIcon(
            "Settings",
            "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38" +
                "a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38" +
                "a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18" +
                "a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08" +
                "a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08" +
                "a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z",
            "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z",
        )
    }

    private fun strokeIcon(name: String, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach { data ->
            builder.addPath(
                pathData = addPathNodes(data),
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}
