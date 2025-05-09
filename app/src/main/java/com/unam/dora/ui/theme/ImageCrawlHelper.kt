import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.content.Context
import android.util.Log
import java.io.File

object ImageCrawlHelper {
    fun getFirstWikipediaImage(title: String): String? {
        Log.d("ImageCrawlHelper", "Suche Wikipedia-Bild für: $title")
        val client = OkHttpClient()

        val formattedTitle = title.replace(" ", "_")
        val url = "https://en.wikipedia.org/w/api.php" +
                "?action=query&titles=$formattedTitle&prop=pageimages&format=json&piprop=original"
        Log.d("ImageCrawlHelper", "API-Anfrage an: $url")

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e("ImageCrawlHelper", "API-Anfrage fehlgeschlagen: ${response.code}")
                return null
            }

            val body = response.body?.string()
            if (body == null) {
                Log.e("ImageCrawlHelper", "Leere Antwort von Wikipedia API")
                return null
            }

            try {
                val json = JSONObject(body)
                val pages = json.getJSONObject("query").getJSONObject("pages")
                val page = pages.keys().asSequence().firstOrNull()?.let { pages.getJSONObject(it) }
                val imageUrl = page?.optJSONObject("original")?.optString("source")

                if (imageUrl != null) {
                    Log.d("ImageCrawlHelper", "Bild-URL gefunden: $imageUrl")
                } else {
                    Log.e("ImageCrawlHelper", "Kein Bild für '$title' gefunden")
                }

                return imageUrl
            } catch (e: Exception) {
                Log.e("ImageCrawlHelper", "JSON-Parsing Fehler: ${e.message}")
                return null
            }
        }
    }

    fun fetchAndSaveImage(imageUrl: String, context: Context, filename: String): String? {
        Log.d("ImageCrawlHelper", "Starte Download von: $imageUrl")

        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(imageUrl)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("ImageCrawlHelper", "Bild-Download fehlgeschlagen: ${response.code}")
                    return null
                }

                val imagesDir = File(context.filesDir, "images")
                if (!imagesDir.exists()) {
                    Log.d("ImageCrawlHelper", "Erstelle Bilder-Verzeichnis: ${imagesDir.absolutePath}")
                    imagesDir.mkdirs()
                }

                val imageFile = File(imagesDir, filename)
                val bytesCopied = response.body?.byteStream()?.use { input ->
                    imageFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                Log.d("ImageCrawlHelper", "Bild gespeichert: ${imageFile.absolutePath} ($bytesCopied Bytes)")
                return imageFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e("ImageCrawlHelper", "Fehler beim Speichern des Bildes: ${e.message}", e)
            return null
        }
    }
}