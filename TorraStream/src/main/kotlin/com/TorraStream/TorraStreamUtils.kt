package com.TorraStream

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.annotations.SerializedName
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.SubtitleHelper
import com.lagradost.cloudstream3.utils.getQualityFromName
import org.json.JSONObject

fun getIndexQuality(str: String?): Int {
    return Regex("(\\d{3,4})[pP]").find(str ?: "") ?. groupValues ?. getOrNull(1) ?. toIntOrNull()
        ?: Qualities.Unknown.value
}


fun getQuality(str: String): Int {
    return when (str) {
        "360p" -> Qualities.P240.value
        "480p" -> Qualities.P360.value
        "HD" -> Qualities.P720.value
        "HEVC" -> Qualities.P1440.value
        "UHD" -> Qualities.P2160.value
        else -> getQualityFromName(str)
    }
}

fun getLanguage(language: String?): String? {
    return SubtitleHelper.fromTwoLettersToLanguage(language ?: return null)
        ?: SubtitleHelper.fromTwoLettersToLanguage(language.substringBefore("-"))
}


data class TorrentioResponse(
    @SerializedName("streams") val streams: List<TorrentioStream> = emptyList()
)

data class TorrentioStream(
    @SerializedName("name") val name: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("infoHash") val infoHash: String? = null,
    @SerializedName("fileIdx") val fileIdx: Int? = null
)

data class DebianRoot(
    @SerializedName("streams") val streams: List<Stream> = emptyList(),
    @SerializedName("cacheMaxAge") val cacheMaxAge: Long = 0,
    @SerializedName("staleRevalidate") val staleRevalidate: Long = 0,
    @SerializedName("staleError") val staleError: Long = 0
)

data class Stream(
    @SerializedName("name") val name: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("url") val url: String = "",
    @SerializedName("behaviorHints") val behaviorHints: BehaviorHints = BehaviorHints()
)

data class BehaviorHints(
    @SerializedName("bingeGroup") val bingeGroup: String? = null,
    @SerializedName("filename") val filename: String? = null
)

//Subtitles

data class Subtitles(
    val subtitles: List<Subtitle>,
    val cacheMaxAge: Long,
)

data class Subtitle(
    val id: String,
    val url: String,
    @JsonProperty("SubEncoding")
    val subEncoding: String,
    val lang: String,
    val m: String,
    val g: String,
)

fun generateMagnetLinkFromSource(trackersList: List<String>, hash: String?): String {
    // Fetch the content of the file from the provided URL

    // Build the magnet link
    return buildString {
        append("magnet:?xt=urn:btih:$hash")
        for (index in 0 until trackersList.size - 1) {
            if (trackersList[index].isNotBlank()) {
                append("&tr=").append(trackersList[index].trim())
            }
        }
    }
}


fun getAnidbEid(jsonString: String, episodeNumber: Int?): Int? {
    if (episodeNumber == null) return null

    return try {
        val jsonObject = JSONObject(jsonString)
        val episodes = jsonObject.optJSONObject("episodes") ?: return null

        episodes.optJSONObject(episodeNumber.toString())
            ?.optInt("anidbEid", -1)
            ?.takeIf { it != -1 }
    } catch (e: Exception) {
        e.printStackTrace() // Logs the error but prevents breaking the app
        null
    }
}


fun parseAnimeData(jsonString: String): AnimeData {
    val objectMapper = ObjectMapper()
    return objectMapper.readValue(jsonString, AnimeData::class.java)
}

