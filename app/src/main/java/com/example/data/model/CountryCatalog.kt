package com.example.data.model

data class CountryOption(
    val code: String,
    val nameDe: String,
    val stationCount: Int = 0
)

object CountryCatalog {

    val quickCountryCodes = listOf("DE", "AT", "CH", "NL", "FR", "IT", "GB", "PL", "ES")

    val defaultCountries: List<CountryOption> = listOf(
        CountryOption("DE", "Deutschland"),
        CountryOption("AT", "Österreich"),
        CountryOption("CH", "Schweiz"),
        CountryOption("NL", "Niederlande"),
        CountryOption("BE", "Belgien"),
        CountryOption("LU", "Luxemburg"),
        CountryOption("FR", "Frankreich"),
        CountryOption("IT", "Italien"),
        CountryOption("ES", "Spanien"),
        CountryOption("PT", "Portugal"),
        CountryOption("GB", "Großbritannien"),
        CountryOption("IE", "Irland"),
        CountryOption("DK", "Dänemark"),
        CountryOption("SE", "Schweden"),
        CountryOption("NO", "Norwegen"),
        CountryOption("FI", "Finnland"),
        CountryOption("PL", "Polen"),
        CountryOption("CZ", "Tschechien"),
        CountryOption("HU", "Ungarn"),
        CountryOption("HR", "Kroatien"),
        CountryOption("GR", "Griechenland"),
        CountryOption("TR", "Türkei"),
        CountryOption("US", "USA"),
        CountryOption("CA", "Kanada"),
        CountryOption("AU", "Australien")
    )

    private val germanNamesByCode: Map<String, String> = defaultCountries.associate {
        it.code to it.nameDe
    }

    fun normalizeCode(code: String?): String {
        val clean = code?.trim()?.uppercase().orEmpty()
        return if (clean.isBlank() || clean == "EU" || clean.length != 2) "DE" else clean
    }

    fun displayNameFor(code: String, fallbackName: String = ""): String {
        val norm = normalizeCode(code)
        return germanNamesByCode[norm] ?: fallbackName.ifBlank { norm }
    }

    fun findOption(code: String, catalog: List<CountryOption> = defaultCountries): CountryOption {
        val norm = normalizeCode(code)
        return catalog.firstOrNull { it.code.equals(norm, ignoreCase = true) }
            ?: CountryOption(norm, displayNameFor(norm))
    }
}
