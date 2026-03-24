/// <reference types="cypress" />

export {};

/** Timeout for the full OIDC redirect chain: Keycloak login → next-auth callback → /dashboard. */
const OIDC_REDIRECT_TIMEOUT = 15_000;

declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Log in via the Keycloak OAuth flow and cache the session.
       * Requires chromeWebSecurity: false in cypress.config.ts.
       */
      loginViaKeycloak(username: string, password: string): Chainable;
      /** Log in as the configured admin user (ADMIN_USERNAME / ADMIN_PASSWORD env vars). */
      loginAsAdmin(): Chainable;
      /** Log in as the configured regular user (USER_USERNAME / USER_PASSWORD env vars). */
      loginAsUser(): Chainable;
    }
  }
}

Cypress.Commands.add("loginViaKeycloak", (username: string, password: string) => {
  cy.session(
    ["keycloak", username],
    () => {
      cy.visit("/");
      cy.contains("Sign in with Keycloak").click();

      // Keycloak runs on a different origin — use cy.origin() to interact with its login form.
      // KEYCLOAK_ORIGIN is the *base origin* (scheme + host) of the Keycloak server, e.g.
      //   https://istp-staging-auth.pm4.init-lab.ch
      // Both the OIDC login redirect (/realms/.../protocol/openid-connect/auth) and
      // the Admin Console (/admin/...) share this same origin — it is NOT the admin URL.
      const keycloakOrigin = Cypress.env("KEYCLOAK_ORIGIN") as string;
      if (!keycloakOrigin) {
        throw new Error(
          "KEYCLOAK_ORIGIN must be set in cypress.env.json (e.g. http://localhost:9090)"
        );
      }
      cy.origin(keycloakOrigin, { args: { username, password } }, ({ username, password }) => {
        cy.get("input[name='username']", { timeout: 10000 }).type(username);
        cy.get("input[name='password']").type(password);
        cy.get("input[type='submit']").click();
      });

      cy.url({ timeout: OIDC_REDIRECT_TIMEOUT }).should("include", "/dashboard");
    },
    {
      cacheAcrossSpecs: true,
      validate() {
        // Re-establish the session if the next-auth session has expired or been revoked.
        cy.request({ url: "/api/auth/session", failOnStatusCode: false })
          .its("body.user")
          .should("not.be.undefined");
      },
    }
  );
});

Cypress.Commands.add("loginAsAdmin", () => {
  cy.loginViaKeycloak(
    Cypress.env("ADMIN_USERNAME") as string,
    Cypress.env("ADMIN_PASSWORD") as string
  );
});

Cypress.Commands.add("loginAsUser", () => {
  cy.loginViaKeycloak(
    Cypress.env("USER_USERNAME") as string,
    Cypress.env("USER_PASSWORD") as string
  );
});
