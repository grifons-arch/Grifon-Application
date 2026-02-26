package com.example.grifon.domain.usecase

import com.example.grifon.data.repository.CatalogRepository

class SyncCatalogUseCase(private val catalogRepository: CatalogRepository) {
    suspend operator fun invoke(shopId: String) = catalogRepository.syncCatalog(shopId)
}
