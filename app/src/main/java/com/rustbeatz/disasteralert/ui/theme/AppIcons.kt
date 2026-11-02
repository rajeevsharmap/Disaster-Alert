package com.rustbeatz.disasteralert.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {

    val ArrowBack: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowBack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null,
                strokeLineWidth = 0f
            ) {
                moveTo(20f, 11f)
                horizontalLineTo(7.83f)
                lineTo(13.42f, 5.41f)
                lineTo(12f, 4f)
                lineTo(4f, 12f)
                lineTo(12f, 20f)
                lineTo(13.41f, 18.59f)
                lineTo(7.83f, 13f)
                horizontalLineTo(20f)
                verticalLineTo(11f)
                close()
            }
        }.build()
    }

    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "Search",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null,
                strokeLineWidth = 0f
            ) {
                moveTo(15.5f, 14f)
                horizontalLineTo(14.71f)
                lineTo(14.43f, 13.73f)
                curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
                curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
                curveTo(5.91f, 3f, 3f, 5.91f, 3f, 9.5f)
                curveTo(3f, 13.09f, 5.91f, 16f, 9.5f, 16f)
                curveTo(11.11f, 16f, 12.59f, 15.41f, 13.73f, 14.43f)
                lineTo(14f, 14.71f)
                verticalLineTo(15.5f)
                lineTo(19f, 20.49f)
                lineTo(20.49f, 19f)
                lineTo(15.5f, 14f)
                close()
                moveTo(9.5f, 14f)
                curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
                curveTo(5f, 7.01f, 7.01f, 5f, 9.5f, 5f)
                curveTo(11.99f, 5f, 14f, 7.01f, 14f, 9.5f)
                curveTo(14f, 11.99f, 11.99f, 14f, 9.5f, 14f)
                close()
            }
        }.build()
    }

    val Clear: ImageVector by lazy {
        ImageVector.Builder(
            name = "Clear",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null,
                strokeLineWidth = 0f
            ) {
                moveTo(19f, 6.41f)
                lineTo(17.59f, 5f)
                lineTo(12f, 10.59f)
                lineTo(6.41f, 5f)
                lineTo(5f, 6.41f)
                lineTo(10.59f, 12f)
                lineTo(5f, 17.59f)
                lineTo(6.41f, 19f)
                lineTo(12f, 13.41f)
                lineTo(17.59f, 19f)
                lineTo(19f, 17.59f)
                lineTo(13.41f, 12f)
                close()
            }
        }.build()
    }

    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "Check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null,
                strokeLineWidth = 0f
            ) {
                moveTo(9f, 16.17f)
                lineTo(4.83f, 12f)
                lineTo(3.41f, 13.41f)
                lineTo(9f, 19f)
                lineTo(21f, 7f)
                lineTo(19.59f, 5.59f)
                close()
            }
        }.build()
    }

    val Warning: ImageVector by lazy {
        ImageVector.Builder(
            name = "Warning",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null,
                strokeLineWidth = 0f
            ) {
                moveTo(1f, 21f)
                horizontalLineTo(23f)
                lineTo(12f, 2f)
                lineTo(1f, 21f)
                close()
                moveTo(13f, 18f)
                horizontalLineTo(11f)
                verticalLineTo(16f)
                horizontalLineTo(13f)
                verticalLineTo(18f)
                close()
                moveTo(13f, 14f)
                horizontalLineTo(11f)
                verticalLineTo(10f)
                horizontalLineTo(13f)
                verticalLineTo(14f)
                close()
            }
        }.build()
    }

    val KeyboardArrowDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "KeyboardArrowDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null,
                strokeLineWidth = 0f
            ) {
                moveTo(7.41f, 8.59f)
                lineTo(12f, 13.17f)
                lineTo(16.59f, 8.59f)
                lineTo(18f, 10f)
                lineTo(12f, 16f)
                lineTo(6f, 10f)
                close()
            }
        }.build()
    }
}
