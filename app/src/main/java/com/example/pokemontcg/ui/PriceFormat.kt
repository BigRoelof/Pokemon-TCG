package com.example.pokemontcg.ui

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Formats a Cardmarket price in euros the way the user's locale writes money (e.g. "€ 3,51" in Dutch). */
fun formatEuro(amount: Double, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getCurrencyInstance(locale).apply { currency = Currency.getInstance("EUR") }.format(amount)
