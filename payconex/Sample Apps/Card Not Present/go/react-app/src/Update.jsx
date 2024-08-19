import { useLocation, useNavigate } from 'react-router-dom';
import './App.css'

function cleanFormData(formData) {
  return Object.entries(formData).reduce((acc, [k, v]) => {
    if (v) {
      acc[k] = typeof v === 'object' ? cleanFormData(v) : v;
    }
    return acc;
  }, {});
}

function Update() {
  const data = useLocation()
  const row = data.state?.row
  const navigate = useNavigate();

  const handleSubmit = (e) => {
    e.preventDefault()

    const body = {
      "description": e.target.description.value || row.description,
      "level3": {
        "summaryCommodityCode": e.target.summaryCommodityCode.value || row.level3.summaryCommodityCode,
        "shipFromZip": e.target.shipFromZip.value || row.level3.shipFromZip,
      },
      "customer": {
        "name": e.target.name.value || row.customer.name,
        "billingAddress": {
          "address1": e.target.address1.value || row.customer.billingAddress.address1,
          "address2": e.target.address2.value || row.customer.billingAddress.address2,
          "city": e.target.city.value || row.customer.billingAddress.city,
          "state": e.target.state.value || row.customer.billingAddress.state,
          "zip": e.target.zip.value || row.customer.billingAddress.zip,
          "country": e.target.country.value || row.customer.billingAddress.country,
          "company": e.target.company.value || row.customer.billingAddress.company
        }
      },
      "shippingAddress": {
        "address1": e.target.shippingAddress1.value || row.shippingAddress.address1,
        "address2": e.target.shippingAddress2.value || row.shippingAddress.address2,
        "city": e.target.shippingCity.value || row.shippingAddress.city,
        "state": e.target.shippingState.value || row.shippingAddress.state,
        "zip": e.target.shippingZip.value || row.shippingAddress.zip,
        "country": e.target.shippingCountry.value || row.shippingAddress.country,
        "company": e.target.shippingCompany.value || row.shippingAddress.company,
        "recipient": e.target.shippingRecipient.value || row.shippingAddress.recipient
      },
    }
    const formDataChanges = cleanFormData(body)

    fetch(`http://127.0.0.1:8000/transaction/${row.transactionId}`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(formDataChanges)
    }).then(() => navigate("/")
    ).catch((err) => {
      console.log("Error updating transaction meta-data: ", err.message)
    })
  }

  return (
    <form className='container my-5 flex formContainer' onSubmit={handleSubmit} style={{ width: '27rem' }}>
      <h3>Transaction {row.transactionId}</h3>
      <div className="inputContainer">
        <label className="htmlForm-label">Description</label>
        <input type="text" name='description' className="htmlForm-control"
          placeholder={row.description} />

        <div className="inputContainer">
          <label className="htmlForm-label">Customer</label>

          <label className="htmlForm-label">Name</label>
          <input type="text" name='name' className="htmlForm-control"
            placeholder={row.customer.name} />

          <label className="htmlForm-label">Billing Address</label>
          <label className="htmlForm-label">Address 1</label>
          <input type="text" className="htmlForm-control"
            name='address1'
            placeholder={row.customer.billingAddress.address1} />

          <label className="htmlForm-label">Address 2</label>
          <input type="text" className="htmlForm-control"
            name='address2'
            placeholder={row.customer.billingAddress.address2} />

          <label className="htmlForm-label">City</label>
          <input type="text" className="htmlForm-control"
            name='city'
            placeholder={row.customer.billingAddress.city} />

          <label className="htmlForm-label">State</label>
          <input type="text" className="htmlForm-control"
            name='state'
            placeholder={row.customer.billingAddress.state} />

          <label className="htmlForm-label">Zip</label>
          <input type="text" className="htmlForm-control"
            name='zip'
            placeholder={row.customer.billingAddress.zip} />

          <label className="htmlForm-label">Country</label>
          <input type="text" className="htmlForm-control"
            name='country'
            placeholder={row.customer.billingAddress.country} />

          <label className="htmlForm-label">Company</label>
          <input type="text" className="htmlForm-control"
            name='company'
            placeholder={row.customer.billingAddress.company} />
        </div>
      </div>

      <div className="inputContainer">
        <label className="htmlForm-label">Shipping Address</label>

        <label className="htmlForm-label">Address 1</label>
        <input type="text" className="htmlForm-control"
          name='shippingAddress1'
          placeholder={row.shippingAddress.address1} />

        <label className="htmlForm-label">Address 2</label>
        <input type="text" className="htmlForm-control"
          name='shippingAddress2'
          placeholder={row.shippingAddress.address2} />

        <label className="htmlForm-label">City</label>
        <input type="text" className="htmlForm-control"
          name='shippingCity'
          placeholder={row.shippingAddress.city} />

        <label className="htmlForm-label">State</label>
        <input type="text" className="htmlForm-control"
          name='shippingState'
          placeholder={row.shippingAddress.state} />

        <label className="htmlForm-label">Zip</label>
        <input type="text" className="htmlForm-control"
          name='shippingZip'
          placeholder={row.shippingAddress.zip} />

        <label className="htmlForm-label">Country</label>
        <input type="text" className="htmlForm-control"
          name='shippingCountry'
          placeholder={row.shippingAddress.country} />

        <label className="htmlForm-label">Company</label>
        <input type="text" className="htmlForm-control"
          name='shippingCompany'
          placeholder={row.shippingAddress.company} />

        <label className="htmlForm-label">Recipient</label>
        <input type="text" className="htmlForm-control"
          name='shippingRecipient'
          placeholder={row.shippingAddress.recipient} />
      </div>

      <div className="inputContainer">
        <label className="htmlForm-label">Level 3</label>

        <label className="htmlForm-label">Summary Commodity Code</label>
        <input type="text" className="htmlForm-control"
          name='summaryCommodityCode'
          placeholder={row.level3.summaryCommodityCode} />

        <label className="htmlForm-label">Ship From Zip</label>
        <input type="text" className="htmlForm-control"
          name='shipFromZip'
          placeholder={row.level3.shipFromZip} />
      </div>

      <button type='submit' className="btn btn-primary">Submit</button>
    </form>
  )
}

export default Update 
