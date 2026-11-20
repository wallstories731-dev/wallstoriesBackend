# Arusuvai Junction

E-commerce platform for authentic South-Indian sweets & savouries.

- **Backend** — Java 21, embedded Tomcat 10.1 (Jakarta EE 10), HikariCP, Neon Postgres
- **Frontend** — React + Vite, axios, React Query, React Router
- **Payments** — Razorpay Standard Checkout (2-phase: order → signature verify)
- **Storage** — Cloudflare R2 for product images
- **Mail** — Zoho SMTP for email verification, order confirmations, password resets
- **Search** — Postgres FTS (GIN index) with Elasticsearch optional

## Run locally

```bash
cp .env.example .env       # fill in DB, Razorpay, SMTP, R2 creds
docker compose up --build  # frontend → :5173, backend → :8080/wallstories
```

Without Docker:

```bash
mvn -DskipTests package
java -jar target/ecommerce.jar &
cd frontend/ecommerce && npm install && npm run dev
```

## Database prerequisites and optional search

Startup requires an existing backend base schema. It checks that `products`,
`product_variants`, `cart_items`, and `order_items` are visible in the configured
connection search path, then runs the transactional variant migration before
opening the HTTP listener. It does **not** create missing tables.

`src/main/resources/db_migrations.sql` contains incremental updates only; the
original backend base-schema script is not included here. Restore the schema
from your backend database backup before applying those updates. The frontend's
Supabase migrations target a different schema and cannot initialize this backend.
For missing-relation errors, also check `DB_URL` and use its `currentSchema` JDBC
parameter if the tables live outside the default search path.

Elasticsearch is optional. Set `ELASTICSEARCH_ENABLED=false` in `.env` or the
process environment to use PostgreSQL search without connecting to ES. To enable
it, set the flag to `true` and configure `ELASTICSEARCH_URL`; the existing default
when the flag is unset remains enabled for compatibility. Reindexing skips all
catalog reads if index setup fails.

Run unit tests with `mvn test`. To include the PostgreSQL integration test, set
`TEST_DATABASE_URL` to a disposable PostgreSQL JDBC URL (including credentials).
The integration test uses session-local temporary tables only.

## Deploy to Render

Push to GitHub, then in Render: **New → Blueprint → pick this repo**. The
included [`render.yaml`](render.yaml) provisions both services. Fill in the
`sync: false` env vars in the dashboard.

## Public product and review images (R2)

`R2_PUBLIC_BASE_URL` must be the bucket's **Public Development URL** from
Cloudflare R2 -> bucket -> Settings, or a connected custom domain. The
`https://<account-id>.r2.cloudflarestorage.com` endpoint is only for authenticated
storage operations; it does not serve public images. The backend rejects it
instead of persisting image URLs that the browser cannot load.

For this storefront, the verified public bucket URL is:

```dotenv
R2_PUBLIC_BASE_URL=https://pub-9769fbcd0cd54caf8433f76b6dc9a37b.r2.dev
```

Set this in the backend's deployment environment, not a frontend `VITE_*`
variable. Public access must remain enabled for this bucket; use a bucket
containing only assets intended to be publicly accessible. A custom domain is
recommended for production instead of the rate-limited development URL.

Changing the environment variable fixes **new** uploads, but existing product
and review rows still contain their old URLs. Run
[`repair_r2_image_urls.sql`](repair_r2_image_urls.sql) against the backend
database to repair URLs for this R2 account. It is transactional and rerunnable,
preserves object keys, product links and primary flags, and leaves external
images unchanged. Restart/redeploy the backend afterward to clear catalog
caches and refresh the search index, then reload the storefront.

Run the URL-generation regressions with
`mvn -Dtest=R2StorageServiceTest test`; they use a mocked storage client and
perform no live uploads or database writes.
