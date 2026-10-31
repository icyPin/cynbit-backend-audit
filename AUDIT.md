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

