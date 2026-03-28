import { defineConfig } from "cypress";

export default defineConfig({
  e2e: {
    baseUrl: "http://localhost:3000",
    video: true,
    // Allow Cypress to follow cross-origin redirects during the Keycloak OAuth flow
    chromeWebSecurity: false,
    env: {
      // Base origin (scheme + host) of the Keycloak server — used in cy.origin() to
      // interact with the Keycloak login form during the OIDC OAuth redirect.
      // Both the OIDC login redirect (/realms/.../protocol/openid-connect/auth) and
      // the Admin Console (/admin/...) share the same Keycloak server origin.
      // This value is the BASE ORIGIN, not the full admin console or login URL.
      // Local Docker Compose (infra/docker-compose.yaml):  http://localhost:9090
      // Staging:                                           https://istp-staging-auth.pm4.init-lab.ch
      // Override via cypress.env.json (gitignored) or CYPRESS_KEYCLOAK_ORIGIN env var.
      KEYCLOAK_ORIGIN: "http://localhost:9090",
      // Realm name used to build the expected Keycloak Account Console URL.
      KEYCLOAK_REALM: "interactive-security-training-platform",
      // Default test credentials — override in cypress.env.json (gitignored) for real environments
      ADMIN_USERNAME: "admin",
      ADMIN_PASSWORD: "admin",
      INSTRUCTOR_USERNAME: "instructor",
      INSTRUCTOR_PASSWORD: "instructor",
      USER_USERNAME: "user",
      USER_PASSWORD: "user",
    },
    setupNodeEvents(on, config) {
      // node event listeners
      return config;
    },
  },
});
