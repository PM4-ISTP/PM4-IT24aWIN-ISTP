import { test } from "@playwright/test";
import { expectApiSuccess } from "./helpers";

test.describe.serial("Admin configuration", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/");
    await page.getByRole("button", { name: "Sign in with Keycloak" }).click();
    await page.getByRole("textbox", { name: "Username or email" }).fill("admin");
    await page.getByRole("textbox", { name: "Username or email" }).press("Tab");
    await page.getByRole("textbox", { name: "Password" }).fill("admin");
    await page.getByRole("button", { name: "Sign In" }).click();
    await page.getByRole("link", { name: "space_dashboard Dashboard" }).click();
  });

  test("add configuration", async ({ page }) => {
    await page.getByRole("textbox", { name: "CPU limit" }).click();
    await page.getByRole("textbox", { name: "CPU limit" }).fill("1");
    await page.getByRole("textbox", { name: "Memory limit" }).click();
    await page.getByRole("textbox", { name: "Memory limit" }).fill("512");
    await page.getByRole("combobox", { name: "Memory unit" }).click();
    await page.getByRole("option", { name: "Mi" }).click();
    await page.getByRole("button", { name: "Kubeconfig" }).click();
    await page.locator('input[type="file"]').setInputFiles("tests/files/Kubeconfig_Variant_1");
    await expectApiSuccess(page, () =>
      page.getByRole("button", { name: "Create Kubernetes configuration" }).click()
    );
  });

  test("update resource values", async ({ page }) => {
    await page.getByRole("textbox", { name: "CPU limit" }).click();
    await page.getByRole("textbox", { name: "CPU limit" }).fill("2");
    await page.getByRole("textbox", { name: "Memory limit" }).click();
    await page.getByRole("textbox", { name: "Memory limit" }).fill("1");
    await page.getByRole("combobox", { name: "Memory unit" }).click();
    await page.getByRole("option", { name: "Gi" }).click();
    await expectApiSuccess(page, () =>
      page.getByRole("button", { name: "Update Kubernetes configuration" }).click()
    );
  });

  test("update kubeconfig file", async ({ page }) => {
    await page.getByRole("button", { name: "Kubeconfig" }).click();
    await page.locator('input[type="file"]').setInputFiles("tests/files/Kubeconfig_Variant_2");
    await expectApiSuccess(page, () =>
      page.getByRole("button", { name: "Update Kubernetes configuration" }).click()
    );
  });

  test("delete configuration", async ({ page }) => {
    await expectApiSuccess(
      page,
      () => page.getByRole("button", { name: "Delete Kubernetes configuration" }).click(),
      /\/api\/admin\//,
      "DELETE"
    );
  });
});
