# Task 2 Frontend

This React + Vite frontend is the customer storefront for Task 2. It expects the Spring Boot backend at `http://localhost:8081/api` by default.

## Run

```bash
npm install
npm run dev
```

Set `VITE_API_BASE_URL` when the backend runs elsewhere:

```bash
VITE_API_BASE_URL=https://your-backend.example.com npm run dev
```

Use `npm run build` for a production build and `npm run lint` for linting.

See [../README.md](../README.md) for the complete application setup, Docker Compose instructions, and API documentation.
