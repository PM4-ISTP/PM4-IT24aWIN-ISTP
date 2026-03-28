type RoleCase = {
  label: string;
  login: () => Cypress.Chainable;
};

describe("Profile account link", () => {
  const keycloakOrigin = Cypress.env("KEYCLOAK_ORIGIN") as string;
  const keycloakRealm = Cypress.env("KEYCLOAK_REALM") as string;
  const expectedAccountPath = `/realms/${keycloakRealm}/account`;
  const expectedAccountUrl = `${keycloakOrigin}${expectedAccountPath}`;

  const roleCases: RoleCase[] = [
    { label: "admin", login: () => cy.loginAsAdmin() },
    { label: "instructor", login: () => cy.loginAsInstructor() },
    { label: "student", login: () => cy.loginAsUser() },
  ];

  before(() => {
    void Cypress.session.clearAllSavedSessions();
  });

  roleCases.forEach(({ label, login }) => {
    describe(`as ${label}`, () => {
      beforeEach(() => {
        login();
        cy.visit("/dashboard");
      });

      it("shows an Edit profile link pointing to the Keycloak account console", () => {
        cy.get('[data-testid="user-menu-trigger"]').click();
        cy.get('[data-testid="edit-profile-link"]')
          .should("be.visible")
          .and("have.attr", "href", expectedAccountUrl)
          .and("have.attr", "target", "_blank");
      });

      it("opens the Keycloak account console when clicked", () => {
        cy.get('[data-testid="user-menu-trigger"]').click();
        cy.get('[data-testid="edit-profile-link"]').invoke("removeAttr", "target").click();

        cy.origin(
          keycloakOrigin,
          { args: { expectedAccountPath } },
          ({ expectedAccountPath }: { expectedAccountPath: string }) => {
            cy.location("pathname", { timeout: 15000 }).should((pathname) => {
              expect([expectedAccountPath, `${expectedAccountPath}/`]).to.include(pathname);
            });
            cy.contains("Profile picture").should("be.visible");
          }
        );
      });
    });
  });
});
