# Challenge Docker Images

Challenge labs use container images from GitHub Container Registry (GHCR). Instructors should publish images in their own GitHub user or organization namespace, for example:

```text
ghcr.io/school-org/sql-injection-lab:1.0.0
```

The platform accepts GHCR images from any owner, not only packages from this repository. This keeps image ownership with the instructor or school that created the lab.

## Image Policy

- Images must be public and anonymously readable by default.
- Private GHCR images are supported when the Kubernetes namespace has a pull secret and the admin configuration contains that secret name.
- Tags are supported, but digest references are preferred for reproducible labs, for example `ghcr.io/school-org/sql-injection-lab@sha256:<digest>`.
- Examples may live under the project namespace, but production course images should be owned by the instructor or school running the course.

## Private GHCR Images

To use private GHCR images, create one pull secret in the namespace where challenge pods run:

```bash
kubectl create secret docker-registry ghcr-pull-secret \
  --docker-server=ghcr.io \
  --docker-username=<github-user-or-bot> \
  --docker-password=<personal-access-token> \
  --docker-email=<email>
```

Then enter `ghcr-pull-secret` as the image pull secret in the admin configuration.

The GitHub user or bot behind the token must have `read:packages` access to each private package. For new instructor-owned private repositories or organizations, the instructor must grant that pull account access before the lab can start.

## How It Works

- The secret is stored only in Kubernetes.
- The application stores the secret name, not the GHCR token.
- When a student starts a challenge, the backend adds `imagePullSecrets` to the generated Kubernetes deployment.
- Kubernetes uses that secret to authenticate against `ghcr.io` while pulling the challenge image.
- Public images work without a pull secret.
- Private images need both the configured secret and package access for the GitHub user or bot in that secret.

## Responsibilities

- Platform/admin team: create the Kubernetes pull secret in the challenge namespace and enter its name in the admin configuration.
- Instructor/school: publish the challenge image under their own GitHub user or organization and grant the pull account access if the package is private.
- For a new private instructor repository, no code change is needed, but the pull account must be allowed to read that package before the lab is assigned.

## Troubleshooting

- `ImagePullBackOff` or `ErrImagePull` usually means the secret is missing from the namespace, the secret name in the admin configuration is wrong, or the GitHub user or bot cannot read the package.
- A `401` or `403` during image validation without a configured pull secret usually means the image is private and cannot be accepted yet.
- Prefer digest-pinned images for stable course material. Tags like `latest` can move over time.
