package com.example.grifon.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val CART = "cart"
    const val CHECKOUT = "checkout"
    const val PAYPAL_CHECKOUT = "paypal-checkout?orderReference={orderReference}&approvalUrl={approvalUrl}"
    const val STRIPE_CHECKOUT = "stripe-checkout?orderReference={orderReference}&checkoutUrl={checkoutUrl}"
    const val FAVORITES = "favorites"
    const val ACCOUNT = "account"
    const val ORDERS = "orders"
    const val REGISTER = "register"
    const val WHOLESALE_APPLICATION = "wholesale-application"
    const val PRODUCT = "product/{id}"
    const val PLP = "plp?query={query}&category={category}"
    const val SETTINGS = "settings"
    const val SCAN = "scan"
    const val REGISTER = "register"

    fun productRoute(id: String) = "product/$id"
    fun plpRoute(query: String = "", category: String = ""): String {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val encodedCategory = URLEncoder.encode(category, StandardCharsets.UTF_8.toString())
        return "plp?query=$encodedQuery&category=$encodedCategory"
    }

    fun paypalCheckoutRoute(orderReference: String, approvalUrl: String): String {
        val encodedReference = URLEncoder.encode(orderReference, StandardCharsets.UTF_8.toString())
        val encodedApprovalUrl = URLEncoder.encode(approvalUrl, StandardCharsets.UTF_8.toString())
        return "paypal-checkout?orderReference=$encodedReference&approvalUrl=$encodedApprovalUrl"
    }

    fun stripeCheckoutRoute(orderReference: String, checkoutUrl: String): String {
        val encodedReference = URLEncoder.encode(orderReference, StandardCharsets.UTF_8.toString())
        val encodedCheckoutUrl = URLEncoder.encode(checkoutUrl, StandardCharsets.UTF_8.toString())
        return "stripe-checkout?orderReference=$encodedReference&checkoutUrl=$encodedCheckoutUrl"
    }
}
