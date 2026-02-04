package com.example.grifon.navigation

object Routes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val CART = "cart"
    const val ACCOUNT = "account"
    const val PRODUCT = "product/{id}"
    const val PLP = "plp?query={query}&category={category}"
    const val SETTINGS = "settings"
    const val SCAN = "scan"

    fun productRoute(id: String) = "product/$id"
    fun plpRoute(query: String = "", category: String = "") =
        "plp?query=${query}&category=${category}"
}
