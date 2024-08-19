async function getReport() {
  const response = await fetch('http://127.0.0.1:8000/report', {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json'
    }
  })

  return response.json()
}

async function getConfigs() {
  const response = await fetch('http://127.0.0.1:8000/config', {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json'
    }
  })

  return response.json()
}

async function generateBearerToken(amount, resourceId) {
  const body = { amount, resourceId }

  const response = await fetch('http://127.0.0.1:8000/generate-bearer-token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  })

  return response.json()
}

async function authorizeTransaction(amount, payConexToken) {
  const body = { amount, payConexToken }

  const response = await fetch('http://127.0.0.1:8000/authorize-transaction', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  })

  return response.json()
}

async function captureTransaction(transactionId) {
  const body = { transactionId }

  const response = await fetch('http://127.0.0.1:8000/capture-transaction', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  })

  return response.json()
}

async function refundTransaction(transactionId) {
  const body = { transactionId }

  const response = await fetch('http://127.0.0.1:8000/refund-transaction', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  })

  return response.json()
}

async function getReport() {
  const response = await fetch('http://127.0.0.1:8000/report', {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json'
    }
  })

  return response.json()
}

async function deleteTransaction(transactionId) {
  return fetch(`http://127.0.0.1:8000/transaction/${transactionId}`, {
    method: 'DELETE',
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

let amount = null

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
    authorizeTransaction(amount, data.token).then(() => window.location.reload())
  },
  error: function(data) {
    console.log('Error:', data);
  },
  timeout: function(data) {
    console.log('Timeout:', data);
  },
};

getReport().then(transactionList => {
  const transactionDiv = document.getElementById("transactionList");
  const reportSection = document.getElementById("report");
  let reportList = []

  const reportButton = document.createElement('button');
  const reportsDiv = document.createElement("div");
  reportsDiv.className = "report"
  reportButton.textContent = 'REPORT';

  reportButton.addEventListener('click', (e) => {
    e.preventDefault()
    if (reportList.length < 1) {
      getReport().then((reports) => {
        renderjson.set_icons('+', '-');
        reports?.forEach(report => {
          const reportItem = document.createElement("div");

          const reportTitle = document.createElement("h3");
          reportTitle.textContent = "Report: "

          reportItem.appendChild(reportTitle)
          reportItem.appendChild(renderjson(report))
          reportsDiv.appendChild(reportItem)
        })

        reportList = reports || []
        return
      })
    }
    return
  });

  reportSection.appendChild(reportButton)
  reportSection.appendChild(reportsDiv)

  transactionList?.forEach(transaction => {
    const form = document.createElement("form");
    form.setAttribute("id", transaction.transactionId)
    form.className = "transactionForm"

    const title = document.createElement('h3')
    title.innerHTML = 'Transaction Data'
    form.appendChild(title)

    const amount = document.createElement('p')
    amount.innerHTML = `Amount: ${transaction.amounts.approved}`
    form.appendChild(amount)

    const status = document.createElement('p')
    status.innerHTML = `Status: ${transaction.status}`
    form.appendChild(status)

    if (transaction.status == "AUTHORIZED") {
      const captureTransactionButton = document.createElement('button');
      captureTransactionButton.textContent = 'CAPTURE';

      captureTransactionButton.addEventListener('click', (e) => {
        e.preventDefault()
        captureTransaction(transaction.transactionId).then(() => {
          return window.location.reload()
        })
      });

      form.appendChild(captureTransactionButton)
    }

    const wasRefunded = transaction.trace.history.find(history => history.action == 'refund')
    if (!wasRefunded) {
      const refundTransactionButton = document.createElement('button');
      refundTransactionButton.className = "warning"
      refundTransactionButton.textContent = 'REFUND';

      refundTransactionButton.addEventListener('click', (e) => {
        e.preventDefault()
        refundTransaction(transaction.transactionId).then(() => {
          return window.location.reload()
        })
      });

      form.appendChild(refundTransactionButton)
    }

    const deleteButton = document.createElement('button');
    deleteButton.className = "warning"
    deleteButton.textContent = 'DELETE';

    deleteButton.addEventListener('click', (e) => {
      e.preventDefault()
      deleteTransaction(transaction.transactionId).then(() => window.location.reload())
    });
    form.appendChild(deleteButton)

    transactionDiv.appendChild(form)
  })

  const checkoutForm = document.getElementById("checkoutForm");
  checkoutForm.addEventListener("submit", (e) => {
    e.preventDefault();

    amount = document.getElementById("amount").value;

    if (amount == "") {
      alert("Please add an amount!");
      return
    }

    return getConfigs().then(configs => {
      generateBearerToken(amount, configs[0].resourceId).then(generateBearerTokenResponse => {
        const bearerToken = generateBearerTokenResponse.bearerToken
        window.IframeV2.init(iframeConfig, bearerToken, callbacks, 'https://checkout-cert.payconex.net')
        checkoutForm.style.display = 'none'
        transactionDiv.style.display = 'none'
        reportSection.style.display = 'none'
      })
    })
  })
})
