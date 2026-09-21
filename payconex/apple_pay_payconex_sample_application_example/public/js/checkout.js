/**
 * Apple Pay checkout page.
 *
 * Flow (matches Bluefin docs + Apple Pay JS for multi-browser):
 *   1. Require a secure context (HTTPS).
 *   2. Detect Apple Pay (canMakePayments / applePayCapabilities).
 *   3. onvalidatemerchant → PayConex /applePay/session (browser, per Bluefin docs)
 *   4. onpaymentmethodselected → completePaymentMethodSelection
 *   5. onpaymentauthorized → api/process-payment.php (QSAPI APay SALE)
 *
 * https://developer.apple.com/documentation/applepayontheweb/checking-for-apple-pay-availability
 */

const statusEl = document.getElementById('status');
const amountInput = document.getElementById('amount');
const displayNameEl = document.getElementById('display-name');
const accountIdEl = document.getElementById('account-id');
const buttonEl = document.getElementById('apple-pay-button');
const unsupportedEl = document.getElementById('apple-pay-unsupported');
const flowLogEl = document.getElementById('flow-log');

/** @type {{ account_id: string, display_name: string, default_amount: string, apple_merchant_id?: string, v4_api_host?: string } | null} */
let publicConfig = null;

/** Apple Pay JS API version used for new ApplePaySession(...). */
let applePayVersion = 0;

/**
 * @param {'ok'|'err'|'info'} kind
 * @param {string} message
 */
function setStatus(kind, message) {
  statusEl.className = `status-banner visible ${kind}`;
  statusEl.textContent = message;
}

/**
 * @param {'session'|'token'|'qsapi'} stepId
 * @returns {HTMLElement | null}
 */
function flowStepEl(stepId) {
  return flowLogEl.querySelector(`[data-step="${stepId}"]`);
}

/**
 * @param {'session'|'token'|'qsapi'} stepId
 * @param {'idle'|'active'|'ok'|'err'} state
 * @param {string} summary
 * @param {unknown} [detail]
 */
function updateFlowStep(stepId, state, summary, detail) {
  const el = flowStepEl(stepId);
  if (!el) {
    return;
  }

  el.className = `flow-step is-${state}`;
  const summaryEl = el.querySelector('.flow-step-summary');
  const stateEl = el.querySelector('.flow-step-state');
  const detailEl = el.querySelector('.flow-step-detail');

  if (summaryEl) {
    summaryEl.textContent = summary;
  }
  if (stateEl) {
    const labels = { idle: 'Idle', active: 'In progress', ok: 'Done', err: 'Failed' };
    stateEl.textContent = labels[state] || state;
  }
  if (detailEl) {
    if (detail === undefined || detail === null || detail === '') {
      detailEl.hidden = true;
      detailEl.textContent = '';
    } else {
      detailEl.hidden = false;
      detailEl.textContent = typeof detail === 'string'
        ? detail
        : JSON.stringify(detail, null, 2);
    }
  }
}

function resetFlowLog() {
  updateFlowStep('session', 'idle', 'Waiting for Apple Pay…');
  updateFlowStep('token', 'idle', 'Waiting for authorization…');
  updateFlowStep('qsapi', 'idle', 'Waiting for token…');
}

/**
 * Pick Apple Pay JS version — same approach as Bluefin docs (probe high → low).
 * @returns {number}
 */
function pickApplePayVersion() {
  if (!window.ApplePaySession || typeof ApplePaySession.supportsVersion !== 'function') {
    return 3;
  }
  for (let i = 15; i >= 1; i--) {
    if (ApplePaySession.supportsVersion(i)) {
      return i;
    }
  }
  return 3;
}

/**
 * @returns {Promise<{ available: boolean, detail: string }>}
 */
async function checkApplePayAvailability() {
  if (!window.isSecureContext) {
    return {
      available: false,
      detail:
        'This page is not a secure context (HTTPS). Apple Pay will refuse to start. '
        + 'Use https://applepay.local:8443 (Docker + hosts file) or your registered HTTPS host.',
    };
  }

  if (!window.ApplePaySession) {
    return {
      available: false,
      detail:
        'Apple Pay is not available in this browser. Use Safari, or Chrome/Edge/Firefox '
        + 'with iOS 18+ handoff and the official Apple Pay JS SDK.',
    };
  }

  const merchantId = (publicConfig && publicConfig.apple_merchant_id) || '';

  if (typeof ApplePaySession.applePayCapabilities === 'function' && merchantId) {
    try {
      const capabilities = await ApplePaySession.applePayCapabilities(merchantId);
      const status = capabilities && capabilities.paymentCredentialStatus;
      if (status === 'applePayUnsupported') {
        return { available: false, detail: 'This device does not support Apple Pay.' };
      }
      return { available: true, detail: 'Apple Pay is ready.' };
    } catch (err) {
      console.warn('applePayCapabilities failed; falling back to canMakePayments()', err);
    }
  }

  if (typeof ApplePaySession.canMakePayments === 'function' && ApplePaySession.canMakePayments()) {
    return { available: true, detail: 'Apple Pay is ready.' };
  }

  return {
    available: false,
    detail:
      'Apple Pay is not available on this device right now. '
      + 'On desktop third-party browsers you typically need a nearby iPhone/iPad on iOS 18+.',
  };
}

async function loadPublicConfig() {
  const { res, data } = await fetchJson(apiUrl('public-config.php'));
  if (!res.ok || !data.ok) {
    throw new Error(data.error || 'Failed to load public config.');
  }
  publicConfig = data.config;
  displayNameEl.textContent = publicConfig.display_name;
  accountIdEl.textContent = publicConfig.account_id || '(not configured)';
  if (publicConfig.default_amount) {
    amountInput.value = publicConfig.default_amount;
  }
}

/**
 * Payment request — aligned with Bluefin's integrating-apple-pay example
 * (minus the invalid ApplePayContactField property from that sample).
 *
 * @see https://developers.bluefin.com/payconex/docs/integrating-apple-pay
 */
function buildPaymentRequest(amount, label) {
  return {
    countryCode: 'US',
    currencyCode: 'USD',
    supportedNetworks: ['visa', 'masterCard', 'amex', 'discover'],
    merchantCapabilities: ['supports3DS'],
    requiredBillingContactFields: ['postalAddress', 'name'],
    total: {
      label,
      amount: String(amount),
    },
  };
}

/**
 * Apple requires completePaymentMethodSelection within ~30s when this fires
 * (including for the initially selected card). Missing this is a common cause
 * of "Payment not completed" / oncancel with no onpaymentauthorized.
 *
 * @param {ApplePaySession} session
 * @param {{ label: string, amount: string, type?: string }} total
 */
function acknowledgePaymentMethod(session, total) {
  const update = {
    newTotal: {
      label: total.label,
      type: total.type || 'final',
      amount: total.amount,
    },
  };

  try {
    session.completePaymentMethodSelection(update);
  } catch (err) {
    try {
      session.completePaymentMethodSelection(update.newTotal, []);
    } catch (err2) {
      console.error('completePaymentMethodSelection failed', err, err2);
    }
  }
}

/**
 * Safe preview of an Apple Pay token (no paymentData ciphertext).
 * @param {object} token
 */
function summarizeToken(token) {
  const method = token && token.paymentMethod ? token.paymentMethod : {};
  return {
    transactionIdentifier: token.transactionIdentifier || null,
    paymentMethod: {
      displayName: method.displayName || null,
      network: method.network || null,
      type: method.type || null,
    },
    hasPaymentData: !!(token && token.paymentData),
    note: 'Full paymentData is sent to QSAPI but omitted from this log.',
  };
}

/**
 * Pull a few readable fields from a QSAPI result wrapper.
 * @param {object} data
 */
function summarizeQsapi(data) {
  const result = data && data.result ? data.result : {};
  const body = result.response_json && typeof result.response_json === 'object'
    ? result.response_json
    : null;

  return {
    approved: !!(data && data.approved),
    http_status: result.http_status || null,
    transaction_id: body && (body.transaction_id || body.transactionId) || null,
    error_message: body && (body.error_message || body.error_msg) || data.error || null,
    response: body || result.response_raw || null,
  };
}

function startApplePay() {
  if (!publicConfig || !applePayVersion) {
    setStatus('err', 'Apple Pay is not ready.');
    return;
  }

  if (!window.isSecureContext) {
    setStatus('err', 'Apple Pay requires HTTPS. See README for local testing options.');
    return;
  }

  const amount = amountInput.value.trim();
  if (!/^\d+(\.\d{1,2})?$/.test(amount)) {
    setStatus('err', 'Enter a valid amount such as 1.00');
    return;
  }

  // PayConex referrer must be hostname only (no port).
  const referringUrl = window.location.hostname;

  /** @type {'idle'|'starting'|'validating'|'validated'|'authorized'|'completed'} */
  let stage = 'idle';
  let sessionClosedForError = false;
  let merchantSessionDomain = null;

  resetFlowLog();

  const payData = buildPaymentRequest(amount, publicConfig.display_name);

  let session;
  try {
    session = new ApplePaySession(applePayVersion, payData);
  } catch (err) {
    setStatus('err', 'Could not start Apple Pay on this device.');
    updateFlowStep(
      'session',
      'err',
      'ApplePaySession failed to start.',
      {
        error: err instanceof Error ? err.message : String(err),
        tip: 'On a new Mac user, enroll Touch ID and confirm Wallet shows a usable sandbox card.',
      }
    );
    return;
  }

  session.onpaymentmethodselected = () => {
    acknowledgePaymentMethod(session, {
      label: payData.total.label,
      amount: String(payData.total.amount),
      type: 'final',
    });
  };

  /**
   * Match Bluefin docs exactly: browser calls PayConex, then
   * completeMerchantValidation(merchantSession) with the JSON body.
   * https://developers.bluefin.com/payconex/docs/integrating-apple-pay
   */
  session.onvalidatemerchant = () => {
    stage = 'validating';
    setStatus('info', 'Requesting merchant session from PayConex…');
    updateFlowStep(
      'session',
      'active',
      `POST …/applePay/session for ${referringUrl}`
    );

    const v4Base = (publicConfig.v4_api_host || 'https://api-cert.payconex.net').replace(/\/$/, '');
    const sessionUrl =
      `${v4Base}/api/v4/accounts/${encodeURIComponent(publicConfig.account_id)}/applePay/session`;

    const requestBody = {
      display_name: publicConfig.display_name,
      referrer: referringUrl,
    };

    fetch(sessionUrl, {
      method: 'POST',
      headers: new Headers({ 'content-type': 'application/json' }),
      body: JSON.stringify(requestBody),
    })
      .then(async (res) => {
        const text = await res.text();
        let json = null;
        try {
          json = text ? JSON.parse(text) : null;
        } catch (_) {
          json = null;
        }
        if (!res.ok) {
          const err = new Error(`PayConex session HTTP ${res.status}`);
          err.detail = json || text || null;
          throw err;
        }
        return json;
      })
      .then((merchantSession) => {
        if (!merchantSession || !merchantSession.signature) {
          throw new Error('PayConex did not return a merchant session with a signature.');
        }

        merchantSessionDomain = merchantSession.domainName || null;
        const domainsMatch = merchantSessionDomain === window.location.hostname;

        updateFlowStep(
          'session',
          'ok',
          `Validated for ${merchantSessionDomain || 'unknown domain'}`
            + (domainsMatch ? '' : ' (hostname mismatch)'),
          {
            request: {
              url: sessionUrl,
              body: requestBody,
            },
            response: {
              domainName: merchantSession.domainName || null,
              displayName: merchantSession.displayName || null,
              merchantSessionIdentifier: merchantSession.merchantSessionIdentifier || null,
              epochTimestamp: merchantSession.epochTimestamp || null,
              expiresAt: merchantSession.expiresAt || null,
              hasSignature: !!merchantSession.signature,
              domains_match: domainsMatch,
            },
          }
        );

        session.completeMerchantValidation(merchantSession);
        stage = 'validated';
        setStatus('info', 'Merchant validated. Authorize the payment…');
      })
      .catch((err) => {
        console.error('Error fetching merchant session', err);
        sessionClosedForError = true;
        setStatus('err', err instanceof Error ? err.message : String(err));
        updateFlowStep(
          'session',
          'err',
          err instanceof Error ? err.message : String(err),
          err && err.detail ? err.detail : String(err)
        );
        session.abort();
      });
  };

  session.onpaymentauthorized = async (event) => {
    stage = 'authorized';
    setStatus('info', 'Payment authorized. Sending token to PayConex…');
    updateFlowStep('token', 'active', 'Received authorization from Apple Pay…');

    try {
      let token = event.payment && event.payment.token ? event.payment.token : null;
      if (!token) {
        throw new Error('Apple Pay event did not include payment.token.');
      }

      if (typeof token.paymentData === 'string') {
        try {
          token = Object.assign({}, token, {
            paymentData: JSON.parse(token.paymentData),
          });
        } catch (parseErr) {
          throw new Error('Apple Pay paymentData was a string but not valid JSON.');
        }
      }

      const tokenSummary = summarizeToken(token);
      updateFlowStep(
        'token',
        'ok',
        [
          tokenSummary.paymentMethod.network || 'Card',
          tokenSummary.paymentMethod.displayName || '',
        ].filter(Boolean).join(' · ') || 'Token received',
        tokenSummary
      );

      updateFlowStep('qsapi', 'active', `Charging $${amount} via QSAPI SALE (APay)…`);

      const { data } = await fetchJson(apiUrl('process-payment.php'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ amount, token }),
      }, 45000);

      const qsapiSummary = summarizeQsapi(data);
      stage = 'completed';

      if (data.ok && data.approved) {
        session.completePayment({ status: ApplePaySession.STATUS_SUCCESS });
        setStatus('ok', 'Payment approved.');
        updateFlowStep(
          'qsapi',
          'ok',
          qsapiSummary.transaction_id
            ? `Approved · transaction ${qsapiSummary.transaction_id}`
            : 'Approved by PayConex',
          qsapiSummary
        );
      } else {
        session.completePayment({ status: ApplePaySession.STATUS_FAILURE });
        const detail = qsapiSummary.error_message || 'Payment declined or failed.';
        setStatus('err', String(detail));
        updateFlowStep('qsapi', 'err', String(detail), qsapiSummary);
      }
    } catch (err) {
      console.error(err);
      stage = 'completed';
      sessionClosedForError = true;
      try {
        session.completePayment({ status: ApplePaySession.STATUS_FAILURE });
      } catch (_) {
        // Session may already be closing.
      }
      const message = err instanceof Error ? err.message : String(err);
      setStatus('err', message);

      const tokenEl = flowStepEl('token');
      if (tokenEl && tokenEl.classList.contains('is-active')) {
        updateFlowStep('token', 'err', message, message);
      } else {
        updateFlowStep('qsapi', 'err', message, message);
      }
    }
  };

  session.oncancel = (event) => {
    const sessionError = event && event.sessionError ? event.sessionError : null;
    if (sessionError) {
      console.warn('Apple Pay sessionError', sessionError);
    }

    if (sessionClosedForError || stage === 'completed' || stage === 'authorized') {
      return;
    }

    const errorDetail = sessionError
      ? {
          code: sessionError.code || null,
          message: sessionError.message || null,
          info: sessionError.info || null,
        }
      : null;

    if (stage === 'idle' || stage === 'starting') {
      setStatus('err', 'Apple Pay was cancelled before merchant validation started.');
      updateFlowStep(
        'session',
        'err',
        'Cancelled before the PayConex session request.',
        {
          tip: 'Enroll Touch ID for this Mac user and confirm Wallet has a usable sandbox card.',
          sessionError: errorDetail,
        }
      );
      return;
    }

    if (stage === 'validating') {
      setStatus('err', 'Apple Pay was cancelled while fetching the merchant session.');
      updateFlowStep(
        'session',
        'err',
        'Cancelled during merchant session request.',
        { sessionError: errorDetail }
      );
      return;
    }

    if (stage === 'validated') {
      setStatus('err', 'Apple Pay closed after validation without a payment token.');
      updateFlowStep(
        'token',
        'err',
        'No token received after merchant validation.',
        {
          merchant_session_domainName: merchantSessionDomain,
          sessionError: errorDetail,
        }
      );
      return;
    }

    setStatus('err', 'Apple Pay closed before authorization finished.');
    updateFlowStep('token', 'err', 'Sheet closed unexpectedly.', { sessionError: errorDetail });
  };

  stage = 'starting';
  setStatus('info', 'Opening Apple Pay…');
  try {
    session.begin();
  } catch (err) {
    sessionClosedForError = true;
    setStatus('err', 'Could not open the Apple Pay sheet.');
    updateFlowStep(
      'session',
      'err',
      err instanceof Error ? err.message : String(err)
    );
  }
}

async function init() {
  resetFlowLog();

  try {
    await loadPublicConfig();
  } catch (err) {
    console.error(err);
    setStatus('err', err instanceof Error ? err.message : String(err));
    unsupportedEl.hidden = false;
    return;
  }

  const availability = await checkApplePayAvailability();
  if (!availability.available) {
    unsupportedEl.hidden = false;
    buttonEl.style.display = 'none';
    setStatus('err', availability.detail);
    return;
  }

  applePayVersion = pickApplePayVersion();

  unsupportedEl.hidden = true;
  buttonEl.style.display = 'inline-block';
  buttonEl.addEventListener('click', startApplePay);
  setStatus('ok', 'Apple Pay is ready. Enter an amount and tap the button.');
}

init().catch((err) => {
  console.error(err);
  setStatus('err', err instanceof Error ? err.message : String(err));
});
