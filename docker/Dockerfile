# syntax=docker/dockerfile:1

# ---- Build stage ----
# Compiles the server + shared "common" classes using the exact same
# javac invocation that was verified locally (see README "How to Run").
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY src ./src
RUN javac -d bin src/common/*.java src/server/*.java

# ---- Runtime stage ----
# Small runtime-only image; only the compiled classes are copied in, not
# the JDK's compiler tooling, keeping the final image slim.
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
COPY --from=build /app/bin ./bin

# The server writes chat history to logs/chat.log relative to the
# working directory -- mount a volume here to persist it outside the container.
VOLUME ["/app/logs"]

EXPOSE 5000

# Default port matches ChatServer.DEFAULT_PORT; override with:
#   docker run -e PORT=5001 -p 5001:5001 <image> 5001
ENTRYPOINT ["java", "-cp", "bin", "server.ChatServer"]
CMD ["5000"]
