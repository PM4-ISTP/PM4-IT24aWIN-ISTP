describe("Admin configuration", () => {
  before(() => {
    void Cypress.session.clearAllSavedSessions();
  });

  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit("/dashboard/admin");
  });

  it("Kubeconfig is required, when admin configuration was not created", () => {
    cy.contains("Create Kubernetes configuration").should("be.visible");
    cy.get("#admin-config-form-submit-button").click();
    cy.contains("You need to upload a Kubeconfig file.").should("be.visible");
  });

  it("Create admin configuration", () => {
    const form: FormContent = {
      kubeconfig: "Kubeconfig_Variant_1",
      cpuLimit: "2",
      memoryLimit: "512",
      memoryUnit: "Mi",
    };

    cy.get("#cpu-limit-input").type(form.cpuLimit);
    cy.get("#memory-limit-input").type(form.memoryLimit);
    cy.get("#memory-unit-input").click();
    cy.contains("[role='option']", form.memoryUnit).click();
    cy.get('input[type="file"]').selectFile(`cypress/fixtures/${form.kubeconfig}`, {
      force: true,
    });

    cy.get("#admin-config-form-submit-button").click();
    cy.wait(waitTimeInMiliseconds);

    cy.contains("Admin configuration has been successfully updated.").should("be.visible");
    cy.contains("Update Kubernetes configuration").should("be.visible");
  });

  it("Update admin configuration", () => {
    const form: FormContent = {
      kubeconfig: "Kubeconfig_Variant_2",
      cpuLimit: "4",
      memoryLimit: "1",
      memoryUnit: "Gi",
    };

    cy.contains("Update Kubernetes configuration").should("be.visible");

    cy.get("#cpu-limit-input").clear().type(form.cpuLimit);
    cy.get("#memory-limit-input").clear().type(form.memoryLimit);
    cy.get("#memory-unit-input").click();
    cy.contains("[role='option']", form.memoryUnit).click();
    cy.get('input[type="file"]').selectFile(`cypress/fixtures/${form.kubeconfig}`, {
      force: true,
    });

    cy.get("#admin-config-form-submit-button").click();
    cy.wait(waitTimeInMiliseconds);

    cy.contains("Admin configuration has been successfully updated.").should("be.visible");
  });

  it("Delete admin configuration", () => {
    cy.contains("Update Kubernetes configuration").should("be.visible");

    cy.get("#admin-config-form-delete-button").click();
    cy.wait(waitTimeInMiliseconds);

    cy.contains("Admin configuration has been successfully deleted.").should("be.visible");
    cy.contains("Create Kubernetes configuration").should("be.visible");
  });

  const waitTimeInMiliseconds = 100;

  // Can't import the source's `const enum MemoryUnit` — const enums are inlined by tsc
  // and aren't usable across isolatedModules boundaries (Cypress transpiles each file in isolation).
  type MemoryUnit = "B" | "Mi" | "Gi" | "Ti";

  type FormContent = {
    kubeconfig: string;
    cpuLimit: string;
    memoryLimit: string;
    memoryUnit: MemoryUnit;
  };
});
