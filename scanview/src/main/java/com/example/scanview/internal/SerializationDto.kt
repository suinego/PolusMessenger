package com.example.scanview.internal

import android.graphics.Rect
import com.example.scanview.data.*


data class BoundsDto(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

data class BackgroundStateDto(
    val type: String,           // "color" | "unknown"
    val color: Int? = null,
    val drawableClass: String? = null
)

data class ViewContentDto(
    val type: String,           // "text" | "image_placeholder"
    val text: String? = null,
    val textColor: Int? = null,
    val textSizePx: Float? = null,
    val isBold: Boolean? = null,
    val tint: Int? = null
)

data class ViewNodeDto(
    val className: String,
    val bounds: BoundsDto,
    val background: BackgroundStateDto?,
    val alpha: Float,
    val visibility: Int,
    val content: ViewContentDto?,
    val children: List<ViewNodeDto>
)

data class ViewInfoDto(
    val className: String,
    val id: Int?,
    val idName: String?,
    val bounds: BoundsDto?,
    val text: String?,
    val viewNode: ViewNodeDto? = null
)

data class TouchEventDto(
    val action: String,
    val x: Float,
    val y: Float,
    val localX: Float,
    val localY: Float,
    val timestamp: Long
)

data class GestureDto(
    val type: String,
    val startEvent: TouchEventDto,
    val endEvent: TouchEventDto?,
    val posledovatelnostMoveEvent: List<TouchEventDto>
)

data class InteractionRecordDto(
    val screenName: String,
    val viewInfo: ViewInfoDto,
    val gesture: GestureDto,
    val timestamp: Long
)

private fun BackgroundState.toDto(): BackgroundStateDto = when (this) {
    is BackgroundState.Color -> BackgroundStateDto(type = "color", color = color)
    is BackgroundState.Unknown -> BackgroundStateDto(type = "unknown", drawableClass = drawableClass)
}

private fun ViewContent.toDto(): ViewContentDto = when (this) {
    is ViewContent.Text -> ViewContentDto(
        type = "text",
        text = text,
        textColor = textColor,
        textSizePx = textSizePx,
        isBold = isBold
    )
    is ViewContent.ImagePlaceholder -> ViewContentDto(type = "image_placeholder", tint = tint)
}

private fun ViewNode.toDto(): ViewNodeDto = ViewNodeDto(
    className = className,
    bounds = BoundsDto(bounds.left, bounds.top, bounds.right, bounds.bottom),
    background = background?.toDto(),
    alpha = alpha,
    visibility = visibility,
    content = content?.toDto(),
    children = children.map { it.toDto() }
)

private fun TouchEvent.toDto() = TouchEventDto(
    action = action,
    x = x,
    y = y,
    localX = localX,
    localY = localY,
    timestamp = timestamp
)

// ---- десериализация DTO → domain ----

internal fun TouchEventDto.toDomain() = TouchEvent(
    action = action, x = x, y = y, localX = localX, localY = localY, timestamp = timestamp
)

internal fun BackgroundStateDto.toDomain(): BackgroundState? = when (type) {
    "color" -> color?.let { BackgroundState.Color(it) }
    "unknown" -> drawableClass?.let { BackgroundState.Unknown(it) }
    else -> null
}

internal fun ViewContentDto.toDomain(): ViewContent? = when (type) {
    "text" -> ViewContent.Text(text ?: "", textColor ?: 0, textSizePx ?: 14f, isBold ?: false)
    "image_placeholder" -> ViewContent.ImagePlaceholder(tint)
    else -> null
}

internal fun ViewNodeDto.toDomain(): ViewNode = ViewNode(
    className = className,
    bounds = Rect(bounds.left, bounds.top, bounds.right, bounds.bottom),
    background = background?.toDomain(),
    alpha = alpha,
    visibility = visibility,
    content = content?.toDomain(),
    children = children.map { it.toDomain() }
)

internal fun ViewInfoDto.toDomain() = ViewInfo(
    className = className,
    id = id,
    idName = idName,
    bounds = bounds?.let { Rect(it.left, it.top, it.right, it.bottom) },
    text = text,
    viewNode = viewNode?.toDomain()
)

internal fun GestureDto.toDomain() = Gesture(
    type = GestureType.valueOf(type),
    startEvent = startEvent.toDomain(),
    endEvent = endEvent?.toDomain(),
    posledovatelnostMoveEvent = posledovatelnostMoveEvent.map { it.toDomain() }
)

internal fun InteractionRecordDto.toDomain() = InteractionRecord(
    screenName = screenName,
    viewInfo = viewInfo.toDomain(),
    gesture = gesture.toDomain(),
    timestamp = timestamp
)

// ---- сериализация domain → DTO ----

fun InteractionRecord.toDto(): InteractionRecordDto {
    val viewInfoDto = ViewInfoDto(
        className = viewInfo.className,
        id = viewInfo.id,
        idName = viewInfo.idName,
        bounds = viewInfo.bounds?.let { BoundsDto(it.left, it.top, it.right, it.bottom) },
        text = viewInfo.text,
        viewNode = viewInfo.viewNode?.toDto()
    )

    val gestureDto = GestureDto(
        type = gesture.type.name,
        startEvent = gesture.startEvent.toDto(),
        endEvent = gesture.endEvent?.toDto(),
        posledovatelnostMoveEvent = gesture.posledovatelnostMoveEvent.map { it.toDto() }
    )

    return InteractionRecordDto(
        screenName = screenName,
        viewInfo = viewInfoDto,
        gesture = gestureDto,
        timestamp = timestamp
    )
}
