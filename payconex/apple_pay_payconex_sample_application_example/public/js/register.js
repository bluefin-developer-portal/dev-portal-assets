/**
 * Merchant registration page logic.
 *
 * - Loads current Apple Pay enrollment (registered domains)
 * - Collects domains + merchant metadata, POSTs to the PHP proxy
 * - Renders signed-request debug panel + PayConex response
 *
 * Signing (HMAC) is performed server-side in PayConexHmac.php — this
 * script never sees api_key_secret.
 */

const statusEl = document.getElementById('status');
const domainListEl = document.getElementById('domain-list');
const domainInput = document.getElementById('domain-input');
const merchantUrlInput = document.getElementById('merchant-url');
const partnerNameInput = document.getElementById('partner-name');
const outputEl = document.getElementById('output');
const addDomainBtn = document.getElementById('add-domain');
const submitBtn = document.getElementById('submit-reg');
const refreshEnrollmentBtn = document.getElementById('refresh-enrollment');
const enrollmentDomainsEl = document.getElementById('enrollment-domains');
const enrollmentMetaEl = document.getElementById('enrollment-meta');

/** @type {string[]} */
let domains = [];

/**
 * @param {'ok'|'err'|'info'} kind
 * @param {string} message
 */
function setStatus(kind, message) {
  statusEl.className = `status-banner visible ${kind}`;
  statusEl.textContent = message;
}

/**
 * @param {HTMLElement} container
 * @param {string[]} list
 * @param {{ removable?: boolean, emptyText?: string, onRemove?: (domain: string) => void }} options
 */
function renderDomainChips(container, list, options = {}) {
  const removable = !!options.removable;
  const emptyText = options.emptyText || 'No domains yet.';
  container.innerHTML = '';

  if (!list.length) {
    container.innerHTML = `<span class="text-secondary">${emptyText}</span>`;
    return;
  }

  for (const domain of list) {
    const chip = document.createElement('span');
    chip.className = 'domain-chip';
    chip.textContent = domain;

    if (removable && typeof options.onRemove === 'function') {
      const remove = document.createElement('button');
      remove.type = 'button';
      remove.setAttribute('aria-label', `Remove ${domain}`);
      remove.textContent = '×';
      remove.addEventListener('click', () => options.onRemove(domain));
      chip.appendChild(remove);
    }

    container.appendChild(chip);
  }
}

function renderDomains() {
  renderDomainChips(domainListEl, domains, {
    removable: true,
    onRemove: (domain) => {
      domains = domains.filter((d) => d !== domain);
      renderDomains();
    },
  });
}

/**
 * @param {string} value
 */
function normalizeDomain(value) {
  let d = value.trim().toLowerCase();
  d = d.replace(/^https?:\/\//, '');
  d = d.replace(/\/.*$/, '');
  // Strip :port if someone pastes applepay.local:8443
  d = d.replace(/:\d+$/, '');
  return d;
}

function addDomain() {
  const domain = normalizeDomain(domainInput.value);
  if (!domain) {
    setStatus('err', 'Enter a domain before adding.');
    return;
  }
  if (!/^[a-z0-9.-]+$/i.test(domain)) {
    setStatus('err', `Invalid domain: ${domain}`);
    return;
  }
  if (!domains.includes(domain)) {
    domains.push(domain);
  }
  domainInput.value = '';
  renderDomains();
  setStatus('info', `Domain list updated (${domains.length}).`);
}

async function loadEnrollment() {
  enrollmentMetaEl.textContent = 'Loading enrollment…';
  renderDomainChips(enrollmentDomainsEl, [], { emptyText: 'Loading…' });

  const { res, data } = await fetchJson(apiUrl('enrollment.php'));
  if (!res.ok || !data.ok) {
    const detail = data.error
      || (data.result && data.result.curl_error)
      || `HTTP ${(data.result && data.result.http_status) || res.status}`;
    enrollmentMetaEl.textContent = `Could not load enrollment: ${detail}`;
    renderDomainChips(enrollmentDomainsEl, [], { emptyText: 'Unavailable' });
    throw new Error(detail);
  }

  const enrollment = data.enrollment || {};
  const enrolled = Array.isArray(enrollment.domains) ? enrollment.domains : [];
  const accountLabel = enrollment.payconexAccountId || '(unknown account)';
  const displayName = enrollment.displayName ? ` · ${enrollment.displayName}` : '';

  enrollmentMetaEl.textContent =
    `Account ${accountLabel}${displayName} · ${enrolled.length} registered domain`
    + (enrolled.length === 1 ? '' : 's');

  renderDomainChips(enrollmentDomainsEl, enrolled, {
    emptyText: 'No domains registered yet for this account.',
  });

  return enrolled;
}

async function loadPublicConfig() {
  const { res, data } = await fetchJson(apiUrl('public-config.php'));
  if (!res.ok || !data.ok) {
    throw new Error(data.error || 'Failed to load public config.');
  }

  const cfg = data.config;
  const pageHost = window.location.hostname;

  // Prefer current host so local (applepay.local) and deploy hosts just work.
  // Config overrides win when non-empty. Value is hostname only (no scheme/port).
  const configuredMerchant = cfg.merchant_url && String(cfg.merchant_url).trim();
  merchantUrlInput.value = configuredMerchant
    ? normalizeDomain(configuredMerchant)
    : pageHost;
  partnerNameInput.value = (cfg.partner_name && String(cfg.partner_name).trim())
    || 'Apple Pay Demo';

  const defaultDomain = (cfg.default_domain && String(cfg.default_domain).trim())
    || pageHost;
  domains = defaultDomain ? [defaultDomain] : [];
  renderDomains();

  setStatus('info', `Ready. Using account ${cfg.account_id || '(not configured)'}. Prefill domain: ${defaultDomain || '(none)'}.`);
}

async function submitRegistration() {
  if (domains.length === 0) {
    setStatus('err', 'Add at least one domain.');
    return;
  }

  const payload = {
    domains,
    merchantUrl: normalizeDomain(merchantUrlInput.value),
    partnerMerchantName: partnerNameInput.value.trim(),
  };

  if (!payload.merchantUrl || !/^[a-z0-9.-]+$/i.test(payload.merchantUrl)) {
    setStatus('err', 'Merchant URL must be a hostname (e.g. applepay.local), without https:// or a port.');
    return;
  }

  submitBtn.setAttribute('aria-busy', 'true');
  submitBtn.disabled = true;
  setStatus('info', 'Submitting merchant registration…');

  try {
    const { res, data } = await fetchJson(apiUrl('register-merchant.php'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify(payload),
    });

    outputEl.textContent = JSON.stringify(data, null, 2);

    if (data.ok && data.result && data.result.ok) {
      setStatus('ok', `Registration succeeded (HTTP ${data.result.http_status}).`);
      try {
        await loadEnrollment();
      } catch (_) {
        // Enrollment refresh is best-effort after a successful register.
      }
    } else {
      const detail = data.error
        || (data.result && data.result.curl_error)
        || `HTTP ${(data.result && data.result.http_status) || res.status}`;
      setStatus('err', `Registration failed: ${detail}`);
    }
  } catch (err) {
    setStatus('err', err instanceof Error ? err.message : String(err));
    outputEl.textContent = String(err);
  } finally {
    submitBtn.removeAttribute('aria-busy');
    submitBtn.disabled = false;
  }
}

addDomainBtn.addEventListener('click', addDomain);
domainInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') {
    e.preventDefault();
    addDomain();
  }
});
submitBtn.addEventListener('click', submitRegistration);
refreshEnrollmentBtn.addEventListener('click', () => {
  loadEnrollment()
    .then(() => setStatus('info', 'Enrollment refreshed.'))
    .catch((err) => setStatus('err', err instanceof Error ? err.message : String(err)));
});

(async function init() {
  try {
    await loadPublicConfig();
  } catch (err) {
    setStatus('err', err instanceof Error ? err.message : String(err));
    return;
  }

  try {
    await loadEnrollment();
  } catch (err) {
    // Keep registration usable even if enrollment lookup fails.
    setStatus('err', 'Could not load current enrollment: ' + (err instanceof Error ? err.message : String(err)));
  }
})();
