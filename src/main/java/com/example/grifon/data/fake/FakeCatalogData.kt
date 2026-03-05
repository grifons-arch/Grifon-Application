package com.example.grifon.data.fake

import com.example.grifon.domain.model.Category
import com.example.grifon.domain.model.Product

object FakeCatalogData {
    val categories = listOf(
        Category("4000", "Ceramics", null, 10),
        Category("4060", "More", "4000", 0),
        Category("4010", "Accurate Museum Copies", "4000", 0),
        Category("4015", "Embossed ceramics", "4000", 0),
        Category("4020", "Copies.", "4000", 0),
        Category("4025", "Decorative Ceramics", "4000", 0),
        Category("4030", "Lanterns, Candles", "4000", 0),
        Category("4040", "Cobalt", "4000", 0),
        Category("4035", "Classic Ceramics", "4000", 0),
        Category("4045", "Minoan", "4000", 0),
        Category("4055", "Statuettes", "4000", 0),
        
        Category("5500", "Outdoor", null, 0),
        Category("4500", "Statuettes etc.", null, 0),
        Category("9999", "Packages at a better price", null, 0),
        Category("5000", "Decorative", null, 0),
        Category("6500", "Religious", null, 0),
        Category("7000", "Hobbies and Toys", null, 0),
        Category("7500", "For use", null, 0),
        Category("8000", "Accessory", null, 0),
        Category("900000", "Personalization of products", null, 0),
        Category("8500", "More", null, 0)
    )

    val products = listOf(
        Product(
            id = "p1",
            title = "Decorative Vase (4025)",
            price = 45.0,
            currency = "EUR",
            imageUrl = "diakosmitika_keramikago.jpg",
            brand = "Grifon Art",
            rating = 4.8,
            inStock = true,
            attributesMap = mapOf("Material" to "Clay", "Style" to "Modern"),
        ),
        Product(
            id = "p2",
            title = "Ceramic Lantern (4030)",
            price = 32.0,
            currency = "EUR",
            imageUrl = "kersmiks_diskodmhtiks.jpg",
            brand = "Grifon Art",
            rating = 4.6,
            inStock = true,
            attributesMap = mapOf("Type" to "Lantern"),
        ),
        Product(
            id = "p3",
            title = "Outdoor Statuette (5500)",
            price = 120.0,
            currency = "EUR",
            imageUrl = "paixnidiarouytrina.jpg",
            brand = "Grifon Garden",
            rating = 4.5,
            inStock = true,
            attributesMap = mapOf("Material" to "Stone"),
        )
    )

    val shopProducts = mapOf(
        "shop_gr" to products,
        "shop_4" to products.map { it.copy(title = "${it.title} (Shop 4)") }
    )
}
