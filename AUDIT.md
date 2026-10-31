# Technical Audit 

This audit documents technical, architectural, and security observations for `https://cynbit.in` and `https://www.cynbit.in`. All findings were gathered using strictly **passive methods** (browser DevTools, DOM inspection, and non-intrusive single-request `curl` checks).

---

## Executive Summary Matrix

| ID     | Focus Area | Finding Summary | Severity | Key Evidence |
|:-------| :--- | :--- | :--- | :--- |
| **01** | Cloud Architecture | Dynamic compute and form handling run in US-East (`iad1`) instead of Mumbai (`bom1`) | **Medium** | `x-vercel-id: bom1::iad1::b4mj7-...` |
| **02** | Security Headers | Missing core defense headers: `Content-Security-Policy`, `X-Frame-Options`, `X-Content-Type-Options` | **Medium** | `curl -sI https://www.cynbit.in/contact` |
| **03** | Security Hygiene | Header inconsistency across routes (`x-powered-by` leak, wildcard CORS on home, incomplete HSTS) | **Low** | `curl` response comparisons across `/` and `/contact` |
| **04** | API Architecture | Contact form uses a Server Action returning a bare boolean (`{"success":true}`) while re-sending the whole page tree | **Medium** | `POST /contact` RSC flight stream |
| **05** | Form Validation | Missing length boundaries on form inputs and schema divergence between `/contact` and `/services` | **Medium** | DevTools Elements inspection on `<input>` tags |
| **06** | Canonical & SEO | Canonical domain split: sitemap and apex domain force 307 temporary redirects to `www` | **Medium** | `curl -s https://www.cynbit.in/sitemap.xml` |
| **07** | Frontend Loading | Heavy cold load on mobile: 10 eager route prefetches and render-blocking external Google Fonts | **Low–Med** | DevTools Network tab on `/services` |
| **08** | Privacy & Tracking | First-party analytics cookies (`_ga`, `_gcl_au`) and ad conversion pings fire immediately on page load | **Low–Med** | Application $\rightarrow$ Cookies & Network Fetch/XHR |
| **09** | Route Exposure | `robots.txt` lists private endpoints (`/admin/`, `/portal/`, `/api/`) to the public | **Info** | `robots.txt` body |

---

## MORE DETAILS ABOUT THE FINDINGS

### 1: Dynamic Compute Runs in US-East (`iad1`) Instead of Mumbai (`bom1`)

#### Observation & Evidence
Static assets and cached HTML pages serve directly from Vercel's Mumbai edge node (`bom1`):
```http
x-vercel-id: bom1::hqhpb-1793426907341-b3d9e711e110
x-vercel-cache: HIT
```

However, whenever a request requires server compute,such as dynamic sitemap generation or handling the contact form action,the routing header changes:
```http
x-vercel-id: bom1::iad1::b4mj7-1793426907614-1ceb5bbeb263
```
This indicates the request lands in Mumbai (`bom1`), routes across the Atlantic to Vercel's default US-East compute region in Washington D.C. (`iad1`), and then travels back.

#### Impact
Users based in India pay an avoidable **~200–250 ms round-trip latency penalty** on every form submission and dynamic route execution.

#### Remediation
Pin the serverless compute region to Mumbai in `vercel.json`:
```json
{
  "$schema": "https://openapi.vercel.sh/vercel.json",
  "regions": ["bom1"]
}
```

---

### 02: Contact Form Uses Opaque Server Action & Re-Transmits the Page Tree

#### Observation & Evidence
Submitting the form on `/contact` triggers a `POST` request to the page URL itself:
* **HTTP Status:** `200 OK`
* **Response Payload Line:** `1:{"success":true}`
* **Stream Contents:** The entire React Server Component (RSC) route tree including page layouts, `<head>` metadata, and JSON-LD is serialized and returned alongside the single boolean.

#### Impact
* **No Resource Contract:** The response does not provide an inquiry `id`, a creation timestamp, or machine-readable error details if an issue occurs.
* **Payload Overhead:** Re-sending layout components on form submit increases network transfer over mobile connections.
* **No Deduplication:** There is no idempotency handling to prevent double submissions if a user taps submit multiple times on a slow network.

#### Remediation
Expose a dedicated, versioned REST API endpoint (`POST /api/v1/inquiries`) supported by a clean response envelope:
* Return `201 Created` with an assigned `id` and `receivedAt` timestamp.
* Return `422 Unprocessable Entity` with an explicit field-level error map if validation fails.
* Support an `Idempotency-Key` header so repeated client requests return the existing record without creating duplicate database entries.

---

### 03: Missing Core Defensive Security Headers

#### Observation & Evidence
Inspecting response headers on `/` and `/contact` confirms that several standard security headers are absent:
```bash
curl -sI https://www.cynbit.in/contact
```
**Missing Headers:**
* `Content-Security-Policy` (CSP)
* `X-Frame-Options` (or CSP `frame-ancestors`)
* `X-Content-Type-Options`
* `Referrer-Policy` (only defaults are applied by browsers)
* `Permissions-Policy`

#### Impact
* **Clickjacking Risk:** Without `X-Frame-Options: DENY` or `frame-ancestors 'none'`, third-party websites can embed Cynbit's lead-capture forms inside transparent `<iframe>` overlays.
* **MIME Sniffing & Script Injection:** Without `X-Content-Type-Options: nosniff` and a Content Security Policy, the application lacks defense-in-depth against malicious content injection.

#### Remediation
Define global security headers centrally in `next.config.mjs`:
```javascript
/** @type {import('next').NextConfig} */
const securityHeaders = [
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "X-Frame-Options", value: "DENY" },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
  {
    key: "Content-Security-Policy",
    value: "default-src 'self'; script-src 'self' 'unsafe-inline' https://www.googletagmanager.com; frame-ancestors 'none';"
  }
];

export default {
  async headers() {
    return [
      {
        source: "/:path*",
        headers: securityHeaders
      }
    ];
  }
};
```

---

### 4: Route-Level Header Drift & Incomplete HSTS

#### Observation & Evidence
Header policies vary depending on whether the route is static or dynamic:
1. The root page (`/`) sends `access-control-allow-origin: *`, while `/contact` does not set CORS headers at all.
2. `/contact` leaks the runtime header `x-powered-by: Next.js`, which is hidden on static pages.
3. `Strict-Transport-Security` is set to `max-age=63072000`, but omits `includeSubDomains` and `preload`.

#### Impact
* Exposing `x-powered-by` aids automated technology fingerprinting.
* Wildcard CORS headers (`*`) should not be broadly set at the document level where form endpoints might inherit them.
* Omitting `includeSubDomains; preload` prevents Cynbit from qualifying for browser HSTS preload lists.

#### Remediation
In `next.config.mjs`, disable the powered-by header and tighten the HSTS directive:
```javascript
export default {
  poweredByHeader: false,
  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          {
            key: "Strict-Transport-Security",
            value: "max-age=63072000; includeSubDomains; preload"
          }
        ]
      }
    ];
  }
};
```

---

### 05: Form Inputs Lack Boundary Constraints & Schemas Diverge

#### Observation & Evidence
Inspecting the DOM on `/contact` reveals the following input attributes:
```html
<input required type="text" placeholder="e.g. Alex Rivera" name="fullName">
<input required type="email" placeholder="alex@example.com" name="email">
<input type="text" placeholder="Your Organization" name="company">
<input type="text" placeholder="e.g. Fintech, Healthcare" name="industry">
<textarea required name="message" rows="5"></textarea>
```
* **No Length Boundaries:** None of the fields define `minlength`, `maxlength`, or input `pattern` attributes.
* **Schema Divergence:** On `/services`, the "AI Suite Inquiry" drawer asks for a different field set (`fullName`, `email`, `techStack`, `automationGoal`, `projectBrief`).

#### Impact
Without client-side boundaries, single-character inputs or excessively large text payloads travel across the network before any validation can happen. Maintaining two separate forms with different schemas creates maintenance drift and complicates backend storage.

#### Remediation
Add standard HTML5 attributes to form controls:
```html
<input 
  id="contact-fullName" 
  name="fullName" 
  type="text" 
  required 
  minlength="2" 
  maxlength="100" 
  autocomplete="name"
>
```
On the backend, handle both forms through a unified schema with a `source` discriminant (`CONTACT_PAGE` vs `SERVICES_MODAL`).

---

### 06: Canonical Domain Split & Sitemap 307 Redirects

#### Observation & Evidence
The public website operates under the `www` subdomain (`https://www.cynbit.in`). However:
1. `robots.txt` lists: `Sitemap: https://cynbit.in/sitemap.xml` (apex domain).
2. All **73 `<loc>` URLs** inside `sitemap.xml` point to `https://cynbit.in/...`.
3. Querying the apex domain returns a temporary redirect:
```http
HTTP/2 307
Location: https://www.cynbit.in/
Cache-Control: public, max-age=0, must-revalidate
```

#### Impact
Search engine crawlers reading the sitemap encounter 73 consecutive `307 Temporary Redirect` hops before reaching canonical content. This wastes crawl budget and delays indexation.

#### Remediation
Update `metadataBase` in `app/layout.tsx` so all generated sitemaps and metadata tags emit the canonical domain:
```typescript
export const metadata = {
  metadataBase: new URL("https://www.cynbit.in")
};
```
Configure DNS and Vercel domains so apex-to-www traffic resolves via a single permanent `308` redirect.

---

### 07: Aggressive Route Prefetching & External Google Fonts

#### Observation & Evidence
* **Prefetch Fan-Out:** Navigating to `/services` immediately fires 10 background RSC prefetches (`?_rsc=...` for `story`, `team`, `careers`, `impact`, `products`, `case-studies`, `bitelevate`, `paths`, `diaries`, `contact`) along with their JavaScript chunks.
* **External Assets:** Icons and styles are loaded from `fonts.googleapis.com` (`Material Symbols` and `Mulish`) via `<link>` tags in `<head>`.
* **Cold Load Footprint:** Total transfer on `/services` reaches **66 requests, 1.8 MB transferred, and 3.1 MB uncompressed resources** on mobile emulation.

#### Impact
Eager prefetching of low-priority routes consumes user bandwidth on metered mobile networks. External font stylesheets also introduce network interaction resulting in render blocking.

#### Remediation
1. Disable prefetching on secondary drawer and footer links using `prefetch={false}`:
   ```tsx
   <Link href="/diaries" prefetch={false}>Diaries</Link>
   ```
2. Self-host fonts using Next.js `next/font/google` and replace icon font sheets with lightweight, tree-shaken inline SVG icons (e.g., `lucide-react`).

---

### 08: Tracking and Ad Cookies Fire Before User Action

#### Observation & Evidence
Upon opening the site in a fresh session, DevTools shows immediate network calls to:
* Google Analytics 4 (`collect?v=2&tid=G-LK6CQWCWX0...`)
* Google Ads conversion beacons (`conversion?random=...`)
* Audience sync pixels (`ga-audiences`)

The browser immediately sets first-party cookies on `.cynbit.in` (`_ga`, `_gcl_au`) before any consent banner or privacy prompt is displayed.

#### Impact
Non-essential analytics and marketing cookies should run after affirmative user acknowledgment to align with modern privacy standards (such as India's DPDP Act 2023).

#### Remediation
Integrate Google Tag Manager with **Consent Mode v2** initialized to `denied` by default:
```javascript
gtag('consent', 'default', {
  'analytics_storage': 'denied',
  'ad_storage': 'denied'
});
```
These settings should be updated to `granted` only after a visitor accepts cookies.

---

### 09: Internal Routes Publicly Advertised in `robots.txt`

#### Observation & Evidence
The public `robots.txt` contains:
```text
Disallow: /api/
Disallow: /admin/
Disallow: /portal/
```

#### Impact
While search crawlers respect disallow directives, `robots.txt` is publicly readable by anyone. Listing administrative route prefixes advertises internal URL structures.

#### Remediation
Remove internal URL paths from `robots.txt`. Restrict access to administrative areas through authentication checks, IP whitelisting, and the `X-Robots-Tag: noindex, nofollow` HTTP header.