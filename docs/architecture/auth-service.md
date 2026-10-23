# Auth Service Architecture

## Responsibility
The Auth Service is a planned core business microservice responsible for:
- User registration
- Credential management
- Password hashing
- Login
- JWT access tokens
- Refresh tokens

## Why Separated?
Authentication is isolated in its own microservice so that security logic can be maintained independently of other business processes. It ensures a single source of truth for identities and allows for dedicated scaling for high-traffic operations (like logins).

## Current Infrastructure Integration
### Config Server
The Auth Service utilizes Spring Cloud Config Client (`spring.config.import=optional:configserver:http://localhost:8888`) to dynamically load its configuration properties on startup. This ensures that environmental variables and secrets are maintained centrally without hardcoding.

### Service Registry (Eureka)
Upon startup, Auth Service registers itself as an instance with the Eureka Server using the service ID `AUTH-SERVICE`. This allows other future microservices (or an API Gateway) to dynamically route traffic to it without hardcoded IPs.

## Network Details
- **Local Port:** 8081
- **Service Discovery Name:** AUTH-SERVICE

## Architecture Flow
```text
Auth Service
     │
     ├──────────────→ Config Server :8888
     │
     └──────────────→ Eureka Server :8761
```

## Current Implementation Status
**CURRENTLY IMPLEMENTED:**
- Spring Boot service foundation
- Config Client integration
- Eureka Client integration

**PLANNED:**
- Registration
- Login
- JWT
- Refresh tokens
