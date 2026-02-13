# grifon-gateway

Secure proxy/gateway between an Android app and a PrestaShop multishop installation. The gateway exposes a clean REST JSON API and keeps the PrestaShop API key private.

## Features

- Shop-aware routing with `shopId` (1 or 4, default 4).
- Active-only filtering for categories, products, and CMS pages.
- Price visibility enforced by customer/group rules.
- XML fallback parsing when upstream does not return JSON.
- Caching (in-memory or Redis) with TTLs.
- Security middleware: helmet, CORS, rate limiting, request IDs, structured logs.

## Setup

```bash
cd grifon-gateway
cp .env.example .env
npm install
npm run dev
```

### Build & Run

```bash
npm run build
npm start
```

## Environment Variables

See `.env.example` for full list.

Important for registration sync through the PrestaShop module:

- `GRIFON_CUSTOMER_SYNC_SECRET` (**required**): shared secret configured in the `grifoncustomersync` module.
- `GRIFON_CUSTOMER_SYNC_PATH`: module endpoint path (default: `/module/grifoncustomersync/sync`).

## API Endpoints

### Health

```bash
curl http://localhost:3000/health
```

### Shops

```bash
curl http://localhost:3000/v1/shops
```

### Register (wholesale approval pending)

```bash
curl -X POST http://localhost:3000/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "secret123",
    "socialTitle": "mr",
    "firstName": "Γιώργος",
    "lastName": "Παπαδόπουλος",
    "countryIso": "GR",
    "street": "Πανεπιστημίου 1",
    "city": "Αθήνα",
    "postalCode": "10564",
    "phone": "+30 2100000000",
    "company": "Grifon SA",
    "vatNumber": "EL123456789",
    "iban": "GR1601101250000000012300695",
    "customerDataPrivacyAccepted": true,
    "newsletter": true,
    "termsAndPrivacyAccepted": true,
    "partnerOffers": false
  }'
```

### Categories (active only + tree)

```bash
curl "http://localhost:3000/v1/categories?shopId=4&lang=1&page=1&pageSize=50"
```

### Products in Category (active only)

```bash
curl "http://localhost:3000/v1/categories/12/products?shopId=4&lang=1&page=1&pageSize=20&sort=[id_DESC]"
```

### Product Details (active only, optional price access)

```bash
curl "http://localhost:3000/v1/products/100?shopId=4&lang=1"
curl "http://localhost:3000/v1/products/100?shopId=4&lang=1&customerId=123"
```

### CMS Pages (active only)

```bash
curl "http://localhost:3000/v1/pages?shopId=4&lang=1"
```

### Customer Groups (show prices + members count)

```bash
curl "http://localhost:3000/v1/customer-groups?shopId=4&lang=1"
```

### Price Access Check

```bash
curl "http://localhost:3000/v1/customers/123/price-access?shopId=4"
```

## Notes

- Prices are only returned when a customer is active and their default group has `show_prices=1`.
- When prices are not allowed, price fields are set to `null`.
- Product image URLs are constructed using the selected shop domain.
- `/auth/register` uses the `grifoncustomersync` PrestaShop module endpoint to create/update customer + address records.
- `/auth/register` hashes the submitted password with bcrypt before syncing it to the module endpoint.
- `/auth/register` creates customers with `PENDING_WHOLESALE_APPROVAL` status (inactive or in a pending group).

## Troubleshooting

### `UPSTREAM_ERROR: Failed to fetch upstream data: getaddrinfo ENOTFOUND ...`

This error means the gateway process cannot resolve the hostname in your configured shop URL.

Quick checks:

- Verify `SHOP_GR_BASE_URL`, `SHOP_SE_BASE_URL`, and `PRESTASHOP_BASE_URL` in `.env` are valid URLs.
- Make sure the hostname exists in DNS (or in your Docker/network aliases if running locally).
- For Docker setups, you can route through an internal reverse-proxy host such as `https://replica/grifon.gr/api`.
- If you need to keep the hostname as `replica` but resolve it locally to another host/IP, set `REPLICA_HOSTNAME` (default: `replica`) and `REPLICA_RESOLVE_TO` (no default; set it to your own host/IP like `127.0.0.1` or a Docker alias such as `prestashop-demo`).
- For non-Docker/local runs, you can still use directly resolvable hosts in `SHOP_GR_BASE_URL`, `SHOP_SE_BASE_URL`, and `PRESTASHOP_BASE_URL`.
- The API now normalizes this case as `Unable to resolve upstream hostname: <host>` to make root cause clearer.
- Restart the gateway after changing environment variables.
