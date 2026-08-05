/* ═══════════════════════════════════════════════════════════════
   ULTIMATECRYPTOSUITE — LANDING PAGE INTERACTIONS
   - Live API health check (Render gateway + python core)
   - OS install tab switching
   - Mobile navigation
   - Scroll effects + reveal animations
   ═══════════════════════════════════════════════════════════════ */

(function () {
    "use strict";

    /* ─────────────────────────────────────────────
       1. API HEALTH CHECK
       Probes the live Render backends directly:
       the Node gateway (auth/TOTP/vault) first, then
       the Python core (crypto/forensics). api.emilyedward.me
       is not yet pointed, so it is intentionally absent.
       ───────────────────────────────────────────── */
    const HEALTH_PROBES = [
        { url: "https://ultimate-crypto-node-gateway.onrender.com/status", label: "node-gateway" },
        { url: "https://ultimate-crypto-python.onrender.com/health", label: "python-core" }
    ];

    function setStatus(state, text, latency) {
        const dot = document.getElementById("status-dot");
        const label = document.getElementById("status-text");
        const latencyEl = document.getElementById("status-latency");
        const cDot = document.getElementById("console-status-dot");
        const cText = document.getElementById("console-status-text");
        const cLog = document.getElementById("console-log");

        dot.className = "status-dot status-dot-" + state;
        label.textContent = text;
        label.className = "font-mono text-sm font-bold " +
            (state === "online" ? "text-emerald-400" : state === "offline" ? "text-red-400" : "text-cyan-300");

        if (latencyEl) {
            latencyEl.textContent = latency ? "~" + latency + "ms" : "";
            latencyEl.classList.toggle("hidden", !latency);
        }
        if (cDot) cDot.className = "status-dot status-dot-" + state;
        if (cText) cText.textContent = text.toUpperCase();
        if (cLog) cLog.textContent = state === "online"
            ? "$ curl " + HEALTH_PROBES[0].url + "\n\nHTTP/1.1 200 OK\n{ \"status\": \"online\" }"
            : "$ curl " + HEALTH_PROBES[0].url + "\n\nHTTP/1.1 000\n{ \"status\": \"unreachable\" }";
    }

    async function probe(url, timeoutMs) {
        const controller = new AbortController();
        const timer = setTimeout(function () { controller.abort(); }, timeoutMs);
        const started = performance.now();
        try {
            const res = await fetch(url, {
                method: "GET",
                mode: "cors",
                cache: "no-store",
                signal: controller.signal
            });
            return { ok: res.ok, ms: Math.round(performance.now() - started) };
        } catch (err) {
            return { ok: false, ms: Math.round(performance.now() - started) };
        } finally {
            clearTimeout(timer);
        }
    }

    async function checkHealth() {
        setStatus("checking", "CHECKING...", null);
        for (let i = 0; i < HEALTH_PROBES.length; i++) {
            const r = await probe(HEALTH_PROBES[i].url, 8000);
            if (r.ok) {
                setStatus("online", "ONLINE", r.ms);
                return;
            }
        }
        setStatus("offline", "OFFLINE", null);
    }

    /* ─────────────────────────────────────────────
       2. OS INSTALLATION TABS
       ───────────────────────────────────────────── */
    function initTabs() {
        const tabs = document.querySelectorAll(".os-tab");
        const panels = document.querySelectorAll(".os-panel");

        tabs.forEach(function (tab) {
            tab.addEventListener("click", function () {
                tabs.forEach(function (t) {
                    t.classList.remove("os-tab-active");
                    t.setAttribute("aria-selected", "false");
                });
                panels.forEach(function (p) { p.classList.add("hidden"); });

                tab.classList.add("os-tab-active");
                tab.setAttribute("aria-selected", "true");

                const os = tab.getAttribute("data-os");
                const panel = document.getElementById("panel-" + os);
                if (panel) {
                    panel.classList.remove("hidden");
                    panel.classList.remove("os-panel");
                    void panel.offsetWidth; /* restart animation */
                    panel.classList.add("os-panel");
                }
            });
        });
    }

    /* ─────────────────────────────────────────────
       3. MOBILE NAVIGATION
       ───────────────────────────────────────────── */
    function initMobileMenu() {
        const btn = document.getElementById("mobile-menu-btn");
        const menu = document.getElementById("mobile-menu");
        if (!btn || !menu) return;

        btn.addEventListener("click", function () {
            const open = menu.classList.toggle("hidden");
            btn.setAttribute("aria-expanded", String(!open));
        });

        menu.querySelectorAll(".mobile-link").forEach(function (link) {
            link.addEventListener("click", function () {
                menu.classList.add("hidden");
                btn.setAttribute("aria-expanded", "false");
            });
        });
    }

    /* ─────────────────────────────────────────────
       4. DOWNLOAD TRIGGERS (ring + feedback)
       ───────────────────────────────────────────── */
    function initDownloads() {
        document.querySelectorAll("[id$='-download-btn']").forEach(function (btn) {
            btn.addEventListener("click", function () {
                const original = btn.innerHTML;
                btn.innerHTML = '<span aria-hidden="true">&#8987;</span> STARTING DOWNLOAD...';
                btn.style.pointerEvents = "none";
                setTimeout(function () {
                    btn.innerHTML = original;
                    btn.style.pointerEvents = "";
                }, 1800);
            });
        });
    }

    /* ─────────────────────────────────────────────
       5. SCROLL: sticky header + reveal animations
       ───────────────────────────────────────────── */
    function initScrollEffects() {
        const header = document.getElementById("site-header");
        const revealEls = document.querySelectorAll(".feature-card, .arch-card, .section-tag, .os-tab");

        function onScroll() {
            if (header) {
                header.classList.toggle("header-scrolled", window.scrollY > 24);
            }
            revealEls.forEach(function (el) {
                const rect = el.getBoundingClientRect();
                if (rect.top < window.innerHeight - 60) {
                    el.classList.add("reveal-visible");
                }
            });
        }

        /* Add reveal base classes (skip if already visible on load) */
        revealEls.forEach(function (el) {
            if (!el.classList.contains("reveal")) el.classList.add("reveal");
        });

        window.addEventListener("scroll", onScroll, { passive: true });
        onScroll();
    }

    /* ─────────────────────────────────────────────
       6. FEATURE CARD CURSOR GLOW
       ───────────────────────────────────────────── */
    function initCursorGlow() {
        document.querySelectorAll(".feature-card").forEach(function (card) {
            card.addEventListener("mousemove", function (e) {
                const rect = card.getBoundingClientRect();
                card.style.setProperty("--mx", (e.clientX - rect.left) + "px");
                card.style.setProperty("--my", (e.clientY - rect.top) + "px");
            });
        });
    }

    /* ─────────────────────────────────────────────
       BOOT
       ───────────────────────────────────────────── */
    document.addEventListener("DOMContentLoaded", function () {
        const yearEl = document.getElementById("year");
        if (yearEl) yearEl.textContent = String(new Date().getFullYear());

        initTabs();
        initMobileMenu();
        initDownloads();
        initScrollEffects();
        initCursorGlow();
        checkHealth();
        setInterval(checkHealth, 60000); /* re-check every minute */
    });
})();
