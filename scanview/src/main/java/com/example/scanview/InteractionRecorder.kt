package com.example.scanview

import android.content.Context
import android.graphics.Rect
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

class InteractionRecorder(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val interactionsFile: File by lazy { File(context.filesDir, "user_interactions.json") }
    private val interactions = mutableListOf<InteractionData>()
    fun recordInteraction(info: ViewInteractionInfo) {
        val interaction = InteractionData(
            timestamp = info.timestamp,
            action = info.action,
            viewClassName = info.viewClassName,
            viewId = info.viewId,
            viewIdName = info.viewIdName,
            localX = info.localX,
            localY = info.localY,
            screenX = info.screenX,
            screenY = info.screenY,
            viewWidth = info.viewWidth,
            viewHeight = info.viewHeight,
            viewBoundsLeft = info.viewBoundsLeft,
            viewBoundsTop = info.viewBoundsTop,
            viewBoundsRight = info.viewBoundsRight,
            viewBoundsBottom = info.viewBoundsBottom,
            isInsideBounds = info.isInsideBounds
        )
        interactions.add(interaction)
    }


    suspend fun save(): File = withContext(Dispatchers.IO) {
        val json = gson.toJson(interactions)
        interactionsFile.writeText(json)
        interactionsFile
    }

    fun clearInMemory() = interactions.clear()

    suspend fun deleteFile() = withContext(Dispatchers.IO) {
        if (interactionsFile.exists()) interactionsFile.delete()
    }

    suspend fun load(): List<InteractionData> = withContext(Dispatchers.IO) {
        if (!interactionsFile.exists()) return@withContext emptyList()
        val json = interactionsFile.readText()
        val arr = gson.fromJson(json, Array<InteractionData>::class.java)
        arr.toList()
    }
}

data class InteractionData(
    val timestamp: Long,
    val action: String,
    val viewClassName: String,
    val viewId: Int?,
    val viewIdName: String?,
    val localX: Float,
    val localY: Float,
    val screenX: Float,
    val screenY: Float,
    val viewWidth: Int,
    val viewHeight: Int,
    val viewBoundsLeft: Int?,
    val viewBoundsTop: Int?,
    val viewBoundsRight: Int?,
    val viewBoundsBottom: Int?,
    val isInsideBounds: Boolean
)

