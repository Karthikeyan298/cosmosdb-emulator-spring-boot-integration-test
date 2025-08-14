package com.example.cosmoskotlin

import com.example.cosmoskotlin.repo.ProductRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.io.FileOutputStream
import java.nio.file.Paths
import java.security.KeyStore
import java.time.Duration
import javax.net.ssl.SSLContext

@SpringBootTest
class CosmosContainerTest {

    companion object {
        private lateinit var cosmos: GenericContainer<*>
        val currentPath = Paths.get(System.getProperty("user.dir"))
        val trustStorePath = currentPath.resolve("cosmos-truststore.jks").toFile()

        private fun createTrustStoreFromServerCert(host: String, port: Int, trustStoreFile: String, password: String) {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(object : javax.net.ssl.X509TrustManager {
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            }), java.security.SecureRandom())

            val socketFactory = sslContext.socketFactory
            socketFactory.createSocket(host, port).use { socket ->
                val sslSocket = socket as javax.net.ssl.SSLSocket
                sslSocket.startHandshake()
                val certs = sslSocket.session.peerCertificates
                if (certs.isEmpty()) throw RuntimeException("No certificates retrieved from $host:$port")

                val serverCert = certs[0] as java.security.cert.Certificate

                // Create new empty JKS keystore
                val ks = KeyStore.getInstance(KeyStore.getDefaultType())
                ks.load(null, password.toCharArray())
                ks.setCertificateEntry("cosmosemuroot", serverCert)

                // Save keystore to file
                FileOutputStream(trustStoreFile).use { fos ->
                    ks.store(fos, password.toCharArray())
                }
            }
        }
        @JvmStatic
        @BeforeAll
        fun startContainer() {
            trustStorePath.delete()
            cosmos = GenericContainer(DockerImageName.parse("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator")).apply {
                withExposedPorts(8081)
                withPrivilegedMode(true)
                withStartupTimeout(Duration.ofSeconds(120))
                portBindings = listOf("8081:8081")
            }

            cosmos.start()

            try {
                val trustStorePassword = "changeit"
                createTrustStoreFromServerCert(
                    host = "localhost",
                    port = 8081,
                    trustStoreFile = trustStorePath.absolutePath,
                    password = trustStorePassword
                )

                // Adding to java truststore
                System.setProperty("javax.net.ssl.trustStore", trustStorePath.absolutePath)
                System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @JvmStatic
        @AfterAll
        fun stopContainer() {
            // Cleanup old certificate as a sanity, cosmosdb emulator creates new certificate everytime.
            trustStorePath.delete()
            cosmos.stop()
        }
    }

    @Autowired
    lateinit var productRepo: ProductRepository

    @Test
    fun `can insert and read from cosmos emulator`() {

        val loaded = productRepo.findByVendor("Arista")
        assert(loaded.size > 0) { "No product found!" }
    }
}