# Diploma Project – Kubernetes Stateless vs Stateful Demo

A minimal, runnable demonstration of Kubernetes managing **stateless** (Spring Boot REST API) and **stateful** (PostgreSQL, Redis) components.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      Kubernetes Cluster                     │
│                                                             │
│  ┌───────────────────┐   ┌──────────────┐  ┌───────────┐  │
│  │  book-api          │   │  PostgreSQL   │  │   Redis   │  │
│  │  Deployment (2-5) │──▶│  StatefulSet  │  │ StatefulSet│  │
│  │  (stateless)      │   │  (stateful)   │  │ (stateful) │  │
│  └───────────────────┘   └──────────────┘  └───────────┘  │
│          │                      │                  │        │
│      HPA (cpu)              PVC (1 Gi)        PVC (256 Mi) │
└─────────────────────────────────────────────────────────────┘
```

| Component   | Kind        | Why stateless / stateful               |
|-------------|-------------|----------------------------------------|
| Book API    | Deployment  | No local state – scales horizontally   |
| PostgreSQL  | StatefulSet | Persistent storage, stable network ID  |
| Redis       | StatefulSet | Persistent cache, stable network ID    |

---

## Repository Structure

```
.
├── book-api/                  # Spring Boot application
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── docker/
│   └── docker-compose.yml     # Local development stack
├── k8s/                       # Kubernetes manifests
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── postgres-statefulset.yaml
│   ├── postgres-service.yaml
│   ├── redis-statefulset.yaml
│   ├── redis-service.yaml
│   ├── api-deployment.yaml
│   ├── api-service.yaml
│   ├── api-hpa.yaml
│   └── ingress.yaml
├── .github/workflows/ci.yml   # GitHub Actions CI/CD
└── README.md
```

---

## Prerequisites

| Tool            | Minimum version | Purpose                    |
|-----------------|-----------------|----------------------------|
| Java            | 17              | Build the Spring Boot app  |
| Maven           | 3.9             | Build tool                 |
| Docker          | 24              | Build & run containers     |
| Docker Compose  | v2              | Local stack                |
| kubectl         | 1.28            | Kubernetes CLI             |
| Docker Desktop  | 4.x             | Local Kubernetes cluster   |

---

## Option 1 – Run Locally with Docker Compose

```bash
# From the repo root
cd docker
docker compose up --build
```

The API is available at **http://localhost:8080**.

To stop and clean up:
```bash
docker compose down -v
```

---

## Option 2 – Deploy to Docker Desktop Kubernetes

### 1. Enable Kubernetes in Docker Desktop

Open **Docker Desktop → Settings → Kubernetes → Enable Kubernetes** and click *Apply & Restart*.

### 2. Verify your context

```bash
kubectl config use-context docker-desktop
kubectl cluster-info
```

### 3. Build and push the Docker image

The CI workflow pushes to GHCR automatically on every push to `main`.  
For a manual local build:

```bash
docker build -t ghcr.io/<your-github-username>/book-api:latest ./book-api
docker push ghcr.io/<your-github-username>/book-api:latest
```

> **Tip**: Update `k8s/api-deployment.yaml` line `image:` if you use a different registry or tag.

### 4. Apply the Kubernetes manifests (in order)

```bash
# Create namespace first
kubectl apply -f k8s/namespace.yaml

# Config & secrets
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# Stateful components
kubectl apply -f k8s/postgres-statefulset.yaml
kubectl apply -f k8s/postgres-service.yaml
kubectl apply -f k8s/redis-statefulset.yaml
kubectl apply -f k8s/redis-service.yaml

# Stateless API
kubectl apply -f k8s/api-deployment.yaml
kubectl apply -f k8s/api-service.yaml
kubectl apply -f k8s/api-hpa.yaml

# (Optional) Ingress
kubectl apply -f k8s/ingress.yaml
```

Or apply everything at once:
```bash
kubectl apply -f k8s/
```

### 5. Check pod status

```bash
kubectl get all -n diploma-demo
```

Wait until all pods are `Running` and `READY`.

### 6. Access the API

**Port-forward** (simplest, no ingress required):
```bash
kubectl port-forward svc/book-api 8080:80 -n diploma-demo
```
Then open **http://localhost:8080/api/books**.

**With Ingress** (requires NGINX ingress controller):
```bash
# Install NGINX ingress controller (once)
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.0/deploy/static/provider/cloud/deploy.yaml

# Add to /etc/hosts
echo "127.0.0.1 book-api.local" | sudo tee -a /etc/hosts
```
Then open **http://book-api.local/api/books**.

---

## Verify the API Endpoints

```bash
BASE=http://localhost:8080

# List all books (empty initially)
curl $BASE/api/books

# Create a book
curl -s -X POST $BASE/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"Clean Code","author":"Robert C. Martin","isbn":"9780132350884","description":"Agile software craftsmanship"}' | jq

# Get by ID (replace 1 with the returned id)
curl $BASE/api/books/1

# Update
curl -s -X PUT $BASE/api/books/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Clean Code (Updated)","author":"Robert C. Martin","isbn":"9780132350884","description":"Updated description"}' | jq

# Delete
curl -X DELETE $BASE/api/books/1

# Health / liveness / readiness
curl $BASE/actuator/health
curl $BASE/actuator/health/liveness
curl $BASE/actuator/health/readiness
```

---

## CI / CD (GitHub Actions)

The workflow file is at `.github/workflows/ci.yml`.

| Trigger              | Jobs                                           |
|----------------------|------------------------------------------------|
| Push / PR to `main`  | **build-and-test** – Maven build + unit tests  |
| Push to `main` only  | **docker-build-push** – build & push to GHCR  |

The image is published to:
```
ghcr.io/<owner>/book-api:latest
ghcr.io/<owner>/book-api:sha-<commit-sha>
```

No extra secrets are needed – the workflow uses the built-in `GITHUB_TOKEN`.

---

## Kubernetes Concepts Demonstrated

| Concept               | Resource                        |
|-----------------------|---------------------------------|
| Stateless workload    | `Deployment` (book-api)         |
| Stateful workload     | `StatefulSet` (postgres, redis) |
| Persistent storage    | `PersistentVolumeClaim` (via `volumeClaimTemplates`) |
| Horizontal scaling    | `HorizontalPodAutoscaler` (2–5 replicas, CPU-based) |
| Configuration         | `ConfigMap` + `Secret`          |
| Service discovery     | `Service` (ClusterIP, Headless) |
| External traffic      | `Ingress` (NGINX)               |
| Health checking       | Liveness & Readiness probes     |
| Cache                 | Redis (Spring `@Cacheable`)     |

---

## Optional: Deploy to Google Kubernetes Engine (GKE)

1. Create a GKE Autopilot cluster:
   ```bash
   gcloud container clusters create-auto diploma-cluster \
     --region=europe-west1
   gcloud container clusters get-credentials diploma-cluster \
     --region=europe-west1
   ```

2. Apply the same manifests:
   ```bash
   kubectl apply -f k8s/
   ```

3. Change the `api-service.yaml` type to `LoadBalancer` (or keep Ingress) to expose externally.

4. For the Ingress, install the GKE managed NGINX ingress or use the built-in GCE Ingress controller.

> **Cost tip**: Delete the cluster when done – `gcloud container clusters delete diploma-cluster --region=europe-west1`.
