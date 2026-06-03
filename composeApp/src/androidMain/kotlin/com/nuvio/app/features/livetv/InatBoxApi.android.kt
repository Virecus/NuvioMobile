package com.nuvio.app.features.livetv

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.conscrypt.Conscrypt
import org.json.JSONArray
import org.json.JSONObject
import java.security.Security
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val TAG = "InatBox"
private const val AES_KEY = "ywevqtjrurkwtqgz"

// Domain resolver — DEX'ten çıkarıldı (mtlshash/cert/main/hash)
private const val DOMAIN_HASH_URL = "https://raw.githubusercontent.com/mtlshash/cert/main/hash"

@Volatile private var cachedContentUrl: String? = null
@Volatile private var conscryptInstalled = false

fun ensureConscrypt(context: Context) {
    if (conscryptInstalled) return
    synchronized(InatBoxApi) {
        if (conscryptInstalled) return
        try {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
            Log.d(TAG, "Conscrypt installed")
        } catch (e: Exception) {
            Log.w(TAG, "Conscrypt install failed: ${e.message}")
        }
        conscryptInstalled = true
    }
}

object InatBoxApi

private val okClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
}

private val CATEGORY_PATHS = listOf(
    "https://boxyz.cfd/CDN/001_STR/boxyz.cfd/spor_v2.php" to "Spor Kanalları",
    "/tv/cable.php"           to "Kanallar Liste 1",
    "/tv/list2.php"           to "Kanallar Liste 2",
    "/tv/sinema.php"          to "Sinema Kanalları",
    "/tv/belgesel.php"        to "Belgesel Kanalları",
    "/tv/ulusal.php"          to "Ulusal Kanallar",
    "/tv/haber.php"           to "Haber Kanalları",
    "/tv/cocuk.php"           to "Çocuk Kanalları",
    "/tv/dini.php"            to "Dini Kanallar",
    "/ex/index.php"           to "EXXEN",
    "/ga/index.php"           to "Gain",
    "/max/index.php"          to "Max-BluTV",
    "/nf/index.php"           to "Netflix",
    "/dsny/index.php"         to "Disney+",
    "/amz/index.php"          to "Amazon Prime",
    "/hb/index.php"           to "HBO Max",
    "/tbi/index.php"          to "Tabii",
    "/film/mubi.php"          to "Mubi",
    "/ccc/index.php"          to "TOD",
    "/yabanci-dizi/index.php" to "Yabancı Diziler",
    "/yerli-dizi/index.php"   to "Yerli Diziler",
    "/film/yerli-filmler.php" to "Yerli Filmler",
    "/film/4k-film-exo.php"   to "4K Film İzle | Exo",
)

suspend fun getInatBoxCategories(): List<Pair<String, String>>? {
    val base = resolveContentUrl() ?: return null
    return CATEGORY_PATHS.map { (path, label) ->
        if (path.startsWith("http")) path to label
        else "$base$path" to label
    }
}

private suspend fun resolveContentUrl(): String? = withContext(Dispatchers.IO) {
    cachedContentUrl?.let { return@withContext it }

    // 1. Domain hash URL'sinden çek (mtlshash/cert/main/hash — InatBox DEX'ten)
    val hashDomain = tryFetchHashDomain()
    if (hashDomain != null && testDomain(hashDomain)) {
        Log.d(TAG, "Domain from hash: $hashDomain")
        cachedContentUrl = hashDomain
        return@withContext hashDomain
    }

    // 2. Bilinen domain'leri dene
    val candidates = listOf(
        "https://alexiai.cfd",
        "https://dizibox.cfd",
        "https://dizibox.cc",
        "https://dizibox.in",
        "https://dizibox.net",
    )
    for (candidate in candidates) {
        if (testDomain(candidate)) {
            Log.d(TAG, "Domain works: $candidate")
            cachedContentUrl = candidate
            return@withContext candidate
        }
    }

    Log.e(TAG, "No working domain found")
    null
}

private fun tryFetchHashDomain(): String? = runCatching {
    val req = Request.Builder()
        .url(DOMAIN_HASH_URL)
        .get()
        .header("User-Agent", "speedrestapi")
        .header("X-Requested-With", "com.bp.box")
        .build()
    val resp = okClient.newCall(req).execute()
    val body = resp.body?.string() ?: return null
    resp.close()
    Log.d(TAG, "Hash response len=${body.length}, preview=${body.take(50)}")
    // Şifreli format: base64:base64 → decrypt ile DC1 alanını çıkar
    val decrypted = decryptInat(body)
    if (decrypted != null) {
        Log.d(TAG, "Hash decrypted preview: ${decrypted.take(100)}")
        // JSON'dan DC1 al
        runCatching {
            val json = org.json.JSONObject(decrypted)
            "https://${json.optString("DC1")}"
        }.getOrNull()
    } else null
}.getOrNull()

private fun testDomain(base: String): Boolean = runCatching {
    val host = base.removePrefix("https://").removePrefix("http://")
    val body = "1=$AES_KEY&0=$AES_KEY"
        .toRequestBody("application/x-www-form-urlencoded; charset=UTF-8".toMediaType())
    val req = Request.Builder()
        .url("$base/tv/haber.php")
        .post(body)
        .header("Cache-Control", "no-cache")
        .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        .header("Host", host)
        .header("Referer", "https://speedrestapi.com/")
        .header("X-Requested-With", "com.bp.box")
        .header("User-Agent", "speedrestapi")
        .build()
    val resp = okClient.newCall(req).execute()
    val code = resp.code
    resp.close()
    Log.d(TAG, "testDomain $base -> HTTP $code")
    code == 200
}.getOrDefault(false)

suspend fun fetchInatBoxCategory(apiUrl: String): List<InatBoxItem>? = withContext(Dispatchers.IO) {
    runCatching {
        val json = makeInatRequest(apiUrl) ?: return@runCatching null
        parseInatItems(json)
    }.getOrNull()
}

suspend fun resolveInatBoxStream(itemJson: String): String? = withContext(Dispatchers.IO) {
    runCatching {
        val item = JSONObject(itemJson)
        val chUrl = item.optString("chUrl").ifBlank { return@runCatching null }
        val chType = item.optString("chType")
        if (chType == "tekli_regex_lb_sh_3") {
            val innerJson = makeInatRequest(chUrl) ?: return@runCatching null
            val inner = JSONObject(innerJson)
            inner.optString("chUrl").ifBlank { null }
        } else {
            chUrl.takeIf { it.isNotBlank() }
        }
    }.getOrNull()
}

private fun makeInatRequest(url: String): String? = runCatching {
    val host = java.net.URI(url).host ?: return null
    Log.d(TAG, "makeInatRequest: $url")
    val body = "1=$AES_KEY&0=$AES_KEY"
        .toRequestBody("application/x-www-form-urlencoded; charset=UTF-8".toMediaType())
    val req = Request.Builder()
        .url(url)
        .post(body)
        .header("Cache-Control", "no-cache")
        .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        .header("Host", host)
        .header("Referer", "https://speedrestapi.com/")
        .header("X-Requested-With", "com.bp.box")
        .header("User-Agent", "speedrestapi")
        .build()
    val resp = okClient.newCall(req).execute()
    val code = resp.code
    Log.d(TAG, "makeInatRequest response: $code")
    if (code != 200) { resp.close(); return null }
    val raw = resp.body?.string() ?: run { resp.close(); return null }
    resp.close()
    decryptInat(raw)
}.getOrNull()

private fun decryptInat(response: String): String? = runCatching {
    val key = AES_KEY.toByteArray()
    val iv = IvParameterSpec(key)
    val keySpec = SecretKeySpec(key, "AES")
    val cipher1 = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher1.init(Cipher.DECRYPT_MODE, keySpec, iv)
    val first = cipher1.doFinal(Base64.decode(response.split(":")[0], Base64.DEFAULT))
    val firstStr = String(first, Charsets.ISO_8859_1).split(":")[0]
    val cipher2 = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher2.init(Cipher.DECRYPT_MODE, keySpec, iv)
    val second = cipher2.doFinal(Base64.decode(firstStr, Base64.DEFAULT))
    String(second, Charsets.UTF_8).trim()
}.getOrNull()

fun parseInatItems(json: String): List<InatBoxItem> {
    val result = mutableListOf<InatBoxItem>()
    runCatching {
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i) ?: continue
            val chType = item.optString("chType")
            if (chType in listOf("link", "web")) continue
            if (item.has("chName") && item.has("chUrl") && item.has("chImg")) {
                result.add(InatBoxItem(
                    name = item.optString("chName"),
                    poster = item.optString("chImg"),
                    itemJson = item.toString(),
                    isLive = chType in listOf("live_url", "cable_sh", "tekli_regex_lb_sh_3"),
                ))
            } else if (item.has("diziName")) {
                result.add(InatBoxItem(
                    name = item.optString("diziName"),
                    poster = item.optString("diziImg"),
                    itemJson = item.toString(),
                    isLive = false,
                ))
            }
        }
    }
    return result
}

data class InatBoxItem(
    val name: String,
    val poster: String,
    val itemJson: String,
    val isLive: Boolean,
)

actual suspend fun fetchInatBoxAllChannels(): List<LiveChannel>? = withContext(Dispatchers.IO) {
    val categories = getInatBoxCategories() ?: run {
        Log.e(TAG, "Could not resolve InatBox domain")
        return@withContext null
    }
    Log.d(TAG, "Fetching ${categories.size} InatBox categories")
    val allChannels = mutableListOf<LiveChannel>()
    for ((url, categoryName) in categories) {
        val items = fetchInatBoxCategory(url)
        if (items != null) {
            Log.d(TAG, "Category '$categoryName': ${items.size} items")
            items.forEach { item ->
                allChannels.add(LiveChannel(
                    id = item.itemJson,
                    name = item.name,
                    poster = item.poster,
                    streamPageUrl = item.itemJson,
                    category = categoryName,
                ))
            }
        } else {
            Log.w(TAG, "Category '$categoryName' returned null")
        }
    }
    Log.d(TAG, "Total InatBox channels: ${allChannels.size}")
    allChannels.takeIf { it.isNotEmpty() }
}

actual suspend fun resolveInatBoxStreamUrl(itemJson: String): String? {
    return resolveInatBoxStream(itemJson)
}
