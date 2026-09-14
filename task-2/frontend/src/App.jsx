import { useEffect, useMemo, useState } from 'react'
import './App.css'

const API_BASE = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'}/api`

const fallbackProducts = [
  {
    id: 1,
    name: 'Wireless Headphones',
    category: 'Audio',
    description: 'Noise-cancelling over-ear headphones with deep bass and all-day comfort.',
    price: 9500,
    stock: 12,
    imageUrl: 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=900&q=80',
    active: true,
  },
  {
    id: 2,
    name: 'Smart Watch',
    category: 'Wearables',
    description: 'Track health, messages, workouts, and notifications from your wrist.',
    price: 18500,
    stock: 9,
    imageUrl: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80',
    active: true,
  },
  {
    id: 3,
    name: 'Gaming Mouse',
    category: 'Accessories',
    description: 'RGB precision mouse built for fast reaction times and smooth control.',
    price: 4200,
    stock: 18,
    imageUrl: 'https://images.unsplash.com/photo-1527814050087-3d134e7f0f8d?auto=format&fit=crop&w=900&q=80',
    active: true,
  },
  {
    id: 4,
    name: 'Bluetooth Speaker',
    category: 'Audio',
    description: 'Portable speaker with crisp sound, deep bass, and waterproof design.',
    price: 7600,
    stock: 14,
    imageUrl: 'https://images.unsplash.com/photo-1518444065439-e933c06ce9cd?auto=format&fit=crop&w=900&q=80',
    active: true,
  },
  {
    id: 5,
    name: 'Laptop Stand',
    category: 'Office',
    description: 'Adjustable stand to improve posture and airflow during long work sessions.',
    price: 3200,
    stock: 25,
    imageUrl: 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=900&q=80',
    active: true,
  },
  {
    id: 6,
    name: 'USB-C Cable',
    category: 'Accessories',
    description: 'Reliable charging and data cable for phones, tablets, and laptops.',
    price: 1200,
    stock: 35,
    imageUrl: 'https://images.unsplash.com/photo-1583394838336-acd977736f90?auto=format&fit=crop&w=900&q=80',
    active: true,
  },
]

const formatMoney = (value) => `Rs.${Number(value || 0).toLocaleString()}`

function App() {
  const [products, setProducts] = useState(fallbackProducts)
  const [selectedCategory, setSelectedCategory] = useState('All')
  const [searchText, setSearchText] = useState('')
  const [minPrice, setMinPrice] = useState('')
  const [maxPrice, setMaxPrice] = useState('')
  const [inStockOnly, setInStockOnly] = useState(false)
  const [selectedProduct, setSelectedProduct] = useState(null)
  const [cart, setCart] = useState([])
  const [cartId, setCartId] = useState(() => crypto.randomUUID())
  const [status, setStatus] = useState('')
  const [loading, setLoading] = useState(true)
  const [customerName, setCustomerName] = useState('Online Customer')
  const [paymentMethod, setPaymentMethod] = useState('CARD')
  const [paymentOutcome, setPaymentOutcome] = useState('SUCCESS')
  const [orders, setOrders] = useState([])
  const [processingCheckout, setProcessingCheckout] = useState(false)

  useEffect(() => {
    const loadProducts = async () => {
      try {
        const response = await fetch(`${API_BASE}/products`)

        if (!response.ok) {
          throw new Error('Unable to fetch products')
        }

        const data = await response.json()
        setProducts(data.length ? data : fallbackProducts)
      } catch (error) {
        setProducts(fallbackProducts)
      } finally {
        setLoading(false)
      }
    }

    loadProducts()
    loadOrders()
  }, [])

  const loadOrders = async (name = customerName) => {
    try {
      const query = name.trim() ? `?customerName=${encodeURIComponent(name.trim())}` : ''
      const response = await fetch(`${API_BASE}/orders${query}`)
      if (response.ok) {
        const data = await response.json()
        setOrders(data)
      }
    } catch (error) {
      setOrders([])
    }
  }

  const categories = ['All', ...new Set(products.map((product) => product.category).filter(Boolean))]

  const filteredProducts = products.filter((product) => {
    const matchesCategory = selectedCategory === 'All' || product.category === selectedCategory
    const searchQuery = searchText.trim().toLowerCase()
    const matchesSearch = !searchQuery ||
      product.name.toLowerCase().includes(searchQuery) ||
      product.description.toLowerCase().includes(searchQuery)
    const matchesMinPrice = !minPrice || product.price >= Number(minPrice)
    const matchesMaxPrice = !maxPrice || product.price <= Number(maxPrice)
    const matchesAvailability = !inStockOnly || product.stock > 0
    return matchesCategory && matchesSearch && matchesMinPrice && matchesMaxPrice && matchesAvailability && product.active !== false
  })

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

  const total = useMemo(
    () => cart.reduce((sum, item) => sum + item.price * item.quantity, 0),
    [cart],
  )

  const handleCheckout = async () => {
    if (cart.length === 0 || processingCheckout) {
      setStatus('Your cart is empty.')
      return
    }

    setProcessingCheckout(true)
    try {
      const payload = {
        customerName: customerName || 'Online Customer',
        paymentMethod: paymentMethod,
        cartId,
        items: cart.map((item) => ({
          productId: item.id,
          quantity: item.quantity,
        })),
      }

      const response = await fetch(`${API_BASE}/orders/checkout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })

      const result = await response.json()

      if (!response.ok) {
        throw new Error(result.message || 'Checkout failed')
      }

      const paymentResponse = await fetch(`${API_BASE}/orders/${result.id}/payment`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: paymentOutcome, paymentReference: `mock-${result.id}-${Date.now()}` }),
      })

      if (!paymentResponse.ok) {
        const errorText = await paymentResponse.text()
        throw new Error(errorText || 'Payment processing failed')
      }

      const paymentResult = await paymentResponse.json()
      if (paymentOutcome === 'SUCCESS') {
        // Give instant feedback; the following API refresh remains the source of truth.
        setProducts((currentProducts) => currentProducts.map((product) => {
          const purchased = cart.find((item) => item.id === product.id)
          return purchased ? { ...product, stock: Math.max(0, product.stock - purchased.quantity) } : product
        }))
      }
      setCart([])
      setCartId(crypto.randomUUID())
      const paymentStatusText = paymentOutcome === 'SUCCESS'
        ? `paid successfully via ${paymentResult.paymentMethod}.`
        : paymentOutcome === 'FAILED'
          ? `payment failed and stock was released.`
          : `payment timed out and stock was released.`

      setStatus(`Order #${paymentResult.id} ${paymentStatusText}`)
      const productsResponse = await fetch(`${API_BASE}/products`)
      if (productsResponse.ok) {
        setProducts(await productsResponse.json())
      }
      await loadOrders()
    } catch (error) {
      const message = error.message || 'Checkout failed. Please check the backend and try again.'
      // A cancelled/expired checkout cannot be reused. Keep the shopper's items,
      // but assign the cart a fresh id so they can submit a new order.
      if (message.includes('already submitted')) {
        setCart([])
        setCartId(crypto.randomUUID())
        setStatus('Your previous checkout is closed. A new empty cart is ready—add products and place your new order.')
      } else {
        setStatus(message)
      }
    } finally {
      setProcessingCheckout(false)
    }
  }

  const handleCancelOrder = async (id) => {
    try {
      const response = await fetch(`${API_BASE}/orders/${id}/cancel?reason=Customer%20cancelled`, {
        method: 'POST',
      })

      if (!response.ok) {
        throw new Error('Unable to cancel order')
      }

      await loadOrders()
      setStatus('Order successfully cancelled and stock released.')
    } catch (error) {
      setStatus(error.message || 'Cancellation failed.')
    }
  }

  const startNewOrder = () => {
    setCart([])
    setCartId(crypto.randomUUID())
    setStatus('New order started. Add the products you want to buy.')
  }

  return (
    <div className="shop-app">
      <header className="topbar">
        <div>
          <h1>Marketplace</h1>
        </div>
        <div className="cart-pill">
          <span>Cart</span>
          <strong>{cart.reduce((count, item) => count + item.quantity, 0)}</strong>
        </div>
      </header>

      <main className="content-grid">
        <section className="panel product-panel">
          <div className="panel-header">
            <h2>Products</h2>
            <span>{loading ? 'Loading...' : `${filteredProducts.length} items`}</span>
          </div>

          <div className="toolbar">
            <input
              type="text"
              value={searchText}
              onChange={(event) => setSearchText(event.target.value)}
              placeholder="Search products"
            />
            <select value={selectedCategory} onChange={(event) => setSelectedCategory(event.target.value)}>
              {categories.map((category) => (
                <option key={category} value={category}>{category}</option>
              ))}
            </select>
            <input type="number" min="0" value={minPrice} onChange={(event) => setMinPrice(event.target.value)} placeholder="Min price" />
            <input type="number" min="0" value={maxPrice} onChange={(event) => setMaxPrice(event.target.value)} placeholder="Max price" />
            <label className="stock-filter"><input type="checkbox" checked={inStockOnly} onChange={(event) => setInStockOnly(event.target.checked)} /> In stock</label>
          </div>

          <div className="product-grid">
            {filteredProducts.map((product) => (
              <article key={product.id} className="product-card">
                <div className="product-image-wrap">
                  <img src={product.imageUrl || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=900&q=80'} alt={product.name} />
                </div>

                <div className="product-meta">
                  <span className="badge">{product.category}</span>
                  <h3>{product.name}</h3>
                  <p className="description">{product.description}</p>
                  <div className="product-line">
                    <strong>{formatMoney(product.price)}</strong>
                    <small>Stock: {product.stock}</small>
                  </div>
                </div>

                <div className="product-actions">
                  <button type="button" className="secondary" onClick={() => setSelectedProduct(product)}>View details</button>
                  <button type="button" disabled={product.stock <= 0} onClick={() => addToCart(product)}>{product.stock > 0 ? 'Add to cart' : 'Out of stock'}</button>
                </div>
              </article>
            ))}
          </div>
        </section>

        <aside className="panel cart-panel">
          <div className="panel-header">
            <h2>Your Cart</h2>
            <div className="cart-header-actions">
              {cart.length > 0 && <button type="button" className="new-order-button" onClick={startNewOrder}>Cancel cart & start new order</button>}
              <span>{formatMoney(total)}</span>
            </div>
          </div>

          <div className="checkout-form">
            <label>
              Customer name
              <input value={customerName} onChange={(event) => setCustomerName(event.target.value)} />
            </label>
            <label>
              Payment method
              <select value={paymentMethod} onChange={(event) => setPaymentMethod(event.target.value)}>
                <option value="CARD">Card</option>
                <option value="CASH">Cash</option>
                <option value="UPI">UPI</option>
              </select>
            </label>
            <label>
              Mock payment result
              <select value={paymentOutcome} onChange={(event) => setPaymentOutcome(event.target.value)}>
                <option value="SUCCESS">Successful payment</option>
                <option value="FAILED">Payment failed</option>
                <option value="TIMEOUT">Payment timeout</option>
              </select>
            </label>
          </div>

          {cart.length === 0 ? (
            <p className="empty">Your cart is empty.</p>
          ) : (
            <div className="cart-list">
              {cart.map((item) => (
                <div key={item.id} className="cart-item">
                  <div>
                    <strong>{item.name}</strong>
                    <p>{formatMoney(item.price)} each</p>
                  </div>

                  <div className="qty-box">
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

          <button type="button" className="checkout" disabled={cart.length === 0 || processingCheckout} onClick={handleCheckout}>
            {processingCheckout ? 'Processing payment…' : 'Complete purchase'}
          </button>

          {status && <div className="status-banner">{status}</div>}

          <div className="orders-panel">
            <h3>Recent Orders</h3>
            {orders.length === 0 ? <p className="empty">No orders yet.</p> : (
              <div className="orders-list">
                {orders.map((order) => (
                  <div key={order.id} className="order-row">
                    <div>
                      <strong>Order #{order.id} · {formatMoney(order.totalAmount)}</strong>
                      <p>{order.customerName} · {order.createdAt ? new Date(order.createdAt).toLocaleString() : 'Just now'}</p>
                      <p className="order-items">
                        {(order.items || []).map((item) => `${item.productName} × ${item.quantity}`).join(', ') || 'Order item details unavailable'}
                      </p>
                      {order.paymentReference && <small>Payment: {order.paymentReference}</small>}
                    </div>
                    <div>
                      <span>{order.status}</span>
                      {(order.status === 'RESERVED' || order.status === 'PAID') && <button type="button" onClick={() => handleCancelOrder(order.id)}>{order.status === 'PAID' ? 'Refund' : 'Cancel'}</button>}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </aside>
      </main>

      {selectedProduct && (
        <div className="modal-backdrop" onClick={() => setSelectedProduct(null)}>
          <div className="modal-card" onClick={(event) => event.stopPropagation()}>
            <button type="button" className="close-button" onClick={() => setSelectedProduct(null)}>✕</button>
            <img src={selectedProduct.imageUrl || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=900&q=80'} alt={selectedProduct.name} />
            <div className="modal-copy">
              <span className="badge">{selectedProduct.category}</span>
              <h2>{selectedProduct.name}</h2>
              <p>{selectedProduct.description}</p>
              <div className="modal-meta">
                <strong>{formatMoney(selectedProduct.price)}</strong>
                <span>Available: {selectedProduct.stock}</span>
              </div>
              <button type="button" onClick={() => { addToCart(selectedProduct); setSelectedProduct(null) }}>Add to cart</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default App
