package com.example.cosmoskotlin.repo

import com.azure.spring.data.cosmos.repository.CosmosRepository
import com.azure.spring.data.cosmos.repository.Query
import com.example.cosmoskotlin.model.Product
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : CosmosRepository<Product, String> {
    fun findByVendor(vendor: String): List<Product>

    @Query("SELECT * FROM c")
    fun findExpensive(price: Double): List<Product>
}
