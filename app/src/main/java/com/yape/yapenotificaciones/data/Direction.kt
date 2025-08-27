package com.yape.yapenotificaciones.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

enum class Direction { RECEIVED, SENT }

@Entity(
    tableName = "yapeos",
    indices = [Index(value = ["uniqueKey"], unique = true)]
)
data class Yapeo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long,
    val packageName: String,
    val direction: Direction,
    val amount: Double,
    val currency: String,
    val counterpart: String,
    val rawText: String,
    val uniqueKey: String
)
