package hector

import com.datastax.driver.core.Cluster
import edu.put.apps.ClientApplication
import edu.put.apps.DeliveryApplication
import edu.put.apps.RestaurantApplication
import edu.put.backend.BackendSession
import hector.backend.Config
import lombok.extern.slf4j.Slf4j
import kotlin.system.exitProcess

@Slf4j
object AppRunner {
    private val config: Config by lazy { Config.from("config.json")!! }
    private val cluster: Cluster by lazy { Cluster.builder().addContactPoint(config.contactPoint).build() }
    private val session: BackendSession by lazy { BackendSession(cluster.connect(config.keyspace)) }

    @Throws(InterruptedException::class)
    fun run() {
        val clientReplicas = 2
        val restaurantReplicas = 3
        val deliveryReplicas = 2

        val clients = initialize(clientReplicas, ::ClientApplication)
        val restaurants = initialize(restaurantReplicas, ::RestaurantApplication)
        val deliveries = initialize(deliveryReplicas, ::DeliveryApplication)

        clients.forEach { client -> client.join() }
        restaurants.forEach { restaurant -> restaurant.join() }
        deliveries.forEach { delivery -> delivery.join() }

        exitProcess(0)
    }

    /**
     * @author Jakub Kwiatkowski <jakub.j.kwiatkowski@student.put.poznan.pl>
     * @since 0.1.0
     *
     * Constructs and starts instances of Apps in required number.
     *
     * @param[replicas] Number of instances that should be started.
     * @param[construct] Function (in most cases - constructor) that takes App ID and Session handle and creates instance of App.
     *
     * @return List of App instances.
     */
    private fun <T : Thread> initialize(
        replicas: Int,
        construct: (Int, BackendSession) -> T
    ): List<T> {
        val apps = (0..replicas).map { index -> construct(index, session) }.toList()
        apps.forEach { app -> app.start() }

        return apps
    }
}


fun main() {
    AppRunner.run()
}
