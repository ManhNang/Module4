/**
 * Poke-Sanctuary Multi-Layer 2.5D Diorama Engine & HUD Controller
 * - Layer 1 (Canvas): High-art Sky (Day / Sunset / Night), Sunbeams, Parallax Clouds, Floating Sky Island
 *   with multi-tier rocky cliffs, cobblestone path, Ancient Portal, rounded lush trees, crystal pond & fireflies.
 * - Layer 2 (DOM/CSS 2.5D): Real Animated Pokémon Showdown GIF sprites (zero CORS issues!), isometric
 *   perspective rarity aura rings, separation physics, Y-sorting, directional flipping & click joy-hop.
 * - Layer 3 (Top Overlay): Unoccluded glassmorphic nameplates & 3D spinning metallic gold harvest coins.
 * - Layer 4 (VFX): Golden sparkle bursts, floating "+X.XXX ₫" & joy heart particles.
 */
(function () {
    'use strict';

    const SHOWDOWN_ANI_BASE = 'https://play.pokemonshowdown.com/sprites/ani/';
    const SHOWDOWN_HOME_BASE = 'https://play.pokemonshowdown.com/sprites/home/';
    const SHOWDOWN_DEX_BASE = 'https://play.pokemonshowdown.com/sprites/dex/';
    const FALLBACK_ART = 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/25.png';
    const MAX_ACCUMULATED_TICKS = 12;
    const MIN_TARGET_DIST = 0.24; // ~12% chiều rộng mặt đảo (chỉ dùng khi chọn điểm đến, không chặn di chuyển)

    // Lucide-style crisp inline SVGs (zero OS emojis)
    const SVG_COIN = '<svg class="h-3.5 w-3.5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="8" cy="8" r="6"/><path d="M18.09 10.37A6 6 0 1 1 10.34 18"/><path d="M7 6h1v4"/><path d="m16.71 13.88.7.71-2.82 2.82"/></svg>';
    const SVG_MOON = '<svg class="h-3.5 w-3.5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/></svg>';
    const SVG_CLOCK = '<svg class="h-3.5 w-3.5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>';
    const SVG_GIFT = '<svg class="h-3.5 w-3.5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="8" width="18" height="4" rx="1"/><path d="M12 8v13"/><path d="M19 12v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7"/><path d="M7.5 8a2.5 2.5 0 0 1 0-5A4.8 8 0 0 1 12 8a4.8 8 0 0 1 4.5-5 2.5 2.5 0 0 1 0 5"/></svg>';
    const SVG_ZAP = '<svg class="h-3 w-3 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z"/></svg>';
    const SVG_PLUS = '<svg class="h-4 w-4 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14"/><path d="M12 5v14"/></svg>';
    const SVG_HEART = '<svg class="h-4 w-4 fill-rose-400 text-rose-400" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>';

    // 5 dedicated Home Zones spread generously across the enlarged grassy meadow
    const HOME_ZONES = [
        { u: -0.66, v: 0.04, r: 0.10 },  // Far-West meadow
        { u: -0.06, v: -0.58, r: 0.10 }, // North meadow
        { u: -0.02, v: 0.16, r: 0.10 },  // Center meadow
        { u: 0.62, v: 0.38, r: 0.10 },   // Far-East meadow
        { u: -0.44, v: 0.62, r: 0.10 }   // Far-South-West meadow
    ];

    function isDailyCapReached() {
        if (!sanctuaryState) return false;
        const cap = Number(sanctuaryState.dailyCap || 1000);
        const earned = Number(sanctuaryState.dailyEarned || 0);
        return cap > 0 && earned >= cap;
    }

    let sanctuaryState = null;
    let selectedZoneCode = null;
    let selectedExpeditionCardIds = new Set();
    let isHarvestingAll = false;
    let atmosphere = 'day'; // 'day' | 'sunset' | 'night'

    // DOM Layer Refs
    let canvas = null;
    let ctx = null;
    let actorsLayerEl = null;
    let overlayLayerEl = null;
    let vfxLayerEl = null;
    let canvasWidth = 800;
    let canvasHeight = 500;
    let rafId = null;
    let lastFrameTime = 0;
    let elapsedTime = 0;
    let isPaused = false;
    let countdownTimerId = null;
    let dropPollTimerId = null;
    let activeDropState = null;
    let isClaimingDrop = false;
    let isExchangingShards = false;

    // Entities synced between Physics & DOM
    const islandActors = new Map(); // slotId -> actor

    // Ambient Sky & Floating Motes
    const clouds = [
        { x: 0.08, y: 0.14, scale: 1.15, speed: 0.0085, alpha: 0.85 },
        { x: 0.44, y: 0.09, scale: 0.78, speed: 0.0050, alpha: 0.65 },
        { x: 0.78, y: 0.18, scale: 0.96, speed: 0.0072, alpha: 0.78 },
        { x: 0.26, y: 0.24, scale: 0.62, speed: 0.0042, alpha: 0.50 }
    ];

    const stars = Array.from({ length: 34 }, (_, i) => ({
        x: ((i * 73 + 17) % 100) / 100,
        y: ((i * 41 + 11) % 44) / 100,
        r: 0.9 + (i % 3) * 0.55,
        phase: i * 1.17
    }));

    const ambientMotes = Array.from({ length: 18 }, (_, i) => ({
        x: ((i * 59 + 13) % 100) / 100,
        y: 0.25 + ((i * 37) % 58) / 100,
        vx: (i % 2 === 0 ? 1 : -1) * (0.006 + (i % 4) * 0.002),
        vy: -0.004 - (i % 3) * 0.002,
        phase: i * 0.9,
        size: 1.8 + (i % 3) * 0.9
    }));

    // Stepping stones from center to Ancient Portal (-0.68, -0.68)
    const cobblestonePath = [
        { u: -0.10, v: -0.10, rx: 11, ry: 5.5 },
        { u: -0.22, v: -0.22, rx: 10, ry: 5 },
        { u: -0.35, v: -0.33, rx: 11, ry: 5.2 },
        { u: -0.47, v: -0.46, rx: 10, ry: 4.8 },
        { u: -0.58, v: -0.57, rx: 12, ry: 5.5 }
    ];

    // Scenic Trees pushed out to the island rim so the enlarged meadow is wide open
    const islandTrees = [
        { u: -0.90, v: 0.14, scale: 1.02 },
        { u: -0.72, v: 0.78, scale: 0.88 },
        { u: 0.84, v: 0.28, scale: 0.94 },
        { u: 0.14, v: -0.88, scale: 0.82 }
    ];

    // Flowers & Mossy Boulders
    const islandDecor = [
        { type: 'flower', u: -0.68, v: -0.16, color: '#fb7185' },
        { type: 'flower', u: -0.58, v: -0.08, color: '#fde047' },
        { type: 'flower', u: 0.20, v: 0.62, color: '#f472b6' },
        { type: 'flower', u: 0.30, v: 0.68, color: '#fde047' },
        { type: 'flower', u: -0.28, v: 0.74, color: '#60a5fa' },
        { type: 'flower', u: 0.64, v: 0.04, color: '#fb923c' },
        { type: 'rock', u: -0.34, v: -0.78, scale: 1.05 },
        { type: 'rock', u: 0.74, v: -0.04, scale: 0.85 },
        { type: 'rock', u: -0.14, v: 0.80, scale: 0.80 }
    ];

    function formatVnd(amount) {
        return Number(amount || 0).toLocaleString('vi-VN') + ' Xu';
    }

    function formatCountdown(totalSec) {
        const s = Math.max(0, Math.floor(totalSec));
        const hrs = Math.floor(s / 3600);
        const mins = Math.floor((s % 3600) / 60);
        const secs = s % 60;
        if (hrs > 0) {
            return `${String(hrs).padStart(2, '0')}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
        }
        return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
    }

    function getRarityVisual(rarityName, colorHex) {
        const n = (rarityName || '').toLowerCase();
        if (n.includes('special illustration')) {
            return { color: '#ec4899', rainbow: true };
        }
        if (n.includes('hyper') || n.includes('secret') || n.includes('gold')) {
            return { color: '#f59e0b', rainbow: true };
        }
        if (n.includes('illustration')) {
            return { color: '#c084fc', rainbow: false };
        }
        if (n.includes('ultra') || n.includes('ex') || n.includes('double')) {
            return { color: '#fb923c', rainbow: false };
        }
        if (n.includes('rare') && !n.includes('uncommon')) {
            return { color: '#38bdf8', rainbow: false };
        }
        if (n.includes('uncommon')) {
            return { color: '#34d399', rainbow: false };
        }
        if (colorHex && /^#[0-9a-fA-F]{6}$/.test(colorHex)) {
            return { color: colorHex, rainbow: false };
        }
        return { color: '#94a3b8', rainbow: false };
    }

    function buildSpriteSlugCandidates(speciesName, cardName) {
        const raw = (speciesName || cardName || '').trim();
        if (!raw) return [];
        if (/[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]/i.test(raw)) {
            return [];
        }
        // Bỏ qua thẻ Trainer / Supporter / Item có dấu nháy sở hữu ('s) hoặc ký tự đặc biệt để không gọi Showdown gây lỗi 404 / ORB
        if (/['’&!]/.test(raw)) {
            return [];
        }
        const lower = raw.toLowerCase()
            .replace(/♀/g, 'f')
            .replace(/♂/g, 'm')
            .replace(/\b(ex|gx|v|vmax|vstar|radiant|shining|dark|light|delta|heat|wash|frost|fan|mow)\b/gi, ' ')
            .trim();
        const isMega = /\bmega\b/i.test(lower);
        const withoutMega = lower.replace(/\bmega\b/gi, '').trim();
        // Nếu sau khi bỏ hậu tố TCG mà tên vẫn có từ 2 từ trở lên (VD: Trainer card), dùng thẳng ảnh thẻ gốc
        if (/\s+/.test(withoutMega)) {
            return [];
        }
        const cleanBase = withoutMega.replace(/[^a-z0-9]/g, '');
        if (!cleanBase || cleanBase.length < 2) return [];

        const slugs = [];
        if (isMega) slugs.push(`${cleanBase}-mega`);
        slugs.push(cleanBase);
        return slugs;
    }

    function buildSpriteUrlChain(card) {
        const slugs = buildSpriteSlugCandidates(card && card.speciesName, card && card.name);
        const urls = [];
        slugs.forEach((s) => urls.push({ url: `${SHOWDOWN_ANI_BASE}${s}.gif`, fallbackCard: false }));
        slugs.forEach((s) => urls.push({ url: `${SHOWDOWN_HOME_BASE}${s}.png`, fallbackCard: false }));
        slugs.forEach((s) => urls.push({ url: `${SHOWDOWN_DEX_BASE}${s}.png`, fallbackCard: false }));
        if (card && card.imageUrl) {
            urls.push({ url: card.imageUrl, fallbackCard: true });
        }
        urls.push({ url: FALLBACK_ART, fallbackCard: false });
        return urls;
    }

    // =========================================================================
    // ISOMETRIC PROJECTION & WALKABLE BOUNDS
    // =========================================================================
    function isoToScreen(u, v) {
        const cx = canvasWidth * 0.5;
        const cy = canvasHeight * 0.50;
        const rx = Math.min(canvasWidth * 0.49, 435);
        const ry = rx * 0.47;
        return {
            x: cx + (u - v) * rx * 0.5,
            y: cy + (u + v) * ry * 0.5
        };
    }

    function isInsidePond(u, v) {
        const du = (u - 0.46) / 0.36;
        const dv = (v + 0.36) / 0.30;
        return (du * du + dv * dv) < 1.0;
    }

    function isNearPortal(u, v) {
        return Math.hypot(u + 0.70, v + 0.70) < 0.34;
    }

    function clampToIsland(actor) {
        actor.u = Math.max(-0.72, Math.min(0.72, actor.u));
        actor.v = Math.max(-0.66, Math.min(0.68, actor.v));
        if (isInsidePond(actor.u, actor.v)) {
            actor.u -= 0.08;
            actor.v += 0.08;
        }
        if (isNearPortal(actor.u, actor.v)) {
            actor.u += 0.08;
            actor.v += 0.08;
        }
    }

    // =========================================================================
    // LAYER 4: DOM VFX SPAWNERS (SPARKLES, FLOATING +XU, HEARTS)
    // =========================================================================
    function spawnHarvestDomVfx(x, y, amountText) {
        if (!vfxLayerEl) return;

        // Rising "+2.000 ₫" text
        const label = document.createElement('div');
        label.className = 'pk-vfx-float pk-num';
        label.textContent = amountText;
        label.style.left = `${x}px`;
        label.style.top = `${y - 48}px`;
        vfxLayerEl.appendChild(label);
        setTimeout(() => label.remove(), 1400);

        // 12 golden burst sparkles
        for (let i = 0; i < 12; i++) {
            const sp = document.createElement('div');
            sp.className = 'pk-vfx-spark';
            const angle = (Math.PI * 2 * i) / 12 + Math.random() * 0.3;
            const dist = 28 + Math.random() * 44;
            sp.style.left = `${x}px`;
            sp.style.top = `${y - 54}px`;
            sp.style.setProperty('--dx', `${Math.cos(angle) * dist}px`);
            sp.style.setProperty('--dy', `${Math.sin(angle) * dist - 18}px`);
            vfxLayerEl.appendChild(sp);
            setTimeout(() => sp.remove(), 760);
        }
    }

    function spawnJoyHeartVfx(x, y) {
        if (!vfxLayerEl) return;
        const heart = document.createElement('div');
        heart.className = 'pk-vfx-heart';
        heart.innerHTML = SVG_HEART;
        heart.style.left = `${x + (Math.random() - 0.5) * 18}px`;
        heart.style.top = `${y - 68}px`;
        vfxLayerEl.appendChild(heart);
        setTimeout(() => heart.remove(), 1100);
    }

    function triggerActorJoyHop(actor) {
        if (!actor || !actor.el) return;
        actor.el.classList.remove('is-hopping');
        void actor.el.offsetWidth; // restart CSS animation
        actor.el.classList.add('is-hopping');
        spawnJoyHeartVfx(actor.screenX, actor.screenY);
        setTimeout(() => {
            if (actor.el) actor.el.classList.remove('is-hopping');
        }, 520);
    }

    // =========================================================================
    // LAYER 2 & 3: DOM ACTOR & OVERLAY LIFECYCLE
    // =========================================================================
    function createActorDom(actor) {
        const card = actor.slot.card || {};
        const visual = getRarityVisual(card.rarityName, card.rarityColorHex);

        // Layer 2: Actor Entity
        const el = document.createElement('div');
        el.className = 'pk-actor is-idle';
        el.style.setProperty('--aura-color', visual.color);

        const shadow = document.createElement('div');
        shadow.className = 'pk-actor-shadow';

        const aura = document.createElement('div');
        aura.className = visual.rainbow ? 'pk-actor-aura is-rainbow' : 'pk-actor-aura';

        const spriteWrap = document.createElement('div');
        spriteWrap.className = 'pk-actor-sprite-wrap';

        const img = document.createElement('img');
        img.className = 'pk-actor-sprite-img';
        img.alt = card.name || 'Pokémon';
        img.draggable = false;

        // Progressive fallback chain (Animated GIF -> HOME PNG -> Dex PNG -> Smart Card Crop)
        const chain = buildSpriteUrlChain(card);
        let idx = 0;
        function loadNext() {
            if (idx >= chain.length) return;
            const item = chain[idx++];
            img.classList.toggle('is-card-fallback', Boolean(item.fallbackCard));
            img.src = item.url;
        }
        img.onerror = loadNext;
        loadNext();

        spriteWrap.appendChild(img);
        el.appendChild(shadow);
        el.appendChild(aura);
        el.appendChild(spriteWrap);

        el.addEventListener('click', (e) => {
            e.stopPropagation();
            triggerActorJoyHop(actor);
            if (!isDailyCapReached() && actor.slot.readyToHarvest) {
                window.harvestSanctuarySlot(actor.slotId, actor.screenX, actor.screenY);
            } else {
                window.openSanctuaryCardInfoModal(actor.slot);
            }
        });

        actorsLayerEl.appendChild(el);
        actor.el = el;
        actor.imgEl = img;

        // Layer 3: Zzz Sleep Indicator + 3D Spinning Gold Coin (No Nameplates on Island!)
        const overlayUnit = document.createElement('div');
        overlayUnit.className = 'pk-overlay-unit';

        const zzzEl = document.createElement('div');
        zzzEl.className = 'pk-sleep-zzz hidden';
        zzzEl.innerHTML = '<span>Z</span><span>z</span><span>z</span>';

        const coinBtn = document.createElement('button');
        coinBtn.type = 'button';
        coinBtn.className = 'pk-coin-btn hidden';
        coinBtn.title = 'Bấm để thu hoạch Xu!';
        coinBtn.innerHTML = '<span class="pk-coin-disc"></span><span class="pk-coin-badge pk-num">+0 Xu</span>';
        coinBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            if (isDailyCapReached()) return;
            triggerActorJoyHop(actor);
            window.harvestSanctuarySlot(actor.slotId, actor.screenX, actor.screenY);
        });

        overlayUnit.appendChild(zzzEl);
        overlayUnit.appendChild(coinBtn);
        overlayLayerEl.appendChild(overlayUnit);

        actor.overlayEl = overlayUnit;
        actor.zzzEl = zzzEl;
        actor.coinBtnEl = coinBtn;
        actor.coinBadgeEl = coinBtn.querySelector('.pk-coin-badge');
    }

    function updateActorOverlayCoin(actor) {
        const sleeping = isDailyCapReached();
        if (actor.el) {
            actor.el.classList.toggle('is-sleeping', sleeping);
            if (sleeping) {
                actor.el.classList.remove('is-walking', 'is-idle');
            } else if (!actor.el.classList.contains('is-walking')) {
                actor.el.classList.add('is-idle');
            }
        }
        if (actor.zzzEl) {
            actor.zzzEl.classList.toggle('hidden', !sleeping);
        }
        if (!actor.coinBtnEl) return;
        if (!sleeping && actor.slot.readyToHarvest) {
            actor.coinBtnEl.classList.remove('hidden');
            if (actor.coinBadgeEl) {
                actor.coinBadgeEl.textContent = `+${formatVnd(actor.slot.pendingCoins || 0)}`;
            }
        } else {
            actor.coinBtnEl.classList.add('hidden');
        }
    }

    function syncActorsWithSlots(slots) {
        const activeIds = new Set();

        (slots || []).forEach((slot, idx) => {
            activeIds.add(slot.slotId);
            const zone = HOME_ZONES[idx % HOME_ZONES.length];
            const existing = islandActors.get(slot.slotId);
            if (existing) {
                existing.slot = slot;
                existing.homeZone = zone;
                updateActorOverlayCoin(existing);
            } else {
                const actor = {
                    slotId: slot.slotId,
                    slot: slot,
                    homeZone: zone,
                    u: zone.u,
                    v: zone.v,
                    targetU: zone.u,
                    targetV: zone.v,
                    facing: idx % 2 === 0 ? 1 : -1, // 1 = right, -1 = left
                    state: 'IDLE',
                    stateTimer: 1.4 + Math.random() * 2.2,
                    screenX: 0,
                    screenY: 0,
                    el: null,
                    imgEl: null,
                    overlayEl: null,
                    coinBtnEl: null,
                    coinBadgeEl: null
                };
                createActorDom(actor);
                updateActorOverlayCoin(actor);
                islandActors.set(slot.slotId, actor);
            }
        });

        for (const [id, actor] of Array.from(islandActors.entries())) {
            if (!activeIds.has(id)) {
                if (actor.el) actor.el.remove();
                if (actor.overlayEl) actor.overlayEl.remove();
                islandActors.delete(id);
            }
        }

        const emptyOverlay = document.getElementById('sanctuary-empty-overlay');
        if (emptyOverlay) {
            emptyOverlay.classList.toggle('hidden', (slots || []).length > 0);
        }
    }

    function pickSafeWalkTarget(actor) {
        const zone = actor.homeZone || HOME_ZONES[0];
        let bestU = zone.u;
        let bestV = zone.v;
        let bestMinDist = -1;
        let fallbackU = null;
        let fallbackV = null;

        // Thử tối đa 5 lần để chọn điểm đích cách các Pokémon khác tối thiểu ~12% chiều rộng đảo
        for (let tries = 0; tries < 5; tries++) {
            const candU = Math.max(-0.68, Math.min(0.68, (Math.random() * 1.36) - 0.68));
            const candV = Math.max(-0.60, Math.min(0.64, (Math.random() * 1.24) - 0.60));
            if (isInsidePond(candU, candV) || isNearPortal(candU, candV)) continue;

            if (fallbackU === null) {
                fallbackU = candU;
                fallbackV = candV;
            }

            let minDist = 99;
            islandActors.forEach((other) => {
                if (other.slotId === actor.slotId) return;
                const d1 = Math.hypot(candU - other.u, candV - other.v);
                const d2 = Math.hypot(candU - other.targetU, candV - other.targetV);
                minDist = Math.min(minDist, d1, d2);
            });

            if (minDist > bestMinDist) {
                bestMinDist = minDist;
                bestU = candU;
                bestV = candV;
            }
            if (minDist >= MIN_TARGET_DIST) break;
        }

        if (bestMinDist < 0 && fallbackU !== null) {
            bestU = fallbackU;
            bestV = fallbackV;
        }
        actor.targetU = bestU;
        actor.targetV = bestV;
    }

    function updatePhysicsAndDom(dt) {
        const list = Array.from(islandActors.values());
        const sleeping = isDailyCapReached();

        if (!sleeping) {
            list.forEach((actor) => {
                actor.stateTimer -= dt;
                if (actor.state === 'IDLE') {
                    if (actor.stateTimer <= 0) {
                        actor.state = 'WALK';
                        pickSafeWalkTarget(actor);
                        actor.stateTimer = 2.6 + Math.random() * 2.8;
                        if (actor.el) {
                            actor.el.classList.remove('is-idle');
                            actor.el.classList.add('is-walking');
                        }
                    }
                } else if (actor.state === 'WALK') {
                    const du = actor.targetU - actor.u;
                    const dv = actor.targetV - actor.v;
                    const dist = Math.hypot(du, dv);
                    const speed = 0.21;
                    if (dist < 0.035 || actor.stateTimer <= 0) {
                        actor.state = 'IDLE';
                        actor.stateTimer = 2.0 + Math.random() * 3.0;
                        if (actor.el) {
                            actor.el.classList.remove('is-walking');
                            actor.el.classList.add('is-idle');
                        }
                    } else {
                        const stepU = (du / dist) * speed * dt;
                        const stepV = (dv / dist) * speed * dt;
                        const screenDx = stepU - stepV;
                        if (Math.abs(screenDx) > 0.0005) {
                            actor.facing = screenDx >= 0 ? 1 : -1;
                        }
                        actor.u += stepU;
                        actor.v += stepV;
                    }
                }
            });
        }

        // Giữ Pokémon trong phạm vi mặt đảo & né vật cản cố định (không va chạm chặn nhau)
        list.forEach((actor) => {
            clampToIsland(actor);
            const p = isoToScreen(actor.u, actor.v);
            actor.screenX = p.x;
            actor.screenY = p.y;
        });

        // Y-sorting động theo tọa độ screenY (con đứng thấp hơn có z-index cao hơn)
        const sortedByY = list.slice().sort((a, b) => a.screenY - b.screenY);
        sortedByY.forEach((actor, idx) => {
            const z = 10 + idx;
            if (actor.el) {
                actor.el.style.transform = `translate3d(${actor.screenX.toFixed(1)}px, ${actor.screenY.toFixed(1)}px, 0)`;
                actor.el.style.zIndex = String(z);
            }
            if (actor.imgEl && !actor.imgEl.classList.contains('is-card-fallback')) {
                // Showdown sprites naturally face left -> flip when walking right (facing === 1)
                actor.imgEl.style.transform = actor.facing === 1 ? 'scaleX(-1)' : 'scaleX(1)';
            }
            if (actor.overlayEl) {
                actor.overlayEl.style.transform = `translate3d(${actor.screenX.toFixed(1)}px, ${actor.screenY.toFixed(1)}px, 0)`;
                actor.overlayEl.style.zIndex = String(z);
            }
        });

        // Clouds & ambient motes
        clouds.forEach((c) => {
            c.x += c.speed * dt;
            if (c.x > 1.18) c.x = -0.18;
        });

        ambientMotes.forEach((m) => {
            m.x += m.vx * dt;
            m.y += m.vy * dt;
            if (m.x < -0.05) m.x = 1.05;
            if (m.x > 1.05) m.x = -0.05;
            if (m.y < 0.15) m.y = 0.85;
        });
    }

    // =========================================================================
    // LAYER 1: HIGH-ART FLOATING SKY ISLAND & ATMOSPHERE CANVAS
    // =========================================================================
    function getPalette() {
        if (atmosphere === 'sunset') {
            return {
                skyTop: '#311042',
                skyMid: '#be4b62',
                skyBot: '#f9a86b',
                cloudColor: 'rgba(255, 224, 198, 0.72)',
                sunbeam: 'rgba(253, 186, 116, 0.12)',
                grassTop: '#65a30d',
                grassMid: '#4d7c0f',
                grassBot: '#365314',
                cliffL: '#7c2d12',
                cliffR: '#581c0c',
                cliffDeep: '#3b1208',
                waterIn: '#fb923c',
                waterOut: '#7c3aed',
                tree1: '#365314',
                tree2: '#4d7c0f',
                tree3: '#84cc16',
                mote: '#fde047'
            };
        }
        if (atmosphere === 'night') {
            return {
                skyTop: '#060a1c',
                skyMid: '#0f1b3d',
                skyBot: '#16324f',
                cloudColor: 'rgba(148, 163, 184, 0.14)',
                sunbeam: 'rgba(186, 230, 253, 0.06)',
                grassTop: '#0f766e',
                grassMid: '#065f46',
                grassBot: '#064e3b',
                cliffL: '#334155',
                cliffR: '#1e293b',
                cliffDeep: '#0f172a',
                waterIn: '#38bdf8',
                waterOut: '#0c4a6e',
                tree1: '#064e3b',
                tree2: '#047857',
                tree3: '#10b981',
                mote: '#a7f3d0'
            };
        }
        // Day (default)
        return {
            skyTop: '#38bdf8',
            skyMid: '#7dd3fc',
            skyBot: '#dcfce7',
            cloudColor: 'rgba(255, 255, 255, 0.88)',
            sunbeam: 'rgba(255, 251, 215, 0.16)',
            grassTop: '#86efac',
            grassMid: '#4ade80',
            grassBot: '#16a34a',
            cliffL: '#b45309',
            cliffR: '#92400e',
            cliffDeep: '#5c280a',
            waterIn: '#7dd3fc',
            waterOut: '#0284c7',
            tree1: '#15803d',
            tree2: '#22c55e',
            tree3: '#86efac',
            mote: '#fef9c3'
        };
    }

    function drawSkyAndAtmosphere(pal) {
        // Sky gradient
        const sky = ctx.createLinearGradient(0, 0, 0, canvasHeight);
        sky.addColorStop(0, pal.skyTop);
        sky.addColorStop(0.55, pal.skyMid);
        sky.addColorStop(1, pal.skyBot);
        ctx.fillStyle = sky;
        ctx.fillRect(0, 0, canvasWidth, canvasHeight);

        // Night stars & crescent moon OR warm glowing sun
        if (atmosphere === 'night') {
            stars.forEach((s) => {
                const twinkle = 0.4 + 0.6 * Math.abs(Math.sin(elapsedTime * 1.8 + s.phase));
                ctx.fillStyle = `rgba(248, 250, 252, ${twinkle.toFixed(2)})`;
                ctx.beginPath();
                ctx.arc(s.x * canvasWidth, s.y * canvasHeight, s.r, 0, Math.PI * 2);
                ctx.fill();
            });
            // Glowing moon
            const mx = canvasWidth * 0.84;
            const my = canvasHeight * 0.18;
            const mg = ctx.createRadialGradient(mx, my, 4, mx, my, 48);
            mg.addColorStop(0, 'rgba(224, 242, 254, 0.95)');
            mg.addColorStop(0.35, 'rgba(186, 230, 253, 0.35)');
            mg.addColorStop(1, 'rgba(186, 230, 253, 0)');
            ctx.fillStyle = mg;
            ctx.beginPath();
            ctx.arc(mx, my, 48, 0, Math.PI * 2);
            ctx.fill();
        } else {
            const sx = canvasWidth * (atmosphere === 'sunset' ? 0.5 : 0.16);
            const sy = canvasHeight * (atmosphere === 'sunset' ? 0.30 : 0.15);
            const sg = ctx.createRadialGradient(sx, sy, 6, sx, sy, 90);
            sg.addColorStop(0, atmosphere === 'sunset' ? 'rgba(254, 240, 138, 0.92)' : 'rgba(255, 255, 240, 0.92)');
            sg.addColorStop(0.4, atmosphere === 'sunset' ? 'rgba(251, 146, 60, 0.32)' : 'rgba(254, 249, 195, 0.28)');
            sg.addColorStop(1, 'rgba(255, 255, 255, 0)');
            ctx.fillStyle = sg;
            ctx.beginPath();
            ctx.arc(sx, sy, 90, 0, Math.PI * 2);
            ctx.fill();
        }

        // Diagonal Soft Sunbeams / Moonbeams
        ctx.save();
        ctx.fillStyle = pal.sunbeam;
        for (let i = 0; i < 3; i++) {
            const bx = canvasWidth * (0.14 + i * 0.28);
            ctx.beginPath();
            ctx.moveTo(bx, 0);
            ctx.lineTo(bx + 68, 0);
            ctx.lineTo(bx - 60, canvasHeight);
            ctx.lineTo(bx - 130, canvasHeight);
            ctx.closePath();
            ctx.fill();
        }
        ctx.restore();

        // Parallax Clouds
        clouds.forEach((c) => {
            ctx.save();
            ctx.translate(c.x * canvasWidth, c.y * canvasHeight);
            ctx.scale(c.scale, c.scale);
            ctx.globalAlpha = c.alpha;
            ctx.fillStyle = pal.cloudColor;
            ctx.beginPath();
            ctx.arc(0, 0, 18, 0, Math.PI * 2);
            ctx.arc(20, -7, 24, 0, Math.PI * 2);
            ctx.arc(43, -2, 19, 0, Math.PI * 2);
            ctx.arc(22, 7, 17, 0, Math.PI * 2);
            ctx.fill();
            ctx.restore();
        });
    }

    function drawFloatingSkyIsland(pal) {
        const bob = Math.sin(elapsedTime * 1.1) * 2.5;
        const top = isoToScreen(-1, -1);
        const right = isoToScreen(1, -1);
        const bottom = isoToScreen(1, 1);
        const left = isoToScreen(-1, 1);
        const cliffH = Math.max(38, canvasHeight * 0.12);

        ctx.save();
        ctx.translate(0, bob);

        // Companion Tiny Floating Pebble Islets (adds magical sky-island depth!)
        const miniIslets = [
            { x: left.x - 36, y: left.y + 34 + Math.sin(elapsedTime * 1.6) * 4, r: 14 },
            { x: right.x + 34, y: right.y + 24 + Math.cos(elapsedTime * 1.4) * 4, r: 12 }
        ];
        miniIslets.forEach((mi) => {
            ctx.beginPath();
            ctx.moveTo(mi.x - mi.r, mi.y);
            ctx.lineTo(mi.x, mi.y + mi.r * 1.3);
            ctx.lineTo(mi.x + mi.r, mi.y);
            ctx.closePath();
            ctx.fillStyle = pal.cliffR;
            ctx.fill();
            ctx.beginPath();
            ctx.ellipse(mi.x, mi.y, mi.r, mi.r * 0.45, 0, 0, Math.PI * 2);
            ctx.fillStyle = pal.grassMid;
            ctx.fill();
        });

        // Deep Floating Rocky Spire Underside (Multi-tier tapered cliff)
        ctx.beginPath();
        ctx.moveTo(left.x + 8, left.y + 10);
        ctx.lineTo(left.x + (bottom.x - left.x) * 0.35, bottom.y + cliffH * 0.85);
        ctx.lineTo(bottom.x, bottom.y + cliffH * 1.55);
        ctx.lineTo(bottom.x + (right.x - bottom.x) * 0.65, bottom.y + cliffH * 0.85);
        ctx.lineTo(right.x - 8, right.y + 10);
        ctx.closePath();
        ctx.fillStyle = pal.cliffDeep;
        ctx.fill();

        // Mid Rocky Strata Left
        ctx.beginPath();
        ctx.moveTo(left.x, left.y);
        ctx.lineTo(bottom.x, bottom.y);
        ctx.lineTo(bottom.x, bottom.y + cliffH);
        ctx.lineTo(left.x + (bottom.x - left.x) * 0.25, left.y + cliffH * 0.92);
        ctx.lineTo(left.x + 4, left.y + cliffH * 0.48);
        ctx.closePath();
        ctx.fillStyle = pal.cliffL;
        ctx.fill();

        // Mid Rocky Strata Right
        ctx.beginPath();
        ctx.moveTo(right.x, right.y);
        ctx.lineTo(bottom.x, bottom.y);
        ctx.lineTo(bottom.x, bottom.y + cliffH);
        ctx.lineTo(right.x - (right.x - bottom.x) * 0.25, right.y + cliffH * 0.92);
        ctx.lineTo(right.x - 4, right.y + cliffH * 0.48);
        ctx.closePath();
        ctx.fillStyle = pal.cliffR;
        ctx.fill();

        // Rounded Isometric Grass Plateau (Smooth natural island edges)
        const grassGrad = ctx.createLinearGradient(top.x, top.y, bottom.x, bottom.y);
        grassGrad.addColorStop(0, pal.grassTop);
        grassGrad.addColorStop(0.55, pal.grassMid);
        grassGrad.addColorStop(1, pal.grassBot);

        ctx.beginPath();
        const r = 0.08;
        const pTL = { x: top.x + (left.x - top.x) * r, y: top.y + (left.y - top.y) * r };
        const pTR = { x: top.x + (right.x - top.x) * r, y: top.y + (right.y - top.y) * r };
        const pRT = { x: right.x + (top.x - right.x) * r, y: right.y + (top.y - right.y) * r };
        const pRB = { x: right.x + (bottom.x - right.x) * r, y: right.y + (bottom.y - right.y) * r };
        const pBR = { x: bottom.x + (right.x - bottom.x) * r, y: bottom.y + (right.y - bottom.y) * r };
        const pBL = { x: bottom.x + (left.x - bottom.x) * r, y: bottom.y + (left.y - bottom.y) * r };
        const pLB = { x: left.x + (bottom.x - left.x) * r, y: left.y + (bottom.y - left.y) * r };
        const pLT = { x: left.x + (top.x - left.x) * r, y: left.y + (top.y - left.y) * r };

        ctx.moveTo(pTR.x, pTR.y);
        ctx.lineTo(pRT.x, pRT.y);
        ctx.quadraticCurveTo(right.x, right.y, pRB.x, pRB.y);
        ctx.lineTo(pBR.x, pBR.y);
        ctx.quadraticCurveTo(bottom.x, bottom.y, pBL.x, pBL.y);
        ctx.lineTo(pLB.x, pLB.y);
        ctx.quadraticCurveTo(left.x, left.y, pLT.x, pLT.y);
        ctx.lineTo(pTL.x, pTL.y);
        ctx.quadraticCurveTo(top.x, top.y, pTR.x, pTR.y);
        ctx.closePath();

        ctx.fillStyle = grassGrad;
        ctx.fill();
        ctx.lineWidth = 2.5;
        ctx.strokeStyle = 'rgba(255, 255, 255, 0.35)';
        ctx.stroke();

        // Natural sunlit meadow patches (organic shading)
        const patch1 = isoToScreen(-0.18, 0.15);
        const pg = ctx.createRadialGradient(patch1.x, patch1.y, 10, patch1.x, patch1.y, 110);
        pg.addColorStop(0, 'rgba(255, 255, 255, 0.14)');
        pg.addColorStop(1, 'rgba(255, 255, 255, 0)');
        ctx.fillStyle = pg;
        ctx.beginPath();
        ctx.ellipse(patch1.x, patch1.y, 115, 52, 0, 0, Math.PI * 2);
        ctx.fill();

        // Cobblestone Path leading to the Ancient Portal
        cobblestonePath.forEach((st) => {
            const p = isoToScreen(st.u, st.v);
            ctx.fillStyle = 'rgba(15, 23, 42, 0.14)';
            ctx.beginPath();
            ctx.ellipse(p.x + 1, p.y + 2, st.rx, st.ry, 0, 0, Math.PI * 2);
            ctx.fill();

            ctx.fillStyle = atmosphere === 'night' ? '#94a3b8' : '#e2e8f0';
            ctx.beginPath();
            ctx.ellipse(p.x, p.y, st.rx, st.ry, 0, 0, Math.PI * 2);
            ctx.fill();
        });

        // Translucent Crystal Pond with Lily Pads, Lotus & Ripples
        const pond = isoToScreen(0.46, -0.36);
        ctx.save();
        ctx.beginPath();
        ctx.ellipse(pond.x, pond.y, 54, 25, -0.12, 0, Math.PI * 2);
        ctx.fillStyle = atmosphere === 'night' ? 'rgba(71, 85, 105, 0.5)' : 'rgba(254, 240, 138, 0.58)';
        ctx.fill();

        const wg = ctx.createRadialGradient(pond.x - 12, pond.y - 4, 4, pond.x, pond.y, 48);
        wg.addColorStop(0, pal.waterIn);
        wg.addColorStop(1, pal.waterOut);
        ctx.beginPath();
        ctx.ellipse(pond.x, pond.y, 48, 21, -0.12, 0, Math.PI * 2);
        ctx.fillStyle = wg;
        ctx.fill();

        for (let rIdx = 0; rIdx < 2; rIdx++) {
            const ph = ((elapsedTime * 11) + rIdx * 17) % 34;
            const alpha = Math.max(0, 1 - ph / 34) * 0.55;
            ctx.beginPath();
            ctx.ellipse(pond.x - 4, pond.y, 7 + ph, (7 + ph) * 0.42, -0.12, 0, Math.PI * 2);
            ctx.strokeStyle = `rgba(255, 255, 255, ${alpha.toFixed(2)})`;
            ctx.lineWidth = 1.4;
            ctx.stroke();
        }

        // Lily pads + tiny pink lotus blossom
        ctx.fillStyle = '#22c55e';
        ctx.beginPath();
        ctx.ellipse(pond.x + 20, pond.y + 6, 8, 4, 0.2, 0.4, Math.PI * 2);
        ctx.fill();
        ctx.beginPath();
        ctx.ellipse(pond.x - 20, pond.y - 4, 6.5, 3.2, -0.2, 0.5, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = '#f472b6';
        ctx.beginPath();
        ctx.arc(pond.x + 20, pond.y + 4, 3.2, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();

        // Flowers & Mossy Boulders
        islandDecor.forEach((d) => {
            const p = isoToScreen(d.u, d.v);
            if (d.type === 'flower') {
                ctx.fillStyle = '#15803d';
                ctx.fillRect(p.x - 0.8, p.y - 4, 1.6, 5);
                ctx.fillStyle = d.color;
                ctx.beginPath();
                ctx.arc(p.x, p.y - 5.5, 3.4, 0, Math.PI * 2);
                ctx.fill();
                ctx.fillStyle = '#fef9c3';
                ctx.beginPath();
                ctx.arc(p.x, p.y - 5.5, 1.4, 0, Math.PI * 2);
                ctx.fill();
            } else if (d.type === 'rock') {
                const sc = d.scale || 1;
                ctx.fillStyle = 'rgba(15, 23, 42, 0.2)';
                ctx.beginPath();
                ctx.ellipse(p.x + 1, p.y + 3, 11 * sc, 5 * sc, 0, 0, Math.PI * 2);
                ctx.fill();
                ctx.fillStyle = '#64748b';
                ctx.beginPath();
                ctx.ellipse(p.x, p.y, 10 * sc, 6 * sc, -0.1, 0, Math.PI * 2);
                ctx.fill();
                ctx.fillStyle = '#94a3b8';
                ctx.beginPath();
                ctx.ellipse(p.x - 2 * sc, p.y - 2 * sc, 5 * sc, 2.5 * sc, -0.1, 0, Math.PI * 2);
                ctx.fill();
            }
        });

        // Ancient Expedition Portal
        drawAncientPortal(isoToScreen(-0.70, -0.70));

        // Multi-Puff Rounded Deciduous Trees (Organic Lush Canopy)
        islandTrees.forEach((t) => drawRoundedTree(isoToScreen(t.u, t.v), t.scale, pal));

        ctx.restore();

        // Floating Fireflies (night) or Golden Pollen Motes (day/sunset)
        ambientMotes.forEach((m) => {
            const glow = 0.45 + 0.55 * Math.sin(elapsedTime * 3.2 + m.phase);
            ctx.save();
            ctx.globalAlpha = atmosphere === 'night' ? glow : glow * 0.6;
            ctx.fillStyle = pal.mote;
            ctx.shadowColor = pal.mote;
            ctx.shadowBlur = atmosphere === 'night' ? 10 : 4;
            ctx.beginPath();
            ctx.arc(m.x * canvasWidth, m.y * canvasHeight, m.size, 0, Math.PI * 2);
            ctx.fill();
            ctx.restore();
        });
    }

    function drawRoundedTree(pos, scale, pal) {
        ctx.save();
        ctx.translate(pos.x, pos.y);
        ctx.scale(scale, scale);

        // Soft elliptical ground shadow
        ctx.fillStyle = 'rgba(15, 23, 42, 0.24)';
        ctx.beginPath();
        ctx.ellipse(4, 5, 24, 9.5, 0, 0, Math.PI * 2);
        ctx.fill();

        // Tapered wooden trunk
        ctx.fillStyle = '#78350f';
        ctx.beginPath();
        ctx.moveTo(-4.5, 3);
        ctx.lineTo(-3, -18);
        ctx.lineTo(3, -18);
        ctx.lineTo(5, 3);
        ctx.closePath();
        ctx.fill();

        // Back/bottom shaded foliage puffs
        ctx.fillStyle = pal.tree1;
        ctx.beginPath();
        ctx.arc(-13, -22, 13, 0, Math.PI * 2);
        ctx.arc(13, -21, 12.5, 0, Math.PI * 2);
        ctx.arc(0, -18, 14, 0, Math.PI * 2);
        ctx.fill();

        // Mid lush canopy puffs
        ctx.fillStyle = pal.tree2;
        ctx.beginPath();
        ctx.arc(-9, -28, 13.5, 0, Math.PI * 2);
        ctx.arc(10, -27, 13, 0, Math.PI * 2);
        ctx.arc(0, -35, 14.5, 0, Math.PI * 2);
        ctx.fill();

        // Top sunlit highlight canopy
        ctx.fillStyle = pal.tree3;
        ctx.beginPath();
        ctx.arc(-4, -37, 8.5, 0, Math.PI * 2);
        ctx.arc(6, -33, 6.5, 0, Math.PI * 2);
        ctx.fill();

        ctx.restore();
    }

    function drawAncientPortal(pos) {
        ctx.save();
        const pulse = 0.65 + 0.35 * Math.sin(elapsedTime * 3.0);

        // Runic floor circle
        ctx.beginPath();
        ctx.ellipse(pos.x, pos.y + 4, 30, 11, 0, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(168, 85, 247, ${(0.28 * pulse).toFixed(2)})`;
        ctx.fill();

        // Portal energy vortex
        ctx.beginPath();
        ctx.ellipse(pos.x, pos.y - 20, 16, 22, 0, 0, Math.PI * 2);
        const pg = ctx.createRadialGradient(pos.x, pos.y - 20, 2, pos.x, pos.y - 20, 22);
        pg.addColorStop(0, '#fdf4ff');
        pg.addColorStop(0.45, '#c084fc');
        pg.addColorStop(0.85, '#6b21a8');
        pg.addColorStop(1, 'rgba(88, 28, 135, 0.1)');
        ctx.fillStyle = pg;
        ctx.fill();

        // Rotating mystic ring
        ctx.save();
        ctx.translate(pos.x, pos.y - 20);
        ctx.rotate(elapsedTime * 1.9);
        ctx.strokeStyle = 'rgba(255, 255, 255, 0.8)';
        ctx.lineWidth = 1.8;
        ctx.setLineDash([5, 5]);
        ctx.beginPath();
        ctx.arc(0, 0, 11, 0, Math.PI * 2);
        ctx.stroke();
        ctx.restore();

        // Ancient carved stone arch
        ctx.strokeStyle = atmosphere === 'night' ? '#cbd5e1' : '#475569';
        ctx.lineWidth = 5.5;
        ctx.beginPath();
        ctx.arc(pos.x, pos.y - 16, 20, Math.PI, 0);
        ctx.stroke();

        // Orbiting golden rune
        const ox = pos.x + Math.cos(elapsedTime * 2.5) * 23;
        const oy = pos.y - 22 + Math.sin(elapsedTime * 2.5) * 9;
        ctx.fillStyle = '#fde047';
        ctx.beginPath();
        ctx.arc(ox, oy, 2.8, 0, Math.PI * 2);
        ctx.fill();

        ctx.restore();
    }

    function renderFrame() {
        if (!ctx) return;
        const pal = getPalette();
        ctx.clearRect(0, 0, canvasWidth, canvasHeight);
        drawSkyAndAtmosphere(pal);
        drawFloatingSkyIsland(pal);
    }

    function gameLoop(now) {
        if (isPaused || document.hidden) {
            rafId = null;
            return;
        }
        const dt = Math.min(0.05, Math.max(0, (now - lastFrameTime) / 1000));
        lastFrameTime = now;
        elapsedTime += dt;

        updatePhysicsAndDom(dt);
        renderFrame();

        rafId = requestAnimationFrame(gameLoop);
    }

    // =========================================================================
    // ENGINE BOOTSTRAP, RESIZE & ATMOSPHERE SWITCHING
    // =========================================================================
    function setupDiorama() {
        canvas = document.getElementById('sanctuary-canvas');
        actorsLayerEl = document.getElementById('sanctuary-actors-layer');
        overlayLayerEl = document.getElementById('sanctuary-overlay-layer');
        vfxLayerEl = document.getElementById('sanctuary-vfx-layer');
        if (!canvas) return;
        ctx = canvas.getContext('2d');

        // Default atmosphere matches site theme (dark -> night, light -> day)
        const siteTheme = document.documentElement.getAttribute('data-theme');
        setAtmosphere(siteTheme === 'dark' ? 'night' : 'day');

        document.querySelectorAll('.pk-atmos-btn').forEach((btn) => {
            btn.addEventListener('click', () => {
                setAtmosphere(btn.getAttribute('data-atmos') || 'day');
            });
        });

        resizeCanvas();
        window.addEventListener('resize', resizeCanvas);
        document.addEventListener('visibilitychange', handleVisibilityChange);
        window.addEventListener('pagehide', cleanupEngine);
        window.addEventListener('beforeunload', cleanupEngine);

        lastFrameTime = performance.now();
        rafId = requestAnimationFrame(gameLoop);
    }

    function setAtmosphere(mode) {
        atmosphere = mode;
        document.querySelectorAll('.pk-atmos-btn').forEach((btn) => {
            btn.classList.toggle('is-active', btn.getAttribute('data-atmos') === mode);
        });
    }

    function resizeCanvas() {
        if (!canvas || !ctx) return;
        const rect = canvas.parentElement.getBoundingClientRect();
        const dpr = Math.min(window.devicePixelRatio || 1, 2);
        canvasWidth = Math.max(320, Math.floor(rect.width));
        canvasHeight = Math.max(280, Math.floor(rect.height));
        canvas.width = Math.floor(canvasWidth * dpr);
        canvas.height = Math.floor(canvasHeight * dpr);
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    }

    function handleVisibilityChange() {
        if (document.hidden) {
            isPaused = true;
            if (rafId !== null) {
                cancelAnimationFrame(rafId);
                rafId = null;
            }
        } else {
            isPaused = false;
            lastFrameTime = performance.now();
            if (rafId === null) {
                rafId = requestAnimationFrame(gameLoop);
            }
        }
    }

    function cleanupEngine() {
        if (rafId) {
            cancelAnimationFrame(rafId);
            rafId = null;
        }
        if (countdownTimerId) {
            clearInterval(countdownTimerId);
            countdownTimerId = null;
        }
    }

    // =========================================================================
    // API INTERACTIONS & HUD RENDERING
    // =========================================================================
    async function loadSanctuaryState() {
        try {
            const res = await fetch('/api/v1/sanctuary/state');
            if (!res.ok) throw new Error('Không thể tải dữ liệu Đảo Pokémon');
            sanctuaryState = await res.json();
            renderAllHud();
            syncActorsWithSlots(sanctuaryState.slots);
        } catch (err) {
            console.error(err);
            if (window.showToast) window.showToast('error', 'Không thể tải Đảo Pokémon', err.message || 'Vui lòng thử lại sau.');
        }
    }

    function estimateCoinsPerMinute(card) {
        const dbRate = Number(card && card.raritySanctuaryCoinsPerMin);
        return Number.isFinite(dbRate) && dbRate > 0 ? dbRate : 2;
    }

    let activeInfoModalSlotId = null;

    window.openSanctuaryCardInfoModal = function (slot) {
        if (!slot || !slot.card) return;
        activeInfoModalSlotId = slot.slotId;
        const c = slot.card;
        const visual = getRarityVisual(c.rarityName, c.rarityColorHex);
        const accentColor = (c.rarityColorHex && /^#[0-9a-fA-F]{6}$/.test(c.rarityColorHex)) ? c.rarityColorHex : visual.color;
        const nameEl = document.getElementById('sanctuary-card-modal-name');
        const metaEl = document.getElementById('sanctuary-card-modal-meta');
        const rarityEl = document.getElementById('sanctuary-card-modal-rarity');
        const frameEl = document.getElementById('sanctuary-card-modal-frame');
        const imgEl = document.getElementById('sanctuary-card-modal-img');
        const rateEl = document.getElementById('sanctuary-card-modal-rate');
        const linkEl = document.getElementById('sanctuary-card-modal-link');
        const modal = document.getElementById('modal-sanctuary-card-info');

        if (nameEl) nameEl.textContent = c.name || 'Pokémon';
        if (metaEl) {
            const parts = [c.setName, c.formattedCardNumber].filter(Boolean);
            metaEl.textContent = parts.join(' • ');
            metaEl.classList.toggle('hidden', parts.length === 0);
        }
        if (rarityEl) {
            rarityEl.textContent = c.rarityName || 'Common';
            rarityEl.style.borderColor = accentColor;
            rarityEl.style.color = accentColor;
            rarityEl.style.backgroundColor = '';
        }
        if (frameEl) {
            const isHolo = Boolean(c.rarityIsHolographic);
            frameEl.classList.toggle('pk-holo-card', isHolo);
            frameEl.classList.toggle('is-holo-active', isHolo);
            frameEl.style.transform = '';
            if (isHolo) {
                frameEl.style.borderColor = accentColor;
                frameEl.style.boxShadow = `0 0 26px -2px ${accentColor}99, 0 14px 28px -8px rgba(0, 0, 0, 0.45)`;
                if (typeof window.bindHoloTiltEffects === 'function' && frameEl.parentElement) {
                    window.bindHoloTiltEffects(frameEl.parentElement);
                }
            } else {
                frameEl.style.borderColor = '';
                frameEl.style.boxShadow = '';
            }
        }
        if (imgEl) imgEl.src = c.imageUrl || FALLBACK_ART;
        const rate = slot.coinPerTick || estimateCoinsPerMinute(c);
        if (rateEl) rateEl.textContent = `+${formatVnd(rate)}/phút`;
        if (linkEl) linkEl.href = c.id ? `/cards/${c.id}` : '/inventory';
        if (modal) modal.classList.remove('hidden');
    };

    window.closeSanctuaryCardInfoModal = function () {
        activeInfoModalSlotId = null;
        document.getElementById('modal-sanctuary-card-info')?.classList.add('hidden');
    };

    window.removeCardFromInfoModal = async function () {
        const slotId = activeInfoModalSlotId;
        if (!slotId) return;
        const btn = document.getElementById('btn-sanctuary-modal-remove');
        if (btn) btn.disabled = true;
        try {
            await window.removeSanctuarySlot(slotId);
            window.closeSanctuaryCardInfoModal();
        } finally {
            if (btn) btn.disabled = false;
        }
    };

    window.openIslandCardPicker = function () {
        if (!sanctuaryState) return;
        const slots = sanctuaryState.slots || [];
        const maxSlots = sanctuaryState.maxSlots || 5;
        if (slots.length >= maxSlots) {
            if (window.showToast) window.showToast('info', 'Đảo đã đầy', `Đảo đã đạt tối đa ${maxSlots} Pokémon!`);
            return;
        }
        const searchEl = document.getElementById('picker-search-input');
        const rarityEl = document.getElementById('picker-rarity-filter');
        if (searchEl) searchEl.value = '';

        // Populate full rarities dynamically from DB (sorted Common -> Mega Hyper Rare by dropWeight DESC)
        if (rarityEl) {
            const dbRarities = Array.isArray(sanctuaryState.rarities) && sanctuaryState.rarities.length > 0
                ? sanctuaryState.rarities.map((r) => r.name).filter(Boolean)
                : Array.from(new Set(
                    (sanctuaryState.availableCards || [])
                        .map((uc) => (uc.card && uc.card.rarityName) ? uc.card.rarityName : 'Common')
                ));
            rarityEl.innerHTML = '<option value="">Tất cả độ hiếm</option>' +
                dbRarities.map((r) => `<option value="${window.escapeHtml(r)}">${window.escapeHtml(r)}</option>`).join('');
            rarityEl.value = '';
        }

        renderPickerModalGrid();
        document.getElementById('modal-pick-island-card').classList.remove('hidden');
        if (searchEl) searchEl.focus();
    };

    window.closeIslandCardPicker = function () {
        const modal = document.getElementById('modal-pick-island-card');
        if (modal) modal.classList.add('hidden');
    };

    function renderPickerModalGrid() {
        const grid = document.getElementById('picker-cards-grid');
        const countBadge = document.getElementById('picker-available-count');
        if (!grid || !sanctuaryState) return;

        const query = (document.getElementById('picker-search-input')?.value || '').trim().toLowerCase();
        const rarityFilter = document.getElementById('picker-rarity-filter')?.value || '';

        const rawCards = (sanctuaryState.availableCards || []).slice();
        if (countBadge) countBadge.textContent = `${rawCards.length} thẻ rảnh`;

        // Group duplicate cards by template id (card.id) with copy count badge x2, x3...
        const groupedMap = new Map();
        rawCards.forEach((uc) => {
            const c = uc.card || {};
            const key = c.id != null ? `tpl-${c.id}` : `uc-${uc.userCardId}`;
            if (!groupedMap.has(key)) {
                groupedMap.set(key, { uc, count: 1 });
            } else {
                groupedMap.get(key).count += 1;
            }
        });

        // Filter & sort by highest coins/minute DESC, then card name ASC
        const filtered = Array.from(groupedMap.values())
            .filter(({ uc }) => {
                const c = uc.card || {};
                const nameMatch = !query || (c.name || '').toLowerCase().includes(query) || (c.speciesName || '').toLowerCase().includes(query);
                const rarityMatch = !rarityFilter || (c.rarityName || 'Common') === rarityFilter;
                return nameMatch && rarityMatch;
            })
            .sort((a, b) => {
                const rateDiff = estimateCoinsPerMinute(b.uc.card) - estimateCoinsPerMinute(a.uc.card);
                if (rateDiff !== 0) return rateDiff;
                return ((a.uc.card && a.uc.card.name) || '').localeCompare((b.uc.card && b.uc.card.name) || '');
            });

        if (filtered.length === 0) {
            grid.innerHTML = '<div class="col-span-full pk-surface-muted rounded-2xl p-8 text-center text-xs pk-text-secondary">Không tìm thấy thẻ bài phù hợp trong Kho đồ (hãy thử đổi bộ lọc hoặc mở thêm gói bài).</div>';
            return;
        }

        grid.innerHTML = filtered.map(({ uc, count }) => {
            const c = uc.card || {};
            const safeName = window.escapeHtml(c.name || 'Pokémon');
            const safeRarity = window.escapeHtml(c.rarityName || 'Common');
            const visual = getRarityVisual(c.rarityName, c.rarityColorHex);
            const rate = estimateCoinsPerMinute(c);
            const safeCardImg = window.escapeHtml(c.imageUrl || FALLBACK_ART);
            const qtyBadge = count > 1
                ? `<span class="pk-num absolute top-2 right-2 z-10 rounded-full bg-slate-950/85 px-2 py-0.5 text-[10px] font-extrabold text-amber-400 border border-white/15 shadow">x${count}</span>`
                : '';

            return `
                <button type="button" onclick="window.placeCardFromPicker(${uc.userCardId})"
                        class="group relative pk-surface-elevated hover:border-amber-500/80 border border-slate-300/35 dark:border-slate-700/60 rounded-2xl p-2 flex flex-col items-center text-center gap-2 cursor-pointer transition hover:-translate-y-0.5 hover:shadow-lg">
                    ${qtyBadge}
                    <div class="w-[88%] aspect-[2.5/3.5] rounded-xl overflow-hidden pk-surface-muted shadow-md transition group-hover:scale-[1.03] shrink-0">
                        <img src="${safeCardImg}" alt="${safeName}" loading="lazy" decoding="async"
                             onerror="this.onerror=null;this.src='${FALLBACK_ART}';"
                             class="h-full w-full object-cover"/>
                    </div>
                    <div class="w-full px-1 space-y-1">
                        <div class="text-xs font-extrabold pk-text-primary truncate" title="${safeName}">${safeName}</div>
                        <div class="flex items-center justify-center">
                            <span class="pk-rarity-badge px-2 py-0.5 text-[10px] truncate max-w-full"
                                  style="border-color: ${visual.color}; color: ${visual.color}">
                                ${safeRarity}
                            </span>
                        </div>
                        <div class="pk-num tabular-nums text-[11px] font-extrabold text-amber-600 dark:text-amber-400">
                            +${formatVnd(rate)}/phút
                        </div>
                    </div>
                </button>
            `;
        }).join('');
    }

    function renderAllHud() {
        if (!sanctuaryState) return;
        renderDailyCap();
        renderIslandSlotsHud();
        renderExpeditionsHud();
        renderQuestsHud();
        if (window.updateNavbarWallet && sanctuaryState.walletBalance != null) {
            window.updateNavbarWallet(sanctuaryState.walletBalance);
        }
    }

    function renderDailyCap() {
        const earned = Number(sanctuaryState.dailyEarned || 0);
        const cap = Number(sanctuaryState.dailyCap || 1000);
        const pct = Math.min(100, Math.max(2, Math.round((earned / cap) * 100)));
        const txt = document.getElementById('sanctuary-daily-cap-text');
        const bar = document.getElementById('sanctuary-daily-cap-bar');
        const sleepPill = document.getElementById('sanctuary-cap-sleep-pill');
        if (txt) txt.textContent = `${earned.toLocaleString('vi-VN')} / ${cap.toLocaleString('vi-VN')} Xu`;
        if (bar) bar.style.width = `${pct}%`;

        const sleeping = isDailyCapReached();
        if (sleepPill) sleepPill.classList.toggle('hidden', !sleeping);
    }

    function renderIslandSlotsHud() {
        const slots = sanctuaryState.slots || [];
        const tickSec = Number(sanctuaryState.harvestTickSeconds || 60);
        const sleeping = isDailyCapReached();

        const maxSlots = sanctuaryState.maxSlots || 5;
        const badge = document.getElementById('sanctuary-slot-badge');
        if (badge) badge.textContent = `${slots.length}/${maxSlots}`;

        const totalRatePerMin = slots.reduce((sum, s) => sum + Number(s.coinPerTick || 0), 0);
        const totalRateBadge = document.getElementById('sanctuary-total-rate-badge');
        if (totalRateBadge) {
            totalRateBadge.textContent = `Tổng: +${totalRatePerMin.toLocaleString('vi-VN')} Xu/phút`;
        }

        const totalPending = slots.reduce((sum, s) => sum + (s.readyToHarvest ? Number(s.pendingCoins || 0) : 0), 0);
        const readyCount = slots.filter((s) => s.readyToHarvest).length;
        const btnAll = document.getElementById('btn-harvest-all');
        const btnAllLabel = document.getElementById('btn-harvest-all-label');
        const totalEl = document.getElementById('sanctuary-total-pending');

        if (sleeping) {
            if (btnAll) {
                btnAll.disabled = true;
                btnAll.className = 'w-full rounded-xl pk-surface-muted border border-slate-300/40 dark:border-slate-700/80 text-sky-600 dark:text-sky-300 px-3.5 py-2 text-xs font-bold flex items-center justify-center gap-2 cursor-default transition';
            }
            if (btnAllLabel) {
                btnAllLabel.innerHTML = `${SVG_MOON}<span class="text-[11px] whitespace-nowrap">Đạt hạn mức ngày (00:00 làm mới)</span>`;
            }
            if (totalEl) totalEl.classList.add('hidden');
        } else {
            if (btnAll) {
                btnAll.disabled = readyCount === 0 || isHarvestingAll;
                btnAll.className = 'w-full rounded-xl bg-gradient-to-r from-amber-400 via-amber-500 to-orange-500 hover:from-amber-500 hover:to-orange-600 disabled:opacity-50 disabled:cursor-not-allowed text-slate-950 px-3.5 py-2 text-xs font-extrabold flex items-center justify-between gap-2 shadow-sm cursor-pointer transition';
            }
            if (btnAllLabel) {
                btnAllLabel.innerHTML = `${SVG_COIN}<span class="truncate">Thu hoạch tất cả</span>`;
            }
            if (totalEl) {
                totalEl.classList.remove('hidden');
                totalEl.textContent = `+${formatVnd(totalPending)}`;
            }
        }

        const container = document.getElementById('sanctuary-slots-list');
        if (!container) return;

        const remainingEmptySlots = Math.max(0, maxSlots - slots.length);
        const emptySlotCardHtml = remainingEmptySlots > 0
            ? `<button type="button" onclick="window.openIslandCardPicker()"
                       class="w-full rounded-2xl border-2 border-dashed border-slate-300/60 dark:border-slate-700/80 hover:border-amber-500/70 bg-slate-500/5 hover:bg-amber-500/5 px-3.5 py-4 text-center cursor-pointer transition flex flex-col items-center justify-center gap-1">
                   <span class="text-xs font-extrabold pk-text-primary">+ Thêm Pokémon vào đảo</span>
                   <span class="text-[11px] font-semibold pk-text-secondary pk-num">Còn ${remainingEmptySlots} ô trống</span>
               </button>`
            : '';

        if (slots.length === 0) {
            container.innerHTML = emptySlotCardHtml;
            return;
        }

        const slotsHtml = slots.map((s) => {
            const c = s.card || {};
            const safeName = window.escapeHtml(c.name || 'Pokémon');
            const safeRarity = window.escapeHtml(c.rarityName || 'Common');
            const visual = getRarityVisual(c.rarityName, c.rarityColorHex);
            const slugs = buildSpriteSlugCandidates(c.speciesName, c.name);
            const thumbSrc = slugs.length > 0 ? `${SHOWDOWN_HOME_BASE}${slugs[0]}.png` : (c.imageUrl || FALLBACK_ART);
            const safeThumb = window.escapeHtml(thumbSrc);
            const safeCardFallback = window.escapeHtml(c.imageUrl || FALLBACK_ART);

            const ready = !sleeping && Boolean(s.readyToHarvest);
            const pending = formatVnd(s.pendingCoins || 0);
            const perTick = formatVnd(s.coinPerTick || 0);
            const readyTicks = Number(s.readyTicks || 0);
            const secLeft = Number(s.secondsToNextTick || 0);
            const isMaxTicks = readyTicks >= MAX_ACCUMULATED_TICKS;
            const nextTickPct = sleeping
                ? 100
                : (isMaxTicks ? 100 : Math.min(100, Math.max(4, Math.round(((tickSec - secLeft) / tickSec) * 100))));

            const statusHtml = sleeping
                ? ''
                : (isMaxTicks
                    ? `<span class="inline-flex items-center gap-1 text-amber-600 dark:text-amber-400 font-semibold">${SVG_ZAP}<span>Đầy (12/12 đợt)</span></span>`
                    : `<span class="pk-text-secondary">Đợt sau: <strong>${secLeft}s</strong> (${readyTicks}/${MAX_ACCUMULATED_TICKS})</span>`);

            const barClass = sleeping
                ? 'bg-sky-500/40'
                : (ready ? 'bg-gradient-to-r from-amber-400 to-orange-500' : 'bg-gradient-to-r from-emerald-500 to-teal-400');

            return `
                <div class="pk-surface-elevated rounded-2xl p-2.5 space-y-1.5 border border-slate-300/30 dark:border-slate-700/50">
                    <div class="flex items-center gap-2">
                        <div class="h-10 w-10 rounded-xl pk-surface-muted flex items-center justify-center overflow-hidden border-2 shrink-0 p-0.5"
                             style="border-color: ${visual.color}">
                            <img src="${safeThumb}" alt="${safeName}"
                                 onerror="this.onerror=null;this.src='${safeCardFallback}';this.classList.add('object-cover','object-top');"
                                 class="h-full w-full object-contain"/>
                        </div>
                        <div class="min-w-0 flex-1 space-y-1">
                            <div class="text-xs font-extrabold pk-text-primary leading-tight truncate" title="${safeName}">${safeName}</div>
                            <div>
                                <span class="pk-rarity-badge px-1.5 py-0.5 text-[9.5px] max-w-full truncate"
                                      style="border-color: ${visual.color}; color: ${visual.color}">
                                    ${safeRarity}
                                </span>
                            </div>
                            <div class="text-[11px] font-extrabold pk-num tabular-nums text-amber-600 dark:text-amber-400">
                                +${perTick}/phút
                            </div>
                        </div>
                        <button type="button" onclick="window.removeSanctuarySlot(${s.slotId})"
                                title="Gỡ khỏi đảo về Kho đồ"
                                class="pk-surface-muted hover:text-rose-500 rounded-lg px-2 py-1 text-[10.5px] font-bold pk-text-secondary cursor-pointer shrink-0">
                            Gỡ
                        </button>
                    </div>

                    ${sleeping ? '' : `
                    <div class="space-y-1">
                        <div class="flex items-center justify-between gap-1 text-[10.5px] pk-num">
                            ${statusHtml}
                            <span class="font-extrabold shrink-0 ${ready ? 'text-amber-600 dark:text-amber-400' : 'pk-text-secondary'}">+${pending}</span>
                        </div>
                        <div class="h-1.5 w-full overflow-hidden rounded-full pk-surface-muted">
                            <div class="h-full rounded-full transition-all duration-500 ${barClass}"
                                 style="width: ${nextTickPct}%"></div>
                        </div>
                        ${ready
                            ? `<button type="button" onclick="window.harvestSanctuarySlot(${s.slotId})"
                                       class="w-full rounded-lg bg-amber-500 hover:bg-amber-600 text-slate-950 py-1 px-2.5 text-[11px] font-extrabold cursor-pointer pk-num inline-flex items-center justify-center gap-1.5 transition">
                                   ${SVG_COIN}<span>Thu hoạch +${pending}</span>
                               </button>`
                            : ''
                        }
                    </div>
                    `}
                </div>
            `;
        }).join('');
        container.innerHTML = slotsHtml + emptySlotCardHtml;
    }

    function renderExpeditionsHud() {
        const list = (sanctuaryState.expeditions || []).slice();
        const maxExp = Number(sanctuaryState.maxActiveExpeditions || 2);
        const isFull = list.length >= maxExp;
        const wrap = document.getElementById('sanctuary-active-expeditions-wrap');
        const container = document.getElementById('sanctuary-active-expeditions');
        const countBadge = document.getElementById('sanctuary-expedition-count-badge');

        if (countBadge) {
            countBadge.textContent = `${list.length} / ${maxExp} đang chạy`;
            countBadge.className = isFull
                ? 'pk-num tabular-nums rounded-full bg-rose-500/15 border border-rose-500/40 px-2.5 py-0.5 text-[10.5px] font-extrabold text-rose-600 dark:text-rose-400'
                : 'pk-num tabular-nums rounded-full bg-amber-500/15 border border-amber-500/35 px-2.5 py-0.5 text-[10.5px] font-extrabold text-amber-600 dark:text-amber-400';
        }

        // Vô hiệu hóa nút Chọn đội hình khi đã đầy 2/2 chuyến đang chạy
        document.querySelectorAll('.sanctuary-zone-start-btn').forEach((btn) => {
            btn.disabled = isFull;
            if (isFull) {
                btn.textContent = `Đã đầy ${maxExp}/${maxExp} chuyến`;
                btn.title = `Bạn đã đạt giới hạn tối đa ${maxExp}/${maxExp} chuyến thám hiểm đang chạy cùng lúc.`;
                btn.classList.add('opacity-55', 'cursor-not-allowed');
                btn.classList.remove('cursor-pointer');
            } else {
                btn.textContent = 'Chọn đội hình →';
                btn.title = '';
                btn.classList.remove('opacity-55', 'cursor-not-allowed');
                btn.classList.add('cursor-pointer');
            }
        });

        if (!container) return;
        if (list.length === 0) {
            if (wrap) wrap.classList.add('hidden');
            container.innerHTML = '';
            return;
        }
        if (wrap) wrap.classList.remove('hidden');

        // Ưu tiên đẩy các chuyến đã hoàn thành (readyToClaim) lên trên cùng
        list.sort((a, b) => {
            const aReady = (Number(a.remainingSeconds || 0) <= 0 || Boolean(a.readyToClaim)) ? 0 : 1;
            const bReady = (Number(b.remainingSeconds || 0) <= 0 || Boolean(b.readyToClaim)) ? 0 : 1;
            if (aReady !== bReady) return aReady - bReady;
            return Number(a.remainingSeconds || 0) - Number(b.remainingSeconds || 0);
        });

        container.innerHTML = list.map((exp) => {
            const safeZone = window.escapeHtml(exp.zoneName || exp.zoneCode);
            const totalSec = Math.max(1, Number(exp.durationMinutes || 15) * 60);
            const remSec = Math.max(0, Number(exp.remainingSeconds || 0));
            const progressPct = Math.min(100, Math.max(3, Math.round(((totalSec - remSec) / totalSec) * 100)));
            const ready = remSec <= 0 || exp.readyToClaim;

            const avatars = (exp.cards || []).map((c) => {
                const slugs = buildSpriteSlugCandidates('', c.name);
                const spriteUrl = slugs.length > 0 ? `${SHOWDOWN_HOME_BASE}${slugs[0]}.png` : (c.imageUrl || FALLBACK_ART);
                const safeSprite = window.escapeHtml(spriteUrl);
                const safeFallback = window.escapeHtml(c.imageUrl || FALLBACK_ART);
                const borderCol = (c.rarityColorHex && /^#[0-9a-fA-F]{6}$/.test(c.rarityColorHex)) ? c.rarityColorHex : '#94a3b8';
                return `
                    <div class="h-10 w-10 rounded-full pk-surface-muted border-2 flex items-center justify-center overflow-hidden p-0.5 -ml-2 first:ml-0 shadow-sm"
                         style="border-color: ${borderCol}"
                         title="${window.escapeHtml(c.name || '')}">
                        <img src="${safeSprite}"
                             alt="${window.escapeHtml(c.name || '')}"
                             onerror="this.onerror=null;this.src='${safeFallback}';this.classList.add('object-cover','object-top');"
                             class="h-full w-full object-contain"/>
                    </div>
                `;
            }).join('');

            const cardHighlightCls = ready
                ? 'border-emerald-500/50 bg-emerald-500/[0.04]'
                : 'border-slate-300/30 dark:border-slate-700/50';

            return `
                <div class="pk-surface-elevated rounded-2xl p-4 space-y-2.5 border ${cardHighlightCls}">
                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                        <div class="flex items-center gap-3">
                            <div class="flex items-center pl-1">${avatars}</div>
                            <div>
                                <div class="text-sm font-extrabold pk-text-primary">${safeZone}</div>
                                <div class="text-xs pk-text-secondary pk-num tabular-nums">
                                    Thưởng: <strong class="text-amber-600 dark:text-amber-400">+${formatVnd(exp.rewardCoins)}</strong>
                                    • Cơ hội Pack: <strong>${exp.bonusPackChance}%</strong>
                                </div>
                            </div>
                        </div>
                        <div class="flex items-center gap-2.5 shrink-0">
                            ${ready
                                ? `<button type="button" onclick="window.claimExpedition(${exp.id})"
                                           class="rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white px-4 py-2 text-xs font-extrabold cursor-pointer shadow-md inline-flex items-center gap-1.5 transition">
                                       ${SVG_GIFT}<span>Nhận Thưởng Ngay</span>
                                   </button>`
                                : `<span class="pk-surface-muted pk-num tabular-nums rounded-xl px-3.5 py-2 text-xs font-extrabold text-amber-600 dark:text-amber-400 inline-flex items-center gap-1.5">
                                       ${SVG_CLOCK}<span>Còn ${formatCountdown(remSec)}</span>
                                   </span>`
                            }
                        </div>
                    </div>
                    <div class="h-1.5 w-full overflow-hidden rounded-full pk-surface-muted">
                        <div class="h-full rounded-full transition-all duration-700 ${ready ? 'bg-emerald-500' : 'bg-gradient-to-r from-violet-500 via-sky-500 to-emerald-400'}"
                             style="width: ${progressPct}%"></div>
                    </div>
                </div>
            `;
        }).join('');
    }

    function renderQuestsHud() {
        const quests = sanctuaryState.quests || [];
        const container = document.getElementById('sanctuary-quests-list');
        const pillEl = document.getElementById('sanctuary-quests-progress-pill');

        const doneCount = quests.filter((q) => q.completed || q.claimed).length;
        if (pillEl) {
            pillEl.textContent = `${doneCount}/${quests.length}`;
        }

        if (!container) return;
        if (quests.length === 0) {
            container.innerHTML = '<div class="pk-surface-muted rounded-2xl p-4 text-center text-xs pk-text-secondary">Không có nhiệm vụ nào hôm nay.</div>';
            return;
        }

        container.innerHTML = quests.map((q) => {
            const pct = Math.min(100, Math.round(((q.currentCount || 0) / Math.max(1, q.targetCount || 1)) * 100));
            const safeTitle = window.escapeHtml(q.title || '');
            const rewardBadge = `<span class="pk-num tabular-nums text-[11px] font-extrabold text-amber-600 dark:text-amber-400">+${formatVnd(q.rewardCoins)}</span>`;

            let actionHtml = `<div class="flex items-center gap-2 shrink-0">${rewardBadge}<span class="pk-num tabular-nums text-[11px] font-bold pk-text-secondary">${q.currentCount}/${q.targetCount}</span></div>`;
            if (q.claimed) {
                actionHtml = `<div class="flex items-center gap-1.5 shrink-0">${rewardBadge}<span class="rounded-full bg-emerald-500/15 border border-emerald-500/30 px-2 py-0.5 text-[10.5px] font-extrabold text-emerald-600 dark:text-emerald-400">✓ Đã nhận</span></div>`;
            } else if (q.completed) {
                actionHtml = `<button type="button" onclick="window.claimDailyQuest(${q.id})"
                                      class="rounded-xl bg-amber-500 hover:bg-amber-600 text-slate-950 px-3 py-1 text-[11px] font-extrabold cursor-pointer pk-num tabular-nums transition">
                                  Nhận +${formatVnd(q.rewardCoins)}
                              </button>`;
            }

            return `
                <div class="pk-surface-elevated rounded-2xl p-3 space-y-2 border border-slate-300/30 dark:border-slate-700/50">
                    <div class="flex items-center justify-between gap-2">
                        <div class="text-xs font-extrabold pk-text-primary min-w-0">${safeTitle}</div>
                        <div class="shrink-0">${actionHtml}</div>
                    </div>
                    <div class="h-1.5 w-full overflow-hidden rounded-full pk-surface-muted">
                        <div class="h-full rounded-full bg-gradient-to-r from-rose-500 to-amber-400 transition-all"
                             style="width: ${pct}%"></div>
                    </div>
                </div>
            `;
        }).join('');
    }

    // =========================================================================
    // USER ACTIONS
    // =========================================================================
    window.placeCardFromPicker = async function (userCardId) {
        if (!userCardId) return;
        try {
            const res = await fetch('/api/v1/sanctuary/slots', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userCardId: userCardId })
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Không thể thả Pokémon lên đảo');
            window.closeIslandCardPicker();
            if (window.showToast) window.showToast('success', 'Đã thả Pokémon lên đảo!', data.message || 'Pokémon đã bắt đầu tích lũy Xu mỗi phút.');
            await loadSanctuaryState();
        } catch (err) {
            if (window.showToast) window.showToast('error', 'Không thể thả Pokémon', err.message || 'Vui lòng thử lại.');
        }
    };

    function markSlotHarvestedImmediately(slotId) {
        const tickSec = Number((sanctuaryState && sanctuaryState.harvestTickSeconds) || 60);
        const actor = islandActors.get(slotId);
        if (actor && actor.slot) {
            actor.slot.readyToHarvest = false;
            actor.slot.pendingCoins = 0;
            actor.slot.readyTicks = 0;
            actor.slot.secondsToNextTick = tickSec;
            updateActorOverlayCoin(actor);
        }
        if (sanctuaryState && Array.isArray(sanctuaryState.slots)) {
            const stSlot = sanctuaryState.slots.find((s) => s.slotId === slotId);
            if (stSlot) {
                stSlot.readyToHarvest = false;
                stSlot.pendingCoins = 0;
                stSlot.readyTicks = 0;
                stSlot.secondsToNextTick = tickSec;
            }
        }
    }

    window.removeSanctuarySlot = async function (slotId) {
        try {
            const res = await fetch(`/api/v1/sanctuary/slots/${slotId}`, { method: 'DELETE' });
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Không thể gỡ Pokémon khỏi đảo');
            if (window.showToast) window.showToast('info', 'Đã gỡ Pokémon khỏi đảo', data.message || 'Thẻ bài đã được đưa về Kho đồ.');
            await loadSanctuaryState();
        } catch (err) {
            if (window.showToast) window.showToast('error', 'Không thể gỡ Pokémon', err.message || 'Vui lòng thử lại.');
        }
    };

    window.harvestSanctuarySlot = async function (slotId, fxX, fxY) {
        try {
            const res = await fetch(`/api/v1/sanctuary/slots/${slotId}/harvest`, { method: 'POST' });
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Chưa thể thu hoạch lúc này');

            const actor = islandActors.get(slotId);
            const px = fxX != null ? fxX : (actor ? actor.screenX : canvasWidth * 0.5);
            const py = fxY != null ? fxY : (actor ? actor.screenY : canvasHeight * 0.5);
            const earned = Number(data.coinsEarned || 0);
            if (earned > 0) {
                spawnHarvestDomVfx(px, py, `+${earned.toLocaleString('vi-VN')} Xu`);
            }

            // Immediately hide coin & reset local pending/ticks before server refresh
            markSlotHarvestedImmediately(slotId);
            renderIslandSlotsHud();

            if (window.showToast) {
                window.showToast(
                    earned > 0 ? 'success' : 'info',
                    earned > 0 ? 'Thu hoạch thành công!' : 'Thông báo thu hoạch',
                    data.message || (earned > 0 ? `Đã thu hoạch +${formatVnd(earned)} vào Ví.` : 'Đã đạt hạn mức thu nhập hôm nay.')
                );
            }
            if (window.updateNavbarWallet && data.newWalletBalance != null) {
                window.updateNavbarWallet(data.newWalletBalance);
            }
            await loadSanctuaryState();
        } catch (err) {
            if (window.showToast) window.showToast('error', 'Chưa thể thu hoạch', err.message || 'Vui lòng thử lại.');
        }
    };

    async function harvestAllReadySlots() {
        if (!sanctuaryState || isHarvestingAll) return;
        const readySlots = (sanctuaryState.slots || []).filter((s) => s.readyToHarvest);
        if (readySlots.length === 0) return;

        isHarvestingAll = true;
        renderIslandSlotsHud();

        let totalEarned = 0;
        let lastBalance = null;
        try {
            for (const slot of readySlots) {
                const res = await fetch(`/api/v1/sanctuary/slots/${slot.slotId}/harvest`, { method: 'POST' });
                const data = await res.json();
                if (res.ok) {
                    const earned = Number(data.coinsEarned || 0);
                    totalEarned += earned;
                    if (data.newWalletBalance != null) lastBalance = data.newWalletBalance;
                    const actor = islandActors.get(slot.slotId);
                    if (earned > 0 && actor) {
                        triggerActorJoyHop(actor);
                        spawnHarvestDomVfx(actor.screenX, actor.screenY, `+${earned.toLocaleString('vi-VN')} Xu`);
                    }
                    markSlotHarvestedImmediately(slot.slotId);
                }
            }
            renderIslandSlotsHud();
            if (window.updateNavbarWallet && lastBalance != null) {
                window.updateNavbarWallet(lastBalance);
            }
            if (window.showToast) {
                window.showToast(
                    totalEarned > 0 ? 'success' : 'info',
                    totalEarned > 0 ? 'Thu hoạch toàn bộ Đảo thành công!' : 'Đạt hạn mức ngày',
                    totalEarned > 0
                        ? `Đã nhận tổng cộng +${formatVnd(totalEarned)} vào Ví.`
                        : 'Đã đạt hạn mức thu nhập Đảo hôm nay (00:00 làm mới)!'
                );
            }
        } catch (err) {
            if (window.showToast) window.showToast('error', 'Lỗi thu hoạch tất cả', err.message || 'Vui lòng thử lại.');
        } finally {
            isHarvestingAll = false;
            await loadSanctuaryState();
        }
    }

    window.openExpeditionModal = function (btn) {
        const activeCount = (sanctuaryState && Array.isArray(sanctuaryState.expeditions)) ? sanctuaryState.expeditions.length : 0;
        const maxExp = Number((sanctuaryState && sanctuaryState.maxActiveExpeditions) || 2);
        if (activeCount >= maxExp) {
            if (window.showToast) {
                window.showToast('info', `Đã đầy ${maxExp}/${maxExp} chuyến`, 'Hãy hoàn thành và nhận thưởng chuyến đang chạy trước khi cử thêm!');
            }
            return;
        }
        selectedZoneCode = btn.getAttribute('data-zone-code');
        const zoneName = btn.getAttribute('data-zone-name') || selectedZoneCode;
        const duration = btn.getAttribute('data-zone-duration') || '';
        selectedExpeditionCardIds.clear();

        document.getElementById('exp-modal-title').textContent = `Xuất phát tới ${zoneName}`;
        document.getElementById('exp-modal-duration').textContent = duration;
        renderExpeditionCardList();
        document.getElementById('modal-start-expedition').classList.remove('hidden');
    };

    window.closeExpeditionModal = function () {
        document.getElementById('modal-start-expedition').classList.add('hidden');
        selectedZoneCode = null;
        selectedExpeditionCardIds.clear();
    };

    const EXPEDITION_ZONE_BASE = {
        GREEN_FOREST: { coins: 20, packChance: 5 },
        CRYSTAL_CAVE: { coins: 65, packChance: 12 },
        LEGEND_PEAK: { coins: 160, packChance: 25 }
    };
    const MAX_EXPEDITION_PACK_CHANCE = 75;

    function getExpeditionCardBonus(card) {
        const eff = String((card && card.rarityRevealEffect) || 'NONE').toUpperCase();
        if (eff === 'MEGA_HYPER') return { coinPct: 100, packPct: 12 };
        if (eff === 'FUTURISTIC') return { coinPct: 80, packPct: 9 };
        if (eff === 'SPECIAL_ILLUSTRATION') return { coinPct: 60, packPct: 7 };
        if (eff === 'ULTRA') return { coinPct: 45, packPct: 5 };
        if (eff === 'ILLUSTRATION') return { coinPct: 35, packPct: 4 };
        if (eff === 'DOUBLE_RARE') return { coinPct: 25, packPct: 3 };
        const rate = estimateCoinsPerMinute(card);
        if (rate >= 3) return { coinPct: 15, packPct: 2 };
        if (rate === 2) return { coinPct: 10, packPct: 1 };
        return { coinPct: 5, packPct: 0.5 };
    }

    function getSortedExpeditionCards() {
        const cards = (sanctuaryState && sanctuaryState.availableCards) ? sanctuaryState.availableCards.slice() : [];
        return cards.sort((a, b) => {
            const rateDiff = estimateCoinsPerMinute(b.card) - estimateCoinsPerMinute(a.card);
            if (rateDiff !== 0) return rateDiff;
            return ((a.card && a.card.name) || '').localeCompare((b.card && b.card.name) || '');
        });
    }

    window.quickSelectTop3ExpeditionCards = function () {
        const sorted = getSortedExpeditionCards();
        selectedExpeditionCardIds.clear();
        sorted.slice(0, 3).forEach((uc) => selectedExpeditionCardIds.add(uc.userCardId));
        renderExpeditionCardList();
    };

    function renderExpeditionCardList() {
        const container = document.getElementById('exp-modal-card-list');
        const countEl = document.getElementById('exp-modal-selected-count');
        const rewardEl = document.getElementById('exp-modal-estimated-reward');
        const confirmBtn = document.getElementById('btn-confirm-start-expedition');

        const cards = getSortedExpeditionCards();
        const selectedCount = selectedExpeditionCardIds.size;
        const zoneBase = EXPEDITION_ZONE_BASE[selectedZoneCode] || { coins: 20, packChance: 5 };

        let totalCoinBonusPct = 0;
        let totalPackBonusPct = 0;
        cards.forEach((uc) => {
            if (!selectedExpeditionCardIds.has(uc.userCardId)) return;
            const b = getExpeditionCardBonus(uc.card);
            totalCoinBonusPct += b.coinPct;
            totalPackBonusPct += b.packPct;
        });

        const estimatedCoins = selectedCount > 0 ? Math.round(zoneBase.coins * (1 + totalCoinBonusPct / 100)) : 0;
        const estimatedPackChance = selectedCount > 0
            ? Math.min(MAX_EXPEDITION_PACK_CHANCE, Math.round(zoneBase.packChance + totalPackBonusPct))
            : zoneBase.packChance;
        const addedPackChance = Math.max(0, estimatedPackChance - zoneBase.packChance);

        if (countEl) countEl.textContent = `Đã chọn: ${selectedCount} / 3 thẻ`;
        if (rewardEl) {
            if (selectedCount > 0) {
                const coinBonusBadge = totalCoinBonusPct > 0
                    ? ` <span class="text-emerald-600 dark:text-emerald-400 font-extrabold">(+${totalCoinBonusPct}%)</span>`
                    : '';
                const packBonusBadge = addedPackChance > 0
                    ? ` <span class="text-emerald-600 dark:text-emerald-400 font-extrabold">(+${addedPackChance}%)</span>`
                    : '';
                rewardEl.innerHTML = `Xu dự kiến: <strong>+${formatVnd(estimatedCoins)}</strong>${coinBonusBadge} <span class="mx-1 opacity-50">•</span> Cơ hội Pack: <strong>${estimatedPackChance}%</strong>${packBonusBadge}`;
            } else {
                rewardEl.textContent = `Gốc: +${formatVnd(zoneBase.coins)} • Cơ hội Pack: ${zoneBase.packChance}% (chọn thẻ để nhận Rarity Bonus)`;
            }
        }
        if (confirmBtn) confirmBtn.disabled = selectedCount === 0;
        if (!container) return;

        if (cards.length === 0) {
            container.innerHTML = '<div class="col-span-full pk-surface-muted rounded-2xl p-4 text-center text-xs pk-text-secondary">Không có thẻ bài rảnh trong Kho đồ (hãy gỡ bớt thẻ trên đảo hoặc mở thêm gói bài).</div>';
            return;
        }

        const isFull = selectedCount >= 3;
        container.innerHTML = cards.map((uc) => {
            const c = uc.card || {};
            const safeName = window.escapeHtml(c.name || 'Pokémon');
            const safeRarity = window.escapeHtml(c.rarityName || 'Common');
            const visual = getRarityVisual(c.rarityName, c.rarityColorHex);
            const bonus = getExpeditionCardBonus(c);
            const selected = selectedExpeditionCardIds.has(uc.userCardId);
            const cardCls = selected
                ? 'border-amber-500 ring-2 ring-amber-500/35 bg-amber-500/10'
                : `pk-surface-elevated border-slate-300/35 dark:border-slate-700/60 ${isFull ? 'opacity-45' : 'hover:border-amber-500/70 hover:-translate-y-0.5 hover:shadow-lg'}`;
            const checkBadge = selected
                ? `<span class="absolute top-2 right-2 z-10 h-5 w-5 rounded-full bg-amber-500 text-slate-950 flex items-center justify-center shadow font-black text-[11px]">✓</span>`
                : `<span class="absolute top-2 right-2 z-10 h-5 w-5 rounded-full bg-slate-950/65 text-slate-300 border border-white/15 flex items-center justify-center font-bold text-xs">+</span>`;

            return `
                <button type="button" onclick="window.toggleExpeditionCard(${uc.userCardId})"
                        class="group relative border-2 ${cardCls} rounded-2xl p-2 flex flex-col items-center text-center gap-2 cursor-pointer transition">
                    ${checkBadge}
                    <div class="w-[88%] aspect-[2.5/3.5] rounded-xl overflow-hidden pk-surface-muted shadow-md transition group-hover:scale-[1.03] shrink-0">
                        <img src="${window.escapeHtml(c.imageUrl || FALLBACK_ART)}" alt="${safeName}" loading="lazy" decoding="async"
                             onerror="this.onerror=null;this.src='${FALLBACK_ART}';"
                             class="h-full w-full object-cover"/>
                    </div>
                    <div class="w-full px-1 space-y-1">
                        <div class="text-xs font-extrabold pk-text-primary truncate" title="${safeName}">${safeName}</div>
                        <div class="flex items-center justify-center">
                            <span class="pk-rarity-badge px-2 py-0.5 text-[10px] truncate max-w-full"
                                  style="border-color: ${visual.color}; color: ${visual.color}">
                                ${safeRarity}
                            </span>
                        </div>
                        <div class="pk-num text-[10px] font-extrabold text-emerald-600 dark:text-emerald-400">
                            +${bonus.coinPct}% Xu • +${bonus.packPct}% Pack
                        </div>
                    </div>
                </button>
            `;
        }).join('');
    }

    window.toggleExpeditionCard = function (userCardId) {
        if (selectedExpeditionCardIds.has(userCardId)) {
            selectedExpeditionCardIds.delete(userCardId);
        } else {
            if (selectedExpeditionCardIds.size >= 3) {
                if (window.showToast) window.showToast('info', 'Đội hình đã đủ', 'Mỗi chuyến thám hiểm chỉ chọn tối đa 3 thẻ!');
                return;
            }
            selectedExpeditionCardIds.add(userCardId);
        }
        renderExpeditionCardList();
    };

    window.confirmStartExpedition = async function () {
        if (!selectedZoneCode || selectedExpeditionCardIds.size === 0) {
            if (window.showToast) window.showToast('info', 'Chưa chọn đội hình', 'Hãy chọn ít nhất 1 thẻ bài để xuất phát!');
            return;
        }
        const btn = document.getElementById('btn-confirm-start-expedition');
        if (btn) btn.disabled = true;
        try {
            const res = await fetch('/api/v1/sanctuary/expeditions', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    zoneCode: selectedZoneCode,
                    userCardIds: Array.from(selectedExpeditionCardIds)
                })
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Không thể bắt đầu thám hiểm');
            if (window.showToast) window.showToast('success', 'Đã cử đội hình thám hiểm thành công!', data.message || 'Đội hình của bạn đã lên đường khám phá.');
            window.closeExpeditionModal();
            await loadSanctuaryState();
        } catch (err) {
            if (window.showToast) window.showToast('error', 'Không thể xuất phát', err.message || 'Vui lòng thử lại.');
        } finally {
            if (btn) btn.disabled = false;
        }
    };

    window.claimExpedition = async function (expeditionId) {
        try {
            const res = await fetch(`/api/v1/sanctuary/expeditions/${expeditionId}/claim`, { method: 'POST' });
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Chưa thể nhận thưởng thám hiểm');
            if (window.showToast) window.showToast('success', 'Đã nhận thưởng thành công!', data.message || 'Phần thưởng thám hiểm đã được cộng vào Ví.');
            if (window.updateNavbarWallet && data.newWalletBalance != null) {
                window.updateNavbarWallet(data.newWalletBalance);
            }
            await loadSanctuaryState();
        } catch (err) {
            if (window.showToast) window.showToast('error', 'Chưa thể nhận thưởng', err.message || 'Vui lòng thử lại.');
        }
    };

    window.claimDailyQuest = async function (questId) {
        try {
            const res = await fetch(`/api/v1/sanctuary/quests/${questId}/claim`, { method: 'POST' });
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Chưa thể nhận thưởng nhiệm vụ');
            if (window.showToast) window.showToast('success', 'Đã nhận thưởng nhiệm vụ!', data.message || 'Phần thưởng Xu đã được cộng vào Ví.');
            if (window.updateNavbarWallet && data.newWalletBalance != null) {
                window.updateNavbarWallet(data.newWalletBalance);
            }
            await loadSanctuaryState();
        } catch (err) {
            if (window.showToast) window.showToast('error', 'Chưa thể nhận thưởng', err.message || 'Vui lòng thử lại.');
        }
    };

    function startCountdownTicker() {
        countdownTimerId = setInterval(() => {
            if (!sanctuaryState) return;
            let needHudUpdate = false;
            let needServerSync = false;

            (sanctuaryState.expeditions || []).forEach((exp) => {
                if (exp.remainingSeconds > 0) {
                    exp.remainingSeconds -= 1;
                    needHudUpdate = true;
                    if (exp.remainingSeconds === 0) exp.readyToClaim = true;
                }
            });

            if (!isDailyCapReached()) {
                (sanctuaryState.slots || []).forEach((slot) => {
                    const readyTicks = Number(slot.readyTicks || 0);
                    if (readyTicks < MAX_ACCUMULATED_TICKS && slot.secondsToNextTick > 0) {
                        slot.secondsToNextTick -= 1;
                        needHudUpdate = true;
                        if (slot.secondsToNextTick === 0) needServerSync = true;
                    }
                });
            }

            if (needServerSync) {
                loadSanctuaryState();
            } else if (needHudUpdate) {
                renderIslandSlotsHud();
                renderExpeditionsHud();
            }
        }, 1000);
    }

    // =========================================================================
    // RANDOM EVENT DROPS ON ISLAND (POKEBALL / POND SPLASH / TREASURE CHEST)
    // =========================================================================
    function getDropVisualConfig(type) {
        const t = String(type || 'POKEBALL').toUpperCase();
        if (t === 'POND_SPLASH') {
            return {
                auraClass: 'is-pond',
                label: 'Cá nhảy!',
                title: 'Cá nhảy ở hồ nước — Bấm để nhặt (+100~150 Xu hoặc +1 Mảnh Pack)!',
                svg: `<svg class="relative z-10 h-10 w-10 drop-shadow-[0_4px_8px_rgba(0,0,0,0.55)]" viewBox="0 0 48 48" fill="none">
                    <ellipse cx="24" cy="36" rx="16" ry="5" stroke="#38BDF8" stroke-width="2.5" fill="rgba(56,189,248,0.22)"/>
                    <path d="M13 26c3-9 13-13 20-8-2 3-2 7 0 10-7 5-17 1-20-2Z" fill="#FB923C" stroke="#FFF7ED" stroke-width="2"/>
                    <path d="M33 18l7-4-2 9 2 8-7-3" fill="#FBBF24" stroke="#FFF7ED" stroke-width="1.8" stroke-linejoin="round"/>
                    <circle cx="20" cy="23" r="2" fill="#0F172A"/>
                    <circle cx="19.5" cy="22.5" r="0.7" fill="#FFFFFF"/>
                    <path d="M14 14l2-4M24 11l0-5M32 13l3-3" stroke="#7DD3FC" stroke-width="2" stroke-linecap="round"/>
                </svg>`
            };
        }
        if (t === 'TREASURE_CHEST') {
            return {
                auraClass: 'is-chest',
                label: 'Kho báu!',
                title: 'Rương kho báu — Bấm để mở (+200 Xu hoặc +1 Mảnh Pack)!',
                svg: `<svg class="relative z-10 h-10 w-10 drop-shadow-[0_4px_8px_rgba(0,0,0,0.6)]" viewBox="0 0 48 48" fill="none">
                    <rect x="8" y="18" width="32" height="20" rx="4" fill="#B45309" stroke="#FDE68A" stroke-width="2.2"/>
                    <path d="M8 21c0-6 6-10 16-10s16 4 16 10H8Z" fill="#D97706" stroke="#FDE68A" stroke-width="2.2"/>
                    <rect x="8" y="21" width="32" height="4" fill="#FBBF24"/>
                    <rect x="20" y="20" width="8" height="9" rx="2" fill="#FEF08A" stroke="#92400E" stroke-width="1.8"/>
                    <circle cx="24" cy="24.5" r="1.4" fill="#92400E"/>
                    <path d="M12 10l1.5 3 3 1.5-3 1.5L12 19l-1.5-3-3-1.5 3-1.5L12 10Z" fill="#FEF08A"/>
                    <path d="M38 9l1 2 2 1-2 1-1 2-1-2-2-1 2-1 1-2Z" fill="#FDE047"/>
                </svg>`
            };
        }
        return {
            auraClass: 'is-pokeball',
            label: 'Pokéball!',
            title: 'Pokéball rơi trên đảo — Bấm để nhặt (+40~80 Xu)!',
            svg: `<svg class="relative z-10 h-9 w-9 drop-shadow-[0_4px_8px_rgba(0,0,0,0.55)]" viewBox="0 0 48 48" fill="none">
                <circle cx="24" cy="24" r="17" fill="#FFFFFF" stroke="#0F172A" stroke-width="2.6"/>
                <path d="M7 24a17 17 0 0 1 34 0H7Z" fill="#EF4444" stroke="#0F172A" stroke-width="2.6"/>
                <line x1="7" y1="24" x2="41" y2="24" stroke="#0F172A" stroke-width="3.2"/>
                <circle cx="24" cy="24" r="6" fill="#FFFFFF" stroke="#0F172A" stroke-width="2.6"/>
                <circle cx="24" cy="24" r="2.5" fill="#FBBF24"/>
                <path d="M15 14a11 11 0 0 1 6-3" stroke="#FCA5A5" stroke-width="2.2" stroke-linecap="round"/>
            </svg>`
        };
    }

    function updateDropHudBar(data) {
        if (!data) return;
        const shards = Number(data.packShards || 0);
        const shardsPerPack = Number(data.shardsPerPack || 5);
        const claimed = Number(data.dailyClaimed ?? (activeDropState ? activeDropState.dailyClaimed : 0));
        const limit = Number(data.dailyLimit || 8);

        const shardsText = document.getElementById('sanctuary-shards-text');
        const dailyText = document.getElementById('sanctuary-drops-daily-text');
        const exchangeBtn = document.getElementById('btn-exchange-pack-shards');

        if (shardsText) shardsText.textContent = `${shards} / ${shardsPerPack}`;
        if (dailyText) dailyText.textContent = `${claimed} / ${limit}`;
        if (exchangeBtn) {
            const canExchange = shards >= shardsPerPack && !isExchangingShards;
            exchangeBtn.disabled = !canExchange;
            if (canExchange) {
                exchangeBtn.className = 'rounded-full bg-amber-400 hover:bg-amber-300 text-slate-950 px-2.5 py-0.5 text-[10.5px] font-black shadow-[0_0_12px_rgba(251,191,36,0.75)] transition cursor-pointer whitespace-nowrap animate-pulse';
            } else {
                exchangeBtn.className = 'rounded-full bg-white/10 text-slate-300 px-2.5 py-0.5 text-[10.5px] font-extrabold transition cursor-not-allowed whitespace-nowrap';
            }
        }
    }

    function renderActiveDropOnIsland() {
        const layer = document.getElementById('sanctuary-drops-layer');
        if (!layer) return;
        layer.innerHTML = '';

        const drop = activeDropState && activeDropState.activeDrop;
        if (!drop) return;

        const pos = isoToScreen(Number(drop.posX || 0), Number(drop.posY || 0));
        const cfg = getDropVisualConfig(drop.type);

        const btn = document.createElement('button');
        btn.type = 'button';
        btn.id = `sanctuary-drop-${drop.id}`;
        btn.className = 'pk-island-drop-btn';
        btn.title = cfg.title;
        btn.style.left = `${pos.x.toFixed(1)}px`;
        btn.style.top = `${pos.y.toFixed(1)}px`;
        btn.innerHTML = `
            <span class="pk-island-drop-aura ${cfg.auraClass}"></span>
            ${cfg.svg}
            <span class="pk-island-drop-tag">${cfg.label}</span>
        `;
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            claimSanctuaryDrop(drop.id, pos.x, pos.y);
        });
        layer.appendChild(btn);
    }

    async function checkSanctuaryDrops() {
        try {
            const res = await fetch('/api/sanctuary/drops/check');
            if (!res.ok) return;
            const data = await res.json();
            activeDropState = data;
            updateDropHudBar(data);
            renderActiveDropOnIsland();
        } catch (err) {
            // Silent ignore on transient poll errors
        }
    }

    async function claimSanctuaryDrop(dropId, screenX, screenY) {
        if (isClaimingDrop) return;
        isClaimingDrop = true;
        try {
            const res = await fetch(`/api/sanctuary/drops/${dropId}/claim`, { method: 'POST' });
            const data = await res.json();
            if (!res.ok) {
                if (window.showToast) window.showToast('info', 'Sự kiện Đảo', data.message || 'Vật phẩm đã hết hạn.');
                await checkSanctuaryDrops();
                return;
            }
            // Hiệu ứng chữ thưởng bay lên ngay tại vị trí vừa click
            spawnHarvestDomVfx(screenX, screenY, data.rewardLabel || '+Xu');
            if (window.updateNavbarWallet && data.newWalletBalance != null) {
                window.updateNavbarWallet(data.newWalletBalance);
            }
            if (window.showToast) {
                window.showToast('success', 'Nhặt quà sự kiện thành công!', data.message || `Bạn nhận được ${data.rewardLabel}!`);
            }
            activeDropState = {
                activeDrop: null,
                packShards: data.packShards,
                shardsPerPack: data.shardsPerPack,
                dailyClaimed: data.dailyClaimed,
                dailyLimit: data.dailyLimit,
                nextSpawnInSeconds: data.nextSpawnInSeconds
            };
            updateDropHudBar(activeDropState);
            renderActiveDropOnIsland();
        } catch (err) {
            if (window.showToast) window.showToast('error', 'Lỗi nhặt quà', err.message || 'Vui lòng thử lại.');
        } finally {
            isClaimingDrop = false;
        }
    }

    window.exchangeSanctuaryPackShards = async function () {
        if (isExchangingShards) return;
        isExchangingShards = true;
        updateDropHudBar(activeDropState);
        try {
            const res = await fetch('/api/sanctuary/shards/exchange', { method: 'POST' });
            const data = await res.json();
            if (!res.ok) {
                if (window.showToast) window.showToast('error', 'Chưa thể đổi Pack', data.message || 'Chưa đủ 5 Mảnh Pack.');
                return;
            }
            if (activeDropState) {
                activeDropState.packShards = data.packShards;
            }
            updateDropHudBar({
                packShards: data.packShards,
                shardsPerPack: data.shardsPerPack,
                dailyClaimed: activeDropState ? activeDropState.dailyClaimed : 0,
                dailyLimit: activeDropState ? activeDropState.dailyLimit : 8
            });
            if (window.showToast) {
                window.showToast('success', 'Đổi Mảnh Pack thành công!', data.message || 'Đã cộng 1 Pack mới vào Kho Pack & Box!');
            }
        } catch (err) {
            if (window.showToast) window.showToast('error', 'Lỗi kết nối', err.message || 'Vui lòng thử lại.');
        } finally {
            isExchangingShards = false;
            updateDropHudBar(activeDropState);
        }
    };

    function initSanctuaryPage() {
        setupDiorama();
        if (window.__INITIAL_SANCTUARY_STATE__) {
            sanctuaryState = window.__INITIAL_SANCTUARY_STATE__;
            window.__INITIAL_SANCTUARY_STATE__ = null;
            renderAllHud();
            syncActorsWithSlots(sanctuaryState.slots);
        } else {
            loadSanctuaryState();
        }
        startCountdownTicker();
        checkSanctuaryDrops();
        dropPollTimerId = setInterval(() => {
            if (!document.hidden) checkSanctuaryDrops();
        }, 60000);
        window.addEventListener('resize', renderActiveDropOnIsland);

        const btnOpenPicker = document.getElementById('btn-open-card-picker');
        if (btnOpenPicker) btnOpenPicker.addEventListener('click', window.openIslandCardPicker);

        const pickerSearch = document.getElementById('picker-search-input');
        if (pickerSearch) pickerSearch.addEventListener('input', renderPickerModalGrid);

        const pickerRarity = document.getElementById('picker-rarity-filter');
        if (pickerRarity) pickerRarity.addEventListener('change', renderPickerModalGrid);

        const btnHarvestAll = document.getElementById('btn-harvest-all');
        if (btnHarvestAll) btnHarvestAll.addEventListener('click', harvestAllReadySlots);

        const btnRefresh = document.getElementById('btn-sanctuary-refresh');
        if (btnRefresh) btnRefresh.addEventListener('click', () => {
            loadSanctuaryState();
            checkSanctuaryDrops();
        });

        window.addEventListener('pagehide', () => {
            if (rafId != null) {
                cancelAnimationFrame(rafId);
                rafId = null;
            }
            if (countdownTimerId != null) {
                clearInterval(countdownTimerId);
                countdownTimerId = null;
            }
            if (dropPollTimerId != null) {
                clearInterval(dropPollTimerId);
                dropPollTimerId = null;
            }
            window.removeEventListener('resize', renderActiveDropOnIsland);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initSanctuaryPage);
    } else {
        initSanctuaryPage();
    }
})();
