package com.unam.dora

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromSender(sender: Sender): String = sender.name
    @TypeConverter fun toSender(name: String): Sender = Sender.valueOf(name)
}