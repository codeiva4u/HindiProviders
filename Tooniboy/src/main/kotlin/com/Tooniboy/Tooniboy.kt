package com.Tooniboy

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Tooniboy : MainAPI() {
    override var mainUrl              = "https://www.tooniboy.com"
    override var name                 = "Tooniboy"
    override val hasMainPage          = true
    override var lang                 = "hi"
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.Movie,TvType.Anime,TvType.Cartoon)

    override val mainPage = mainPageOf(
        "series" to "Series",
        "movies" to "Movies",
        "category/crunchyroll" to "Crunchyroll",
        "category/netflix" to "Netflix",
        "category/cartoon-network" to "Cartoon Network",
        "category/disney" to "Disney",
        "category/hungama" to "Hungama",
        "category/sony-yay" to "Sony Yay",
    )


    companion object
    {
        val header= mapOf("Cookie" to com.phisher98.BuildConfig.TooniboyCookie)
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/${request.data}/page/$page/", headers = header).document
        val home     = document.select("#site article").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list    = HomePageList(
                name               = request.name,
                list               = home,
                isHorizontalImages = false
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title     = this.select("header > h2").text().trim().replace("Watch Online","")
        val href      = fixUrl(this.select("a").attr("href"))
        val posterUrl = this.select("figure > img").attr("src")
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    private fun Element.toSearch(): SearchResponse {
        val title     = this.select("header > h2").text().trim().replace("Watch Online","")
        val href      = fixUrl(this.select("a").attr("href"))
        val posterUrl = fixUrlNull(this.select("figure > img").attr("src"))
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..3) {
            val document = app.get("${mainUrl}/page/$i/?s=$query",headers = header).document

            val results = document.select("#site article").mapNotNull { it.toSearch() }

            if (!searchResponse.containsAll(results)) {
                searchResponse.addAll(results)
            } else {
                break
            }

            if (results.isEmpty()) break
        }

        return searchResponse
    }

    @Suppress("SuspiciousIndentation")
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url, headers = header).document
        val title= document.selectFirst("h2.title")?.text()?.trim().toString().replace("Watch Online","")
        val poster = document.select("figure.im.brd1 img").attr("src")
        val description = document.selectFirst("div.entry p")?.text()?.trim()
        val tags = document.select("div.rght.fg1").map { it.text() }
        val tvtag=if (url.contains("series")) TvType.TvSeries else TvType.Movie
        return if (tvtag == TvType.TvSeries) {
            val episodes = mutableListOf<Episode>()
                document.select("div.serie-sidebar article").map {
                        val href = it.selectFirst("a")?.attr("href") ?:""
                        val posterUrl=it.selectFirst("figure > img")?.attr("data-src")
                        val episode = it.select("header h2").text()
                        val seasonnumber= it.selectFirst("header span")?.text()?.substringBefore("-")?.trim()?.toInt()
                        episodes.add(Episode(href, episode, posterUrl = posterUrl, season = seasonnumber))
                    }
            newTvSeriesLoadResponse(title, url, TvType.Anime, episodes) {
                this.tags=tags
                this.posterUrl = poster
                this.plot = description
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.tags=tags
                this.posterUrl = poster
                this.plot = description
            }
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data, headers=header).document
        document.select("div.op-srv.brd1").forEach {
            val encodedlink=it.attr("data-src")
            val serverurl= base64Decode(encodedlink)
            val truelink= app.get(serverurl).document.selectFirst("iframe")?.attr("src") ?:""
            Log.d("Phisher",truelink)
            if (truelink.contains("streamruby"))
            {
                    StreamRuby().getUrl(truelink)
            }
            else loadExtractor(truelink,subtitleCallback, callback)
        }
        return true
    }
}


