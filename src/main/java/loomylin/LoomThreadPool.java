package loomylin;

import org.eclipse.jetty.util.thread.ThreadPool;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class LoomThreadPool implements ThreadPool {

    ExecutorService executorService = Executors.newVirtualThreadExecutor();

    @Override
    public void join() throws InterruptedException {
        executorService.awaitTermination(Long.MAX_VALUE, NANOSECONDS);
    }

    @Override
    public void execute(@NotNull Runnable command) {
        executorService.submit(command);
    }

    @Override
    public int getThreads() {
        return 1;
    }

    @Override
    public int getIdleThreads() {
        return 1;
    }

    @Override
    public boolean isLowOnThreads() {
        return false;
    }

}
