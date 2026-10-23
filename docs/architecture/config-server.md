# Config Server

## What is Centralized Configuration?
In a microservices architecture, managing configuration settings (like database URLs, external API keys, feature flags) for many independent services becomes challenging. Centralized configuration solves this by moving these settings out of the individual application deployments and into a central repository, managed by a dedicated service.

## Why ShopSphere Uses a Config Server
ShopSphere utilizes a Config Server to externalize its configuration. This enables configurations to be changed, managed, and version-controlled centrally without needing to rebuild or redeploy individual microservices.

## Why Configuration Should Not Be Hard-Coded
Hard-coding configurations independently inside every microservice leads to:
- Configuration drift: It's hard to ensure all instances of a service use the correct settings.
- Security risks: Secrets might accidentally be committed in source code repositories alongside business logic.
- Operational overhead: Changing a configuration (e.g., updating a database password) requires rebuilding and redeploying the affected microservice(s).

## What the Config Server Does
The Spring Cloud Config Server provides an HTTP resource-based API for external configuration. It connects to a backend repository (like Git, or a local file system for development) to retrieve configurations. When a client requests its configuration, the Config Server reads it from the repository and serves it as a structured payload (JSON).

## What a Config Client Does
A Config Client is any microservice that uses the Config Server to fetch its configuration on startup. The client connects to the Config Server, identifies itself (usually by its application name and active profiles), and retrieves its specific property sources.

## Why the Config Server is Infrastructure
The Config Server doesn't handle business operations, such as creating orders or processing payments. It's a foundational component that supports the operational aspects of the platform.

## Local Config Server URL
When running locally, the Config Server is available at:
[http://localhost:8888](http://localhost:8888)

## Location of the Development Configuration Repository
For local development, we use a native backend configuration repository located at:
`infrastructure/config-repository/`

## Simple Request/Response Flow
1. A microservice starts up and acts as a **Config Client**.
2. It sends an HTTP request to the **Config Server** (e.g., `GET /application/default`).
3. The Config Server reads from the **Configuration Repository**.
4. The Config Server responds with the aggregated configuration properties in JSON format.
5. The microservice binds these properties to its environment and completes startup.

Future Architecture:
```text
Microservice
     ↓
Config Client
     ↓
Config Server
     ↓
Configuration Repository
```

Future services such as:
- auth-service
- user-service
- product-service
- cart-service
- inventory-service
- order-service
- payment-service
- notification-service
- review-service

will eventually consume configuration from this Config Server.
