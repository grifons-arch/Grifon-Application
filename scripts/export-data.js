const axios = require('axios');
const fs = require('fs');
const path = require('path');

// Ρυθμίσεις από το .env σου
const API_KEY = "G3FTY8FNVRSXAD8UWDHJDA9Y42W83FPX";
const SHOPS = [
    { name: 'gr', url: "https://replica.grifon.gr/api" },
    { name: 'se', url: "https://replica.grifon.se/api" }
];

async function fetchData(baseUrl, resource) {
    console.log(`Κατέβασμα ${resource} από ${baseUrl}...`);
    try {
        const response = await axios.get(`${baseUrl}/${resource}`, {
            params: {
                ws_key: API_KEY,
                output_format: "JSON",
                display: "full",
                limit: "0,500"
            }
        });
        return response.data;
    } catch (error) {
        console.error(`Σφάλμα στο ${resource} (${baseUrl}): ${error.message}`);
        return null;
    }
}

async function main() {
    const exportDir = path.join(__dirname, '../exports');
    if (!fs.existsSync(exportDir)) {
        fs.mkdirSync(exportDir);
    }

    for (const shop of SHOPS) {
        const categories = await fetchData(shop.url, 'categories');
        const products = await fetchData(shop.url, 'products');

        if (categories) {
            fs.writeFileSync(
                path.join(exportDir, `categories_${shop.name}.json`),
                JSON.stringify(categories, null, 2)
            );
        }
        if (products) {
            fs.writeFileSync(
                path.join(exportDir, `products_${shop.name}.json`),
                JSON.stringify(products, null, 2)
            );
        }
    }
    console.log("\nΗ εξαγωγή ολοκληρώθηκε! Τα αρχεία βρίσκονται στον φάκελο 'exports'.");
}

main();
