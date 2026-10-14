package com.example.clockapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AlarmStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("alarms", Context.MODE_PRIVATE)

    fun load(): List<Alarm> {
        val raw = prefs.getString("list", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val d = o.getJSONArray("days")
            Alarm(
                id = o.getInt("id"),
                hour = o.getInt("hour"),
                minute = o.getInt("minute"),
                label = o.optString("label", ""),
                enabled = o.optBoolean("enabled", true),
                days = (0 until d.length()).map { d.getInt(it) }.toSet(),
                ringtone = o.optString("ringtone", "").ifEmpty { null }
            )
        }
    }

    fun save(alarms: List<Alarm>) {
        val arr = JSONArray()
        alarms.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("hour", a.hour)
                put("minute", a.minute)
                put("label", a.label)
                put("enabled", a.enabled)
                put("days", JSONArray(a.days.toList()))
                put("ringtone", a.ringtone ?: "")
            })
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }

    fun get(id: Int): Alarm? = load().firstOrNull { it.id == id }

    fun nextId(): Int = (load().maxOfOrNull { it.id } ?: 0) + 1
}
