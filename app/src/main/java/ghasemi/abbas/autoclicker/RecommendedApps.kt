package ghasemi.abbas.autoclicker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

object RecommendedApps {
    data class App(
        val title: String,
        val icon: String,
        val url: String,
        val badge: String?,
        val badgeColor: String?
    )

    private const val CACHE = "recommended_apps"
    private const val JSON = "json"
    private const val EXPIRY = "expiry"
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun load(context: Context, callback: (List<App>) -> Unit) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(CACHE, Context.MODE_PRIVATE)
        val cached = prefs.getString(JSON, null)
        val valid = prefs.getLong(EXPIRY, 0) > System.currentTimeMillis()
        if (valid && cached != null) {
            callback(parse(cached))
            return
        }
        val endpoint = "https://farasource.com/products/v0/${context.packageName}/${BuildConfig.FLAVOR}/apps.json"
        executor.execute {
            val result = runCatching {
                val connection = URL(endpoint).openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                try {
                    if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val objectJson = JSONObject(body)
                    val expiry = objectJson.getLong("expiresAt")
                    require(expiry > System.currentTimeMillis())
                    val apps = parse(body)
                    prefs.edit().putString(JSON, body).putLong(EXPIRY, expiry).apply()
                    File(appContext.cacheDir, "recommended_icons").listFiles()?.forEach { it.delete() }
                    apps
                } finally {
                    connection.disconnect()
                }
            }.getOrElse { emptyList() }
            main.post { callback(result) }
        }
    }

    private fun parse(json: String): List<App> = runCatching {
        val array = JSONObject(json).getJSONArray("apps")
        (0 until array.length()).mapNotNull { index ->
            val item = array.getJSONObject(index)
            val title = item.optString("title").trim()
            val icon = item.optString("icon")
            val url = item.optString("url")
            if (title.isBlank() || !url.startsWith("https://")) null else App(
                title, icon, url,
                if (item.isNull("badge")) null else item.optString("badge").trim().takeIf { it.isNotEmpty() },
                if (item.isNull("badgeColor")) null else item.optString("badgeColor").trim().takeIf { it.isNotEmpty() }
            )
        }
    }.getOrDefault(emptyList())

    fun loadIcon(context: Context, url: String, callback: (Bitmap?) -> Unit) {
        if (!url.startsWith("https://")) return
        val directory = File(context.applicationContext.cacheDir, "recommended_icons")
        executor.execute {
            val bitmap = runCatching {
                directory.mkdirs()
                val key = MessageDigest.getInstance("SHA-256")
                    .digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
                val file = File(directory, key)
                if (!file.exists()) {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    try {
                        if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
                        connection.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
                    } finally {
                        connection.disconnect()
                    }
                }
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, options)
                options.inSampleSize = (maxOf(options.outWidth, options.outHeight) / 128).coerceAtLeast(1)
                options.inJustDecodeBounds = false
                BitmapFactory.decodeFile(file.absolutePath, options).also {
                    if (it == null) file.delete()
                }
            }.getOrNull()
            main.post { callback(bitmap) }
        }
    }
}
