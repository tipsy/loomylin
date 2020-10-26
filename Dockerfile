# Install the latest version of the official Ubuntu image (Ubuntu is a popular Linux flavor)
FROM ubuntu:latest

# Set the working directory of the container to be "/loomylin" (this can be whatever)
WORKDIR /loomylin

# Download openjdk-16-loom to the /loomylin dir
# These URLs are not permanent, check https://jdk.java.net/loom/ for the latest version (Linux/x64)
ADD "https://download.java.net/java/early_access/loom/7/openjdk-16-loom+7-285_linux-x64_bin.tar.gz" /loomylin

# Untar (unzip/unpack) the jdk-16 file
RUN tar -xzvf "openjdk-16-loom+7-285_linux-x64_bin.tar.gz"

# Set the JDK path to use jdk-16
ENV PATH="/loomylin/jdk-16/bin:${PATH}"

# Copy project files into the working dir in the docker container
COPY . /loomylin

# Run Maven wrapper - the first line replaces Windows line endings (this can be deleted if not running on Windows)
RUN sed -i 's/\r$//' mvnw
RUN ./mvnw clean install

# The jar contains all three QueuedThreadPool and the LoomThreadPool servers
ENTRYPOINT ["java", "-jar", "target/loomylin-jar-with-dependencies.jar"]
