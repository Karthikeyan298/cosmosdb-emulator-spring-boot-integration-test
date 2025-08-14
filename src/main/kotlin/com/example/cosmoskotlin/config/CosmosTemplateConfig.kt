package com.example.cosmoskotlin.config

import com.azure.cosmos.CosmosClientBuilder
import com.azure.spring.data.cosmos.CosmosFactory
import com.azure.spring.data.cosmos.config.CosmosConfig
import com.azure.spring.data.cosmos.core.CosmosTemplate
import com.azure.spring.data.cosmos.core.convert.MappingCosmosConverter
import com.azure.spring.data.cosmos.core.mapping.CosmosMappingContext
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CosmosTemplateConfig(
    @Value("\${azure.cosmos.endpoint}") private val endpoint: String,
    @Value("\${azure.cosmos.key}") private val key: String,
    @Value("\${azure.cosmos.database}") private val database: String
) {
    @Bean
    fun cosmosTemplate(objectMapper: ObjectMapper): CosmosTemplate {
        val cosmosClient = CosmosClientBuilder()
            .endpoint(endpoint)
            .key(key)
            .buildAsyncClient()

        val cosmosConfig = CosmosConfig.builder().build()
        val cosmosFactory = CosmosFactory(cosmosClient, database)

        val mappingContext = CosmosMappingContext()
        val mappingConverter = MappingCosmosConverter(mappingContext, objectMapper)

        return CosmosTemplate(cosmosFactory, cosmosConfig, mappingConverter)
    }
}
