FROM clojure

RUN mkdir -p /usr/src/app/src
WORKDIR /usr/src/app

# Install dependencies separately for better cacheability
COPY project.clj /usr/src/app/
RUN lein deps

# Create application jar
COPY src /usr/src/app/src
RUN lein uberjar

# Run application
EXPOSE 8080
CMD ["java", "-jar", "target/server.jar"]
