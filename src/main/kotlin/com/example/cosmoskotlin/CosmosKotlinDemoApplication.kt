package com.example.cosmoskotlin

import com.azure.spring.data.cosmos.repository.config.EnableCosmosRepositories
import com.example.cosmoskotlin.model.Product
import com.example.cosmoskotlin.repo.ProductRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
@EnableCosmosRepositories(basePackages = ["com.example.cosmoskotlin.repo"])
class CosmosKotlinDemoApplication {
    @Bean
    fun demo(productRepository: ProductRepository) = CommandLineRunner {
        // Create container automatically if not present (Spring Data does this by default).
        val p1 = Product(id = "p-100", vendor = "Cisco",  name = "Router X", price = 499.0)
        val p2 = Product(id = "p-101", vendor = "Arista", name = "Switch Y", price = 1299.0)
        val p3 = Product(id = "p-102", vendor = "Cisco",  name = "Firewall Z", price = 2599.0)

        productRepository.saveAll(listOf(p1, p2, p3))

        println("All Cisco: " + productRepository.findByVendor("Cisco").joinToString())
        println("Expensive > 1000: " + productRepository.findExpensive(1000.0).joinToString())
    }
}

fun main(args: Array<String>) {
    runApplication<CosmosKotlinDemoApplication>(*args)
}
