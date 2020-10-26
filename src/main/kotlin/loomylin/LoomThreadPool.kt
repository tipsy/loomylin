package loomylin

import org.eclipse.jetty.util.thread.ThreadPool
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LoomThreadPoolKt : ThreadPool {

    private val executorService = Executors.newVirtualThreadExecutor()

    override fun join() {
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
    }

    override fun execute(command: Runnable) {
        executorService.submit(command)
    }

    override fun getThreads() = 1
    override fun getIdleThreads() = 1
    override fun isLowOnThreads() = false
}
