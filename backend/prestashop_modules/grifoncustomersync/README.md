# Grifon Customer Sync (PS 8.1.2) - Hashed Passwords

## Endpoint
POST /module/grifoncustomersync/sync

## Auth headers
X-Grifon-Timestamp: unix seconds
X-Grifon-Signature: base64(HMAC_SHA256("<timestamp>\n<body>", secret))

Secret: Modules -> Grifon Customer Sync -> Configure

## Password
Prefer:
- customer.password_hashed  (bcrypt like $2y$12$...)

Fallback (optional):
- customer.password (plain) -> module hashes inside PS

## Payload example
{
  "externalCustomerId": "cust_123",
  "customer": {
    "email": "user@example.com",
    "firstname": "Name",
    "lastname": "Surname",
    "password_hashed": "$2y$12$....",
    "company": "ACME SA"
  },
  "groups": { "default": 4, "list": [4, 7] },
  "addresses": [
    {
      "externalAddressId": "addr_1",
      "alias": "Billing",
      "address1": "Ermou 1",
      "postcode": "10563",
      "city": "Athens",
      "countryIso": "GR",
      "vat_number": "EL123456789"
    }
  ]
}
