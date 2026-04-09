import { test } from "@playwright/test";
import { expectApiSuccess } from "./helpers";

const username = process.env.CYPRESS_ADMIN_USERNAME ?? "admin";
const password = process.env.CYPRESS_ADMIN_PASSWORD ?? "admin";

test.describe.serial("Admin configuration", () => {
  test.beforeAll(async ({ browser }) => {
    const page = await browser.newPage();
    await page.goto("/");
    await page.getByRole("button", { name: "Sign in with Keycloak" }).click();
    await page.getByRole("textbox", { name: "Username or email" }).fill(username);
    await page.getByRole("textbox", { name: "Username or email" }).press("Tab");
    await page.getByRole("textbox", { name: "Password" }).fill(password);
    await page.getByRole("button", { name: "Sign In" }).click();
    await page.getByRole("link", { name: "space_dashboard Dashboard" }).click();
    const deleteButton = page.getByRole("button", {
      name: "Delete Kubernetes configuration",
    });
    if (await deleteButton.isVisible()) {
      await expectApiSuccess(page, () => deleteButton.click(), /\/api\/admin\//, "DELETE");
    }
    await page.close();
  });

  test.beforeEach(async ({ page }) => {
    await page.goto("/");
    await page.getByRole("button", { name: "Sign in with Keycloak" }).click();
    await page.getByRole("textbox", { name: "Username or email" }).fill(username);
    await page.getByRole("textbox", { name: "Username or email" }).press("Tab");
    await page.getByRole("textbox", { name: "Password" }).fill(password);
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
      page.getByRole("button", { name: "Create Kubernetes configuration" }).click(),
      /\/api\/admin\//
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
      page.getByRole("button", { name: "Update Kubernetes configuration" }).click(),
      /\/api\/admin\//
    );
  });

  test("update kubeconfig file", async ({ page }) => {
    await page.getByRole("button", { name: "Kubeconfig" }).click();
    await page.locator('input[type="file"]').setInputFiles("tests/files/Kubeconfig_Variant_2");
    await expectApiSuccess(page, () =>
      page.getByRole("button", { name: "Update Kubernetes configuration" }).click(),
      /\/api\/admin\//
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
