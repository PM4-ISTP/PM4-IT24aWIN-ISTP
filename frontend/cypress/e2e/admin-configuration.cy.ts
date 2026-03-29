import { getRequiredEnvValue } from "../support/commands";
import { MemoryUnit } from "./../../src/lib/memoryUnit";

describe("Admin configuration", () => {
  before(() => {
    Cypress.session.clearAllSavedSessions();
  });

  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit("/dashboard/admin");
  });

  it("Clean up admin configuration before actual testing", () => {
    cy.get(`#${adminConfigFormDeleteButtonId}`).click();
    cy.reload();
    validateNoAdminCofigCreated();
  });
});

// === helpers ===
// IDs used in AdminConfigForm
const cpuLimitInputId = "cpu-limit-input";
const cpuLimitInputLabelId = "cpu-limit-input-label";
const memoryLimitInputId = "memory-limit-input";
const memoryLimitInputLabelId = "memory-limit-input-label";
const memoryUnitInputId = "memory-unit-input";
const kubeconfigInputId = "kubeconfig-input";
const kubeconfigInputLabelId = "kubeconfig-input-label";
const adminConfigFormSubmitButtonId = "admin-config-form-submit-button";
const adminConfigFormDeleteButtonId = "admin-config-form-delete-button";

type FormContent = {
  kubeconfig: string,
  cpuLimit: string,
  memoryLimit: string,
  memoryUnit: MemoryUnit
}

const kubeconfigPath = getRequiredEnvValue("KUBECONFIG_PATH");

const validateNoAdminCofigCreated = () => {
  validateFormContent({
    kubeconfig: "",
    cpuLimit: "",
    memoryLimit: "",
    memoryUnit: MemoryUnit.Byte
  });
  cy.get(`#${adminConfigFormSubmitButtonId}`).should("have.text", "Create K3d configuration");
  validateInputHasNoStar(cpuLimitInputLabelId);
  validateInputHasNoStar(memoryLimitInputLabelId);
  validateInputHasStar(kubeconfigInputLabelId);
}

const validateFormContent = (expected: FormContent) => {
  cy.get(`#${cpuLimitInputId}`).should("have.value", expected.cpuLimit);
  cy.get(`#${memoryLimitInputId}`).should("have.value", expected.memoryLimit);
  cy.get(`#${memoryUnitInputId}`).should("have.value", expected.memoryUnit);
  cy.get(`#${kubeconfigInputId}`).should("have.text", expected.kubeconfig);
}

const validateInputHasStar = (labelId: string) => {
  cy.get(`#${labelId} > span`).should("have.text", " *");
}

const validateInputHasNoStar = (labelId: string) => {
  cy.get(`#${labelId} > span`).should("not.exist");
}