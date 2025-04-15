# Fact Fetcher Service

## Overview

This service fetches random facts from the Useless Facts API, provides shortened URLs, caches them in-memory, and offers a private area to consult access statistics.

## Requirements

- [x] Fetches random facts from the Useless Facts API (`https://uselessfacts.jsph.pl/random.json?language=en`).
- [x] Provides a shortened URL for each fetched fact.
- [x] Implements in-memory caching for facts and their shortened URLs.
- [x] Provides a private endpoint to consult access statistics for each shortened URL.
- [x] Uses in-memory storage instead of a database.

## Endpoints

1. **Fetch Fact and Shorten URL**
    - Endpoint: `POST /facts`
    - Description: Fetches a random fact, stores it, and returns a shortened URL.
    - Response:
   ```json
   {
     "original_fact": "string",
     "shortened_url": "string"
   }
   ```

2. **Return single cached fact**
    - Endpoint: `GET /facts/{shortenedUrl}`
    - Description: Returns the cached fact and increments the access count.
    - Response:
   ```json
   {
     "fact": "string",
     "original_permalink": "string"
   }
   ```

3. **Return all cached facts**
    - Endpoint: `GET /facts`
    - Description: Returns all cached facts without incrementing the access count.
    - Response:
   ```json
   [
     {
       "fact": "string",
       "original_permalink": "string"
     }
   ]
   ```

4. **Redirect to original fact**
    - Endpoint: `GET /facts/{shortenedUrl}/redirect`
    - Description: Redirects to the original fact and increments the access count.

5. **Access statistics**
    - Endpoint: `GET /admin/statistics`
    - Description: Provides access statistics for all shortened URLs.
    - Response:
   ```json
   [
     {
       "shortened_url": "string",
       "access_count": "integer"
     }
   ]
   ```

## Configuration

The following properties can be configured in the `application.yaml` file:

- `ktor.deployment.port`: The port the application will run on. Default is `8080` and can be configured through the `PORT` environment variable.
- `services.facts.cacheSize`: The size of the in-memory cache for facts. Default is `100`. This can also be set using the environment variable `CACHE`.
- `services.facts.remote.uselessFacts.url`: The URL of the Useless Facts API. Default is `"https://uselessfacts.jsph.pl/random.json?language=en"`. Environment variable is `FACTS_API`.

## Instructions

### 1. Project Setup

- Clone the repository:
  ```bash
  git clone https://github.com/altrao/FactFetcher.git
  cd FactFetcher
  ```

### 2. Running the Application

#### Option 1: Build and Run with Gradle

1. **Build the application:**
   ```bash
   ./gradlew clean build
   ```

2. **Run the application:**
   ```bash
   ./gradlew run
   ```

#### Option 2: Run with Docker

1. **Build the Docker image:**
   ```bash
   docker build -t factfetcher .
   ```

2. **Run the Docker container:**
   ```bash
   docker run -p 8080:8080 factfetcher
   ```

### 3. Accessing Endpoints

- Once the application is running, you can access the endpoints as described above. For example, to fetch a fact and shorten the URL, send a `POST` request to `/facts`. To access statistics, send a `GET` request to `/admin/statistics`.
