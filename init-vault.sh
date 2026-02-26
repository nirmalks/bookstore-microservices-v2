#!/bin/bash
# ---------------------------------------------------------
# Vault Initialization Script
# ---------------------------------------------------------

echo "Starting Vault initialization..."

# 1. Login
vault login my-root-token

# ---------------------------------------------------------
# 2. Key/Value Secrets Engine (Static Secrets)
# ---------------------------------------------------------

# Create shared secrets (e.g., RabbitMQ, Redis)
vault kv put secret/application \
  spring.rabbitmq.password="guest" \
  spring.data.redis.password="redis_admin_pass"
  
# Create service-specific secrets for the checkout-service
vault kv put secret/checkout-service \
  stripe.api.key="sk_test_123456789"

# ---------------------------------------------------------
# 3. Database Secrets Engine (Dynamic Credentials)
# ---------------------------------------------------------

# Enable the database secrets engine
vault secrets enable database || true

# --- User Service DB ---
vault write database/config/user-postgres-database \
    plugin_name=postgresql-database-plugin \
    allowed_roles="user-role" \
    connection_url="postgresql://{{username}}:{{password}}@user-db:5432/bookstore_user_service_db?sslmode=disable" \
    username="postgres" \
    password="admin123"

vault write database/roles/user-role \
    db_name=user-postgres-database \
    creation_statements="CREATE ROLE \"{{name}}\" WITH SUPERUSER LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}';" \
    revocation_statements="REASSIGN OWNED BY \"{{name}}\" TO postgres; DROP OWNED BY \"{{name}}\"; DROP ROLE \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"

# --- Catalog Service DB ---
vault write database/config/catalog-postgres-database \
    plugin_name=postgresql-database-plugin \
    allowed_roles="catalog-role" \
    connection_url="postgresql://{{username}}:{{password}}@catalog-db:5432/bookstore_catalog_service_db?sslmode=disable" \
    username="postgres" \
    password="admin123"

vault write database/roles/catalog-role \
    db_name=catalog-postgres-database \
    creation_statements="CREATE ROLE \"{{name}}\" WITH SUPERUSER LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}';" \
    revocation_statements="REASSIGN OWNED BY \"{{name}}\" TO postgres; DROP OWNED BY \"{{name}}\"; DROP ROLE \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"

# --- Checkout Service DB ---
vault write database/config/checkout-postgres-database \
    plugin_name=postgresql-database-plugin \
    allowed_roles="checkout-role" \
    connection_url="postgresql://{{username}}:{{password}}@checkout-db:5432/bookstore_checkout_service_db?sslmode=disable" \
    username="postgres" \
    password="admin123"

vault write database/roles/checkout-role \
    db_name=checkout-postgres-database \
    creation_statements="CREATE ROLE \"{{name}}\" WITH SUPERUSER LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}';" \
    revocation_statements="REASSIGN OWNED BY \"{{name}}\" TO postgres; DROP OWNED BY \"{{name}}\"; DROP ROLE \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"

# --- Notification Service DB ---
vault write database/config/notification-postgres-database \
    plugin_name=postgresql-database-plugin \
    allowed_roles="notification-role" \
    connection_url="postgresql://{{username}}:{{password}}@notification-db:5432/bookstore_notification_service_db?sslmode=disable" \
    username="postgres" \
    password="admin123"

vault write database/roles/notification-role \
    db_name=notification-postgres-database \
    creation_statements="CREATE ROLE \"{{name}}\" WITH SUPERUSER LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}';" \
    revocation_statements="REASSIGN OWNED BY \"{{name}}\" TO postgres; DROP OWNED BY \"{{name}}\"; DROP ROLE \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"

# --- Audit Service DB ---
vault write database/config/audit-postgres-database \
    plugin_name=postgresql-database-plugin \
    allowed_roles="audit-role" \
    connection_url="postgresql://{{username}}:{{password}}@audit-db:5432/bookstore_audit_service_db?sslmode=disable" \
    username="postgres" \
    password="admin123"

vault write database/roles/audit-role \
    db_name=audit-postgres-database \
    creation_statements="CREATE ROLE \"{{name}}\" WITH SUPERUSER LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}';" \
    revocation_statements="REASSIGN OWNED BY \"{{name}}\" TO postgres; DROP OWNED BY \"{{name}}\"; DROP ROLE \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"

echo "Vault secrets initialized successfully!"
