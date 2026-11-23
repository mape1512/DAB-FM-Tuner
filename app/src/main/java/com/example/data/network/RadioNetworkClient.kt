package com.example.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

data class IcyProbeResult(
    val streamTitle: String = "",
    val streamUrl: String = ""
)

object RadioNetworkClient {

    private const val PRIMARY_BASE_URL = "https://de1.api.radio-browser.info/"
    private const val FALLBACK_BASE_URL = "https://at1.api.radio-browser.info/"
    private const val USER_AGENT = "DAB-FM-Tuner/1.0 (Android Automotive Radio)"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    private val probeClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    val primaryApi: RadioBrowserApi by lazy {
        Retrofit.Builder()
            .baseUrl(PRIMARY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RadioBrowserApi::class.java)
    }

    val fallbackApi: RadioBrowserApi by lazy {
        Retrofit.Builder()
            .baseUrl(FALLBACK_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RadioBrowserApi::class.java)
    }

    suspend fun fetchImmediateIcyMetadata(streamUrl: String): IcyProbeResult? =
        withContext(Dispatchers.IO) {
            if (!streamUrl.startsWith("http://", ignoreCase = true) &&
                !streamUrl.startsWith("https://", ignoreCase = true)
            ) {
                return@withContext null
            }

            runCatching {
                val request = Request.Builder()
                    .url(streamUrl)
                    .header("Icy-MetaData", "1")
                    .header("Connection", "close")
                    .get()
                    .build()

                probeClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val metaInt = response.header("icy-metaint")?.trim()?.toIntOrNull()
                        ?.takeIf { it in 1024..131072 }
                    val body = response.body ?: return@use null

                    if (metaInt != null) {
                        body.byteStream().use { input ->
                            if (!skipExactBytes(input, metaInt.toLong())) return@use null
                            val lenByte = input.read()
                            if (lenByte <= 0) return@use null
                            val metaBytes = ByteArray(lenByte * 16)
                            val readCount = readFullyUpTo(input, metaBytes)
                            if (readCount <= 0) return@use null
                            val rawMeta = String(metaBytes, 0, readCount, StandardCharsets.UTF_8)
                                .trim('\u0000', ' ')
                            parseIcyBlock(rawMeta)
                        }
                    } else {
                        null
                    }
                }
            }.getOrNull()
        }

    suspend fun verifyImageUrlReachable(url: String): Boolean =
        withContext(Dispatchers.IO) {
            val trimmed = url.trim()
            if (!trimmed.startsWith("http://", ignoreCase = true) &&
                !trimmed.startsWith("https://", ignoreCase = true)
            ) {
                return@withContext false
            }

            runCatching {
                val headReq = Request.Builder().url(trimmed).head().build()
                probeClient.newCall(headReq).execute().use { resp ->
                    val contentType = resp.header("Content-Type").orEmpty().lowercase()
                    if (resp.isSuccessful && contentType.startsWith("image/")) {
                        return@withContext true
                    }
                    if (resp.code != 405 && resp.code != 403 && resp.isSuccessful) {
                        return@withContext contentType.contains("octet-stream")
                    }
                }
                val getReq = Request.Builder()
                    .url(trimmed)
                    .header("Range", "bytes=0-1023")
                    .get()
                    .build()
                probeClient.newCall(getReq).execute().use { resp ->
                    val contentType = resp.header("Content-Type").orEmpty().lowercase()
                    resp.isSuccessful && (contentType.startsWith("image/") || contentType.contains("icon"))
                }
            }.getOrDefault(false)
        }

    private fun parseIcyBlock(rawMeta: String): IcyProbeResult? {
        val titleMatch = Regex("""StreamTitle='(.*?)';""", RegexOption.IGNORE_CASE).find(rawMeta)
            ?: Regex("""StreamTitle="(.*?)";""", RegexOption.IGNORE_CASE).find(rawMeta)
        val urlMatch = Regex("""StreamUrl='(.*?)';""", RegexOption.IGNORE_CASE).find(rawMeta)
            ?: Regex("""StreamUrl="(.*?)";""", RegexOption.IGNORE_CASE).find(rawMeta)

        val streamTitle = titleMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val streamUrl = urlMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (streamTitle.isBlank() && streamUrl.isBlank()) return null
        return IcyProbeResult(streamTitle = streamTitle, streamUrl = streamUrl)
    }

    private fun skipExactBytes(input: InputStream, count: Long): Boolean {
        val buffer = ByteArray(8192)
        var remaining = count
        while (remaining > 0) {
            val toRead = minOf(remaining, buffer.size.toLong()).toInt()
            val read = input.read(buffer, 0, toRead)
            if (read < 0) return false
            remaining -= read
        }
        return true
    }

    private fun readFullyUpTo(input: InputStream, target: ByteArray): Int {
        var offset = 0
        while (offset < target.size) {
            val read = input.read(target, offset, target.size - offset)
            if (read <= 0) break
            offset += read
        }
        return offset
    }
}
