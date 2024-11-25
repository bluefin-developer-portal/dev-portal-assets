import { useCallback, useEffect, useMemo, useState } from 'react';
import DataTable, { createTheme } from 'react-data-table-component'
import differenceBy from 'lodash.differenceby';
import { Link } from 'react-router-dom';


async function getReport() {
  const response = await fetch('/api/report', {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json'
    }
  })

  return response.json()
}

function findAction(action, history) {
  return history.find(historyAct => historyAct.action === action)
}

function Table() {
  const [rows, setRows] = useState([])
  const [selectedRows, setSelectedRows] = useState([]);
  const [toggleCleared, setToggleCleared] = useState(false);

  useEffect(() => {
    getReport().then((data) => {
      if (data?.length > 0) {
        setRows(data)
      }
    })
      .catch((err) => {
        console.log(err.message)
      })
  }, [])

  const contextActions = useMemo(() => {
    const handleDelete = () => {
      setToggleCleared(!toggleCleared);
      setRows(differenceBy(rows, selectedRows, 'transactionId'));

      selectedRows.forEach(row => {
        fetch(`/api/transaction/${row.transactionId}`, {
          method: 'DELETE',
          headers: {
            'Content-Type': 'application/json'
          }
        }).then(() => { })
          .catch((err) => {
            console.log(err.message)
          })
      })
    };

    const handleCapture = () => {
      setToggleCleared(!toggleCleared);
      selectedRows.forEach(row => {
        const wasCaptured = findAction('capture', row.trace.history)
        const wasRefunded = findAction('refund', row.trace.history)

        if (wasCaptured || wasRefunded) {
          alert("Transaction action already performed.")
          return
        }

        const body = { transactionId: row.transactionId }

        fetch('/api/capture-transaction', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(body)
        }).then(() => getReport().then((data) => {
          setRows(data)
        }))
          .catch((err) => {
            console.log(err.message)
          })
      })
    };

    const handleRefund = () => {
      setToggleCleared(!toggleCleared);
      selectedRows.forEach(row => {
        const wasRefunded = findAction('refund', row.trace.history)

        if (wasRefunded) {
          alert("Transaction action already performed.")
          return
        }

        const body = { transactionId: row.transactionId }

        fetch('/api/refund-transaction', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(body)
        }).then(() => getReport().then((data) => {
          setRows(data)
        }))
          .catch((err) => {
            console.log(err.message)
          })
      })
    };

    return (
      <>
        <button key="delete" className='action warning' onClick={handleDelete}>
          DELETE
        </button>

        <button key="capture" className='action' onClick={handleCapture}>
          CAPTURE
        </button>

        <button key="refund" className='action warning' onClick={handleRefund}>
          REFUND
        </button>

        <Link to={'/update'} state={{ row: selectedRows[0] }}>
          <button key="update" className='action'>
            UPDATE
          </button>
        </Link>
      </>
    );
  }, [rows, selectedRows, toggleCleared]);

  createTheme('bluefin', {
    text: {
      primary: '#FFFFFF',
      secondary: '#FFFFFF',
    },
    background: {
      default: '#00A8E0',
    },
    context: {
      background: '#071e49',
      text: '#FFFFFF',
    },
    divider: {
      default: '#071e49',
    },
  }, 'light');

  const columns = [
    {
      name: "Transaction ID",
      selector: row => row.transactionId,
      sortable: true
    },
    {
      name: "Transaction Date",
      selector: row => row.timestamp,
      sortable: true
    },
    {
      name: "Name",
      selector: row => row.customer.name,
      sortable: true
    },
    {
      name: "Last 4",
      selector: row => row.card.last4,
      sortable: true
    },
    {
      name: "Amounts",
      selector: row => row.amounts.approved,
      sortable: true
    },
    {
      name: "Transanction Type",
      selector: row => row.transactionType,
      sortable: true
    },
    {
      name: "Last Action",
      selector: row => row.trace.history[row.trace.history.length - 1].action,
      sortable: true
    }
  ];

  const ExpandedComponent = ({ data }) => <pre>{JSON.stringify(data, null, 2)}</pre>;

  const handleRowSelected = useCallback(state => {
    setSelectedRows(state.selectedRows);
  }, [])

  return (
    <div className='container my-5 .text-info'>
      <DataTable
        columns={columns}
        data={rows}
        fixedHeader
        title='Transaction Management'
        expandableRows
        expandableRowsComponent={ExpandedComponent}
        pagination
        selectableRowsNoSelectAll
        selectableRows
        contextActions={contextActions}
        onSelectedRowsChange={handleRowSelected}
        clearSelectedRows={toggleCleared}
        theme="bluefin"
      />
    </div>
  )
}

export default Table
