async function generateBearerToken(amount) {
  const body = { amount }

  const response = await fetch('/generate-bearer-token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  })

  return response.json()
}

async function createSubscription(payConexToken, typeSubscription, amount) {
  const body = { payConexToken, typeSubscription, amount }

  const response = await fetch('/subscription', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  })

  return response.json()
}

async function cancelSubscription(subscriptionId) {
  return fetch(`/subscription/${subscriptionId}`, {
    method: 'DELETE',
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

async function getSubscriptions() {
  const response = await fetch('/subscription', {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json'
    }
  })

  return response.json()
}

let amount = null
let typeSubscription = null

const iframeConfig = {
  parentDivId: 'bluefin-container',
  width: '800px',
  height: '600px'
};

const callbacks = {
  iframeLoaded: function() {
    console.log('Iframe loaded');
  },
  checkoutComplete: function(data) {
    console.log('Checkout complete:', data);
    createSubscription(data.bfTokenReference, typeSubscription, amount).then(() => window.location.reload())
  },
  error: function(data) {
    console.log('Error:', data);
  },
  timeout: function(data) {
    console.log('Timeout:', data);
  },
};

getSubscriptions().then(subscriptionsResponse => {
  const subscriptionList = subscriptionsResponse.subscriptionList
  const subscriptionDiv = document.getElementById("subscriptionList");

  subscriptionList.forEach(subscription => {
    const form = document.createElement("form");
    form.setAttribute("id", subscription.token)

    const title = document.createElement('h3')
    title.innerHTML = 'Subscription Data'

    const amount = document.createElement('p')
    amount.innerHTML = `Amount: ${subscription.amount}`

    const typeSubscription = document.createElement('p')
    typeSubscription.innerHTML = `Type: ${subscription.typeSubscription}`

    const cancelButton = document.createElement('button');
    cancelButton.textContent = 'CANCEL';

    cancelButton.addEventListener('click', () => {
      cancelSubscription(subscription.token)
    });

    form.appendChild(title)
    form.appendChild(amount)
    form.appendChild(typeSubscription)
    form.appendChild(cancelButton)

    subscriptionDiv.appendChild(form)
  })

  const checkoutForm = document.getElementById("checkoutForm");
  checkoutForm.addEventListener("submit", (e) => {
    e.preventDefault();

    amount = document.getElementById("amount").value;
    typeSubscription = document.getElementById("typeSubOption").value

    if (amount == "") {
      alert("Please add an amount!");
      return
    }

    return generateBearerToken(amount).then(generateBearerTokenResponse => {
      const bearerToken = generateBearerTokenResponse.bearerToken
      window.IframeV2.init(iframeConfig, bearerToken, callbacks, null, 'https://checkout-cert.payconex.net')
      checkoutForm.style.display = 'none'
      subscriptionDiv.style.display = 'none'
    })
  })
})
