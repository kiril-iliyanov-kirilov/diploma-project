#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="diploma-demo"

echo "==> Local DOWN (safe pause) started"
kubectl config current-context

kubectl scale deployment book-api -n "${NAMESPACE}" --replicas=0 || true
kubectl scale statefulset postgres -n "${NAMESPACE}" --replicas=0 || true
kubectl scale statefulset redis -n "${NAMESPACE}" --replicas=0 || true

echo "==> Optional: remove ingress if exists"
kubectl delete ingress book-api-ingress -n "${NAMESPACE}" --ignore-not-found=true

echo "==> Current state"
kubectl get pods,svc,ingress,pvc -n "${NAMESPACE}" || true
echo "==> Local DOWN done (data preserved in PVC)"
