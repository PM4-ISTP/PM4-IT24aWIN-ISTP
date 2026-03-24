/// <reference types="cypress" />

export {};

/** Timeout for the full OIDC redirect chain: Keycloak login → next-auth callback → /dashboard. */
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
      /** Log in as the configured regular user (USER_USERNAME / USER_PASSWORD env vars). */
      loginAsUser(): Chainable;
    }
  }
}

Cypress.Commands.add("loginViaKeycloak", (username: string, password: string) => {
  // Keycloak runs on a different origin — use cy.origin() to interact with its login form.
  // KEYCLOAK_ORIGIN is the *base origin* (scheme + host) of the Keycloak server, e.g.
  //   https://istp-staging-auth.pm4.init-lab.ch
  // Both the OIDC login redirect (/realms/.../protocol/openid-connect/auth) and
  // the Admin Console (/admin/...) share this same origin — it is NOT the admin URL.
  const keycloakOrigin = Cypress.env("KEYCLOAK_ORIGIN") as string;
  if (!keycloakOrigin) {
    throw new Error("KEYCLOAK_ORIGIN must be set in cypress.env.json (e.g. http://localhost:9090)");
  }

  // Cache the authenticated browser state for the lifetime of the current spec so
  // that each test that calls loginViaKeycloak restores cookies instead of performing
  // a full OIDC round-trip.  Running a complete OAuth flow before every test causes
  // Chrome to exhaust memory and crash (code 2147483651).
  // cacheAcrossSpecs defaults to false, so the cache is automatically discarded at
  // the end of each spec.  Specs that need a completely fresh session on re-runs
  // should call Cypress.session.clearAllSavedSessions() in a before() hook.
  cy.session(
    ["keycloak", username],
    () => {
      // Clear ALL browser cookies before logging in — including the Keycloak SSO
      // cookie at the Keycloak origin.  Cypress uses the Chrome DevTools Protocol
      // which clears cookies across all domains, so this prevents Keycloak from
      // silently re-authenticating via SSO and bypassing the credentials form.
      cy.clearAllCookies();

      // Use next-auth's built-in sign-in page instead of the app's home page.
      // The built-in page renders a native HTML form (no React hydration required),
      // so the "Sign in with Keycloak" button is immediately clickable even on
      // slower environments where client-side JS may not yet be loaded.
      cy.visit(`/api/auth/signin?callbackUrl=${encodeURIComponent("/dashboard")}`);
      cy.contains("Sign in with Keycloak").click();

      cy.origin(keycloakOrigin, { args: { username, password } }, ({ username, password }) => {
        cy.get("input[name='username']", { timeout: 10000 }).type(username);
        cy.get("input[name='password']").type(password);
        cy.get("input[type='submit'], button[type='submit']").click();
      });

      cy.url({ timeout: OIDC_REDIRECT_TIMEOUT }).should("include", "/dashboard");
    }
    // No validate() here. validate() was causing stale sessions to be silently
    // reused across "Run All" re-runs in the Cypress UI: while tokens were still
    // alive the check passed, setup was skipped, and tests started already logged in.
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
