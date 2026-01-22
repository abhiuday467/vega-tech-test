Grafana Dashboard Plan

Goal
Split the current single dashboard into three focused dashboards while keeping the existing Transaction API Dashboard name for API health.

Dashboards
1) Business Metrics Dashboard (new)
2) Transaction API Dashboard (existing, repurposed for API health)
3) System/JVM Health Dashboard (new)

Phase 1: Inventory and Mapping
- Inventory panels in monitoring/grafana/dashboards/transaction-api-dashboard.json.
- Map each panel to Business vs API health vs System/JVM.
- Identify missing panels for each dashboard based on available Prometheus metrics.

Phase 2: Create New Dashboards
- Add new files in monitoring/grafana/dashboards:
  - business-metrics-dashboard.json
  - system-jvm-dashboard.json
- Keep Prometheus datasource and tags consistent with existing provisioning.

Phase 3: Move Existing Panels
- Business Metrics Dashboard: move all transaction_* panels (submission/retrieval, error/success rates, trends).
- Transaction API Dashboard: keep/move all http_server_requests_seconds_* panels (status codes, error/success %, latency).
- System/JVM Dashboard: no existing panels; create fresh.

Phase 4: Add Missing Panels
- Business Metrics:
  - transaction amount avg/max (transaction_amount_sum/count/max)
  - item count avg/max (transaction_item_count_sum/count/max)
  - breakdowns by store/till/payment method
- Transaction API:
  - p50/p99 latency in addition to p95
  - request rate by endpoint (uri/method)
  - error rate by endpoint
- System/JVM:
  - heap/non-heap usage
  - GC pause time
  - CPU usage
  - live thread count
  - HikariCP pool usage (if available)

Phase 5: Validate and Iterate
- Start Grafana/Prometheus and confirm dashboards load and panels show data.
- Tune units, thresholds, and time ranges.
- Optionally delete or archive legacy panels from the original dashboard file once split is stable.

Status
- Phase 4 implemented in dashboard JSON files.
- Phase 5 requires running Grafana/Prometheus to validate panel data and adjust thresholds.
