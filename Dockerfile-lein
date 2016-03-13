FROM clojure

RUN mkdir -p /usr/src/app/src
WORKDIR /usr/src/app

# Install dependencies separately for better cacheability
COPY project.clj /usr/src/app/
RUN lein deps

# Copy application sources
COPY src /usr/src/app/src

# Run application
EXPOSE 8080
CMD ["lein", "run"]
