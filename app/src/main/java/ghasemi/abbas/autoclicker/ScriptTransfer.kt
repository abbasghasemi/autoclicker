package ghasemi.abbas.autoclicker

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Text payload for Android's share sheet. The on-device stream format remains unchanged. */
object ScriptTransfer {
    private const val PREFIX = "AUTOCLICKER_SCRIPT_V1:"
    private const val MAX_BYTES = 2 * 1024 * 1024

    fun encode(script: AppServiceHelper.ScriptsConfig): String {
        val steps = JSONArray()
        script.widgets.forEach { step ->
            steps.put(JSONObject().apply {
                put("type", step.type)
                put("count", step.count)
                put("durationType", step.durationType)
                put("duration", step.duration)
                put("longDuration", step.longDuration)
                put("swipeDuration", step.swipeDuration)
                put("number", step.number)
                put("x1", step.x1)
                put("y1", step.y1)
                put("x2", step.x2)
                put("y2", step.y2)
                put("enabled", step.enabled)
                put("waitText", step.waitText)
                put("waitTimeoutSeconds", step.waitTimeoutSeconds)
                put("gestureMode", step.gestureMode)
                put("curvePercent", step.curvePercent)
                put("waypoints", step.waypoints)
            })
        }
        val json = JSONObject().apply {
            put("version", 1)
            put("name", script.name)
            put("synchronousExecution", script.synchronousExecution)
            put("durationTime", script.durationTime)
            put("relativeCoordinates", script.relativeCoordinates)
            put("referenceWidth", script.referenceWidth)
            put("referenceHeight", script.referenceHeight)
            put("targetPackage", script.targetPackage)
            put("onLeaveAction", script.onLeaveAction)
            put("widgets", steps)
        }
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(json.toString().toByteArray(Charsets.UTF_8)) }
        return PREFIX + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    fun decode(text: String?): AppServiceHelper.ScriptsConfig? {
        if (text == null || !text.startsWith(PREFIX)) return null
        return runCatching {
            require(text.length <= MAX_BYTES * 2)
            val compressed = Base64.decode(text.removePrefix(PREFIX).trim(), Base64.DEFAULT)
            val input = GZIPInputStream(ByteArrayInputStream(compressed))
            val output = ByteArrayOutputStream()
            input.use { stream ->
                val buffer = ByteArray(8192)
                while (true) {
                    val size = stream.read(buffer)
                    if (size < 0) break
                    if (output.size() + size > MAX_BYTES) return null
                    output.write(buffer, 0, size)
                }
            }
            val json = JSONObject(output.toString("UTF-8"))
            require(json.getInt("version") == 1)
            val list = json.getJSONArray("widgets")
            require(list.length() in 0..1000)
            val widgets = ArrayList<AppServiceHelper.Widget>(list.length())
            for (index in 0 until list.length()) {
                val step = list.getJSONObject(index)
                val type = step.getInt("type")
                require(type == 0 || type == 1)
                require(step.getInt("count") >= 0)
                require(step.getInt("durationType") in 0..2)
                require(step.getLong("duration") >= 0)
                require(step.getInt("gestureMode") in 0..4)
                widgets.add(AppServiceHelper.Widget(
                    type = type,
                    count = step.getInt("count"),
                    durationType = step.getInt("durationType"),
                    duration = step.getLong("duration"),
                    longDuration = step.getInt("longDuration"),
                    swipeDuration = step.getInt("swipeDuration"),
                    number = index + 1,
                    x1 = step.getInt("x1"),
                    y1 = step.getInt("y1"),
                    x2 = step.getInt("x2"),
                    y2 = step.getInt("y2"),
                    enabled = step.getBoolean("enabled"),
                    waitText = step.getString("waitText"),
                    waitTimeoutSeconds = step.getInt("waitTimeoutSeconds"),
                    gestureMode = step.getInt("gestureMode"),
                    curvePercent = step.getInt("curvePercent"),
                    waypoints = step.getString("waypoints")
                ))
            }
            val name = json.getString("name").take(32)
            require(name.isNotBlank())
            AppServiceHelper.ScriptsConfig(
                name = name,
                synchronousExecution = json.getBoolean("synchronousExecution"),
                durationTime = json.getLong("durationTime"),
                widgets = widgets,
                relativeCoordinates = json.getBoolean("relativeCoordinates"),
                referenceWidth = json.getInt("referenceWidth"),
                referenceHeight = json.getInt("referenceHeight"),
                targetPackage = json.getString("targetPackage"),
                onLeaveAction = json.getInt("onLeaveAction").coerceIn(0, 2)
            )
        }.getOrNull()
    }
}
