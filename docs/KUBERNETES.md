# Kubernetes Setup

This project can run challenge infrastructure on Kubernetes. For local Kubernetes development, K3d is a lightweight option.

## Create a K3d Cluster

```bash
k3d cluster create istp --port "80:80@loadbalancer" --port "443:443@loadbalancer"
```

Verify the cluster is running:

```bash
k3d cluster list
kubectl cluster-info
```

## Adminer

Adminer is exposed on staging and production via a dedicated subdomain listed in the root README.

For local clusters, or if Adminer is not exposed through Ingress, access it with port forwarding. Replace the namespace with the one you deployed into:

```bash
kubectl -n <namespace> port-forward svc/adminer 8888:8080
```

Then open `http://localhost:8888` and connect to PostgreSQL using host `postgres` and your namespace credentials.

## kubectl on Windows 11

If kubectl points to `host.docker.internal`, edit `.kube/config` in your local user folder and replace:

```text
server: https://host.docker.internal:<port>
```

with:

```text
server: https://127.0.0.1:<port>
```

The port can differ between cluster starts. Use the port shown in your kubeconfig.

## Keycloak Admin API Secret

Create a secret named `keycloak-admin-api-client` with key `client-secret` in your namespace.

Example:

```bash
kubectl create secret generic keycloak-admin-api-client \
  --from-literal=client-secret=<keycloak-client-secret> \
  -n <namespace>
```

## Backend Kubeconfig

The backend needs a kubeconfig file to communicate with the Kubernetes cluster.

### Linux

Run this from the project root:

```bash
mkdir -p backend/src/main/resources
k3d kubeconfig get istp | sed 's/0\.0\.0\.0/127.0.0.1/g' > backend/src/main/resources/Kubeconfig
```

The `sed` command replaces `0.0.0.0` with `127.0.0.1`. k3d can generate kubeconfigs with `0.0.0.0` as the host, which is valid as a bind address but not as a client destination.

### Windows 11

Run this from the project root in PowerShell:

```powershell
New-Item -ItemType Directory -Path backend/src/main/resources -Force
k3d kubeconfig get istp > backend/src/main/resources/Kubeconfig
```

Then edit `backend/src/main/resources/Kubeconfig` and replace:

```text
server: https://host.docker.internal:<port>
```

with:

```text
server: https://127.0.0.1:<port>
```

Use the port from your generated kubeconfig.

## Notes

- `backend/src/main/resources/Kubeconfig` contains sensitive cluster credentials and is gitignored.
- Re-run the kubeconfig export after every cluster restart because k3d assigns a new random port.
- Verify the file exists with:

```bash
ls -la backend/src/main/resources/Kubeconfig
```
