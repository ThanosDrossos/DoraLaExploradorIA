package com.unam.dora

import androidx.room.TypeConverter
import java.util.Date

class Converters {
    @TypeConverter fun fromSender(sender: Sender): String = sender.name
    @TypeConverter fun toSender(name: String): Sender = Sender.valueOf(name)
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}