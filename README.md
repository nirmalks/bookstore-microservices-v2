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

### Starting the Application

The infrastructure has been upgraded to automatically configure self-signed certificates and provision HashiCorp Vault with AppRole authentication. The `vault-init` and `vault-cert-generator` Docker services securely handle this lifecycle on your behalf.

You do **NOT** need to manually initialize Vault, configure policies, or generate certificates. The microservices will automatically wait for Vault to become ready and extract their secure credentials into the local `./vault/approles` directory.

To start the entire microservices stack from scratch, simply run:
```bash
docker compose up -d
```

*(Note: The databases and Vault take about 30-45 seconds to fully initialize and bind their dynamic credentials. The microservices are configured to patiently wait for this initialization sequence to finish).*

**Important Security Note**: Dynamic credentials and self-signed certificates are written respectively to the `./vault/approles` and `./vault/certs` directories on your host machine to be mounted into the containers. These folders have explicitly been added to your `.gitignore` to prevent committing highly sensitive, dynamically generated credentials to version control. Please do not commit any `.properties` or `.env` files located inside the `vault/` directory.

## credentials
* admin cred - admin/admin123
* customer cred - john_doe/admin123

## swagger
* User service - http://localhost:8081/swagger-ui/index.html
* Catalog service - http://localhost:8082/swagger-ui/index.html
* Checkout service - http://localhost:8083/swagger-ui/index.html
