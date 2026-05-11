import { expect, type Page, test } from "@playwright/test";
import { expectApiSuccess } from "./helpers";

const username = process.env.CYPRESS_ADMIN_USERNAME ?? "admin";
const password = process.env.CYPRESS_ADMIN_PASSWORD ?? "admin";

async function signInAndOpenAdminConfig(page: Page) {
  await page.goto("/");
  await page.getByRole("button", { name: "Login" }).click();
  await page.getByRole("textbox", { name: "Username or email" }).fill(username);
  await page.getByRole("textbox", { name: "Username or email" }).press("Tab");
  await page.getByRole("textbox", { name: "Password" }).fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();

  await page.waitForURL(/\/dashboard(?:\/.*)?$/);
  await page.goto("/dashboard/admin");
  await expect(page.getByRole("heading", { name: "Admin Dashboard" })).toBeVisible();
}

async function expectCreateMode(page: Page) {
  await expect(page.getByRole("button", { name: "Create Kubernetes configuration" })).toBeVisible();
}

async function expectUpdateMode(page: Page) {
  await expect(page.getByRole("button", { name: "Update Kubernetes configuration" })).toBeVisible();
}

async function fetchAdminConfigState(page: Page) {
  return page.evaluate(async () => {
    const response = await fetch("/api/backend/api/admin/config", { cache: "no-store" });
    if (!response.ok) {
      throw new Error(`Could not fetch admin config: ${response.status}`);
    }

    return (await response.json()) as { kubeconfigUploaded?: boolean };
  });
}

async function deleteAdminConfigViaApi(page: Page) {
  await page.evaluate(async () => {
    const response = await fetch("/api/backend/api/admin/config", { method: "DELETE" });
    if (!response.ok) {
      throw new Error(`Could not delete admin config: ${response.status}`);
    }
  });

  await expect
    .poll(async () => (await fetchAdminConfigState(page)).kubeconfigUploaded ?? false)
    .toBe(false);
}

async function resetAdminConfig(page: Page) {
  await deleteAdminConfigViaApi(page);
  await page.goto("/dashboard/admin");
  await expectCreateMode(page);
}

async function uploadKubeconfig(page: Page, filePath: string) {
  await page.locator('#kubeconfig-input input[type="file"]').setInputFiles(filePath);
  await expect(page.getByText(filePath.split("/").at(-1) ?? filePath)).toBeVisible();
}

async function selectMemoryUnit(page: Page, unit: "Mi" | "Gi" | "Ti") {
  const memoryUnit = page.getByRole("combobox", { name: "Memory unit" });
  await memoryUnit.click();
  await page.keyboard.press("Home");

  if (unit === "Gi") {
    await page.keyboard.press("ArrowDown");
  } else if (unit === "Ti") {
    await page.keyboard.press("ArrowDown");
    await page.keyboard.press("ArrowDown");
  }

  await page.keyboard.press("Enter");
  await expect(memoryUnit).toHaveValue(unit);
}

test("can create, update, replace, and delete the admin configuration", async ({ page }) => {
  await signInAndOpenAdminConfig(page);
  await resetAdminConfig(page);

  try {
    await test.step("create configuration", async () => {
      await page.getByRole("textbox", { name: "CPU limit" }).fill("1");
      await page.getByRole("textbox", { name: "Memory limit" }).fill("512");
      await expect(page.getByRole("combobox", { name: "Memory unit" })).toHaveValue("Mi");
      await uploadKubeconfig(page, "tests/files/Kubeconfig_Variant_1");
      await expectApiSuccess(
        page,
        () => page.getByRole("button", { name: "Create Kubernetes configuration" }).click(),
        /\/api\/admin\//,
        "POST"
      );
      await expectUpdateMode(page);
    });

    await test.step("update resource values", async () => {
      await page.getByRole("textbox", { name: "CPU limit" }).fill("2");
      await page.getByRole("textbox", { name: "Memory limit" }).fill("1");
      await selectMemoryUnit(page, "Gi");
      await expectApiSuccess(
        page,
        () => page.getByRole("button", { name: "Update Kubernetes configuration" }).click(),
        /\/api\/admin\//,
        "PUT"
      );
      await expectUpdateMode(page);
    });

    await test.step("replace kubeconfig file", async () => {
      await uploadKubeconfig(page, "tests/files/Kubeconfig_Variant_2");
      await expectApiSuccess(
        page,
        () => page.getByRole("button", { name: "Update Kubernetes configuration" }).click(),
        /\/api\/admin\//,
        "PUT"
      );
      await expectUpdateMode(page);
    });
  } finally {
    await test.step("delete configuration", async () => {
      await resetAdminConfig(page);
    });
  }
});
