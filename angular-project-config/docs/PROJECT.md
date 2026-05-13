# Project Description

## What This Application Does
<!-- Replace with your actual project description -->
[Application Name] is a [type of system] for [target users/industry].
It handles [core business processes] and integrates with [key external systems].

## Business Domain
<!-- Describe the real-world domain this software models -->
- **Industry**: [e.g., wholesale distribution, healthcare, fintech]
- **Core problem solved**: [e.g., automates order-to-invoice lifecycle]
- **Users**: [who uses this system and what are their roles]

## Key Business Entities
<!-- List the main domain objects — this helps agents name things correctly -->
| Entity | Description |
|--------|-------------|
| Order | Represents a customer purchase with line items, pricing, and status lifecycle |
| Customer | B2B customer account with billing info, credit terms, and order history |
| Product | Catalog item with SKU, pricing tiers, and inventory tracking |
| Warehouse | Physical location managing stock levels and fulfillment |
| Invoice | Financial document generated from fulfilled orders |

## Business Rules (Critical)
<!-- These are the rules agents MUST respect when generating code -->
- An order cannot be placed if the customer exceeds their credit limit
- Inventory must be reserved at order placement, not at fulfillment
- Orders over $10,000 require manager approval before processing
- Price changes do not affect existing confirmed orders
- Cancelled orders must release reserved inventory immediately

## External Integrations
<!-- Agents need to know what external systems exist -->
| System | Purpose | Direction |
|--------|---------|-----------|
| SAP ERP | Inventory sync, financial posting | Bidirectional |
| Stripe | Payment processing | Outbound |
| SendGrid | Transactional emails (confirmations, invoices) | Outbound |
| Kafka | Domain event streaming between microservices | Bidirectional |

## Scale & Performance Requirements
<!-- Helps agents make appropriate technical choices -->
- Expected load: ~10,000 orders/day, peak 500/hour
- Response time target: < 200ms for read APIs, < 500ms for write APIs
- Data retention: 7 years for financial records
- Availability target: 99.9% uptime

## Environments
| Environment | Purpose | URL |
|-------------|---------|-----|
| local | Developer machine | http://localhost:8080 |
| dev | Shared development | https://dev-api.company.com |
| staging | Pre-production testing | https://staging-api.company.com |
| production | Live | https://api.company.com |

## Key Decisions Already Made
<!-- Prevents agents from re-litigating settled decisions -->
- **Frontend Framework**: Angular 21 with standalone components, signals, and zoneless change detection (not AngularJS, not older versions)
- **Database**: PostgreSQL 16 (not MySQL — chosen for JSON support and ACID compliance)
- **ORM**: JPA/Hibernate via Spring Data (not JOOQ — team familiarity)
- **Messaging**: Kafka (not RabbitMQ — needed for event sourcing future)
- **Auth**: JWT + OAuth2 via Keycloak (not custom auth)
- **Architecture**: Hexagonal (not layered — domain complexity justifies it)
