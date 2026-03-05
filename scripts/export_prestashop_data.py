import requests
import json
import os

# Ρυθμίσεις από το .env σου
BASE_URL_GR = "https://replica.grifon.gr/api"
BASE_URL_SE = "https://replica.grifon.se/api"
API_KEY = "G3FTY8FNVRSXAD8UWDHJDA9Y42W83FPX"

def fetch_data(base_url, resource):
    print(f"Κατέβασμα {resource} από {base_url}...")
    url = f"{base_url}/{resource}"
    params = {
        "ws_key": API_KEY,
        "output_format": "JSON",
        "display": "full",
        "limit": "0,500" # Κατεβάζει τα πρώτα 500
    }
    try:
        response = requests.get(url, params=params, timeout=20)
        response.raise_for_status()
        return response.json()
    except Exception as e:
        print(f"Σφάλμα στο {resource}: {e}")
        return None

def save_to_json(data, filename):
    if data:
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f"Το αρχείο {filename} δημιουργήθηκε με επιτυχία!")

def main():
    # Δημιουργία φακέλου exports αν δεν υπάρχει
    if not os.path.exists("exports"):
        os.makedirs("exports")

    # Κατέβασμα για το Ελληνικό κατάστημα (Shop 4)
    categories_gr = fetch_data(BASE_URL_GR, "categories")
    products_gr = fetch_data(BASE_URL_GR, "products")

    save_to_json(categories_gr, "exports/categories_gr.json")
    save_to_json(products_gr, "exports/products_gr.json")

    # Κατέβασμα για το Σουηδικό κατάστημα (Shop 1)
    categories_se = fetch_data(BASE_URL_SE, "categories")
    products_se = fetch_data(BASE_URL_SE, "products")

    save_to_json(categories_se, "exports/categories_se.json")
    save_to_json(products_se, "exports/products_se.json")

if __name__ == "__main__":
    main()
