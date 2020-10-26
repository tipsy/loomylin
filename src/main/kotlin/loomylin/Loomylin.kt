package loomylin

import io.javalin.Javalin
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.util.thread.ThreadPool
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit.MILLISECONDS

const val DELAY: Long = 100
val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

fun main() {
    createServer(QueuedThreadPool(4, 4)).start(7000) // start a server with a thread pool equal to number of physical cores
    createServer(QueuedThreadPool(32, 32)).start(7001) // start a server with wanted concurrency
    createServer(LoomThreadPoolKt()).start(7002) // start a loom server
}

fun createServer(threadPool: ThreadPool): Javalin = Javalin.create { config ->
    config.server { Server(threadPool) }
    config.showJavalinBanner = false
}.apply {
    get("/computational") { ctx ->
        for (x in 0 until 25000) {
            Math.random()
        }
        ctx.result("Hello, World")
    }
    get("/wait-blocking") { ctx ->
        Thread.sleep(DELAY)
        ctx.result("Hello, World")
    }
    get("/wait-async") { ctx ->
        val future = CompletableFuture<String>()
        executor.schedule({ future.complete("Hello, World") }, DELAY, MILLISECONDS)
        ctx.result(future)
    }
}
