# Technical Details

## 🛒 What You're Building

You're implementing an event-driven consumer to handle supermarket till transactions. The system currently uses REST
APIs, and you'll add Kafka event processing.

### Current System

- **REST API** - Tills send transactions via HTTP
- **Database** - PostgreSQL stores transaction data
- **Statistics** - Calculated from database records

### What You'll Add

- **Event Consumer** - Process Kafka messages
- **Data Integration** - Combine REST and event data
- **Bug Fix** - Investigate statistics calculation issues

### External Services (Already Running)

- **Till Simulator** - Sends REST transactions
- **Kafka Producer** - Sends event messages

### Kafka Message Format

The Kafka producer sends transaction events to the `transactions` topic in the following format:

```json
{
  "eventId": "uuid",
  "eventType": "TRANSACTION_CREATED",
  "eventTimestamp": "2025-06-27T12:00:00.000Z",
  "source": "till-system",
  "version": "1.0",
  "data": {
    "transactionId": "TXN-12345678",
    "customerId": "CUST-12345",
    "storeId": "STORE-001",
    "tillId": "TILL-1",
    "paymentMethod": "card",
    "totalAmount": 25.50,
    "currency": "GBP",
    "timestamp": "2025-06-27T12:00:00.000Z",
    "items": [
      {
        "productName": "Milk",
        "productCode": "MILK001",
        "unitPrice": 2.50,
        "quantity": 2,
        "category": "Dairy"
      },
      {
        "productName": "Bread",
        "productCode": "BREAD001",
        "unitPrice": 1.20,
        "quantity": 1,
        "category": "Bakery"
      }
    ]
  }
}
```

**Key Differences from REST API:**

- Transaction data is wrapped in an event envelope with metadata
- `totalAmount` is a `float` instead of `BigDecimal`
- Timestamps are ISO strings instead of `ZonedDateTime`
- Event metadata includes `eventId`, `eventType`, `source`, and `version`

### Kafka Message Model (Already Implemented)

**Good news:** The `KafkaTransactionEvent` model is already implemented and tested!

- **Location:** `src/main/java/com/vega/techtest/dto/KafkaTransactionEvent.java`
- **Test:** `src/test/java/com/vega/techtest/KafkaMessageDeserializationTest.java`
- **Features:** Jackson annotations for easy deserialization, generic `Map<String, Object>` for data field

**What you need to do:** Configure your Kafka consumer to deserialize messages into this model. The test shows exactly
how it works with the JSON format above.

### Technical Stack

- **Backend**: Spring Boot (Java)
- **Database**: PostgreSQL
- **Message Broker**: Apache Kafka
- **Monitoring**: Grafana + Prometheus
- **Observability**: OpenTelemetry
- **Containerization**: Docker & Docker Compose
- **API Testing**: Postman Collections

### What's Already Implemented

**Good news:** Most of the system is already built and ready to go!

- ✅ Complete REST API for transaction processing
- ✅ Database schema and entities
- ✅ Transaction service and repository
- ✅ Docker setup with all infrastructure
- ✅ External simulators (till simulator + Kafka producer)
- ✅ Monitoring setup (Grafana/Prometheus)
- ✅ Kafka message model (`KafkaTransactionEvent`) with working deserialization test

**What you need to implement:**

- 🔧 Event consumer service (Kafka or your preferred system)
- 🔧 Message deserialization configuration (using the provided `KafkaTransactionEvent` model)
- 🔧 Integration between REST and event flows
- 🔧 Investigation and resolution of reported issues

### No Kafka Experience? No Problem!

**If you've never used Kafka before, that's completely fine!**

You can use any queuing system you're comfortable with:

- **RabbitMQ** - Message broker with pub/sub capabilities
- **Redis Pub/Sub** - Simple publish/subscribe messaging
- **In-memory queues** - Using Java's BlockingQueue or similar
- **Database-based queues** - Using PostgreSQL with polling
- **Any other message queue** you're familiar with

**What we're really testing:** Your ability to design and implement event-driven architecture vs synchronous design.

### Monitoring (Optional)

**Note: Monitoring is completely optional and NOT required to pass the interview.**

The project includes Grafana and Prometheus for monitoring:

- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Application Metrics**: http://localhost:8080/actuator/prometheus

**Bonus points** for implementing monitoring dashboards and observability features. 