// Run before the app's styles load so a saved dark theme never flashes white.
(() => {
  let theme = "system";
  try { theme = localStorage.getItem("crackcs-theme") || "system"; } catch { /* Use the system preference. */ }
  document.documentElement.dataset.theme = theme === "light" || theme === "dark"
    ? theme : (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
})();
