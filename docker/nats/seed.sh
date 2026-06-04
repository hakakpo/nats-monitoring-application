#!/bin/sh
set -eu

SERVER="${NATS_URL:-nats://local:local@nats:4222}"

run_nats() {
  nats --server "$SERVER" "$@"
}

wait_for_nats() {
  echo "Waiting for NATS at $SERVER..."
  until run_nats server check connection >/dev/null 2>&1; do
    sleep 1
  done

  echo "Waiting for JetStream..."
  until run_nats server check jetstream >/dev/null 2>&1; do
    sleep 1
  done
}

ensure_stream() {
  name="$1"
  subjects="$2"
  description="$3"

  if run_nats stream info "$name" >/dev/null 2>&1; then
    echo "Stream $name already exists"
    return
  fi

  echo "Creating stream $name"
  run_nats stream add "$name" \
    --subjects "$subjects" \
    --description "$description" \
    --storage file \
    --retention limits \
    --discard old \
    --max-msgs 10000 \
    --max-age 24h \
    --defaults
}

ensure_consumer() {
  stream="$1"
  consumer="$2"
  filter="$3"
  description="$4"

  if run_nats consumer info "$stream" "$consumer" >/dev/null 2>&1; then
    echo "Consumer $stream/$consumer already exists"
    return
  fi

  echo "Creating consumer $stream/$consumer"
  run_nats consumer add "$stream" "$consumer" \
    --pull \
    --ack explicit \
    --deliver all \
    --filter "$filter" \
    --description "$description" \
    --max-pending 2000 \
    --wait 45s \
    --defaults
}

publish_messages() {
  now="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

  echo "Publishing sample order traffic"
  i=1
  while [ "$i" -le 80 ]; do
    run_nats pub "orders.created" "{\"id\":\"ORD-$i\",\"customer\":\"customer-$((i % 12))\",\"amount\":$((20 + i * 3)),\"createdAt\":\"$now\"}" >/dev/null 2>&1
    i=$((i + 1))
  done

  i=1
  while [ "$i" -le 25 ]; do
    run_nats pub "orders.updated" "{\"id\":\"ORD-$i\",\"status\":\"PACKED\",\"updatedAt\":\"$now\"}" >/dev/null 2>&1
    i=$((i + 1))
  done

  i=1
  while [ "$i" -le 8 ]; do
    run_nats pub "orders.cancelled" "{\"id\":\"ORD-CANCEL-$i\",\"reason\":\"payment_timeout\",\"cancelledAt\":\"$now\"}" >/dev/null 2>&1
    i=$((i + 1))
  done

  echo "Publishing sample payment traffic"
  i=1
  while [ "$i" -le 45 ]; do
    run_nats pub "payments.authorized" "{\"paymentId\":\"PAY-$i\",\"orderId\":\"ORD-$i\",\"amount\":$((30 + i * 2)),\"authorizedAt\":\"$now\"}" >/dev/null 2>&1
    i=$((i + 1))
  done

  i=1
  while [ "$i" -le 12 ]; do
    run_nats pub "payments.failed" "{\"paymentId\":\"PAY-FAILED-$i\",\"orderId\":\"ORD-$((i * 3))\",\"error\":\"card_declined\",\"failedAt\":\"$now\"}" >/dev/null 2>&1
    i=$((i + 1))
  done

  echo "Publishing sample telemetry and audit traffic"
  i=1
  while [ "$i" -le 35 ]; do
    run_nats pub "telemetry.api" "{\"service\":\"checkout\",\"latencyMs\":$((75 + i * 4)),\"status\":200,\"observedAt\":\"$now\"}" >/dev/null 2>&1
    run_nats pub "audit.user" "{\"actor\":\"user-$((i % 7))\",\"action\":\"order.view\",\"resource\":\"ORD-$i\",\"recordedAt\":\"$now\"}" >/dev/null 2>&1
    i=$((i + 1))
  done

  echo "Publishing sample dead-letter traffic"
  i=1
  while [ "$i" -le 6 ]; do
    run_nats pub "dlq.orders" "{\"id\":\"DLQ-$i\",\"source\":\"orders.created\",\"error\":\"schema_validation_failed\",\"failedAt\":\"$now\"}" >/dev/null 2>&1
    i=$((i + 1))
  done
}

create_ack_pending_samples() {
  echo "Creating a few unacked deliveries for visible ack_pending metrics"
  run_nats consumer next ORDERS orders-worker --count 5 --no-ack --wait 1s >/dev/null 2>&1 || true
  run_nats consumer next PAYMENTS payments-worker --count 3 --no-ack --wait 1s >/dev/null 2>&1 || true
}

wait_for_nats

ensure_stream "ORDERS" "orders.*" "Local demo order events"
ensure_stream "PAYMENTS" "payments.*" "Local demo payment events"
ensure_stream "AUDIT" "audit.>" "Local demo audit events"
ensure_stream "TELEMETRY" "telemetry.>" "Local demo telemetry events"
ensure_stream "DLQ" "dlq.>" "Local demo dead-letter events"

ensure_consumer "ORDERS" "orders-worker" "orders.created" "Worker intentionally left with pending order messages"
ensure_consumer "ORDERS" "orders-audit" "orders.*" "Audit consumer with broad pending backlog"
ensure_consumer "PAYMENTS" "payments-worker" "payments.*" "Payment worker with pending messages"
ensure_consumer "DLQ" "dlq-review" "dlq.>" "Manual review queue for failed events"

publish_messages
create_ack_pending_samples

echo "NATS local seed complete"
