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

  it("Kubeconfig is required, when admin configuration was not created", () => {
    cy.wait(waitTimeInMiliseconds);
    validateKubeconfigIsRequired();
  })

  it("Create and update admin configuration", () => {
    createAdminConfig(
      {
        kubeconfig: kubeconfigVariantOne,
        cpuLimit: "5",
        memoryLimit: "20",
        memoryUnit: MemoryUnit.GibiByte
      },
      kubeconfigVariantOneContent
    );
    updateAdminConfig(
      {
        kubeconfig: kubeconfigVariantOne,
        cpuLimit: "5",
        memoryLimit: "20",
        memoryUnit: MemoryUnit.GibiByte
      },
      {
        kubeconfig: kubeconfigVariantTwo,
        cpuLimit: "10",
        memoryLimit: "50",
        memoryUnit: MemoryUnit.MebiByte
      },
      kubeconfigVariantOneContent,
      kubeconfigVariantTwoContent
    );
    cy.reload();
    validateAdminConfigAfterCreationOrUpdate(
      {
        kubeconfig: kubeconfigVariantTwo,
        cpuLimit: "10",
        memoryLimit: "50",
        memoryUnit: MemoryUnit.MebiByte
      },
      kubeconfigVariantTwoContent
    )
  });
});

// === helpers ===
// IDs used in AdminConfigForm
const adminConfigFormId = "admin-config-form";
const cpuLimitInputId = "cpu-limit-input";
const cpuLimitInputLabelId = "cpu-limit-input-label";
const memoryLimitInputId = "memory-limit-input";
const memoryLimitInputLabelId = "memory-limit-input-label";
const memoryUnitInputId = "memory-unit-input";
const kubeconfigInputId = "kubeconfig-input";
const kubeconfigInputLabelId = "kubeconfig-input-label";
const kubeconfigInputErrorId = "kubeconfig-input-error";
const adminConfigFormSubmitButtonId = "admin-config-form-submit-button";
const adminConfigFormDeleteButtonId = "admin-config-form-delete-button";
const backButtonId = "admin-config-form-back-button"

const kubeconfigVariantOne = "Kubeconfig_Variant_1";
const kubeconfigVariantOneContent = "Kubeconfig variant 1";
const kubeconfigVariantTwo = "Kubeconfig_Variant_2";
const kubeconfigVariantTwoContent = "Kubeconfig variant 2";

const waitTimeInMiliseconds = 100;

type FormContent = {
  kubeconfig: string,
  cpuLimit: string,
  memoryLimit: string,
  memoryUnit: MemoryUnit
}

const kubeconfigPath = getRequiredEnvValue("KUBECONFIG_PATH");

const getPathToFixtureFile = (filename: string) => {
  return `cypress/fixtures/${filename}`;
}

const createAdminConfig = (formContent: FormContent, kubeconfigContent: string) => {
  validateNoAdminCofigCreated();
  createOrUpdateAdminConfig(formContent, kubeconfigContent);
}

const updateAdminConfig = (oldFormContent: FormContent, newFormContent: FormContent, oldKubeconfigContent: string, newKubeconfigContent: string) => {
  validateAdminConfigAfterCreationOrUpdate(oldFormContent, oldKubeconfigContent);
  createOrUpdateAdminConfig(newFormContent, newKubeconfigContent);
}

const createOrUpdateAdminConfig = (formContent: FormContent, kubeconfigContent: string) => {
  cy.wait(waitTimeInMiliseconds);
  cy.get(`#${adminConfigFormId}`).should("exist").should("not.have.attr", "disabled");
  cy.get(`#${cpuLimitInputId}`).should("not.have.attr", "disabled");
  cy.get(`#${cpuLimitInputId}`).clear();
  cy.get(`#${cpuLimitInputId}`).should("not.have.attr", "disabled");
  cy.get(`#${cpuLimitInputId}`).type(formContent.cpuLimit);
  cy.get(`#${memoryLimitInputId}`).clear();
  cy.get(`#${memoryLimitInputId}`).type(formContent.memoryLimit);
  cy.get(`#${memoryUnitInputId}`).click();
  cy.contains('div[role="option"]', formContent.memoryUnit).click();
  cy.get('input[type="file"]').selectFile(getPathToFixtureFile(formContent.kubeconfig), { force: true });
  cy.get(`#${adminConfigFormSubmitButtonId}`).click();
  cy.get(`#${adminConfigFormId}`).should("not.exist");
  cy.get(`#${backButtonId}`).click();
  validateAdminConfigAfterCreationOrUpdate(formContent, kubeconfigContent);
}

const validateNoAdminCofigCreated = () => {
  cy.wait(waitTimeInMiliseconds);
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

const validateAdminConfigAfterCreationOrUpdate = (formContent: FormContent, kubeconfigContent: string) => {
  cy.wait(waitTimeInMiliseconds);
  validateFormContent({
    kubeconfig: "",
    cpuLimit: formContent.cpuLimit,
    memoryLimit: formContent.memoryLimit,
    memoryUnit: formContent.memoryUnit
  });
  validateInputHasStar(cpuLimitInputLabelId);
  validateInputHasStar(memoryLimitInputLabelId);
  validateInputHasNoStar(kubeconfigInputLabelId);
  cy.get(`#${adminConfigFormSubmitButtonId}`).should("have.text", "Update K3d configuration");
  cy.readFile(kubeconfigPath).should("eq", kubeconfigContent);
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

const validateKubeconfigIsRequired = () => {
  cy.get(`#${adminConfigFormSubmitButtonId}`).click();
  cy.get(`#${kubeconfigInputErrorId}`).should("have.text", "You need to upload a Kubeconfig file.");
}