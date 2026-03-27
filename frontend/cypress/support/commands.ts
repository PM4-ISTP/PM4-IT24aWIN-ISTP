/// <reference types="cypress" />

export {};

const OIDC_REDIRECT_TIMEOUT = 15_000;

declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Log in via the Keycloak OAuth flow and cache the session for the current spec.
       * Every test in the same spec that calls this command will restore cached cookies
       * instead of performing a full OIDC round-trip.
       * Requires chromeWebSecurity: false in cypress.config.ts.
       */
      loginViaKeycloak(username: string, password: string): Chainable;
      /** Log in as the configured admin user (ADMIN_USERNAME / ADMIN_PASSWORD env vars). */
      loginAsAdmin(): Chainable;
      /** Log in as the configured instructor user (INSTRUCTOR_USERNAME / INSTRUCTOR_PASSWORD env vars). */
      loginAsInstructor(): Chainable;
      /** Log in as the configured regular user (USER_USERNAME / USER_PASSWORD env vars). */
      loginAsUser(): Chainable;
    }
  }
}

const PLACEHOLDER_VALUE = /^<.*>$/;

function getRequiredEnvValue(name: string): string {
  const value = Cypress.env(name) as string | undefined;

  if (!value || PLACEHOLDER_VALUE.test(value)) {
    throw new Error(`${name} must be set in cypress.env.json before running this test`);
  }

  return value;
}

Cypress.Commands.add("loginViaKeycloak", (username: string, password: string) => {
  const keycloakOrigin = getRequiredEnvValue("KEYCLOAK_ORIGIN");
  if (!keycloakOrigin) {
    throw new Error("KEYCLOAK_ORIGIN must be set in cypress.env.json (e.g. http://localhost:9090)");
  }

  cy.session(
    ["keycloak", username],
    () => {
      cy.clearAllCookies();

      cy.visit(`/api/auth/signin?callbackUrl=${encodeURIComponent("/dashboard")}`);
      cy.contains("Sign in with Keycloak").click();

      cy.origin(keycloakOrigin, { args: { username, password } }, ({ username, password }) => {
        cy.get("input[name='username']", { timeout: 10000 }).type(username);
        cy.get("input[name='password']").type(password);
        cy.get("input[type='submit'], button[type='submit']").click();
      });

      cy.url({ timeout: OIDC_REDIRECT_TIMEOUT }).should("include", "/dashboard");
    }
    // No validate() — it causes stale sessions to be silently reused across re-runs.
  );
});

Cypress.Commands.add("loginAsAdmin", () => {
  cy.loginViaKeycloak(getRequiredEnvValue("ADMIN_USERNAME"), getRequiredEnvValue("ADMIN_PASSWORD"));
});

Cypress.Commands.add("loginAsInstructor", () => {
  cy.loginViaKeycloak(
    getRequiredEnvValue("INSTRUCTOR_USERNAME"),
    getRequiredEnvValue("INSTRUCTOR_PASSWORD")
  );
});

Cypress.Commands.add("loginAsUser", () => {
  cy.loginViaKeycloak(getRequiredEnvValue("USER_USERNAME"), getRequiredEnvValue("USER_PASSWORD"));
});
