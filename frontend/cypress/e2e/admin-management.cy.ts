/**
 * Cypress E2E tests for Admin User Management
 *
 * Covers:
 *  - Admin can access the Admin Dashboard
 *  - Admin Dashboard shows the "Manage Users" button linking to the Keycloak Admin Console
 *  - Admin can reach the Keycloak Admin Console to edit a user
 *  - Admin can reach the Keycloak Admin Console to delete a user
 *  - Regular (non-admin) user is blocked from accessing the Admin Dashboard
 */
describe("Admin User Management", () => {
  // ─── Admin scenarios ────────────────────────────────────────────────────────

  describe("as ADMIN user", () => {
    beforeEach(() => {
      cy.loginAsAdmin();
    });

    it("can access the Admin Dashboard page", () => {
      cy.visit("/dashboard/admin");
      cy.contains("h1", "Admin Dashboard").should("be.visible");
      cy.contains("Manage your platform settings and users.").should("be.visible");
    });

    it("Admin Dashboard shows the Manage Users section", () => {
      cy.visit("/dashboard/admin");
      cy.contains("Keycloak Admin Console").should("be.visible");
      cy.contains("Manage users and roles directly via the Keycloak Admin Console.").should(
        "be.visible"
      );
    });

    it("Admin Dashboard shows the Manage Users button", () => {
      cy.visit("/dashboard/admin");
      cy.contains("Manage Users & Roles with Keycloak").should("be.visible");
    });

    it("Manage Users button links to the Keycloak Admin Console URL", () => {
      cy.visit("/dashboard/admin");
      cy.contains("Manage Users & Roles with Keycloak")
        .closest("a")
        .should("have.attr", "href")
        .and("not.be.empty");
    });

    it("Manage Users button opens in a new tab (target=_blank)", () => {
      cy.visit("/dashboard/admin");
      cy.contains("Manage Users & Roles with Keycloak")
        .closest("a")
        .should("have.attr", "target", "_blank");
    });

    it("Admin navigation shows Admin Dashboard link", () => {
      cy.visit("/dashboard");
      cy.get('a[href="/dashboard/admin"]').should("exist");
    });

    /**
     * Log in as ADMIN → open Admin Panel → open Keycloak
     *
     * Verifies that the Admin Dashboard provides a working link to the Keycloak
     * Admin Console. From there, an admin can edit or delete users.
     * (Full edit/delete actions occur inside the Keycloak Admin Console UI.)
     */
    it("can navigate to the Keycloak Admin Console for user management (edit / delete)", () => {
      cy.visit("/dashboard/admin");
      cy.contains("Manage Users & Roles with Keycloak")
        .closest("a")
        .invoke("attr", "href")
        .then((href) => {
          // Confirm a valid URL is present so an admin can open it and manage users
          expect(href).to.be.a("string").and.not.be.empty;
          // Navigate to the Keycloak Admin Console URL (cross-origin, requires chromeWebSecurity: false)
          cy.visit(href as string);
          // Keycloak Admin Console should load — verify by presence of the page title or body
          cy.origin(Cypress.env("KEYCLOAK_ORIGIN") as string, () => {
            cy.document().its("title").should("not.be.empty");
          });
        });
    });
  });

  // ─── Regular user scenarios ──────────────────────────────────────────────────

  describe("as regular USER (non-admin)", () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    /**
     * Log in as regular USER → Admin Panel is not accessible
     *
     * The middleware (proxy.ts) redirects non-admin users to /unauthorized.
     */
    it("is redirected away from the Admin Dashboard", () => {
      cy.visit("/dashboard/admin");
      cy.url().should("not.include", "/dashboard/admin");
    });

    it("lands on the /unauthorized page when accessing Admin Dashboard", () => {
      cy.visit("/dashboard/admin");
      cy.url().should("include", "/unauthorized");
      cy.contains("Access Denied").should("be.visible");
    });

    it("does not see the Admin section in navigation", () => {
      cy.visit("/dashboard");
      cy.get('a[href="/dashboard/admin"]').should("not.exist");
    });
  });
});
