# Task 1: Techloom POS

A point-of-sale and inventory management application with a React frontend and Spring Boot backend.

## Features

- Product inventory listing
- Create, edit, and delete products
- Shopping cart and quantity management
- Checkout with stock reservation
- Mock card and cash payment flows
- Simulated successful, failed, and timed-out payments
- Order cancellation and reservation release
- Reservation expiry cleanup
- Order and payment APIs

## Project Layout

```text
task-1/
├── backend/POS/       Spring Boot API
└── frontend/           React + Vite application
```

## Requirements

- Java 21 or newer
- Maven 3.9+ or the included Maven wrapper
- Node.js 20+
- npm
- MySQL 8+

The backend is configured for MySQL database `pos` on `localhost:3306`.
Create the database before starting the application:

```sql
CREATE DATABASE pos;
```

Update the credentials in `backend/POS/src/main/resources/application.properties` or provide equivalent Spring datasource environment configuration before running.

## Run the Backend

From the `task-1/backend/POS` directory:

```bash
./mvnw spring-boot:run
```

If the wrapper is not executable:

```bash
bash mvnw spring-boot:run
```

The API starts at `http://localhost:8080`.

## Run the Frontend

In a second terminal:

```bash
cd task-1/frontend
npm install
npm run dev
```

Open the Vite URL, normally `http://localhost:5173`.

The frontend currently calls `http://localhost:8080/api`.

## Test and Build

Backend:

```bash
cd task-1/backend/POS
./mvnw test
```

Frontend:

```bash
cd task-1/frontend
npm run build
npm run lint
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
| `POST` | `/orders/{id}/payments` | Process a mock payment |
| `POST` | `/orders/{id}/cancel` | Cancel an order and release reserved stock |
| `GET` | `/orders` | List orders |
| `GET` | `/orders/{id}` | Get one order |

Checkout accepts a customer name, optional cart ID, and product items. Payment accepts a payment method, reference, amount, and one of the simulated statuses supported by the backend.

## Troubleshooting

- `Communications link failure`: MySQL is not running or the credentials/database name are incorrect.
- `Unresolved compilation problem`: run `./mvnw clean test` from `task-1/backend/POS`, then retry.
- Frontend cannot load products: verify the backend is running on port `8080` and that the browser is opened from the Vite server.
