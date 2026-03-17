package com.example.grifon.core

object ShopConfig {
    const val GreekShopId = "4"
    const val SwedishShopId = "1"

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

    fun displayName(rawShopId: String?): String =
        if (isSwedishShop(rawShopId)) {
            "Σουηδικό κατάστημα χονδρικής"
        } else {
            "Ελληνικό κατάστημα"
        }

    fun languageLabel(rawShopId: String?): String =
        if (isSwedishShop(rawShopId)) "Svenska" else "Ελληνικά"
}
