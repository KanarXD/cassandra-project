package hector

import com.datastax.driver.core.Cluster
import hector.backend.Config
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

fun main() {
    val logger = LoggerFactory.getLogger("INITIALIZE")

    Config.from("config.json")?.also { config ->
        Cluster.builder().addContactPoint(config.contactPoint).build().apply {
            newSession().also { session ->
                session.execute(
                    """
                    CREATE KEYSPACE IF NOT EXISTS ${config.keyspace} WITH REPLICATION = {
                        'class': '${config.replication.strategy}',
                        'replication_factor': ${config.replication.factor}
                    };
                    """
                )
                session.execute("USE ${config.keyspace};")
                logger.info("Created keyspace `${config.keyspace}` with `${config.replication.strategy}` class and replication factor of ${config.replication.factor}")

                javaClass.getResource("/initialize.cql")
                    ?.readText()
                    ?.split(';')
                    ?.map { it.trim() }
                    ?.filterNot { it.isBlank() || it.isEmpty() }
                    ?.forEach { statement ->
                        session.execute(statement)
                    }
                logger.info("Initialized keyspace with script.")
            }
        }
    }

    exitProcess(0)
}