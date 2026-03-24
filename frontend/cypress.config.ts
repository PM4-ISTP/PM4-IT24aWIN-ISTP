import { defineConfig } from "cypress";

export default defineConfig({
  e2e: {
    baseUrl: "http://localhost:3000",
    // Allow Cypress to follow cross-origin redirects during the Keycloak OAuth flow
    chromeWebSecurity: false,
    env: {
      // Keycloak origin used in cy.origin() — override via cypress.env.json or CYPRESS_* env vars
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
