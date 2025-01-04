package com.BanglaPlex

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import org.json.JSONObject
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


class Vectorx : Chillx() {
    override val name = "BanglaPlex"
    override val mainUrl = "https://bestx.stream"
}


open class Chillx : ExtractorApi() {
    override val name = "Chillx"
    override val mainUrl = "https://chillx.top"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val res = app.get(url).toString()
        val encodedString =
            Regex("Encrypted\\s*=\\s*'(.*?)';").find(res)?.groupValues?.get(1) ?:""
        val decoded = decrypt(encodedString)
        val m3u8 =Regex("file:\\s*\"(.*?)\"").find(decoded)?.groupValues?.get(1) ?:""
        val header =
            mapOf(
                "accept" to "*/*",
                "accept-language" to "en-US,en;q=0.5",
                "Origin" to mainUrl,
                "Accept-Encoding" to "gzip, deflate, br",
                "Connection" to "keep-alive",
                "Sec-Fetch-Dest" to "empty",
                "Sec-Fetch-Mode" to "cors",
                "Sec-Fetch-Site" to "cross-site",
                "user-agent" to USER_AGENT,
            )
        callback.invoke(
            ExtractorLink(
                name,
                name,
                m3u8,
                mainUrl,
                Qualities.P1080.value,
                INFER_TYPE,
                headers = header
            )
        )

        val subtitles = extractSrtSubtitles(decoded)

        subtitles.forEachIndexed { _, (language, url) ->
            subtitleCallback.invoke(
                SubtitleFile(
                    language,
                    url
                )
            )
        }
    }

    private fun extractSrtSubtitles(subtitle: String): List<Pair<String, String>> {
        val regex = """\[([^]]+)](https?://[^\s,]+\.srt)""".toRegex()

        return regex.findAll(subtitle).map { match ->
            val (language, url) = match.destructured
            language.trim() to url.trim()
        }.toList()
    }

    fun decrypt(encrypted: String): String {
        // Decode the Base64-encoded JSON string and extract data
        val data = JSONObject(String(Base64.getDecoder().decode(encrypted)))
        val salt = data.getString("salt")
        val ivBase64 = data.getString("iv")
        val ciphertextBase64 = data.getString("data")

        // Derive the encryption key
        val key = deriveKey("NMhG08LLwixKRmgx", salt)

        // Decode IV and ciphertext
        val iv = IvParameterSpec(Base64.getDecoder().decode(ivBase64))
        val ciphertext = Base64.getDecoder().decode(ciphertextBase64)

        // Decrypt the ciphertext using AES
        val decrypted = decryptAES(key.copyOf(32), iv, ciphertext)

        return String(decrypted, Charsets.UTF_8)
    }

    private fun deriveKey(password: String, salt: String): ByteArray {
        val saltBytes = if (salt.contains("=")) Base64.getDecoder().decode(salt) else salt.toByteArray()
        val passwordBytes = (password + "sB0mZOqlRTy8CVpL").toCharArray()
        val spec = PBEKeySpec(passwordBytes, saltBytes, 1000, 64 * 8) // 64 bytes = 512 bits
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        return factory.generateSecret(spec).encoded
    }

    private fun decryptAES(key: ByteArray, iv: IvParameterSpec, ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), iv)
        return cipher.doFinal(ciphertext)
    }

}

