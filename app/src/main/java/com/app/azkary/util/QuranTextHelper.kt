package com.app.azkary.util

private val QURAN_ITEM_PREFIXES = listOf(
    "itm-ikhlas-",
    "itm-falaq-",
    "itm-nas-",
    "itm-ayat-al-kursi-",
    "itm-recitation-",
    "itm-ayatain-",
    "itm-ikhlas-falaq-nas-"
)

fun isQuranicItem(itemId: String): Boolean =
    QURAN_ITEM_PREFIXES.any { itemId.startsWith(it) }
