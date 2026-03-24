import { defineConfig } from "cypress";

export default defineConfig({
  e2e: {
    baseUrl: "http://localhost:3000",
    // Allow Cypress to follow cross-origin redirects during the Keycloak OAuth flow
    chromeWebSecurity: false,
    env: {
      // Origin of your Keycloak server, used in cy.origin() to interact with the Keycloak login form.
      // Local Docker Compose (infra/docker-compose.yaml):  http://localhost:9090
      // Staging:                                           https://istp-staging-auth.pm4.init-lab.ch
      // Override via cypress.env.json (gitignored) or CYPRESS_KEYCLOAK_ORIGIN env var.
      KEYCLOAK_ORIGIN: "http://localhost:9090",
      // Default test credentials — override in cypress.env.json (gitignored) for real environments
      ADMIN_USERNAME: "admin",
      ADMIN_PASSWORD: "admin",
      USER_USERNAME: "user",
      USER_PASSWORD: "user",
    },
    setupNodeEvents(on, config) {
      // node event listeners
      return config;
    },
  },
});
