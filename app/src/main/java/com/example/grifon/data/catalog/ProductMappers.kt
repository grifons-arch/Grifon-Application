package com.example.grifon.data.catalog

import com.example.grifon.domain.model.CatalogFacet
import com.example.grifon.domain.model.CatalogFacetOption
import com.example.grifon.domain.model.CatalogFacetType
import com.example.grifon.domain.model.Product

fun ProductDto.toDomainProduct(
    gatewayBaseUrl: String,
    brand: String = "Grifon",
    showPrice: Boolean = true,
): Product {
    val normalizedBaseUrl = gatewayBaseUrl.removeSuffix("/")
    val rawUrl = defaultImage?.url.orEmpty()
    
    var imageUrl = when {
        rawUrl.isBlank() -> ""
        rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
        rawUrl.startsWith("/") -> "$normalizedBaseUrl$rawUrl"
        else -> "$normalizedBaseUrl/$rawUrl"
    }

    // Αντικατάσταση localhost με την IP του gateway για να φαίνονται οι εικόνες στο κινητό
    if (imageUrl.contains("localhost") || imageUrl.contains("127.0.0.1") || imageUrl.contains("10.0.2.2")) {
        val baseIp = normalizedBaseUrl
            .replace("http://", "")
            .replace("https://", "")
            .split(":")[0]
        
        imageUrl = imageUrl
            .replace("localhost", baseIp)
            .replace("127.0.0.1", baseIp)
            .replace("10.0.2.2", baseIp)
    }

    return Product(
        id = id.toString(),
        title = name ?: "#$id",
        price = price?.takeIf { showPrice },
        currency = "EUR",
        imageUrl = imageUrl,
        images = emptyList(),
        brand = brand,
        rating = 4.5,
        inStock = true,
        attributesMap = buildMap {
            val normalizedReference = reference?.takeIf { it.isNotBlank() }
            if (normalizedReference != null) {
                put("reference", listOf(normalizedReference))
            }
            attributes.forEach { (key, values) ->
                val normalizedValues = values
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                if (key.isNotBlank() && normalizedValues.isNotEmpty()) {
                    put(key, normalizedValues)
                }
            }
        },
    )
}

fun CatalogFacetDto.toDomainFacet(): CatalogFacet {
    val facetType = when (type.lowercase()) {
        "color" -> CatalogFacetType.COLOR
        "brand" -> CatalogFacetType.BRAND
        "price" -> CatalogFacetType.PRICE
        "availability" -> CatalogFacetType.AVAILABILITY
        else -> CatalogFacetType.ATTRIBUTE
    }

    return CatalogFacet(
        key = key,
        title = title,
        type = facetType,
        options = options.map {
            CatalogFacetOption(
                key = it.key,
                label = it.label,
                count = it.count,
            )
        },
        minValue = minValue,
        maxValue = maxValue,
    )
}
