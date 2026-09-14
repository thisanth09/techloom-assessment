import { useEffect, useMemo, useState } from 'react'
import './App.css'

const API_BASE = 'http://localhost:8080/api'

const fallbackProducts = [
  { id: 1, name: 'Laptop', category: 'Tech', price: 250000, stock: 10 },
  { id: 2, name: 'Mouse', category: 'Accessories', price: 3500, stock: 20 },
  { id: 3, name: 'Keyboard', category: 'Accessories', price: 5000, stock: 15 },
  { id: 4, name: 'Monitor', category: 'Display', price: 45000, stock: 8 },
]

const PAGE = {
  INVENTORY: 'inventory',
  PRODUCTS: 'products',
  CART: 'cart',
  PAYMENT: 'payment',
}

function App() {
  const [page, setPage] = useState(PAGE.INVENTORY)
  const [products, setProducts] = useState(fallbackProducts)
  const [cart, setCart] = useState([])
  const [cartId, setCartId] = useState(() => crypto.randomUUID())
  const [status, setStatus] = useState('')
  const [loading, setLoading] = useState(true)
  const [customerName, setCustomerName] = useState('Walk-in Customer')
  const [paymentMethod, setPaymentMethod] = useState('Card')
  const [paymentOutcome, setPaymentOutcome] = useState('SUCCESS')
  const [processingPayment, setProcessingPayment] = useState(false)
  const [paymentDetails, setPaymentDetails] = useState({
    cardNumber: '',
    holderName: '',
    expiry: '',
    cvv: '',
    cashAmount: '',
  })
  const [editingProductId, setEditingProductId] = useState(null)
  const [newProduct, setNewProduct] = useState({
    name: '',
    category: 'General',
    price: '',
    stock: '',
  })

  useEffect(() => {
    const loadProducts = async () => {
      try {
        const response = await fetch(`${API_BASE}/products`)

        if (!response.ok) {
          throw new Error('Unable to fetch products')
        }

        const data = await response.json()
        setProducts(data)
      } catch (error) {
        setProducts(fallbackProducts)
      } finally {
        setLoading(false)
      }
    }

    loadProducts()
  }, [])

  const total = useMemo(
    () => cart.reduce((sum, item) => sum + item.price * item.quantity, 0),
    [cart],
  )

  const addToCart = (product) => {
    // Starting a new cart must never reuse an abandoned checkout id.
    if (cart.length === 0) {
      setCartId(crypto.randomUUID())
    }
    setCart((currentCart) => {
      const existing = currentCart.find((item) => item.id === product.id)

      if (existing) {
        return currentCart.map((item) =>
          item.id === product.id
            ? { ...item, quantity: Math.min(item.quantity + 1, product.stock) }
            : item,
        )
      }

      return [...currentCart, { ...product, quantity: 1 }]
    })

    setStatus(`${product.name} added to cart.`)
  }

  const updateQuantity = (productId, change) => {
    setCart((currentCart) =>
      currentCart
        .map((item) =>
          item.id === productId
            ? { ...item, quantity: Math.max(0, Math.min(item.stock, item.quantity + change)) }
            : item,
        )
        .filter((item) => item.quantity > 0),
    )
  }

  const handleProductInputChange = (event) => {
    const { name, value } = event.target
    setNewProduct((current) => ({ ...current, [name]: value }))
  }

  const resetProductForm = () => {
    setEditingProductId(null)
    setNewProduct({ name: '', category: 'General', price: '', stock: '' })
  }

  const handleEditProduct = (product) => {
    setEditingProductId(product.id)
    setNewProduct({
      name: product.name,
      category: product.category || 'General',
      price: String(product.price),
      stock: String(product.stock),
    })
    setPage(PAGE.PRODUCTS)
    setStatus(`Editing "${product.name}".`)
  }

  const handleDeleteProduct = async (productId) => {
    const productToDelete = products.find((product) => product.id === productId)

    if (!productToDelete) return

    const confirmed = window.confirm(`Delete "${productToDelete.name}"?`)
    if (!confirmed) return

    try {
      const response = await fetch(`${API_BASE}/products/${productId}`, { method: 'DELETE' })

      if (!response.ok) {
        throw new Error('Delete failed')
      }

      setProducts((currentProducts) => currentProducts.filter((product) => product.id !== productId))
      if (editingProductId === productId) {
        resetProductForm()
      }
      setStatus(`Product "${productToDelete.name}" deleted successfully.`)
    } catch (error) {
      setStatus('Unable to delete product right now.')
    }
  }

  const handleCreateProduct = async (event) => {
    event.preventDefault()

    const productPayload = {
      name: newProduct.name.trim(),
      category: newProduct.category.trim() || 'General',
      price: Number(newProduct.price),
      stock: Number(newProduct.stock),
    }

    if (!productPayload.name || Number.isNaN(productPayload.price) || Number.isNaN(productPayload.stock)) {
      setStatus('Please enter a valid product name, price, and stock.')
      return
    }

    try {
      const url = editingProductId ? `${API_BASE}/products/${editingProductId}` : `${API_BASE}/products`
      const method = editingProductId ? 'PUT' : 'POST'

      const response = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(productPayload),
      })

      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(errorText || (editingProductId ? 'Product update failed' : 'Product creation failed'))
      }

      const savedProduct = await response.json()

      if (editingProductId) {
        setProducts((currentProducts) =>
          currentProducts.map((product) => (product.id === editingProductId ? savedProduct : product)),
        )
        setStatus(`Product "${savedProduct.name}" updated successfully.`)
      } else {
        setProducts((currentProducts) => [savedProduct, ...currentProducts])
        setStatus(`Product "${savedProduct.name}" added successfully.`)
      }

      resetProductForm()
    } catch (error) {
      setStatus(error.message || (editingProductId ? 'Product update failed.' : 'Product creation failed.'))
    }
  }

  const handlePaymentDetails = (event) => {
    const { name, value } = event.target
    setPaymentDetails((current) => ({ ...current, [name]: value }))
  }

  const handleCompletePurchase = async () => {
    if (cart.length === 0 || processingPayment) {
      setStatus('Your cart is empty. Add items before checkout.')
      return
    }

    if (paymentMethod === 'Card' && !paymentDetails.cardNumber) {
      setStatus('Please enter a card number for mock card payment.')
      return
    }

    if (paymentMethod === 'Cash' && Number(paymentDetails.cashAmount || 0) < total) {
      setStatus('Cash amount is less than the total bill.')
      return
    }

    setProcessingPayment(true)
    try {
      const customer = customerName.trim() || 'Walk-in Customer'
      const checkoutPayload = {
        customerName: customer,
        cartId,
        items: cart.map((item) => ({ productId: item.id, quantity: item.quantity })),
      }

      const checkoutResponse = await fetch(`${API_BASE}/orders/checkout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(checkoutPayload),
      })

      if (!checkoutResponse.ok) {
        const errorText = await checkoutResponse.text()
        throw new Error(errorText || 'Checkout failed')
      }

      const order = await checkoutResponse.json()

      const paymentResponse = await fetch(`${API_BASE}/orders/${order.id}/payments`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          paymentMethod: paymentMethod.toUpperCase(),
          paymentReference: `mock-${Date.now()}`,
          amount: total,
          status: paymentOutcome,
        }),
      })

      if (!paymentResponse.ok) {
        const errorText = await paymentResponse.text()
        throw new Error(errorText || 'Payment failed')
      }

      const payment = await paymentResponse.json()

      setCart([])
      setCartId(crypto.randomUUID())
      setPaymentDetails({
        cardNumber: '',
        holderName: '',
        expiry: '',
        cvv: '',
        cashAmount: '',
      })
      setPage(PAGE.INVENTORY)
      const outcomeMessage = paymentOutcome === 'SUCCESS'
        ? `Mock ${paymentMethod} payment successful. Order #${order.id} for ${customer} is complete.`
        : paymentOutcome === 'FAILED'
          ? `Payment failed for order #${order.id}; reserved stock was released.`
          : `Payment timed out for order #${order.id}; the reservation expired and stock was released.`
      setStatus(`${outcomeMessage} Ref: ${payment.paymentReference}`)
      const productsResponse = await fetch(`${API_BASE}/products`)
      if (productsResponse.ok) setProducts(await productsResponse.json())
    } catch (error) {
      setStatus(error.message || 'Purchase failed. Please try again.')
    } finally {
      setProcessingPayment(false)
    }
  }

  const renderPage = () => {
    if (page === PAGE.PRODUCTS) {
      return (
        <section className="page-panel">
          <div className="panel-header">
            <h2>Product Manager</h2>
            <button type="button" className="secondary-button" onClick={() => setPage(PAGE.INVENTORY)}>
              Back to Inventory
            </button>
          </div>

          <form className="product-form" onSubmit={handleCreateProduct}>
            <input
              type="text"
              name="name"
              placeholder="Product name"
              value={newProduct.name}
              onChange={handleProductInputChange}
            />
            <input
              type="text"
              name="category"
              placeholder="Category"
              value={newProduct.category}
              onChange={handleProductInputChange}
            />
            <input
              type="number"
              name="price"
              min="0"
              step="0.01"
              placeholder="Price"
              value={newProduct.price}
              onChange={handleProductInputChange}
            />
            <input
              type="number"
              name="stock"
              min="0"
              step="1"
              placeholder="Stock"
              value={newProduct.stock}
              onChange={handleProductInputChange}
            />
            <button type="submit" className="primary-button">
              {editingProductId ? 'Save Changes' : 'Add Product'}
            </button>
            {editingProductId && (
              <button type="button" className="secondary-button" onClick={resetProductForm}>
                Cancel
              </button>
            )}
          </form>

          <div className="product-list">
            {products.map((product) => (
              <article key={product.id} className="product-card">
                <div>
                  <h3>{product.name}</h3>
                  <p>{product.category || 'General'}</p>
                  <p>Rs.{product.price.toLocaleString()}</p>
                  <small>Stock: {product.stock}</small>
                </div>

                <div className="product-actions">
                  <button type="button" className="edit-button" onClick={() => handleEditProduct(product)}>
                    Edit
                  </button>
                  <button type="button" className="delete-button" onClick={() => handleDeleteProduct(product.id)}>
                    Delete
                  </button>
                </div>
              </article>
            ))}
          </div>
        </section>
      )
    }

    if (page === PAGE.CART) {
      return (
        <section className="page-panel">
          <div className="panel-header">
            <h2>Cart & Purchase</h2>
            <button type="button" className="primary-button" onClick={() => setPage(PAGE.PAYMENT)} disabled={cart.length === 0}>
              Proceed to Payment
            </button>
          </div>

          <label className="customer-field">
            <span>Customer</span>
            <input
              type="text"
              value={customerName}
              onChange={(event) => setCustomerName(event.target.value)}
              placeholder="Walk-in Customer"
            />
          </label>

          {cart.length === 0 ? (
            <p className="empty-cart">Your cart is empty. Add products from the inventory page.</p>
          ) : (
            <div className="cart-list">
              {cart.map((item) => (
                <div key={item.id} className="cart-item">
                  <div>
                    <strong>{item.name}</strong>
                    <p>Rs.{item.price.toLocaleString()} each</p>
                  </div>

                  <div className="qty-controls">
                    <button type="button" onClick={() => updateQuantity(item.id, -1)}>
                      −
                    </button>
                    <span>{item.quantity}</span>
                    <button type="button" onClick={() => updateQuantity(item.id, 1)}>
                      +
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="purchase-summary">
            <span>Subtotal</span>
            <strong>Rs.{total.toLocaleString()}</strong>
          </div>
        </section>
      )
    }

    if (page === PAGE.PAYMENT) {
      return (
        <section className="page-panel">
          <div className="panel-header">
            <h2>Mock Payment</h2>
            <button type="button" className="secondary-button" onClick={() => setPage(PAGE.CART)}>
              Back to Cart
            </button>
          </div>

          <div className="payment-methods">
            {['Card', 'Cash', 'UPI'].map((method) => (
              <label key={method} className={`payment-option ${paymentMethod === method ? 'active' : ''}`}>
                <input
                  type="radio"
                  name="paymentMethod"
                  value={method}
                  checked={paymentMethod === method}
                  onChange={(event) => setPaymentMethod(event.target.value)}
                />
                <span>{method}</span>
              </label>
            ))}
          </div>

          <label className="customer-field">
            <span>Mock gateway outcome</span>
            <select value={paymentOutcome} onChange={(event) => setPaymentOutcome(event.target.value)}>
              <option value="SUCCESS">Success</option>
              <option value="FAILED">Failure (releases stock)</option>
              <option value="TIMEOUT">Timeout (expires reservation)</option>
            </select>
          </label>

          {paymentMethod === 'Card' && (
            <div className="payment-form">
              <input type="text" name="holderName" placeholder="Card holder name" value={paymentDetails.holderName} onChange={handlePaymentDetails} />
              <input type="text" name="cardNumber" placeholder="1234 5678 9012 3456" value={paymentDetails.cardNumber} onChange={handlePaymentDetails} />
              <div className="split-fields">
                <input type="text" name="expiry" placeholder="MM/YY" value={paymentDetails.expiry} onChange={handlePaymentDetails} />
                <input type="text" name="cvv" placeholder="CVV" value={paymentDetails.cvv} onChange={handlePaymentDetails} />
              </div>
            </div>
          )}

          {paymentMethod === 'Cash' && (
            <div className="payment-form">
              <input
                type="number"
                name="cashAmount"
                min="0"
                step="0.01"
                placeholder="Cash amount"
                value={paymentDetails.cashAmount}
                onChange={handlePaymentDetails}
              />
            </div>
          )}

          {paymentMethod === 'UPI' && (
            <div className="payment-form">
              <input type="text" name="holderName" placeholder="UPI ID (e.g. name@upi)" value={paymentDetails.holderName} onChange={handlePaymentDetails} />
            </div>
          )}

          <div className="purchase-summary">
            <span>Total</span>
            <strong>Rs.{total.toLocaleString()}</strong>
          </div>

          <button type="button" className="checkout-button" onClick={handleCompletePurchase} disabled={processingPayment}>
            {processingPayment ? 'Processing…' : 'Confirm Mock Payment'}
          </button>
        </section>
      )
    }

    return (
      <section className="page-panel">
        <div className="panel-header">
          <h2>Inventory</h2>
          <button type="button" className="secondary-button" onClick={() => setPage(PAGE.PRODUCTS)}>
            Manage Products
          </button>
        </div>

        <div className="inventory-grid">
          {products.map((product) => (
            <article key={product.id} className="inventory-card">
              <div className="inventory-header">
                <h3>{product.name}</h3>
                <span className="inventory-badge">{product.category || 'General'}</span>
              </div>
              <p>Price: Rs.{product.price.toLocaleString()}</p>
              <p>Stock: {product.stock}</p>
              <button type="button" className="primary-button" disabled={product.stock <= 0} onClick={() => addToCart(product)}>
                {product.stock > 0 ? 'Add to Cart' : 'Out of Stock'}
              </button>
            </article>
          ))}
        </div>
      </section>
    )
  }

  return (
    <div className="pos-app">
      <header className="topbar">
        <div>
          <h1>POS Order & Inventory</h1>
        </div>
        <nav className="nav-tabs" aria-label="POS pages">
          <button type="button" className={page === PAGE.INVENTORY ? 'nav-button active' : 'nav-button'} onClick={() => setPage(PAGE.INVENTORY)}>
            Inventory
          </button>
          <button type="button" className={page === PAGE.PRODUCTS ? 'nav-button active' : 'nav-button'} onClick={() => setPage(PAGE.PRODUCTS)}>
            Products
          </button>
          <button type="button" className={page === PAGE.CART ? 'nav-button active' : 'nav-button'} onClick={() => setPage(PAGE.CART)}>
            Cart
          </button>
          <button type="button" className={page === PAGE.PAYMENT ? 'nav-button active' : 'nav-button'} onClick={() => setPage(PAGE.PAYMENT)} disabled={cart.length === 0}>
            Purchase
          </button>
        </nav>
        <div className="cart-summary">
          <span>Cart Items</span>
          <strong>{cart.reduce((count, item) => count + item.quantity, 0)}</strong>
        </div>
      </header>

      <div className="status-row">
        {loading ? <span>Loading products...</span> : <span>{products.length} products available</span>}
        {status && <span className="status-banner">{status}</span>}
      </div>

      {renderPage()}
    </div>
  )
}

export default App
