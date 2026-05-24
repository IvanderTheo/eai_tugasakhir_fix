# OpenAPI – GrowBusiness API Gateway

## File

- **`growbusiness-gateway-api.yaml`** – spesifikasi lengkap semua endpoint via gateway (`http://localhost:8000`), dengan contoh JSON sesuai `API_TESTING_GUIDE.md`.

## Swagger UI (gateway)

> **Penting:** Buka lewat `http://localhost:8000`, **bukan** membuka file YAML/HTML langsung (`file://`) — itu menyebabkan error *Failed to fetch / URL scheme must be http or https*.

1. Jalankan semua service + **api-gateway** (port 8000).
2. Buka: **http://localhost:8000/swagger-ui.html** (spec di-load dari `/v3/api-docs`)
3. `POST /api/auth/login` → body JSON:
   ```json
   { "username": "admin", "password": "admin123" }
   ```
4. **Authorize** → `Bearer <token>`
5. Test endpoint lain (body JSON sudah terisi contoh).

## Postman / Insomnia

1. **Import** → pilih `growbusiness-gateway-api.yaml`
2. Buat environment: `baseUrl` = `http://localhost:8000`
3. Setelah login, simpan `token` ke variable dan header `Authorization: Bearer {{token}}`

## Raw spec URL

http://localhost:8000/openapi/growbusiness-gateway-api.yaml
