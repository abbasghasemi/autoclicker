package ghasemi.abbas.autoclicker

object GestureWaypoints {
    // Waypoints are screen percentages: "25,40;70,55".
    fun parse(value: String, width: Float, height: Float): List<Pair<Float, Float>> =
        value.split(';').mapNotNull { entry ->
            val parts = entry.trim().split(',')
            if (parts.size != 2) return@mapNotNull null
            val x = parts[0].trim().toFloatOrNull() ?: return@mapNotNull null
            val y = parts[1].trim().toFloatOrNull() ?: return@mapNotNull null
            if (x !in 0f..100f || y !in 0f..100f) null
            else Pair(width * x / 100f, height * y / 100f)
        }
}
