Bookstore APi using Microservices

- Bookstore API using microservices architecture . Converted from the monolith version -https://github.com/nirmalks/bookstore-spring-be
- Backend Stack: Spring Boot, Spring Security (JWT), Spring Data JPA, PostgreSQL, Flyway, Spring Eureka client, Spring Eureka server, Spring Cloud gateway, Spring Boot Oauth2, Spring Webflux
- Tools & Libraries: Swagger

Key Features

* RESTful APIs for managing books, authors, users, orders, carts, and genres
* JWT-based authentication and role-based authorization (user, admin)
* Advanced book search with filtering and pagination using JPA Specifications
* Swagger documentation for API testing
* Flyway integration for consistent DB migrations
  
Admin Capabilities
* Add or update books, authors, and genres

User Capabilities
* Register, log in, and manage account profile
* Browse and search books
* Add items to cart and place orders
* View orders

### starting the application 
Because microservices require hashicorp vault to generate their database credentials on startup, **they will crash if Vault or the Databases are not fully initialized first.** 

To prevent this "dependency hell", follow this exact sequence when starting the project from scratch:

**Phase 1: Boot the Infrastructure**
Start only Vault and the Databases first:
```bash
docker compose up -d vault user-db catalog-db checkout-db notification-db audit-db redis rabbitmq
```
*(Wait a few seconds for the databases to finish their initialization)*

**Phase 2: Initialize Vault**
Copy and execute your `init-vault.sh` script to create the static secrets and dynamic database roles:
```bash
# Copy the script inside the Vault container
docker cp init-vault.sh vault:/tmp/init-vault.sh

# Execute the script
docker exec vault sh /tmp/init-vault.sh
```

**Phase 3: Boot the Microservices**
Now that Vault is fully armed with the capability to generate PostgreSQL users, you can safely boot the rest of the Spring Boot applications:
```bash
docker compose up -d
```

## credentials
* admin cred - admin/admin123
* customer cred - john_doe/admin123

## swagger
* User service - http://localhost:8081/swagger-ui/index.html
* Catalog service - http://localhost:8082/swagger-ui/index.html
* Checkout service - http://localhost:8083/swagger-ui/index.html
