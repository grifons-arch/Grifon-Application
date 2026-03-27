package com.example.grifon.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val CART = "cart"
    const val FAVORITES = "favorites"
    const val ACCOUNT = "account"
    const val PRODUCT = "product/{id}"
    const val PLP = "plp?query={query}&category={category}"
    const val SETTINGS = "settings"
    const val SCAN = "scan"

    fun productRoute(id: String) = "product/$id"
    fun plpRoute(query: String = "", category: String = ""): String {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val encodedCategory = URLEncoder.encode(category, StandardCharsets.UTF_8.toString())
        return "plp?query=$encodedQuery&category=$encodedCategory"
    }
}
