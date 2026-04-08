package com.example.grifon.core

object ShopConfig {
    const val GreekShopId = "4"
    const val SwedishShopId = "1"
    private const val GreekStorefrontBaseUrl = "https://grifon.gr/"
    private const val SwedishStorefrontBaseUrl = "https://grifon.se/"

    fun normalizeShopId(rawShopId: String?): String {
        val normalized = rawShopId?.trim().orEmpty()
        if (normalized.isEmpty()) return GreekShopId

        return when (normalized.uppercase()) {
            GreekShopId, "GR", "SHOP_GR", "SHOP_4", "SHOP_A" -> GreekShopId
            SwedishShopId, "SE", "SHOP_SE", "SHOP_1", "SHOP_B" -> SwedishShopId
            else -> normalized.toIntOrNull()?.let {
                if (it == SwedishShopId.toInt()) SwedishShopId else GreekShopId
            } ?: GreekShopId
        }
    }

    fun isSwedishShop(rawShopId: String?): Boolean = normalizeShopId(rawShopId) == SwedishShopId

    fun displayName(rawShopId: String?): String {
        val language = AppLanguage.currentLanguage()
        return if (isSwedishShop(rawShopId)) {
            when (language) {
                "sv" -> "Svensk grossistbutik"
                "el" -> "Σουηδικό κατάστημα χονδρικής"
                else -> "Swedish Wholesale Store"
            }
        } else {
            when (language) {
                "sv" -> "Grekisk butik"
                "el" -> "Ελληνικό κατάστημα"
                else -> "Greek Store"
            }
        }
    }

    fun languageLabel(rawShopId: String?): String =
        if (isSwedishShop(rawShopId)) "Svenska" else "Ελληνικά"

    fun storefrontBaseUrl(rawShopId: String?): String =
        if (isSwedishShop(rawShopId)) SwedishStorefrontBaseUrl else GreekStorefrontBaseUrl

    fun storefrontCartUrl(rawShopId: String?): String =
        storefrontBaseUrl(rawShopId) + "index.php?controller=cart"

    fun storefrontCheckoutUrl(rawShopId: String?): String =
        storefrontBaseUrl(rawShopId) + "index.php?controller=order"

    fun wholesaleApplicationEntryUrl(rawShopId: String?): String =
        storefrontBaseUrl(rawShopId) + "index.php?fc=module&module=ets_wholesale&controller=registration"
}
