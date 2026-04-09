package com.example.grifon.data.auth

import com.squareup.moshi.Json

data class LoginRequestDto(
    val email: String,
    val password: String,
    val countryIso: String = "GR" // Προσθήκη για να ξέρει ο Gateway σε ποιο shop να ψάξει
)

data class LoginResponseDto(
    @Json(name = "ok")
    val ok: Boolean = false,
    @Json(name = "id_customer")
    val idCustomer: Int? = null, // Το PrestaShop στέλνει Integer
    val firstname: String? = null,
    val lastname: String? = null,
    val email: String? = null,
    val company: String? = null,
    @Json(name = "can_view_prices")
    val canViewPrices: Boolean? = null,
    val error: String? = null,
    val message: String? = null
)
