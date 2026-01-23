# Tech Test (BETA)

Vega Tech Test For Software Engineers

## 🎯 Quick Overview

**Your task:** Implement an event-driven consumer to handle supermarket till transactions. This application is a legacy
application developed in a rush and vibe coded, so some cleanup is also required.

**Take-home test:** The time to complete will be mentioned in your invite email.

**OR** (Decided by Vega at the time of interview organisation, this is dependent on the person reviewing it, you should
have been informed which one you are doing if not please email us and ask)

**Live interview:** 45 minutes - 1 hour (different scope)

**What you get:** Complete working system with infrastructure, just add your consumer.

**Note:** This is a legacy application that has been in production for some time. While it works, there are areas that
could benefit from improvements and refactoring.

## 🐛 User Report

**From:** Store Manager, STORE-001  
**Subject:** Statistics showing wrong numbers  
**Priority:** High

> "The average and total amounts in our store statistics don't match what I expect based on our till receipts. The
> numbers seem off, and I can't figure out why. Can anyone look into this? We're using the
> `/api/transactions/stats/STORE-001` endpoint."

## 📋 What You Need To Do

### Required Tasks:

1. **Get the system running** - Use the commands above
2. **Investigate reported issues** - Fix the statistics calculation bug reported by store manager
3. **Implement event consumer** - Create service to process transaction events
4. **Handle both data sources** - Make REST and events work together
5. **Improve code quality** - Identify and refactor areas of the legacy/vibe-coded codebase that could be improved

### Optional Tasks (Bonus):

- **Monitoring** - Grafana dashboards
- **Error handling** - Dead letter queues, retry mechanisms
- **Testing** - Improve Unit and integration test coverage (we do need a few to evaluate your testing knowledge!)
- **Performance optimization** - Identify and improve any performance bottlenecks
- **Security improvements** - Review and enhance security aspects of the application

## 🚀 Get Started in 5 Minutes

### Start Infrastructure

```bash
# Option A: Use startup script (recommended)
./scripts/start-all.sh

# Option B: Start manually
docker-compose up -d
```

This launches the supporting services (Postgres, Kafka, Grafana/Prometheus). The application itself runs locally via Gradle.

### Start Application

```bash
# Linux/macOS
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

### Test the System

```bash
# Test the statistics endpoint mentioned in the user report
curl http://localhost:8080/api/transactions/stats/STORE-001

# Health check
curl http://localhost:8080/api/transactions/health
```

**That's it!** You now have a working system with sample data pre-populated to build upon.

## 🔍 Swagger / OpenAPI

- Swagger UI: `http://localhost:8080/swagger-ui`
- OpenAPI spec file: `openapi.yaml`

### View OpenAPI via Swagger UI (Docker)

```bash
docker run -p 8082:8080 \
  -e SWAGGER_JSON=/app/openapi.yaml \
  -v "$(pwd)/openapi.yaml:/app/openapi.yaml" \
  swaggerapi/swagger-ui
```

Then open `http://localhost:8082`.

## 📚 Additional Documentation

- **[Windows Setup](docs/WINDOWS-SETUP.md)** - Windows-specific setup instructions
- **[Technical Details](docs/TECHNICAL.md)** - Architecture, message formats, APIs
- **[Troubleshooting](docs/TROUBLESHOOTING.md)** - Common issues and solutions
- **[Evaluation Guide](docs/EVALUATION.md)** - What we're looking for and how you'll be assessed

## ❓ Quick Questions

**Q: What if I don't know Kafka?**
A: Use any queuing system you're comfortable with. We're testing event-driven design, not specific tools.

**Q: How much should I implement?**
A: Complete the four required tasks above. Bonus tasks are exactly that - bonus!

**Q: What if I get stuck?**
A: Document your approach and troubleshooting steps. We're more interested in your problem-solving process.

**Q: Can I use external libraries?**
A: Absolutely! Use whatever helps you solve the problem efficiently.

## 🎪 Live Interviews

If you prefer this style of interviewing (or really don't like it) please let us know we are happy to accommodate your
preference. Email us if you would like to switch the style of the tech test.

**Scope:** Live interviews (45 min - 1 hour) focus on problem-solving and collaboration, not full implementation. You'll
work on a subset of tasks with guidance.

**Preparation:** Get the system running before the interview. The session focuses on demonstrating your approach and
working together effectively.

## ✅ Success Criteria

The Success Criteria will be different for live interviews, which will be explained on the day, but for take home see
below.

**You've succeeded when:**

- ✅ System starts up and runs without errors
- ✅ Any reported issues are investigated and resolved
- ✅ Event consumer processes messages from the queue
- ✅ Both REST and event data sources work together
- ✅ Code is clean, readable, and well-structured
- ✅ Legacy code has been improved where appropriate

**Focus on:** Working solution, clean code, and demonstrating your problem-solving approach.

---

**Ready to start?** Use the commands above and get coding!
