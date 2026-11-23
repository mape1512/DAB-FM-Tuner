package com.example.data.local

import com.example.data.model.RadioBand

object CuratedDachStations {

    data class ChannelHint(
        val band: RadioBand,
        val frequencyOrChannel: String
    )

    private val channelHints = mapOf(
        "deutschlandfunk" to ChannelHint(RadioBand.DAB_PLUS, "5C DR Deutschland"),
        "deutschlandfunk kultur" to ChannelHint(RadioBand.DAB_PLUS, "5C DR Deutschland"),
        "deutschlandfunk nova" to ChannelHint(RadioBand.DAB_PLUS, "5C DR Deutschland"),
        "radio bob" to ChannelHint(RadioBand.DAB_PLUS, "5C DR Deutschland"),
        "sunshine live" to ChannelHint(RadioBand.DAB_PLUS, "5C DR Deutschland"),
        "klassik radio" to ChannelHint(RadioBand.DAB_PLUS, "5C DR Deutschland"),
        "schlagerparadies" to ChannelHint(RadioBand.DAB_PLUS, "5C DR Deutschland"),
        "energy" to ChannelHint(RadioBand.DAB_PLUS, "5C DR Deutschland"),
        "rock antenne" to ChannelHint(RadioBand.DAB_PLUS, "9B Antenne DE"),
        "absolut relax" to ChannelHint(RadioBand.DAB_PLUS, "9B Antenne DE"),
        "absolut top" to ChannelHint(RadioBand.DAB_PLUS, "9B Antenne DE"),
        "80s80s" to ChannelHint(RadioBand.DAB_PLUS, "9B Antenne DE"),
        "90s90s" to ChannelHint(RadioBand.DAB_PLUS, "9B Antenne DE"),
        "1live" to ChannelHint(RadioBand.FM, "106.7 MHz"),
        "wdr 2" to ChannelHint(RadioBand.FM, "100.4 MHz"),
        "wdr 4" to ChannelHint(RadioBand.FM, "101.3 MHz"),
        "swr3" to ChannelHint(RadioBand.FM, "98.4 MHz"),
        "swr1" to ChannelHint(RadioBand.FM, "94.7 MHz"),
        "bayern 3" to ChannelHint(RadioBand.FM, "97.3 MHz"),
        "bayern 1" to ChannelHint(RadioBand.FM, "93.7 MHz"),
        "br-klassik" to ChannelHint(RadioBand.DAB_PLUS, "11D Bayern"),
        "ndr 2" to ChannelHint(RadioBand.FM, "92.1 MHz"),
        "n-joy" to ChannelHint(RadioBand.FM, "94.2 MHz"),
        "antenne bayern" to ChannelHint(RadioBand.FM, "103.8 MHz"),
        "hit radio ffh" to ChannelHint(RadioBand.FM, "105.9 MHz"),
        "radioeins" to ChannelHint(RadioBand.FM, "95.8 MHz"),
        "fritz" to ChannelHint(RadioBand.FM, "102.6 MHz"),
        "ö3" to ChannelHint(RadioBand.FM, "99.9 MHz"),
        "oe3" to ChannelHint(RadioBand.FM, "99.9 MHz"),
        "srf 3" to ChannelHint(RadioBand.DAB_PLUS, "12C SRG")
    )

    fun findChannelHint(stationName: String): ChannelHint? {
        val normalized = stationName.lowercase()
        return channelHints.entries.firstOrNull { (key, _) ->
            normalized.contains(key)
        }?.value
    }

    val starterStations: List<StationEntity> = listOf(
        StationEntity(
            id = "curated_dlf",
            name = "Deutschlandfunk",
            streamUrl = "https://st01.sslstream.dlf.de/dlf/01/128/mp3/stream.mp3",
            faviconUrl = "https://www.deutschlandfunk.de/static/img/dlf/icons/apple-touch-icon-180x180.png",
            band = RadioBand.DAB_PLUS.name,
            frequencyOrChannel = "5C DR Deutschland",
            countryCode = "DE",
            genreTags = "dab+, nachrichten, kultur, info",
            codec = "MP3",
            bitrate = 128,
            votes = 4800,
            isFavorite = true,
            presetSlot = 1
        ),
        StationEntity(
            id = "curated_1live",
            name = "WDR 1LIVE",
            streamUrl = "https://wdr-1live-live.icecastssl.wdr.de/wdr/1live/live/mp3/128/stream.mp3",
            faviconUrl = "https://www1.wdr.de/resources/img/favicon/apple-touch-icon.png",
            band = RadioBand.FM.name,
            frequencyOrChannel = "106.7 MHz",
            countryCode = "DE",
            genreTags = "fm, pop, charts, ukw",
            codec = "MP3",
            bitrate = 128,
            votes = 4600,
            isFavorite = true,
            presetSlot = 2
        ),
        StationEntity(
            id = "curated_swr3",
            name = "SWR3",
            streamUrl = "https://liveradio.swr.de/sw282p3/swr3/play.mp3",
            faviconUrl = "https://www.swr3.de/assets/swr3/apple-touch-icon.png",
            band = RadioBand.FM.name,
            frequencyOrChannel = "98.4 MHz",
            countryCode = "DE",
            genreTags = "fm, pop, rock, verkehr",
            codec = "MP3",
            bitrate = 128,
            votes = 4500,
            isFavorite = true,
            presetSlot = 3
        ),
        StationEntity(
            id = "curated_radiobob",
            name = "RADIO BOB! (DAB+)",
            streamUrl = "https://streams.radiobob.de/bob-national/mp3-192/mediaplayer",
            faviconUrl = "https://www.radiobob.de/apple-touch-icon.png",
            band = RadioBand.DAB_PLUS.name,
            frequencyOrChannel = "5C DR Deutschland",
            countryCode = "DE",
            genreTags = "dab+, rock, classic rock",
            codec = "MP3",
            bitrate = 192,
            votes = 4300,
            isFavorite = true,
            presetSlot = 4
        ),
        StationEntity(
            id = "curated_sunshinelive",
            name = "sunshine live",
            streamUrl = "https://stream.sunshine-live.de/live/mp3-192/stream.sunshine-live.de/",
            faviconUrl = "https://www.sunshine-live.de/apple-touch-icon.png",
            band = RadioBand.DAB_PLUS.name,
            frequencyOrChannel = "5C DR Deutschland",
            countryCode = "DE",
            genreTags = "dab+, electronic, dance, techno",
            codec = "MP3",
            bitrate = 192,
            votes = 4100,
            presetSlot = 5
        ),
        StationEntity(
            id = "curated_bayern3",
            name = "BAYERN 3",
            streamUrl = "https://dispatcher.rndfnk.com/br/br3/live/mp3/mid",
            faviconUrl = "https://www.bayern3.de/apple-touch-icon.png",
            band = RadioBand.FM.name,
            frequencyOrChannel = "97.3 MHz",
            countryCode = "DE",
            genreTags = "fm, pop, charts, bayern",
            codec = "MP3",
            bitrate = 128,
            votes = 4000,
            presetSlot = 6
        ),
        StationEntity(
            id = "curated_wdr2",
            name = "WDR 2 Rheinland",
            streamUrl = "https://wdr-wdr2-rheinland.icecastssl.wdr.de/wdr/wdr2/rheinland/mp3/128/stream.mp3",
            faviconUrl = "https://www1.wdr.de/resources/img/favicon/apple-touch-icon.png",
            band = RadioBand.FM.name,
            frequencyOrChannel = "100.4 MHz",
            countryCode = "DE",
            genreTags = "fm, pop, nachrichten, ukw",
            codec = "MP3",
            bitrate = 128,
            votes = 3900
        ),
        StationEntity(
            id = "curated_rockantenne",
            name = "ROCK ANTENNE",
            streamUrl = "https://stream.rockantenne.de/rockantenne/stream/mp3",
            faviconUrl = "https://www.rockantenne.de/apple-touch-icon.png",
            band = RadioBand.DAB_PLUS.name,
            frequencyOrChannel = "9B Antenne DE",
            countryCode = "DE",
            genreTags = "dab+, rock, metal",
            codec = "MP3",
            bitrate = 128,
            votes = 3850
        ),
        StationEntity(
            id = "curated_klassik",
            name = "Klassik Radio",
            streamUrl = "https://live.streams.klassikradio.de/klassikradio-deutschland/stream/mp3",
            faviconUrl = "https://www.klassikradio.de/apple-touch-icon.png",
            band = RadioBand.DAB_PLUS.name,
            frequencyOrChannel = "5C DR Deutschland",
            countryCode = "DE",
            genreTags = "dab+, klassik, filmmusik, lounge",
            codec = "MP3",
            bitrate = 128,
            votes = 3700
        ),
        StationEntity(
            id = "curated_ndr2",
            name = "NDR 2 Niedersachsen",
            streamUrl = "https://icecast.ndr.de/ndr/ndr2/niedersachsen/mp3/128/stream.mp3",
            faviconUrl = "https://www.ndr.de/apple-touch-icon.png",
            band = RadioBand.FM.name,
            frequencyOrChannel = "92.1 MHz",
            countryCode = "DE",
            genreTags = "fm, pop, adult contemporary",
            codec = "MP3",
            bitrate = 128,
            votes = 3650
        ),
        StationEntity(
            id = "curated_dlf_nova",
            name = "Deutschlandfunk Nova",
            streamUrl = "https://st03.sslstream.dlf.de/dlf/03/128/mp3/stream.mp3",
            faviconUrl = "https://www.deutschlandfunknova.de/apple-touch-icon.png",
            band = RadioBand.DAB_PLUS.name,
            frequencyOrChannel = "5C DR Deutschland",
            countryCode = "DE",
            genreTags = "dab+, indie, pop, wissen",
            codec = "MP3",
            bitrate = 128,
            votes = 3600
        ),
        StationEntity(
            id = "curated_antenne_bayern",
            name = "ANTENNE BAYERN",
            streamUrl = "https://stream.antenne.de/antenne/stream/mp3",
            faviconUrl = "https://www.antenne.de/apple-touch-icon.png",
            band = RadioBand.FM.name,
            frequencyOrChannel = "103.8 MHz",
            countryCode = "DE",
            genreTags = "fm, hits, pop, verkehr",
            codec = "MP3",
            bitrate = 128,
            votes = 3550
        ),
        StationEntity(
            id = "curated_radioeins",
            name = "rbb radioeins",
            streamUrl = "https://dispatcher.rndfnk.com/rbb/radioeins/live/mp3/mid",
            faviconUrl = "https://www.radioeins.de/apple-touch-icon.png",
            band = RadioBand.FM.name,
            frequencyOrChannel = "95.8 MHz",
            countryCode = "DE",
            genreTags = "fm, alternative, indie, berlin",
            codec = "MP3",
            bitrate = 128,
            votes = 3400
        ),
        StationEntity(
            id = "curated_oe3",
            name = "Hitradio Ö3",
            streamUrl = "https://orf-live.ors-shoutcast.at/oe3-q2a",
            faviconUrl = "https://oe3.orf.at/apple-touch-icon.png",
            band = RadioBand.FM.name,
            frequencyOrChannel = "99.9 MHz",
            countryCode = "AT",
            genreTags = "fm, pop, charts, österreich",
            codec = "AAC",
            bitrate = 128,
            votes = 3350
        ),
        StationEntity(
            id = "curated_srf3",
            name = "Radio SRF 3",
            streamUrl = "https://stream.srg-ssr.ch/m/drs3/mp3_128",
            faviconUrl = "https://www.srf.ch/apple-touch-icon.png",
            band = RadioBand.DAB_PLUS.name,
            frequencyOrChannel = "12C SRG",
            countryCode = "CH",
            genreTags = "dab+, pop, rock, schweiz",
            codec = "MP3",
            bitrate = 128,
            votes = 3250
        )
    )
}
