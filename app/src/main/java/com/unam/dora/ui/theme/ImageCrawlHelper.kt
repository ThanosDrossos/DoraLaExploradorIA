import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.content.Context
import android.util.Log
import java.io.File
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.text.Normalizer

object ImageCrawlHelper {

    fun fetchFirstDuckDuckGoImage(query: String): String? {
        //guess that will never work
        return null
    }


    fun removeAccents(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    fun getFirstWikipediaImage(query: String): String? {
        val client = OkHttpClient()
        val cleanQuery = removeAccents(query)
        val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")

        Log.d("ImageCrawlHelper", "Suche Artikel für: $cleanQuery")

        // Schritt 1: Suche den ersten passenden Wikipedia-Artikel
        val searchUrl = "https://en.wikipedia.org/w/api.php" +
                "?action=query&list=search&srsearch=$encodedQuery&format=json"

        val searchRequest = Request.Builder()
            .url(searchUrl)
            .build()

        val pageTitle = client.newCall(searchRequest).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e("ImageCrawlHelper", "Suche fehlgeschlagen: ${response.code}")
                return null
            }
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val searchArray = json.getJSONObject("query").getJSONArray("search")
            if (searchArray.length() == 0) return null
            searchArray.getJSONObject(0).getString("title")
        }

        Log.d("ImageCrawlHelper", "Gefundener Artikel: $pageTitle")

        // Schritt 2: Hole das erste Bild des Artikels
        val imageUrl = try {
            val encodedTitle = URLEncoder.encode(pageTitle, "UTF-8")
            val imageRequestUrl = "https://en.wikipedia.org/w/api.php" +
                    "?action=query&titles=$encodedTitle&prop=pageimages&format=json&piprop=original"

            val imageRequest = Request.Builder().url(imageRequestUrl).build()

            client.newCall(imageRequest).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val pages = json.getJSONObject("query").getJSONObject("pages")
                val page = pages.keys().asSequence().firstOrNull()?.let { pages.getJSONObject(it) }
                page?.optJSONObject("original")?.optString("source")
            }
        } catch (e: Exception) {
            Log.e("ImageCrawlHelper", "Fehler beim Laden des Bildes: ${e.message}")
            null
        }

        if (imageUrl != null) {
            Log.d("ImageCrawlHelper", "Bild-URL: $imageUrl")
        } else {
            Log.e("ImageCrawlHelper", "Kein Bild gefunden für '$pageTitle'")
        }

        return imageUrl
    }

    fun getImage(location: String, city: String, filename: String, context: Context): String? {
        // Erst Wikipedia versuchen
        Log.d("ChatViewModel", "Versuche Wikipedia für: $location")
        ImageCrawlHelper.getFirstWikipediaImage(location)?.let { url ->
            Log.d("ChatViewModel", "Wikipedia URL gefunden: $url")
            val path = ImageCrawlHelper.fetchAndSaveImage(url, context, filename)
            if (path != null) return path
        }

        // Falls Wikipedia fehlschlägt, DuckDuckGo versuchen
        Log.d("ChatViewModel", "Versuche DuckDuckGo für: $location")
        val duckDuckGoUrl = ImageCrawlHelper.fetchFirstDuckDuckGoImage("$location $city")
        duckDuckGoUrl?.let { url ->
            Log.d("ChatViewModel", "DuckDuckGo URL gefunden: $url")
            return ImageCrawlHelper.fetchAndSaveImage(url, context, filename)
        }

        return null
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