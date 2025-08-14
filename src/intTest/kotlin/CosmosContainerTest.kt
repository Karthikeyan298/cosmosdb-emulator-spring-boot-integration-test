package com.example.cosmoskotlin

import com.example.cosmoskotlin.model.Product
import com.example.cosmoskotlin.repo.ProductRepository
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.time.Duration
import java.util.function.Consumer
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

@SpringBootTest
class CosmosContainerTest {

    companion object {
        private lateinit var cosmos: GenericContainer<*>
        val currentPath = Paths.get(System.getProperty("user.dir"))
        val cosmosCertificatePath = currentPath.resolve("cosmos.pem").toFile()
        val trustStorePath = currentPath.resolve("cosmos-truststore.jks").toFile()

        private fun extractCertificate(host: String, port: Int): String {
            // openssl s_client -connect localhost:8081 -showcerts -servername localhost
            val command = listOf("openssl", "s_client", "-connect", "$host:$port", "-showcerts", "-servername", host)

            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            process.outputStream.close()

            val certBuilder = StringBuilder()
            var isCert = false
            // Extract cert part alone from complete response.
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.forEachLine { line ->
                    when {
                        line == "-----BEGIN CERTIFICATE-----" -> {
                            isCert = true
                            certBuilder.append(line).append("\n")
                        }
                        line == "-----END CERTIFICATE-----" -> {
                            certBuilder.append(line).append("\n")
                            isCert = false
                        }
                        isCert -> certBuilder.append(line).append("\n")
                    }
                }
            }
            return certBuilder.toString()
        }

        @JvmStatic
        @BeforeAll
        fun startContainer() {

            // Cleanup old certificate as a sanity, cosmosdb emulator creates new certificate everytime.
            cosmosCertificatePath.delete()
            trustStorePath.delete()
            cosmos = GenericContainer(DockerImageName.parse("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator")).apply {
                withExposedPorts(8081)
                withPrivilegedMode(true)
                withStartupTimeout(Duration.ofSeconds(120))
                portBindings = listOf("8081:8081")
            }

            cosmos.start()

            try {
                val certificate = extractCertificate("localhost", 8081)
                cosmosCertificatePath.writeText(certificate)
                val defaultPassword = "changeit"
                // Creating java keystore using the above certificate
                // keytool -importcert -file <certificate_file> -keystore <keystore_file_name> -alias cosmosemuroot -storepass changeit
                val keytoolCmd = arrayOf(
                    "keytool", "-importcert",
                    "-alias", "cosmosemuroot",
                    "-file", cosmosCertificatePath.absolutePath,
                    "-keystore", trustStorePath.absolutePath,
                    "-storepass", defaultPassword,
                    "-noprompt"
                )

                val keytoolExitCode = ProcessBuilder(*keytoolCmd)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor()

                if (keytoolExitCode != 0) {
                    throw RuntimeException("keytool command failed with exit code $keytoolExitCode")
                }
                // Adding to java truststore
                System.setProperty("javax.net.ssl.trustStore", trustStorePath.absolutePath)
                System.setProperty("javax.net.ssl.trustStorePassword", defaultPassword)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @JvmStatic
        @AfterAll
        fun stopContainer() {
            // Cleanup old certificate as a sanity, cosmosdb emulator creates new certificate everytime.
            cosmosCertificatePath.delete()
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