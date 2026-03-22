package com.example.scanview.internal

import com.example.scanview.data.*
import com.google.gson.annotations.SerializedName


data class BoundsDto(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

data class ViewInfoDto(
    val className: String,
    val id: Int?,
    val idName: String?,
    val bounds: BoundsDto?,
    val text: String?
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
    val viewInfo: ViewInfoDto,
    val gesture: GestureDto,
    val timestamp: Long
)

fun InteractionRecord.toDto(): InteractionRecordDto {
    val boundsDto = viewInfo.bounds?.let {
        BoundsDto(it.left, it.top, it.right, it.bottom)
    }
    
    val viewInfoDto = ViewInfoDto(
        className = viewInfo.className,
        id = viewInfo.id,
        idName = viewInfo.idName,
        bounds = boundsDto,
        text = viewInfo.text
    )
    
    val startEventDto = TouchEventDto(
        action = gesture.startEvent.action,
        x = gesture.startEvent.x,
        y = gesture.startEvent.y,
        localX = gesture.startEvent.localX,
        localY = gesture.startEvent.localY,
        timestamp = gesture.startEvent.timestamp
    )
    
    val endEventDto = gesture.endEvent?.let {
        TouchEventDto(
            action = it.action,
            x = it.x,
            y = it.y,
            localX = it.localX,
            localY = it.localY,
            timestamp = it.timestamp
        )
    }
    
    val intermediateEventsDto = gesture.posledovatelnostMoveEvent.map {
        TouchEventDto(
            action = it.action,
            x = it.x,
            y = it.y,
            localX = it.localX,
            localY = it.localY,
            timestamp = it.timestamp
        )
    }
    
    val gestureDto = GestureDto(
        type = gesture.type.name,
        startEvent = startEventDto,
        endEvent = endEventDto,
        posledovatelnostMoveEvent = intermediateEventsDto
    )
    
    return InteractionRecordDto(
        viewInfo = viewInfoDto,
        gesture = gestureDto,
        timestamp = timestamp
    )
}
