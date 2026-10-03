/**
 * App Vault Dashboard — Premium JavaScript
 * Animated canvas background, scroll-reveal, counters, and full API integration
 */

const VAULT_URL = 'http://localhost:8200';
const ADMIN_TOKEN_STORAGE_KEY = 'vault_admin_token';

let currentAuditPage = 0;
const AUDIT_PAGE_SIZE = 15;

// ═══════════════════════════════════════════════════════
//  Initialization
// ═══════════════════════════════════════════════════════
document.addEventListener('DOMContentLoaded', () => {
    initCanvas();
    initNavScroll();
    initScrollReveal();
    setupTabs();
    setupClock();
    setupModal();
    setupPolicyForm();
    setupSmoothScroll();
    checkConnection();
    loadDashboardData();
    setInterval(loadDashboardData, 30000);
});

function getAdminToken() {
    let token = sessionStorage.getItem(ADMIN_TOKEN_STORAGE_KEY);
    if (!token || !token.trim()) {
        token = prompt('Enter admin token for dashboard actions:');
        if (!token || !token.trim()) {
            return null;
        }
        sessionStorage.setItem(ADMIN_TOKEN_STORAGE_KEY, token.trim());
        return token.trim();
    }
    return token.trim();
}

async function authorizedFetch(url, options = {}) {
    const token = getAdminToken();
    if (!token) {
        throw new Error('Missing admin token');
    }
    const headers = {
        ...(options.headers || {}),
        'X-Vault-Token': token
    };
    return fetch(url, { ...options, headers });
}

function escapeHtml(value) {
    if (value === null || value === undefined) return '';
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

// ═══════════════════════════════════════════════════════
//  Animated Canvas Background — Floating Particles + Grid
// ═══════════════════════════════════════════════════════
function initCanvas() {
    const canvas = document.getElementById('bgCanvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    let w, h;
    let particles = [];
    const PARTICLE_COUNT = 60;
    const CONNECTION_DIST = 150;
    let mouse = { x: -1000, y: -1000 };

    function resize() {
        w = canvas.width = window.innerWidth;
        h = canvas.height = window.innerHeight;
    }

    function createParticles() {
        particles = [];
        for (let i = 0; i < PARTICLE_COUNT; i++) {
            particles.push({
                x: Math.random() * w,
                y: Math.random() * h,
                vx: (Math.random() - 0.5) * 0.4,
                vy: (Math.random() - 0.5) * 0.4,
                size: Math.random() * 2 + 0.5,
                color: pickColor(),
                alpha: Math.random() * 0.5 + 0.1,
            });
        }
    }

    function pickColor() {
        const colors = [
            '34, 197, 94',    // green
            '163, 230, 53',   // lime
            '250, 204, 21',   // yellow
            '74, 222, 128',   // light green
        ];
        return colors[Math.floor(Math.random() * colors.length)];
    }

    function drawGrid() {
        const gridSize = 80;
        ctx.strokeStyle = 'rgba(255, 255, 255, 0.012)';
        ctx.lineWidth = 0.5;
        ctx.beginPath();
        for (let x = 0; x <= w; x += gridSize) {
            ctx.moveTo(x, 0);
            ctx.lineTo(x, h);
        }
        for (let y = 0; y <= h; y += gridSize) {
            ctx.moveTo(0, y);
            ctx.lineTo(w, y);
        }
        ctx.stroke();
    }

    function draw() {
        ctx.clearRect(0, 0, w, h);

        // Grid
        drawGrid();

        // Draw ambient glow orbs
        drawGlow(w * 0.15, h * 0.2, 350, '34, 197, 94', 0.04);
        drawGlow(w * 0.85, h * 0.7, 300, '163, 230, 53', 0.035);
        drawGlow(w * 0.5, h * 0.9, 250, '250, 204, 21', 0.025);

        // Connections
        for (let i = 0; i < particles.length; i++) {
            for (let j = i + 1; j < particles.length; j++) {
                const dx = particles[i].x - particles[j].x;
                const dy = particles[i].y - particles[j].y;
                const dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < CONNECTION_DIST) {
                    const alpha = (1 - dist / CONNECTION_DIST) * 0.08;
                    ctx.strokeStyle = `rgba(34, 197, 94, ${alpha})`;
                    ctx.lineWidth = 0.5;
                    ctx.beginPath();
                    ctx.moveTo(particles[i].x, particles[i].y);
                    ctx.lineTo(particles[j].x, particles[j].y);
                    ctx.stroke();
                }
            }
        }

        // Particles
        for (const p of particles) {
            // Mouse interaction
            const dx = mouse.x - p.x;
            const dy = mouse.y - p.y;
            const dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < 200) {
                const force = (200 - dist) / 200 * 0.01;
                p.vx -= dx * force;
                p.vy -= dy * force;
            }

            p.x += p.vx;
            p.y += p.vy;

            // Dampen
            p.vx *= 0.999;
            p.vy *= 0.999;

            // Wrap
            if (p.x < 0) p.x = w;
            if (p.x > w) p.x = 0;
            if (p.y < 0) p.y = h;
            if (p.y > h) p.y = 0;

            ctx.beginPath();
            ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
            ctx.fillStyle = `rgba(${p.color}, ${p.alpha})`;
            ctx.fill();
        }

        requestAnimationFrame(draw);
    }

    function drawGlow(x, y, radius, color, alpha) {
        const grad = ctx.createRadialGradient(x, y, 0, x, y, radius);
        grad.addColorStop(0, `rgba(${color}, ${alpha})`);
        grad.addColorStop(1, 'transparent');
        ctx.fillStyle = grad;
        ctx.beginPath();
        ctx.arc(x, y, radius, 0, Math.PI * 2);
        ctx.fill();
    }

    window.addEventListener('resize', () => {
        resize();
    });

    window.addEventListener('mousemove', (e) => {
        mouse.x = e.clientX;
        mouse.y = e.clientY;
    });

    resize();
    createParticles();
    draw();
}

// ═══════════════════════════════════════════════════════
//  Nav Scroll Effect
// ═══════════════════════════════════════════════════════
function initNavScroll() {
    const nav = document.getElementById('mainNav');
    if (!nav) return;

    const observer = new IntersectionObserver(
        ([entry]) => {
            nav.classList.toggle('scrolled', !entry.isIntersecting);
        },
        { threshold: 0.7 }
    );

    const hero = document.getElementById('hero');
    if (hero) observer.observe(hero);
}

// ═══════════════════════════════════════════════════════
//  Scroll Reveal
// ═══════════════════════════════════════════════════════
function initScrollReveal() {
    const elements = document.querySelectorAll('.reveal-up');

    const observer = new IntersectionObserver(
        (entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    const delay = parseInt(entry.target.dataset.delay || 0);
                    setTimeout(() => {
                        entry.target.classList.add('revealed');
                    }, delay);
                    observer.unobserve(entry.target);
                }
            });
        },
        { threshold: 0.15, rootMargin: '0px 0px -40px 0px' }
    );

    elements.forEach((el) => observer.observe(el));
}

// ═══════════════════════════════════════════════════════
//  Smooth Scroll for Nav Links
// ═══════════════════════════════════════════════════════
function setupSmoothScroll() {
    document.querySelectorAll('a[href^="#"]').forEach((anchor) => {
        anchor.addEventListener('click', function (e) {
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                e.preventDefault();
                target.scrollIntoView({ behavior: 'smooth' });
            }
        });
    });
}

// ═══════════════════════════════════════════════════════
//  Connection Check
// ═══════════════════════════════════════════════════════
async function checkConnection() {
    const dot = document.getElementById('statusDot');
    const status = document.getElementById('connectionStatus');
    try {
        const res = await fetch(`${VAULT_URL}/api/v1/health`);
        if (res.ok) {
            dot.className = 'status-dot connected';
            status.textContent = 'Vault Online';
        } else {
            dot.className = 'status-dot error';
            status.textContent = 'Vault Error';
        }
    } catch (e) {
        dot.className = 'status-dot error';
        status.textContent = 'Offline';
    }
}

// ═══════════════════════════════════════════════════════
//  Dashboard Data — Full Stats
// ═══════════════════════════════════════════════════════
async function loadDashboardData() {
    try {
        const res = await authorizedFetch(`${VAULT_URL}/api/v1/dashboard/summary`);
        if (!res.ok) return;
        const json = await res.json();
        if (json.success && json.data) {
            const d = json.data;
            const stats = d.auditStats || {};

            // Main stat cards
            animateCounter('totalSecrets', d.totalSecrets || 0);
            animateCounter('totalPolicies', d.totalPolicies || 0);
            animateCounter('accessCount', stats.accessLast24h || 0);
            animateCounter('failedCount', stats.failedAccessLast24h || 0);

            // Hero mini stats
            animateCounter('heroSecrets', d.totalSecrets || 0);
            animateCounter('heroPolicies', d.totalPolicies || 0);

            // Extended stats
            animateCounter('activeServices', stats.activeServicesLast24h || 0);
            animateCounter('totalLogs', stats.totalLogs || 0);
            animateCounter('access7d', stats.accessLast7Days || 0);
            animateCounter('failed7d', stats.failedAccessLast7Days || 0);

            // Server time
            if (d.serverTime) {
                const st = new Date(d.serverTime);
                document.getElementById('serverTime').textContent =
                    st.toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit' });
            }

            // Trends (compare 24h vs 7d average)
            const avg7d24h = stats.accessLast7Days ? Math.round(stats.accessLast7Days / 7) : 0;
            const access24h = stats.accessLast24h || 0;
            if (avg7d24h > 0) {
                const diff = Math.round(((access24h - avg7d24h) / avg7d24h) * 100);
                const trendEl = document.getElementById('accessTrendText');
                if (trendEl) {
                    trendEl.textContent = `${diff >= 0 ? '+' : ''}${diff}% vs avg`;
                }
                const trendWrap = document.getElementById('accessTrend');
                if (trendWrap) {
                    trendWrap.className = `stat-trend ${diff >= 0 ? 'stat-trend-up' : 'stat-trend-down'}`;
                }
            }

            const avgFailed7d24h = stats.failedAccessLast7Days ? Math.round(stats.failedAccessLast7Days / 7) : 0;
            const failed24h = stats.failedAccessLast24h || 0;
            if (avgFailed7d24h > 0) {
                const diff = Math.round(((failed24h - avgFailed7d24h) / avgFailed7d24h) * 100);
                const trendEl = document.getElementById('deniedTrendText');
                if (trendEl) {
                    trendEl.textContent = `${diff >= 0 ? '+' : ''}${diff}% vs avg`;
                }
                const trendWrap = document.getElementById('deniedTrend');
                if (trendWrap) {
                    trendWrap.className = `stat-trend ${diff <= 0 ? 'stat-trend-up' : 'stat-trend-down'}`;
                }
            }

            // Audit count badge
            const badge = document.getElementById('auditCountBadge');
            if (badge) {
                badge.textContent = `${(stats.totalLogs || 0).toLocaleString()} total events`;
            }
        }
    } catch (e) {
        console.warn('Dashboard fetch failed:', e);
    }
    loadAuditLogs();
    loadPolicies();
}

// ═══════════════════════════════════════════════════════
//  Animated Counter
// ═══════════════════════════════════════════════════════
function animateCounter(elementId, targetValue) {
    const el = document.getElementById(elementId);
    if (!el) return;
    const current = parseInt(el.textContent) || 0;
    if (current === targetValue) return;

    const duration = 800;
    const startTime = performance.now();

    function step(now) {
        const elapsed = now - startTime;
        const progress = Math.min(elapsed / duration, 1);
        // Ease out cubic
        const eased = 1 - Math.pow(1 - progress, 3);
        const value = Math.round(current + (targetValue - current) * eased);
        el.textContent = value.toLocaleString();
        if (progress < 1) requestAnimationFrame(step);
    }

    requestAnimationFrame(step);
}

// ═══════════════════════════════════════════════════════
//  Audit Logs
// ═══════════════════════════════════════════════════════
async function loadAuditLogs(page = 0) {
    currentAuditPage = page;
    const filter = document.getElementById('auditFilter').value;
    try {
        let url = `${VAULT_URL}/api/v1/audit/logs?page=${page}&size=${AUDIT_PAGE_SIZE}`;
        if (filter) url = `${VAULT_URL}/api/v1/audit/logs/action/${filter}?page=${page}&size=${AUDIT_PAGE_SIZE}`;
        const res = await authorizedFetch(url);
        if (!res.ok) { showAuditEmpty('Failed to load audit logs'); return; }
        const json = await res.json();
        if (json.success && json.data) {
            renderAuditTable(json.data);
        } else { showAuditEmpty('No audit logs found'); }
    } catch (e) {
        showAuditEmpty('Cannot connect to vault');
    }
}

function renderAuditTable(pageData) {
    const tbody = document.getElementById('auditTableBody');
    const logs = pageData.content || [];
    if (logs.length === 0) { showAuditEmpty('No audit logs found'); return; }
    tbody.innerHTML = logs.map((log, i) => `
        <tr style="animation: fadeSlideIn 0.3s ease ${i * 30}ms both">
            <td class="timestamp-cell">${escapeHtml(formatTimestamp(log.timestamp))}</td>
            <td><strong style="color: var(--text-bright)">${escapeHtml(log.serviceName || '—')}</strong></td>
            <td><span class="badge badge-action">${escapeHtml(log.action || '—')}</span></td>
            <td style="font-family:var(--font-mono);font-size:0.8rem">${escapeHtml(log.resourcePath || '—')}</td>
            <td><span class="badge badge-env">${escapeHtml(log.environment || '—')}</span></td>
            <td>${log.success ? '<span class="badge badge-success">Success</span>' : '<span class="badge badge-error">Denied</span>'}</td>
        </tr>
    `).join('');
    renderPagination(pageData);
}

function showAuditEmpty(msg) {
    document.getElementById('auditTableBody').innerHTML = `<tr><td colspan="6" class="empty-state">
        <div class="empty-icon"><svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg></div>
        ${escapeHtml(msg)}
    </td></tr>`;
    document.getElementById('auditPagination').innerHTML = '';
}

function renderPagination(pageData) {
    const div = document.getElementById('auditPagination');
    const total = pageData.totalPages || 1;
    const current = pageData.number || 0;
    let html = `<button ${current === 0 ? 'disabled' : ''} onclick="loadAuditLogs(${current - 1})">← Prev</button>`;
    html += `<span class="page-info">Page ${current + 1} of ${total}</span>`;
    html += `<button ${current >= total - 1 ? 'disabled' : ''} onclick="loadAuditLogs(${current + 1})">Next →</button>`;
    div.innerHTML = html;
}

document.getElementById('refreshAudit')?.addEventListener('click', () => loadAuditLogs(currentAuditPage));
document.getElementById('auditFilter')?.addEventListener('change', () => loadAuditLogs(0));

// ═══════════════════════════════════════════════════════
//  Policies
// ═══════════════════════════════════════════════════════
async function loadPolicies() {
    try {
        const res = await authorizedFetch(`${VAULT_URL}/api/v1/policies`);
        if (!res.ok) {
            document.getElementById('policiesGrid').innerHTML = '<div class="empty-state"><div class="empty-icon"><svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg></div>Failed to load policies</div>';
            return;
        }
        const json = await res.json();
        if (json.success && json.data) { renderPolicies(json.data); }
        else { document.getElementById('policiesGrid').innerHTML = '<div class="empty-state"><div class="empty-icon"><svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg></div>No policies found</div>'; }
    } catch (e) {
        document.getElementById('policiesGrid').innerHTML = '<div class="empty-state"><div class="empty-icon"><svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg></div>Cannot connect to vault</div>';
    }
}

function renderPolicies(policies) {
    const grid = document.getElementById('policiesGrid');
    if (policies.length === 0) {
        grid.innerHTML = '<div class="empty-state"><div class="empty-icon"><svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg></div>No policies configured</div>';
        return;
    }
    grid.innerHTML = policies.map((p, i) => `
        <div class="policy-card" style="animation: fadeSlideIn 0.4s ease ${i * 60}ms both">
            <div class="policy-card-header">
                <h4>${escapeHtml(p.name)}</h4>
                <span class="badge ${p.effect === 'ALLOW' ? 'badge-allow' : 'badge-deny'}">${escapeHtml(p.effect)}</span>
            </div>
            <div class="policy-details">
                <div class="policy-detail">
                    <span class="policy-detail-label">Service</span>
                    <span class="policy-detail-value">${escapeHtml(p.serviceName)}</span>
                </div>
                <div class="policy-detail">
                    <span class="policy-detail-label">Environment</span>
                    <span class="policy-detail-value">${escapeHtml(p.environment || '*')}</span>
                </div>
                <div class="policy-detail">
                    <span class="policy-detail-label">Region</span>
                    <span class="policy-detail-value">${escapeHtml(p.region || '*')}</span>
                </div>
            </div>
            <div class="policy-paths">
                <div class="policy-paths-label">Allowed Paths</div>
                ${(p.allowedPaths || '').split(',').map(path => `<span class="policy-path-tag">${escapeHtml(path.trim())}</span>`).join('')}
            </div>
            <div class="policy-card-actions">
                <button class="btn-danger" onclick="deletePolicy(${Number(p.id)})">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                    Delete
                </button>
            </div>
        </div>
    `).join('');
}

async function deletePolicy(id) {
    if (!confirm('Delete this policy?')) return;
    try {
        await authorizedFetch(`${VAULT_URL}/api/v1/policies/${id}`, { method: 'DELETE' });
        showToast('Policy deleted', 'success');
        loadPolicies();
        loadDashboardData();
    } catch (e) { showToast('Failed to delete policy', 'error'); }
}

// ═══════════════════════════════════════════════════════
//  Policy Modal
// ═══════════════════════════════════════════════════════
function setupModal() {
    document.getElementById('addPolicyBtn')?.addEventListener('click', () => document.getElementById('policyModal').classList.add('open'));
    document.getElementById('closePolicyModal')?.addEventListener('click', () => document.getElementById('policyModal').classList.remove('open'));
    document.getElementById('policyModal')?.addEventListener('click', (e) => { if (e.target.id === 'policyModal') e.target.classList.remove('open'); });
}

function setupPolicyForm() {
    document.getElementById('policyForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const body = {
            name: document.getElementById('policyName').value,
            serviceName: document.getElementById('policyService').value,
            effect: document.getElementById('policyEffect').value,
            environment: document.getElementById('policyEnv').value || '*',
            region: document.getElementById('policyRegion').value || '*',
            allowedPaths: document.getElementById('policyPaths').value
        };
        try {
            const res = await authorizedFetch(`${VAULT_URL}/api/v1/policies`, {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
            });
            if (res.ok) {
                showToast('Policy created!', 'success');
                document.getElementById('policyModal').classList.remove('open');
                document.getElementById('policyForm').reset();
                loadPolicies();
                loadDashboardData();
            } else {
                const err = await res.json();
                showToast(err.error || 'Failed to create policy', 'error');
            }
        } catch (e) { showToast('Connection error', 'error'); }
    });
}

// ═══════════════════════════════════════════════════════
//  Tabs
// ═══════════════════════════════════════════════════════
function setupTabs() {
    document.querySelectorAll('.dash-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            document.querySelectorAll('.dash-tab').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.dash-content').forEach(c => c.classList.remove('active'));
            tab.classList.add('active');
            document.getElementById(tab.dataset.tab + 'Content').classList.add('active');
        });
    });
}

// ═══════════════════════════════════════════════════════
//  Clock
// ═══════════════════════════════════════════════════════
function setupClock() {
    const el = document.getElementById('currentTime');
    if (!el) return;
    const update = () => {
        el.textContent = new Date().toLocaleTimeString('en-US', { hour12: false });
    };
    update();
    setInterval(update, 1000);
}

// ═══════════════════════════════════════════════════════
//  Helpers
// ═══════════════════════════════════════════════════════
function formatTimestamp(ts) {
    if (!ts) return '—';
    const d = new Date(ts);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) + ' ' +
           d.toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

function showToast(message, type = 'success') {
    const existing = document.querySelector('.toast');
    if (existing) existing.remove();
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => {
        toast.classList.add('hide');
        setTimeout(() => toast.remove(), 300);
    }, 2700);
}
