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

## 2. Noise Filtering & False-Positive Exclusion

Before cataloguing application defects, local environment artifacts were isolated and verified:

---
