# loomylin

![Banner](gfx/banner.png?raw=true)

Loomylin is a tech demo for [Project Loom](https://openjdk.java.net/projects/loom/),
which brings lightweight user-mode threads (fibers/green-threads) to the JVM.

In the JDK, they refer to these threads as
[Virtual Threads](https://wiki.openjdk.java.net/display/loom/Differences+between+regular+Threads+and+virtual+Threads).
Not all operations are "Virtual Thread Friendly", you can find a list in the
[OpenJDK Wiki](https://wiki.openjdk.java.net/pages/viewpage.action?pageId=46956620).
There's also a nice [Getting started](https://wiki.openjdk.java.net/display/loom/Getting+started) guide in the Wiki,
but this is not required reading to understand this project.

The idea behind this project is to gain knowledge of the practical value of running Loom "in the wild",
by trying to use it with a traditional blocking HttpServlet server (in our case [Jetty](https://www.eclipse.org/jetty/)).

The project implements a [ThreadPool](https://docs.oracle.com/javase/tutorial/essential/concurrency/pools.html)
with virtual threads, passes this `ThreadPool` to a [Jetty](https://www.eclipse.org/jetty/) `Server`,
and passes this Jetty `Server` to a [Javalin](https://javalin.io/) instance (for easy project setup).

The project has three servers:

* A server with a `QueuedThreadPool`, 4 threads (the number of physical cores on my machine), running on port 7000
* A server with a `QueuedThreadPool`, 32 threads, running on port 7001
* A server with a custom `LoomThreadPool`, running on port 7002

The project contains instructions both for running with [Docker](https://www.docker.com/)
and without (using [Maven Wrapper](https://github.com/takari/maven-wrapper), so please, pick your poison.

The project contains two equivalent implementations, one Java and one Kotlin.

## Running with Docker

The fastest way to run the project (provided you have Docker installed), is to:

```
git clone https://github.com/tipsy/loomylin/
cd loomylin
docker build -t loomylin:1.0 .
docker run -p 7000:7000 -p 7001:7001 -p 7002:7002 --detach --name loomylin loomylin:1.0
```

After this you'll have all three servers running, and can proceed to load testing.

## Running without Docker

First, you'll need to grab the latest version of OpenJDK.
The easiest way to do this is to install [SDKMAN](https://sdkman.io/). This will also make it
easy to switch back to a stable JDK later, if you need to.

To list all the available JDKs, you do:

```shell
sdk list java
```

Locate an SDK with Loom (ex: `16.ea.7.lm-open`), and do:

```
sdk install java 19-open
```

That's it. Now you can either run the project via Maven Wrapper by doing:

```
./mvnw clean install
java -jar target/loomylin-jar-with-dependencies.jar
```

Or open the project in your favorite IDE. If you get `Error: Cannot determine path to
'tools.jar' library for 16 (path/to/jdk-16)` when running from IDEA, you should update to the latest version.

The main function in `src/main/kotlin/loomylin/Loomylin.kt` will start all three servers.

## Load testing with hey

_Disclaimer: I have very little experience with load testing, so take these results with a pinch of salt. Feedback to improve this section would be much appreciated!_

You can load test using whatever you want, this section just outlines a quick and easy approach
with the library [hey](https://github.com/rakyll/hey). If you want something from NPM,
[autocannon](https://github.com/mcollina/autocannon) seems like a pretty good alternative.

The `hey` tool runs requests per worker sequentially, so if each worker has 20 requests to run,
and each request takes 100ms, that worker should be done in 2s.

To warm everything up, all tests are run thrice in a for-loop.
Only the third result is included below.

### Computational endpoint

This endpoint runs `Math.random()` 25000 times, then returns "Hello, World".
This number was chosen because it made the tests bearable to run.

#### QueuedThreadPool (4 threads)

```
for i in {1..3}; do hey -n 1000 -c 50 -m GET http://localhost:7000/computational; done

Summary
  Total:    2.0975 secs
  Slowest:  0.2383 secs
  Fastest:  0.0011 secs
  Average:  0.0800 secs
  Requests/sec: 476.7507

  Total data:   14000 bytes
  Size/request: 14 bytes

Response time histogram:
  0.001 [1]     |
  0.025 [491]   |■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  0.049 [14]    |■
  0.072 [29]    |■■
  0.096 [54]    |■■■■
  0.120 [51]    |■■■■
  0.143 [43]    |■■■■
  0.167 [99]    |■■■■■■■■
  0.191 [56]    |■■■■■
  0.215 [112]   |■■■■■■■■■
  0.238 [50]    |■■■■
```

#### QueuedThreadPool (32 threads)

```
for i in {1..3}; do hey -n 1000 -c 50 -m GET http://localhost:7001/computational; done

Summary:
  Total:    5.8990 secs
  Slowest:  0.9932 secs
  Fastest:  0.0038 secs
  Average:  0.2530 secs
  Requests/sec: 169.5216

  Total data:   14000 bytes
  Size/request: 14 bytes

Response time histogram:
  0.004 [1]     |
  0.103 [137]   |■■■■■■■■■■■
  0.202 [481]   |■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  0.301 [167]   |■■■■■■■■■■■■■■
  0.400 [47]    |■■■■
  0.498 [40]    |■■■
  0.597 [15]    |■
  0.696 [19]    |■■
  0.795 [45]    |■■■■
  0.894 [35]    |■■■
  0.993 [13]    |■
```

#### LoomThreadPool (infinite threads)

```
for i in {1..3}; do hey -n 1000 -c 50 -m GET http://localhost:7002/computational; done

Summary:
  Total:    3.7353 secs
  Slowest:  2.6117 secs
  Fastest:  0.0119 secs
  Average:  0.1413 secs
  Requests/sec:	267.7162

  Total data:   14000 bytes
  Size/request: 14 bytes

Response time histogram:
  0.012 [1]     |
  0.272 [983]   |■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  0.532 [2]     |
  0.792 [2]     |
  1.052 [0]     |
  1.312 [0]     |
  1.572 [0]     |
  1.832 [0]     |
  2.092 [0]     |
  2.352 [0]     |
  2.612 [12]    |
```

#### Discussion

When doing pure computation, having number of threads equal to the number of
physical cores should be the most efficient. Using 32 threads is almost three times as slow,
most likely because the overhead introduced by thread switching (and hyper-threading).

I expected the Loom server to be similar in performance to 4 threads, but it's almost twice as slow.
You can see that the request time is super consistent at 272ms, but there are a few
outliers in the results that ruin everything (12 requests took 2.6s to finish). These outliers
might be slow because of garbage collection, but I haven't had the time to look into that yet.

### Waiting endpoint (blocking)

This endpoint is where Loom should shine. It does a simple `Thread.sleep(100)`, before
settings the result to "Hello, World".

#### QueuedThreadPool (4 threads)

```
for i in {1..3}; do hey -n 1000 -c 50 -m GET http://localhost:7000/wait-blocking; done

Summary:
  Total:    52.4542 secs
  Slowest:  6.3712 secs
  Fastest:  0.1005 secs
  Average:  2.2467 secs
  Requests/sec: 19.0642

  Total data:   12000 bytes
  Size/request: 12 bytes

Response time histogram:
  0.100 [1]     |
  0.728 [525]   |■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  1.355 [15]    |■
  1.982 [18]    |■
  2.609 [20]    |■■
  3.236 [19]    |■
  3.863 [15]    |■
  4.490 [55]    |■■■■
  5.117 [175]   |■■■■■■■■■■■■■
  5.744 [87]    |■■■■■■■
  6.371 [70]    |■■■■■
```

#### QueuedThreadPool (32 threads)

```
for i in {1..3}; do hey -n 1000 -c 50 -m GET http://localhost:7001/wait-blocking; done

Summary:
  Total:    3.8441 secs
  Slowest:  0.3177 secs
  Fastest:  0.1004 secs
  Average:  0.1733 secs
  Requests/sec:	260.1360

  Total data:   12000 bytes
  Size/request: 12 bytes

Response time histogram:
  0.100 [1]     |
  0.122 [361]   |■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  0.144 [0]     |
  0.166 [0]     |
  0.187 [0]     |
  0.209 [381]   |■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  0.231 [228]   |■■■■■■■■■■■■■■■■■■■■■■■■
  0.253 [0]     |
  0.274 [0]     |
  0.296 [7]     |■
  0.318 [22]    |■■
```

#### LoomThreadPool (infinite threads)

```
for i in {1..3}; do hey -n 1000 -c 50 -m GET http://localhost:7002/wait-blocking; done

Summary:
  Total:    2.0853 secs
  Slowest:  0.1082 secs
  Fastest:  0.1005 secs
  Average:  0.1042 secs
  Requests/sec: 479.5443

  Total data:   12000 bytes
  Size/request: 12 bytes

Response time histogram:
  0.100 [1]     |
  0.101 [41]    |■■■■■■■■
  0.102 [119]   |■■■■■■■■■■■■■■■■■■■■■■■
  0.103 [128]   |■■■■■■■■■■■■■■■■■■■■■■■■■
  0.104 [87]    |■■■■■■■■■■■■■■■■■
  0.104 [85]    |■■■■■■■■■■■■■■■■■
  0.105 [143]   |■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  0.106 [204]   |■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  0.107 [121]   |■■■■■■■■■■■■■■■■■■■■■■■■
  0.107 [48]    |■■■■■■■■■
  0.108 [23]    |■■■■■
```

#### Discussion
The 4 thread server is basically useless in this scenario. In theory it should spend
`1000 * 100` `/` `min(4, 50)` `=` `25s`, but it gets severely clogged and stuck switching between threads,
spending almost a full minute on servicing the 1000 requests.

The 32 thread server should spend `1000 * 100` `/` `min(32, 50)` `=` `3.1s`, but
since there are more clients than threads, some thread switching and waiting does occur,
and it ends up spending 3.8s.

The infinite thread server (Loom) should spend `1000 * 100` `/` `min(infinity, 50)` `=` `2s`,
and in this case the number of clients is the limiting factor (50). For once reality
is close to theory, and the server finishes in 2.1s.

These are the type of scenarios where Loom should be very useful.
If your server is spending a lot of time waiting for blocking calls, plugging in Loom to
replace a `QueuedThreadPool` would give you instants results, without any further need
for configuration.

### BONUS: Waiting endpoint (async)

For fun, there's also an async endpoint attached to the servers. It doesn't matter which
server you test this against, as async requests are lifted out of the Jetty's request `ThreadPool`.
When running our async test we should get results more or less identical to the Loom test:

```
for i in {1..3}; do hey -n 1000 -c 50 -m GET http://localhost:7000/wait-async; done

Summary:
  Total:    2.0764 secs
  Slowest:  0.1112 secs
  Fastest:  0.1004 secs
  Average:  0.1037 secs
  Requests/sec:	481.6112

  Total data:   12000 bytes
  Size/request: 12 bytes

Response time histogram:
  0.100 [1]     |
  0.101 [86]    |■■■■■■■■■■■
  0.103 [169]   |■■■■■■■■■■■■■■■■■■■■■■
  0.104 [313]   |■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  0.105 [162]   |■■■■■■■■■■■■■■■■■■■■■
  0.106 [161]   |■■■■■■■■■■■■■■■■■■■■■
  0.107 [58]    |■■■■■■■
  0.108 [0]     |
  0.109 [13]    |■■
  0.110 [23]    |■■■
  0.111 [14]    |■■
```

And we do! The downside here is of course that you have to write your code using async concepts,
so instead of a simple `Thread.sleep()` you have to create an executor, wrap your result in a
`CompletableFuture` and schedule the completion of this future on your executor.

### Conclusion

Setting up Project Loom is surprisingly easy, but load testing is surprisingly hard.
Loom seems like it could give huge performance gains for virtually no effort, but it's important
to know when it's appropriate to use it. Remember that not all APIs are "Loom friendly",
so consult the [OpenJDK Wiki](https://wiki.openjdk.java.net/pages/viewpage.action?pageId=46956620)
before getting your hopes up.

If you have any suggestions for improvements or want to expand the test cases and/or load testing sections,
please submit an issue or PR, I would greatly appreciate it.
