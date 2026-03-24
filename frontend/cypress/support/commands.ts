/// <reference types="cypress" />

export {};

/** Timeout for the full OIDC redirect chain: Keycloak login → next-auth callback → /dashboard. */
const OIDC_REDIRECT_TIMEOUT = 15_000;

declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Log in via the Keycloak OAuth flow.
       * Clears all cookies first so every call goes through a full credential-based login.
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

  // Clear ALL browser cookies before every login — including both the next-auth session
  // cookie and the Keycloak SSO cookie (at the Keycloak origin).
  // Cypress uses the Chrome DevTools Protocol which can clear cookies for all domains,
  // so this removes the Keycloak single-sign-on cookie that would otherwise auto-login
  // the user without showing the credentials form.
  // Intentionally NOT using cy.session: its in-memory cache persists across "Run All"
  // button clicks in the Cypress UI (the process never restarts), and the validate()
  // check keeps passing while tokens are valid, silently skipping the login setup.
  // A direct login on every call guarantees a clean, isolated state per test.
  cy.clearAllCookies();

  // Use next-auth's built-in sign-in page instead of the app's home page.
  // The built-in page renders a native HTML form (no React hydration required),
  // so the "Sign in with Keycloak" submit button is always immediately clickable —
  // even on slower environments where client-side JS may not yet be loaded.
  cy.visit(`/api/auth/signin?callbackUrl=${encodeURIComponent("/dashboard")}`);
  cy.contains("Sign in with Keycloak").click();

  cy.origin(keycloakOrigin, { args: { username, password } }, ({ username, password }) => {
    cy.get("input[name='username']", { timeout: 10000 }).type(username);
    cy.get("input[name='password']").type(password);
    cy.get("input[type='submit'], button[type='submit']").click();
  });

  cy.url({ timeout: OIDC_REDIRECT_TIMEOUT }).should("include", "/dashboard");
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
