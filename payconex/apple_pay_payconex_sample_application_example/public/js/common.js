/**
 * Shared helpers for demo pages.
 * Uses paths relative to the current HTML page so the app works at the
 * domain root or in a subdirectory on cPanel.
 */

/**
 * Resolve an API path next to this page (e.g. "api/public-config.php").
 * @param {string} file
 */
function apiUrl(file) {
  // checkout.html / register.html live in the same directory as api/
  return new URL(`api/${file}`, window.location.href).toString();
}

/**
 * Fetch JSON with a timeout and clearer errors for PHP/HTML failure pages.
 * @param {string} url
 * @param {RequestInit} [options]
 * @param {number} [timeoutMs]
 */
async function fetchJson(url, options = {}, timeoutMs = 15000) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const res = await fetch(url, { ...options, signal: controller.signal });
    const text = await res.text();
    let data;
    try {
      data = text ? JSON.parse(text) : {};
    } catch {
      throw new Error(
        `Expected JSON from ${url}, got HTTP ${res.status}. `
        + `Is PHP running, and is config.php present one level above public/? `
        + `Body starts with: ${text.slice(0, 120)}`
      );
    }
    return { res, data };
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') {
      throw new Error(`Request timed out after ${timeoutMs}ms: ${url}`);
    }
    throw err;
  } finally {
    clearTimeout(timer);
  }
}
