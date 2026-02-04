package com.example.grifon.data.fake

import com.example.grifon.domain.model.Category
import com.example.grifon.domain.model.Product

object FakeCatalogData {
    val categories = listOf(
        Category("c1", "Electronics", null, 2),
        Category("c2", "Phones", "c1", 0),
        Category("c3", "Laptops", "c1", 0),
        Category("c4", "Fashion", null, 2),
        Category("c5", "Shoes", "c4", 0),
        Category("c6", "Jackets", "c4", 0),
    )

    val products = listOf(
        Product(
            id = "p1",
            title = "Smartphone Alpha",
            price = 399.0,
            currency = "EUR",
            imageUrl = "",
            brand = "Acme",
            rating = 4.4,
            inStock = true,
            attributesMap = mapOf("RAM" to "8GB", "Storage" to "128GB"),
        ),
        Product(
            id = "p2",
            title = "Laptop Pro 14",
            price = 1099.0,
            currency = "EUR",
            imageUrl = "",
            brand = "Techify",
            rating = 4.8,
            inStock = true,
            attributesMap = mapOf("RAM" to "16GB", "Storage" to "512GB"),
        ),
        Product(
            id = "p3",
            title = "Running Shoes",
            price = 89.0,
            currency = "EUR",
            imageUrl = "",
            brand = "Nike",
            rating = 4.1,
            inStock = false,
            attributesMap = mapOf("Size" to "42", "Color" to "Black"),
        ),
    )

    val shopProducts = mapOf(
        "shop_a" to products,
        "shop_b" to products.map {
            it.copy(price = it.price * 1.05, title = "${it.title} B")
        },
    )
}
