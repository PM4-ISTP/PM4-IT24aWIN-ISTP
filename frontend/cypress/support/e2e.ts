import "./commands";

// Suppress React 19 recoverable hydration mismatch errors — the app still works correctly.
// Development builds emit descriptive strings; production/minified builds emit numbered codes
// (#418 = hydration mismatch, #423 = root hydration error, #425 = text content mismatch).
Cypress.on("uncaught:exception", (err) => {
  if (
    err.message.includes("Hydration failed") ||
    err.message.includes("There was an error while hydrating") ||
    err.message.includes("Minified React error #418") ||
    err.message.includes("Minified React error #423") ||
    err.message.includes("Minified React error #425")
  ) {
    return false;
  }
  return true;
});
