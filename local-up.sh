#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="diploma-demo"
K8S_DIR="${K8S_DIR:-k8s}"

echo "==> Local UP started"
kubectl config current-context
kubectl get nodes >/dev/null

echo "==> Apply namespace"
kubectl apply -f "${K8S_DIR}/namespace.yaml"

echo "==> Apply secrets/config"
kubectl apply -f "${K8S_DIR}/secret.yaml"
kubectl apply -f "${K8S_DIR}/configmap.yaml"

echo "==> Apply DB/Cache"
kubectl apply -f "${K8S_DIR}/postgres-service.yaml"
kubectl apply -f "${K8S_DIR}/postgres-statefulset.yaml"
kubectl apply -f "${K8S_DIR}/redis-service.yaml"
kubectl apply -f "${K8S_DIR}/redis-statefulset.yaml"

echo "==> Wait postgres/redis rollout"
kubectl rollout status statefulset/postgres -n "${NAMESPACE}" --timeout=300s
kubectl rollout status statefulset/redis -n "${NAMESPACE}" --timeout=300s

echo "==> Apply API"
kubectl apply -f "${K8S_DIR}/api-deployment.yaml"
kubectl apply -f "${K8S_DIR}/api-service.yaml"

echo "==> Wait API rollout"
kubectl rollout status deployment/book-api -n "${NAMESPACE}" --timeout=300s

echo "==> Apply local ingress"
kubectl apply -f "${K8S_DIR}/ingress.yaml"

echo "==> Current state"
kubectl get pods,svc,ingress -n "${NAMESPACE}"
echo "==> Local UP done"
