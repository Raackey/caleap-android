package com.word2prompt.android.data

import org.json.JSONArray
import org.json.JSONObject

fun JSONObject.stringList(key: String): List<String> = (optJSONArray(key) ?: JSONArray()).let { arr -> buildList { for (i in 0 until arr.length()) arr.optString(i).trim().takeIf { it.isNotEmpty() }?.let(::add) } }
