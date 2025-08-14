package com.example.cosmoskotlin.model

import com.azure.spring.data.cosmos.core.mapping.Container
import com.azure.spring.data.cosmos.core.mapping.PartitionKey
import org.springframework.data.annotation.Id

@Container(containerName = "products")
data class Product(
    @Id
    val id: String,

    @PartitionKey
    val vendor: String,

    val name: String,
    val price: Double
)
