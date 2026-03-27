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

## Price visibility for wholesale customers
If you want prices to be visible only to wholesale customers, send the wholesale PrestaShop group in `groups.default` and `groups.list`.

Then in PrestaShop back office:
- set `Show prices = Yes` on the wholesale group
- set `Show prices = No` on the retail/guest groups

## Module upgrades
The module now includes a proper upgrade script for `1.1.1`.

If the module is already installed and you deploy a newer version, PrestaShop should run:

- `upgrade/upgrade-1.1.1.php`

This upgrade creates the missing activity tables:

- `ps_grifon_favorite_product`
- `ps_grifon_recent_product`

If PrestaShop does not pick up the upgrade automatically, refresh the modules list in back office and upgrade the module from the Modules page.

## Activity table helper
To create the missing activity tables manually from a real PrestaShop install:

```bash
python3 modules/grifoncustomersync/scripts/ensure_activity_tables.py --ps-root=/var/www/html
```

To clear all favorites and recent product rows from PrestaShop:

```bash
python3 modules/grifoncustomersync/scripts/clear_activity_tables.py --ps-root=/var/www/html
```
