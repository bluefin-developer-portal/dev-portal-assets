function onSubmit() {
  scfr.tokenize(
    "sample#echo",
    {
      // Transaction total
      amount: 1,
      // Merchant transaction ID, so that your transaction can be easily correlated with the 3DS transaction
      clientTransactionId: (+ new Date()).toString(),
      // ISO 4217 three-digit currency code (USD - 840)
      currency: '840',
      // Shipping details (optional)
      shipping: {
        line1: '285 Andrew Young International Blvd NW',
        line2: undefined,
        line3: undefined,
        postCode: '30313',
        city: 'Atlanta',
        state: 'GA',
        // ISO 3166-1 three-digit country code e.g. 840
        country: '840'
      },
      // Billing details (optional)
      billing: {
        line1: '285 Andrew Young International Blvd NW',
        line2: undefined,
        line3: undefined,
        postCode: '30313',
        city: 'Atlanta',
        state: 'GA',
        // ISO 3166-1 three-digit country code e.g. 840
        country: '840'
      },
      email: 'sample@email.io',
      merchantRiskIndicator: {
        // Required for AMEX transactions. Indicates shipping method chosen for the transaction
        shipIndicator: '01', // Ship to cardholder's billing address
        // Indicates the merchandise delivery timeframe. Required for AMEX
        deliveryTimeFrame: '01', // Electronic Delivery
        // Indicates whether the cardholder is reordering previously purchased merchandise
        reorderItemsInd: '01' // First time ordered
      },

      // set to true if form should be submitted even if client side validation is not passed
      proceedInvalidForm: false
    });
}

function onShieldconexRendered() {
  console.log("iframe ready");
  var btn = document.getElementById("btnSubmit");
  btn.removeAttribute("disabled");
}
function onShieldconexError(...args) {
  console.log('error: ', args)
}
function onShieldconexToken(...args) {
  console.log('token: ', args)
}
function onShieldconexProcessing(echo, status) {
  console.log("echo: " + echo);
  console.log("status: " + status);
  switch (status) {
    case 'begin':
      // todo: overlay can be shown here
      document.body.style.cursor = 'progress';
      break;
    case 'failure':
      // after error event fired
      break;
    case 'success':
      // after token event fired
      break;
    case 'end':
      // todo: overlay can be hidden here
      document.body.style.cursor = 'auto';
      break;
  }
}
// get template from uri string
var url = new URL('https://secure-staging.shieldconex.com/iframe/');
// configure shieldconex iframe
var config = {
  baseUrl: 'https://secure-staging.shieldconex.com',
  templateId:'<templateId>',
  parent: 'frame1', //this must match the containing element
  attributes: {
    width: "600px",
    height: "400px",
    frameborder: 1
  }
};
var scfr = new ShieldconexIFrame(config);
scfr.onProcessing = onShieldconexProcessing;
scfr.onRendered = onShieldconexRendered; /* a function you define */
scfr.onError = onShieldconexError; /* a function you define returned after an unsuccessful data sent event for validation and required data issues */
scfr.onToken = onShieldconexToken; /* a function you define returned on successful data sent event */
scfr.render();
