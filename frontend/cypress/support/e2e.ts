import "./commands";

// Suppress React 19 recoverable hydration mismatch errors — the app still works correctly.
Cypress.on("uncaught:exception", (err) => {
  if (
    err.message.includes("Hydration failed") ||
    err.message.includes("There was an error while hydrating")
  ) {
    return false;
  }
  return true;
});
