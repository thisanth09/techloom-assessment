# Task 2: Online Store

A customer-facing e-commerce application covering product discovery, cart checkout, mock payments, inventory reservation, cancellation, refunds, and order history.

## Features

- Product listing with text search
- Category, price-range, and availability filters
- Product details view
- Add-to-cart and quantity management
- Checkout with pessimistic stock reservation
- Mock payment success, failure, and timeout outcomes
- Duplicate cart and payment-reference protection
- Automatic release of reserved inventory after failure, timeout, cancellation, or expiry
- Customer-scoped order history
- Cancellation and simulated refund tracking

## Project Layout

```text
task-2/
├── backend/E-Commerce/  Spring Boot API
├── frontend/            React + Vite application
└── docker-compose.yml   MySQL, backend, and frontend stack
```

## Requirements

For local development:

- Java 21 or newer
- Maven 3.9+ or the included Maven wrapper
- Node.js 20+
- npm

The default backend profile uses a file-backed H2 database, so MySQL is not required for local development.

## Run the Backend Locally

From the repository root:

```bash
cd task-2/backend/E-Commerce
./mvnw spring-boot:run
```

If the wrapper is not executable:

```bash
bash mvnw spring-boot:run
```

The API starts at `http://localhost:8081` and uses `data/ecommerce.mv.db` for local persistence.

## Run the Frontend Locally

In a second terminal:

```bash
cd task-2/frontend
npm install
npm run dev
```

Open the Vite URL, normally `http://localhost:5173`.

To point the frontend at another backend:

```bash
VITE_API_BASE_URL=https://your-backend.example.com npm run dev
```

## Run with Docker Compose

Docker Compose starts MySQL, the Spring Boot API, and the Nginx-served frontend:

```bash
cd task-2
docker compose up --build
```

Open:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8081`

The Compose file uses example credentials (`change-me`). Change them before using the stack outside local development. Configure the backend CORS origin with `APP_CORS_ALLOWED_ORIGINS` and the frontend API URL with the `VITE_API_BASE_URL` build argument.

## Test and Build

Backend:

```bash
cd task-2/backend/E-Commerce
./mvnw clean test
```

Frontend:

```bash
cd task-2/frontend
npm run build
npm run lint
```

Validate Compose without starting containers:

```bash
cd task-2
docker compose config
```

## API Endpoints

All endpoints use the `/api` prefix.

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/products` | List products |
| `GET` | `/products/{id}` | Get one product |
| `POST` | `/products` | Create a product |
| `PUT` | `/products/{id}` | Update a product |
| `DELETE` | `/products/{id}` | Delete a product |
| `POST` | `/orders/checkout` | Reserve stock and create an order |
| `POST` | `/orders/{id}/payment` | Submit a mock payment outcome |
| `POST` | `/orders/{id}/cancel` | Cancel an order and release stock |
| `GET` | `/orders?customerName=...` | Get order history for a customer |
| `GET` | `/orders/{id}` | Get one order |

Payment status values are `SUCCESS`, `FAILED`, and `TIMEOUT`. A checkout request should include a stable `cartId` when retrying the same checkout. Payment references must be unique.

## Troubleshooting

- `Connection refused` to MySQL: use the default local H2 profile or start the Compose stack.
- Port `8081` already in use: stop the existing process or change `server.port`.
- Frontend shows fallback products: verify the backend is running and check `VITE_API_BASE_URL`.
- The local H2 database is generated under `backend/E-Commerce/data/` and is ignored by source control.

## Deployment Note

The project includes container definitions and Compose configuration, but a public URL still requires deployment to a hosting provider and a configured database. Do not expose the example database password in a public deployment.
