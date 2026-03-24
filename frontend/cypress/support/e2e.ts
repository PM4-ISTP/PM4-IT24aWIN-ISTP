// ***********************************************************
// This example support/e2e.ts is processed and
// loaded automatically before your test files.
//
// This is a great place to put global configuration and
// behavior that modifies Cypress.
//
// You can change the location of this file or turn off
// automatically serving support files with the
// 'supportFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/configuration
// ***********************************************************

// Import commands.js using ES2015 syntax:
import "./commands";

// React 19 can emit recoverable hydration mismatch errors when the server-rendered HTML
// differs slightly from what the client expects (e.g., Mantine color-scheme attributes,
// browser extensions, or server/client timing differences). React automatically re-renders
// the affected tree on the client and the app works correctly.
// Prevent Cypress from treating these as uncaught exceptions that abort the test.
Cypress.on("uncaught:exception", (err) => {
  if (
    err.message.includes("Hydration failed") ||
    err.message.includes("There was an error while hydrating")
  ) {
    return false;
  }
  return true;
});
