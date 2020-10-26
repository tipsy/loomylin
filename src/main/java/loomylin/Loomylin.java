package loomylin;

import io.javalin.Javalin;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static io.javalin.apibuilder.ApiBuilder.get;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class Loomylin {

    static long DELAY = 100;
    static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
        createServer(new QueuedThreadPool(4, 4)).start(7000); // start a server with a thread pool equal to number of physical cores
        createServer(new QueuedThreadPool(32, 32)).start(7001); // start a server with wanted concurrency
        createServer(new LoomThreadPool()).start(7002); // start a loom server
    }

    private static Javalin createServer(ThreadPool threadPool) {
        return Javalin.create(config -> {
            config.server(() -> new Server(threadPool));
            config.showJavalinBanner = false;
        }).routes(() -> {
            get("/computational", ctx -> {
                for (int i = 0; i < 25000; i++) {
                    Math.random();
                }
                ctx.result("Hello, World");
            });
            get("/wait-blocking", ctx -> {
                Thread.sleep(DELAY);
                ctx.result("Hello, World");
            });
            get("/wait-async", ctx -> {
                CompletableFuture<String> future = new CompletableFuture<>();
                executor.schedule(() -> future.complete("Hello, World"), DELAY, MILLISECONDS);
                ctx.result(future);
            });
        });
    }

}
