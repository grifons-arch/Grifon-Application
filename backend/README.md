# Grifon PrestaShop Proxy Webservice

Proxy REST API για Android που μεσολαβεί προς το PrestaShop Webservice API του https://grifon.gr/.

## Setup

```bash
cd backend
npm install
```

## Περιβάλλον (.env)

Δημιουργήστε ένα `.env` αρχείο στο `backend/` (δείτε `.env.example`).

| Μεταβλητή | Περιγραφή | Default |
| --- | --- | --- |
| `PORT` | Port του server | `3000` |
| `PRESTASHOP_BASE_URL` | Base URL του PrestaShop API | `https://grifon.gr/api` |
| `PRESTASHOP_API_KEY` | API key (username για Basic Auth) | - |
| `CACHE_TTL_SECONDS` | TTL για in-memory cache | `60` |
| `ALLOWED_ORIGINS` | CORS allowlist (comma separated) | `*` |

## Εκκίνηση

```bash
npm run dev
```

Το server τρέχει στο `http://localhost:3000`.

## Endpoints

### GET `/health`
```json
{ "ok": true }
```

### GET `/categories`
Query params:
- `page` (default `1`)
- `pageSize` (default `20`)
- `display` (default `id,name`)

### GET `/products`
Query params:
- `page` (default `1`)
- `pageSize` (default `20`)
- `search` (optional)
- `categoryId` (optional)
- `sort` (π.χ. `id_desc`, `price_asc`)
- `fields` (π.χ. `id,name,price,reference`)

### GET `/products/:id`
Επιστρέφει λεπτομέρειες προϊόντος + εικόνες + stock + manufacturer (αν υπάρχει).

### GET `/products/:id/images`
Επιστρέφει `images` array με URLs βασισμένα στο
`https://grifon.gr/api/images/products/{productId}/{imageId}`.

### GET `/search`
Query params:
- `q` (required)
- `page`, `pageSize`

### GET `/stock/:productId`
Επιστρέφει ποσότητα και out of stock behavior.

## Παραδείγματα curl

```bash
curl "http://localhost:3000/health"
```

```bash
curl "http://localhost:3000/categories?page=1&pageSize=10&display=id,name"
```

```bash
curl "http://localhost:3000/products?page=1&pageSize=10&fields=id,name,price&sort=price_asc"
```

```bash
curl "http://localhost:3000/products/1"
```

```bash
curl "http://localhost:3000/search?q=chair&page=1&pageSize=5"
```

## Error format

```json
{
  "error": {
    "code": 502,
    "message": "Upstream auth failed"
  }
}
```

## Tests

```bash
npm test
```
