package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val layersJson: String = "[]",
    val canvasBgColor: Int = -1, // White
    val canvasTemplate: String = "BLANK", // CanvasTemplate name
    val canvasWidth: Float = 1080f,
    val canvasHeight: Float = 1920f
)
