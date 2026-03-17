package com.example.grifon.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopConfigTest {
    @Test
    fun `normalizeShopId maps legacy values to canonical shop ids`() {
        assertEquals(ShopConfig.GreekShopId, ShopConfig.normalizeShopId("shop_gr"))
        assertEquals(ShopConfig.GreekShopId, ShopConfig.normalizeShopId("GR"))
        assertEquals(ShopConfig.SwedishShopId, ShopConfig.normalizeShopId("shop_se"))
        assertEquals(ShopConfig.SwedishShopId, ShopConfig.normalizeShopId("SE"))
    }

    @Test
    fun `languageLabel follows active shop`() {
        assertEquals("Ελληνικά", ShopConfig.languageLabel("4"))
        assertEquals("Svenska", ShopConfig.languageLabel("1"))
        assertTrue(ShopConfig.isSwedishShop("shop_se"))
    }
}
