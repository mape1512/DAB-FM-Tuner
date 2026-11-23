package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.data.model.RadioBand
import com.example.data.model.RadioStation
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "StationImageRecognizer"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val INFERENCE_TIMEOUT_MS = 60_000L

enum class RecognitionConfidence(val labelDe: String) {
    HIGH("Hoch"),
    MEDIUM("Mittel"),
    LOW("Niedrig")
}

data class RecognizedStationInfo(
    val stationName: String,
    val alternateNames: List<String>,
    val bandHint: RadioBand,
    val frequencyOrChannel: String,
    val countryCode: String,
    val confidence: RecognitionConfidence,
    val visualSummary: String
)

sealed interface ImageRecognitionState {
    data object Idle : ImageRecognitionState
    data class Analyzing(val previewBitmap: Bitmap) : ImageRecognitionState
    data class Result(
        val previewBitmap: Bitmap,
        val recognized: RecognizedStationInfo,
        val matchedStations: List<RadioStation>
    ) : ImageRecognitionState

    data class Error(
        val previewBitmap: Bitmap?,
        val message: String
    ) : ImageRecognitionState
}

class StationImageRecognizer {

    private val stationSchema = Schema.obj(
        properties = mapOf(
            "stationName" to Schema.string(
                description = "Primary recognized radio station name (e.g. SWR3, 1LIVE, Bayern 3, Ö3, SRF 3). Empty string if unrecognizable."
            ),
            "alternateNames" to Schema.array(
                items = Schema.string(),
                description = "Alternative search names, spellings, or parent broadcaster names (e.g. WDR 1LIVE, Hitradio Ö3)."
            ),
            "band" to Schema.enumeration(
                values = listOf("DAB_PLUS", "FM", "UNKNOWN"),
                description = "Detected broadcast band from the image (DAB_PLUS, FM, or UNKNOWN)."
            ),
            "frequencyOrChannel" to Schema.string(
                description = "Detected FM frequency (e.g. 98.4 MHz) or DAB+ ensemble/channel (e.g. 5C, 11D), or empty string."
            ),
            "countryCode" to Schema.string(
                description = "2-letter ISO 3166-1 alpha-2 country code if inferable (e.g. DE, AT, CH, FR, GB), or empty string."
            ),
            "confidence" to Schema.enumeration(
                values = listOf("HIGH", "MEDIUM", "LOW"),
                description = "Confidence of identifying the radio station from the image."
            ),
            "visualSummary" to Schema.string(
                description = "Concise 1-2 sentence explanation in German of what logo, frequency, text, or display was recognized in the image."
            )
        ),
        optionalProperties = listOf("alternateNames", "frequencyOrChannel", "countryCode")
    )

    private val model by lazy {
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = stationSchema
        }
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = config
        )
    }

    suspend fun recognizeStation(bitmap: Bitmap): RecognizedStationInfo = withContext(Dispatchers.IO) {
        val scaledBitmap = scaleBitmapForAi(bitmap, maxDim = 1024)
        val promptText = """
            Analysiere dieses Bild für ein Autoradio (DAB+ und FM/UKW).
            Es kann ein Senderlogo, ein Autoradio-Display, ein RDS-Text, eine Frequenzskala,
            ein Plakat, ein Aufkleber oder ein DAB+-Kanalname abgebildet sein.
            Erkenne den passenden Radiosender möglichst präzise (insbesondere für Deutschland, Österreich, Schweiz oder international),
            inklusive Frequenz (z. B. "98.4 MHz") oder DAB+-Kanal (z. B. "5C"), Empfangsband (DAB_PLUS, FM oder UNKNOWN),
            Ländercode (ISO-2 wie DE, AT, CH) und alternative Suchbegriffe.
            Wenn kein Radiosender erkennbar ist, setze stationName auf "" und confidence auf "LOW" und erkläre kurz auf Deutsch, was sichtbar ist.
        """.trimIndent()

        val response = withTimeout(INFERENCE_TIMEOUT_MS) {
            model.generateContent(
                content {
                    image(scaledBitmap)
                    text(promptText)
                }
            )
        }

        val jsonText = response.text
        if (jsonText.isNullOrBlank()) {
            throw IllegalStateException("Das KI-Modell hat keine Antwort zurückgegeben.")
        }

        parseRecognitionJson(jsonText)
    }

    private fun parseRecognitionJson(jsonText: String): RecognizedStationInfo {
        val json = JSONObject(jsonText)
        val stationName = json.optString("stationName", "").trim()
        val altArray = json.optJSONArray("alternateNames")
        val alternateNames = buildList {
            if (altArray != null) {
                for (i in 0 until altArray.length()) {
                    val value = altArray.optString(i, "").trim()
                    if (value.isNotBlank() && !value.equals(stationName, ignoreCase = true)) {
                        add(value)
                    }
                }
            }
        }
        val band = when (json.optString("band", "UNKNOWN").uppercase()) {
            "DAB_PLUS" -> RadioBand.DAB_PLUS
            "FM" -> RadioBand.FM
            else -> RadioBand.ALL
        }
        val frequencyOrChannel = json.optString("frequencyOrChannel", "").trim()
        val countryCode = json.optString("countryCode", "").trim().uppercase().take(2)
        val confidence = when (json.optString("confidence", "LOW").uppercase()) {
            "HIGH" -> RecognitionConfidence.HIGH
            "MEDIUM" -> RecognitionConfidence.MEDIUM
            else -> RecognitionConfidence.LOW
        }
        val visualSummary = json.optString("visualSummary", "").trim().ifBlank {
            if (stationName.isNotBlank()) {
                "Sender $stationName wurde im Bild erkannt."
            } else {
                "Kein eindeutiger Radiosender im Bild erkannt."
            }
        }

        return RecognizedStationInfo(
            stationName = stationName,
            alternateNames = alternateNames,
            bandHint = band,
            frequencyOrChannel = frequencyOrChannel,
            countryCode = countryCode,
            confidence = confidence,
            visualSummary = visualSummary
        )
    }

    companion object {
        fun decodeAndScaleBitmap(context: Context, uri: Uri, maxDim: Int = 1024): Bitmap? {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            } ?: return null

            val origW = bounds.outWidth
            val origH = bounds.outHeight
            if (origW <= 0 || origH <= 0) return null

            var sampleSize = 1
            while (origW / sampleSize > maxDim * 2 || origH / sampleSize > maxDim * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val rawBitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            return scaleBitmapForAi(rawBitmap, maxDim)
        }

        fun scaleBitmapForAi(bitmap: Bitmap, maxDim: Int = 1024): Bitmap {
            val maxSide = max(bitmap.width, bitmap.height)
            if (maxSide <= maxDim) return bitmap
            val ratio = maxDim.toFloat() / maxSide.toFloat()
            val targetW = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
            val targetH = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
            return try {
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            } catch (e: Exception) {
                Log.w(TAG, "Bitmap scaling fallback to original", e)
                bitmap
            }
        }
    }
}
