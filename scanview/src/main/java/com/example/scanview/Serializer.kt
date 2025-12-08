package com.example.scanview

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object Serializer {
    val gson: Gson = GsonBuilder().setPrettyPrinting().create()
}