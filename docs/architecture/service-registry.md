# Service Registry

## What is Service Discovery?
Service discovery is a mechanism that allows applications to find each other on a network. Instead of hardcoding IP addresses and ports, microservices dynamically register their locations (IP and port) with a central registry when they start up. Other services can then query this registry to find the address of the service they need to communicate with.

## Why ShopSphere Needs Service Discovery
ShopSphere is a microservices-based e-commerce platform. As the system scales, instances of microservices (such as the Product Service or Order Service) will start and stop dynamically across different environments (e.g., Kubernetes, Docker). Service discovery eliminates the need to manually update configurations every time an instance goes up or down, enabling load balancing, seamless scaling, and better resilience.

## What Eureka Server Does
Netflix Eureka Server is a service registry that provides a centralized lookup for microservice instances. It acts as a phonebook for the microservices. Services (Eureka clients) register themselves with the Eureka Server by providing metadata like their host and port. They also send periodic heartbeats to let the registry know they are still alive. If an instance stops sending heartbeats, Eureka removes it from the registry.

## Role of the Service Registry
In ShopSphere, the Service Registry is the foundational infrastructure component where all other microservices will register. It maintains the current state of the system's available instances. It answers the question: 
"Where are the available service instances?"

## Planned Relationship Between Registry and Microservices
Future microservices will act as Eureka clients. They will register with the Service Registry on startup and use it to discover other services they depend on.

Future example:
Product Service
        ↓
registers itself
        ↓
Eureka Server

Later:
Order Service
        ↓
discovers Product Service
        ↓
Eureka Server

## Local URL of the Eureka Dashboard
When running locally, the Eureka dashboard is available at:
[http://localhost:8761](http://localhost:8761)
