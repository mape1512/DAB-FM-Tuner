package com.example.data.model

enum class RadioBand(val label: String, val badgeText: String) {
    ALL("Alle", "ALLE"),
    DAB_PLUS("DAB+", "DAB+"),
    FM("FM / UKW", "FM");

    companion object {
        fun fromStored(value: String?): RadioBand {
            return entries.firstOrNull { it.name == value } ?: ALL
        }
    }
}
