package com.example.grifon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.grifon.ui.GrifonApp
import com.example.grifon.ui.theme.GrifonTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GrifonTheme {
                GrifonApp()
            }// typescript
            // file: grifon-gateway/src/server.ts (ή import αυτό το router στο υπάρχον server)
            import express from 'express';
            import axios from 'axios';

            const router = express.Router();

            const PRESTA_URL = process.env.PRESTASHOP_URL; // π.χ. https://shop.example.com
            const PRESTA_KEY = process.env.PRESTASHOP_API_KEY; // το API key του webservice

            router.get('/shops/:shopId/products', async (req, res) => {
              const { shopId } = req.params;
              if (!PRESTA_URL || !PRESTA_KEY) {
                return res.status(500).json({ error: 'missing_presta_config' });
              }

              try {
                const resp = await axios.get(`${PRESTA_URL}/api/products`, {
                  params: {
                    output_format: 'JSON',
                    'filter[id_shop]': `[${shopId}]` // PrestaShop filter syntax
                  },
                  auth: { username: PRESTA_KEY, password: '' },
                  timeout: 10000
                });

                // Επιστρέφουμε απευθείας τα δεδομένα από PrestaShop (μπορείς να τα φιλτράρεις/μυστικοποιήσεις εδώ)
                return res.json(resp.data);
              } catch (err: any) {
                console.error('PrestaShop fetch error:', err.message || err);
                return res.status(502).json({ error: 'upstream_error', details: err.message || err });
              }
            });

            export default router;// kotlin
            // file: app/src/main/java/com/example/grifon/MainActivity.kt (π.χ. add this function and call it from a coroutine)
            import android.os.Bundle
            import androidx.appcompat.app.AppCompatActivity
            import kotlinx.coroutines.*
            import okhttp3.OkHttpClient
            import okhttp3.Request
            import java.io.IOException

            class MainActivity : AppCompatActivity() {
                private val client = OkHttpClient()
                private val gatewayBase = "https://your.gateway.example.com" // βάλε το URL του gateway

                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    // ... your layout setup

                    // παράδειγμα κλήσης
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val json = fetchProducts(1) // shopId = 1
                            // process json on background then post to UI
                            withContext(Dispatchers.Main) {
                                // update UI
                            }
                        } catch (e: IOException) {
                            // handle error
                        }
                    }
                }

                private fun fetchProducts(shopId: Int): String {
                    val url = "$gatewayBase/shops/$shopId/products"
                    val request = Request.Builder().url(url).get().build()
                    client.newCall(request).execute().use { resp ->
                        if (!resp.isSuccessful) throw IOException("Unexpected code ${resp.code}")
                        return resp.body?.string() ?: ""
                    }
                }
            }
        }
    }
}
