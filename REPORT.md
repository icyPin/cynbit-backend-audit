# Technical Audit Report: `cynbit.in`

**Target:** `https://www.cynbit.in`  
**Audit Date:** 30 October 2026  
**Testing Mode:** Passive / Non-Intrusive Inspection (DevTools, DOM/Storage Analysis, HTTP Header & TLS Handshake Inspection)

---

## 1. Executive Summary

`cynbit.in` , a marketing and lead-generation web application built on **Next.js (App Router) with React Server Components (RSC)** and deployed on **Vercel's Edge Network** (`bom1` Mumbai edge POP) using **TLS 1.3 with Post-Quantum hybrid key exchange (`X25519MLKEM768`)** and **Brotli (`br`) compression**. Static page delivery from the Mumbai edge cache is fast (`DOMContentLoaded` in ~467 ms).

However, deeper passive inspection across HTTP headers, Vercel edge routing tokens, RSC payloads, DOM structure, and SEO manifests revealed **9 concrete findings**

the details of which are shared in `Audit.md` file 

---

## 2. Noise Filtering

Before cataloguing application defects, local environment artifacts were isolated and verified:

---

## 3. What Is Done Well

1.**Brotli Compression & Edge Caching:** HTML and static chunks are compressed via `content-encoding: br` and served with `x-vercel-cache: HIT` from the `bom1` (Mumbai) edge node for Indian users.

2.**HSTS Enabled on HTTPS Responses:** `strict-transport-security: max-age=63072000` (2 years) is present across HTTPS responses.

3.**Dedicated Error & 404 Boundaries:** Non-existent routes (`/this-page-does-not-exist`) return a clean HTTP `404` status (`x-next-error-status: 404`) with custom Next.js error and not-found client chunks (`app/error-8e28947332869732.js`).

---

## Disclosure 

I have used ai for some findings and help me document the rest. 
Also not related but a quick thing , i really liked the frontend of the website. just telling

---

## Thankyou