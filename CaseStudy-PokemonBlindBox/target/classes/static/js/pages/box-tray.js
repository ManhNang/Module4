/* ==================== SHOP / BOX-TRAY PAGE + IMMERSIVE GACHA VFX ==================== */
const activeBoxPanel = document.getElementById('active-box-panel');
const ACTIVE_BOX_ID = activeBoxPanel ? parseInt(activeBoxPanel.getAttribute('data-box-id'), 10) : null;
const TOTAL_PACKS = activeBoxPanel ? parseInt(activeBoxPanel.getAttribute('data-total-packs') || '30', 10) : 30;

// Trạng thái gói bài đang mở trong Immersive Gacha Modal
let currentDrawnCards = [];
let currentPackHighestTier = 'common';
let isTearInProgress = false;

const TIER_RANK = { common: 1, uncommon: 2, rare: 3, ex: 4, hyper: 5 };

function resolveRarityTier(card) {
    const rawName = String(card?.rarityName || '').trim().toLowerCase();
    if (/hyper|secret|gold|special illustration|\bsir\b/.test(rawName)) return 'hyper';
    if (/\bex\b|double rare|ultra rare|full art|illustration rare|\bsar\b|\bur\b/.test(rawName)) return 'ex';
    if (/uncommon|không phổ biến|khong pho bien/.test(rawName)) return 'uncommon';
    if (/\brare\b|hiếm|hiem/.test(rawName) || card?.rarityIsHolographic === true) return 'rare';
    return 'common';
}

function getTierDefaultColor(tier, fallbackHex) {
    if (/^#[0-9a-fA-F]{3,8}$/.test(fallbackHex || '')) return fallbackHex;
    const map = {
        common: '#CBD5E1',
        uncommon: '#34D399',
        rare: '#38BDF8',
        ex: '#FBBF24',
        hyper: '#EC4899'
    };
    return map[tier] || '#CBD5E1';
}

// ==================== LIGHTWEIGHT HTML5 CANVAS PARTICLE / CONFETTI ENGINE ====================
const GachaVfxCanvas = (function () {
    let canvas = null;
    let ctx = null;
    let particles = [];
    let rafId = null;

    function ensureCanvas() {
        canvas = document.getElementById('gacha-vfx-canvas');
        if (!canvas) return false;
        const dpr = window.devicePixelRatio || 1;
        const w = window.innerWidth;
        const h = window.innerHeight;
        if (canvas.width !== Math.floor(w * dpr) || canvas.height !== Math.floor(h * dpr)) {
            canvas.width = Math.floor(w * dpr);
            canvas.height = Math.floor(h * dpr);
        }
        ctx = canvas.getContext('2d');
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        return true;
    }

    function paletteForTier(tier) {
        if (tier === 'hyper') {
            return ['#ff2a85', '#fbbf24', '#34d399', '#38bdf8', '#a855f7', '#ffffff'];
        }
        if (tier === 'ex') {
            return ['#fbbf24', '#f59e0b', '#fef08a', '#ffffff', '#fb7185'];
        }
        if (tier === 'rare') {
            return ['#38bdf8', '#60a5fa', '#bae6fd', '#ffffff'];
        }
        return ['#fde68a', '#ffffff', '#e2e8f0'];
    }

    function loop() {
        if (!ctx || !canvas) return;
        const w = window.innerWidth;
        const h = window.innerHeight;
        ctx.clearRect(0, 0, w, h);

        for (let i = particles.length - 1; i >= 0; i--) {
            const p = particles[i];
            p.x += p.vx;
            p.y += p.vy;
            p.vy += p.gravity;
            p.vx *= 0.985;
            p.rotation += p.vRot;
            p.life -= p.decay;

            if (p.life <= 0) {
                particles.splice(i, 1);
                continue;
            }

            ctx.save();
            ctx.globalAlpha = Math.max(0, Math.min(1, p.life));
            ctx.translate(p.x, p.y);
            ctx.rotate(p.rotation);
            ctx.fillStyle = p.color;
            if (p.shape === 'rect') {
                ctx.fillRect(-p.size / 2, -p.size * 0.3, p.size, p.size * 0.6);
            } else {
                ctx.beginPath();
                ctx.arc(0, 0, p.size * 0.45, 0, Math.PI * 2);
                ctx.fill();
            }
            ctx.restore();
        }

        if (particles.length > 0) {
            rafId = requestAnimationFrame(loop);
        } else {
            rafId = null;
        }
    }

    return {
        burst(originX, originY, tier, countOverride) {
            if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
            if (!ensureCanvas()) return;
            const colors = paletteForTier(tier);
            const count = countOverride || (tier === 'hyper' ? 95 : (tier === 'ex' ? 68 : 28));

            for (let i = 0; i < count; i++) {
                const angle = Math.random() * Math.PI * 2;
                const speed = (tier === 'hyper' ? 5.5 : 4.2) + Math.random() * (tier === 'hyper' ? 9.5 : 7);
                particles.push({
                    x: originX,
                    y: originY,
                    vx: Math.cos(angle) * speed,
                    vy: Math.sin(angle) * speed - (2 + Math.random() * 3.5),
                    gravity: 0.16 + Math.random() * 0.08,
                    size: 5 + Math.random() * 7,
                    color: colors[Math.floor(Math.random() * colors.length)],
                    shape: Math.random() > 0.35 ? 'rect' : 'circle',
                    rotation: Math.random() * Math.PI * 2,
                    vRot: (Math.random() - 0.5) * 0.25,
                    life: 1,
                    decay: 0.013 + Math.random() * 0.012
                });
            }
            if (!rafId) rafId = requestAnimationFrame(loop);
        },
        clear() {
            particles = [];
            if (rafId) {
                cancelAnimationFrame(rafId);
                rafId = null;
            }
            if (ctx && canvas) {
                ctx.clearRect(0, 0, window.innerWidth, window.innerHeight);
            }
        }
    };
})();

function triggerStageFlash(tier) {
    const flashEl = document.getElementById('gacha-flash-overlay');
    if (!flashEl) return;
    if (tier === 'hyper') {
        flashEl.style.background = 'radial-gradient(circle, rgba(255,255,255,0.82) 0%, rgba(236,72,153,0.45) 45%, rgba(56,189,248,0.35) 100%)';
    } else if (tier === 'ex') {
        flashEl.style.background = 'radial-gradient(circle, rgba(255,255,255,0.85) 0%, rgba(251,191,36,0.48) 55%, transparent 100%)';
    } else {
        flashEl.style.background = 'radial-gradient(circle, rgba(255,255,255,0.55) 0%, rgba(253,230,138,0.22) 60%, transparent 100%)';
    }
    flashEl.style.opacity = '1';
    setTimeout(() => {
        flashEl.style.opacity = '0';
    }, 170);
}

function triggerStageShake() {
    const shell = document.getElementById('gacha-stage-shell');
    if (!shell) return;
    shell.classList.remove('pk-screen-shake');
    void shell.offsetWidth;
    shell.classList.add('pk-screen-shake');
    setTimeout(() => shell.classList.remove('pk-screen-shake'), 420);
}

// ==================== 1. KHÓA NÚT MUA NGUYÊN HỘP & ĐỒNG BỘ SỐ GÓI REAL-TIME ====================
window.disableBuyWholeBoxButton = function () {
    const btn = document.getElementById('btn-buy-whole-box');
    const hint = document.getElementById('whole-box-hint');
    if (!btn) return;

    btn.disabled = true;
    btn.className = 'rounded-2xl px-5 py-3 text-xs sm:text-sm font-extrabold transition flex items-center justify-center gap-2 pk-surface-muted pk-text-secondary cursor-not-allowed opacity-75';
    btn.innerHTML = `
        <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
            <rect width="18" height="11" x="3" y="11" rx="2" ry="2"/>
            <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
        </svg>
        <span id="btn-buy-whole-label" class="pk-num">Đã Khóa Mua Nguyên Hộp</span>
    `;
    if (hint) {
        hint.textContent = 'Hộp đã mở bán lẻ, không thể mua nguyên hộp';
    }
};

// Cập nhật số gói còn lại + Progress Bar + ĐỒNG BỘ REAL-TIME TRÊN DROPDOWN (#box-selector & Custom Dropdown)
window.updateRemainingPacksDisplay = function (remaining) {
    if (remaining === undefined || remaining === null) return;
    const remainingEl = document.getElementById('box-remaining-packs');
    const barEl = document.getElementById('box-remaining-progress');
    if (remainingEl) {
        remainingEl.textContent = remaining;
    }
    if (barEl && TOTAL_PACKS > 0) {
        const pct = Math.max(0, Math.min(100, (remaining * 100.0) / TOTAL_PACKS));
        barEl.style.width = pct + '%';
    }

    // Đồng bộ text "Còn XX/30 gói" trên <select id="box-selector"> lẫn Custom Dropdown Trigger đang hiển thị
    const boxSelector = document.getElementById('box-selector');
    if (boxSelector && ACTIVE_BOX_ID) {
        const activeOption = Array.from(boxSelector.options).find(opt => opt.value === String(ACTIVE_BOX_ID));
        if (activeOption) {
            const updatedText = activeOption.textContent.replace(/Còn\s+\d+\s*\/\s*\d+\s*gói/i, `Còn ${remaining}/${TOTAL_PACKS} gói`);
            activeOption.textContent = updatedText;
            // Gọi setter custom value trong common.js để cập nhật Trigger & Menu mà KHÔNG fire sự kiện change (tránh reload)
            boxSelector.value = boxSelector.value;
        }
    }
};

// 3. Chuyển gói bài đã rút sang Ô khay rỗng tối giản (Empty Slot) hoặc nút "Xé gói ngay" nếu user hiện tại vừa mua
window.markPackSlotAsSoldOnUI = function (positionInBox, boughtByCurrentUser, packId) {
    const slotEl = document.getElementById(`pack-slot-${positionInBox}`);
    if (!slotEl) return;

    slotEl.setAttribute('data-status', 'SOLD');
    const resolvedPackId = packId || slotEl.getAttribute('data-pack-id');

    if (boughtByCurrentUser && resolvedPackId) {
        slotEl.classList.remove('is-withdrawn');
        slotEl.classList.add('is-available');
        const actionsEl = document.getElementById(`pack-actions-${positionInBox}`);
        if (actionsEl) {
            actionsEl.innerHTML = `
                <button type="button"
                        onclick="window.openPurchasedPack(${Number(resolvedPackId)})"
                        class="pk-pack-open-btn rounded-xl bg-amber-400 hover:bg-amber-300 font-extrabold text-slate-950 shadow-md cursor-pointer transition">
                    <span>Xé gói ngay</span>
                </button>
            `;
        }
    } else {
        const posLabel = String(positionInBox).padStart(2, '0');
        slotEl.classList.remove('is-available', 'cursor-pointer');
        slotEl.classList.add('is-withdrawn');
        slotEl.innerHTML = `
            <div id="pack-actions-${positionInBox}"
                 class="flex-1 flex flex-col items-center justify-center p-4 text-center gap-2.5 select-none">
                <div class="flex h-10 w-10 items-center justify-center rounded-full pk-surface text-slate-400">
                    <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M20 6 9 17l-5-5"/>
                    </svg>
                </div>
                <span class="pk-num rounded-full pk-surface px-3 py-1 text-[11px] font-bold pk-text-secondary">
                    ✓ Đã rút • #${posLabel}
                </span>
            </div>
        `;
    }
};

// 4. Kết nối STOMP WebSocket tới /topic/boxes/{boxId}
function connectBoxTrayWebSocket() {
    if (!ACTIVE_BOX_ID || typeof SockJS === 'undefined' || typeof StompJs === 'undefined') return;

    const client = new StompJs.Client({
        webSocketFactory: () => new SockJS('/ws-blindbox'),
        reconnectDelay: 4000,
        onConnect: () => {
            const indicator = document.getElementById('ws-box-indicator');
            const statusText = document.getElementById('ws-box-status-text');
            if (indicator) indicator.className = 'h-2 w-2 rounded-full bg-emerald-500 animate-pulse';
            if (statusText) statusText.textContent = 'Trực tuyến';

            client.subscribe(`/topic/boxes/${ACTIVE_BOX_ID}`, (message) => {
                const event = JSON.parse(message.body);
                window.updateRemainingPacksDisplay(event.remainingPacks);

                if (event.canBuyWholeBox === false) {
                    window.disableBuyWholeBoxButton();
                }

                if (event.soldPackPosition) {
                    const slotEl = document.getElementById(`pack-slot-${event.soldPackPosition}`);
                    if (slotEl && slotEl.getAttribute('data-status') === 'AVAILABLE') {
                        window.markPackSlotAsSoldOnUI(event.soldPackPosition, false, null);
                        window.showToast('info', 'Khay hộp vừa cập nhật',
                            `Gói bài #${String(event.soldPackPosition).padStart(2, '0')} vừa được rút khỏi khay.`);
                    }
                } else if (event.remainingPacks === 0) {
                    document.querySelectorAll('.pack-slot-card').forEach(el => {
                        const pos = el.getAttribute('data-pos');
                        if (el.getAttribute('data-status') === 'AVAILABLE') {
                            window.markPackSlotAsSoldOnUI(pos, false, null);
                        }
                    });
                }
            });
        }
    });
    client.activate();
}

// 5. Mua 1 gói lẻ
window.buySinglePackAtPosition = async function (positionInBox) {
    if (!ACTIVE_BOX_ID) return;
    const actionsEl = document.getElementById(`pack-actions-${positionInBox}`);
    const triggerBtn = actionsEl ? actionsEl.querySelector('button') : null;
    if (triggerBtn && triggerBtn.disabled) return;
    const originalBtnHtml = triggerBtn ? triggerBtn.innerHTML : '';
    if (triggerBtn) {
        triggerBtn.disabled = true;
        triggerBtn.classList.add('opacity-70', 'cursor-wait');
        triggerBtn.innerHTML = '<span>Đang xử lý...</span>';
    }
    document.getElementById('box-conflict-banner')?.classList.add('hidden');

    try {
        const res = await fetch(`/api/v1/boxes/${ACTIVE_BOX_ID}/packs/buy`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ positionInBox })
        });
        const data = await res.json();

        if (res.status === 409) {
            const banner = document.getElementById('box-conflict-banner');
            const bannerText = document.getElementById('box-conflict-text');
            if (bannerText) bannerText.textContent = data.message;
            banner?.classList.remove('hidden');
            window.showToast('conflict', 'Gói bài vừa có người rút', data.message);
            if (triggerBtn) {
                triggerBtn.disabled = false;
                triggerBtn.classList.remove('opacity-70', 'cursor-wait');
                triggerBtn.innerHTML = originalBtnHtml;
            }
            return;
        }
        if (!res.ok) {
            window.showToast('error', 'Không thể mua gói bài', data.message || 'Vui lòng kiểm tra số dư Ví');
            if (triggerBtn) {
                triggerBtn.disabled = false;
                triggerBtn.classList.remove('opacity-70', 'cursor-wait');
                triggerBtn.innerHTML = originalBtnHtml;
            }
            return;
        }

        const boughtPack = data.purchasedPacks && data.purchasedPacks[0];
        window.disableBuyWholeBoxButton();
        window.updateRemainingPacksDisplay(data.remainingPacks);
        window.markPackSlotAsSoldOnUI(positionInBox, true, boughtPack ? boughtPack.id : null);
        if (data.remainingWalletBalance !== undefined) {
            const balEl = document.getElementById('nav-wallet-balance');
            if (balEl) balEl.textContent = window.formatVND(data.remainingWalletBalance);
        }

        if (boughtPack && boughtPack.id) {
            await window.openPurchasedPack(boughtPack.id);
        }
    } catch (err) {
        window.showToast('error', 'Lỗi kết nối', err.message);
        if (triggerBtn) {
            triggerBtn.disabled = false;
            triggerBtn.classList.remove('opacity-70', 'cursor-wait');
            triggerBtn.innerHTML = originalBtnHtml;
        }
    }
};

// 6. Mua nguyên hộp
window.buyWholeBoosterBox = async function () {
    if (!ACTIVE_BOX_ID) return;
    try {
        const res = await fetch(`/api/v1/boxes/${ACTIVE_BOX_ID}/buy-whole`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({})
        });
        const data = await res.json();

        if (res.status === 409) {
            window.showToast('conflict', 'Khay hộp vừa thay đổi', data.message);
            return;
        }
        if (!res.ok) {
            window.showToast('error', 'Không thể mua nguyên hộp', data.message);
            return;
        }

        window.disableBuyWholeBoxButton();
        await window.refreshNavbarWallet();
        window.showToast('success', 'Sở hữu Nguyên Hộp thành công!',
            `Bạn đã sở hữu toàn bộ ${data.purchasedPacks.length} gói bài trong hộp ${data.boxCode}.`);
        setTimeout(() => window.location.reload(), 1200);
    } catch (err) {
        window.showToast('error', 'Lỗi kết nối', err.message);
    }
};

// ==================== 7. IMMERSIVE GACHA OPENING, DRAMATIC FLIP & CARD SPOTLIGHT ====================
function checkAllCardsFlippedState() {
    const total = currentDrawnCards.length;
    if (total === 0) return;
    const flippedCount = document.querySelectorAll('#gacha-cards-container .pk-flip-scene.is-flipped').length;
    if (flippedCount >= total) {
        const title = document.getElementById('gacha-modal-title');
        const flipAllBtn = document.getElementById('btn-flip-all-cards');
        const saveBtn = document.getElementById('btn-gacha-save-continue');
        if (title) {
            title.textContent = `Chúc mừng! Bạn đã nhận được ${total} thẻ bài mới`;
        }
        if (flipAllBtn) {
            flipAllBtn.classList.add('hidden');
        }
        if (saveBtn) {
            saveBtn.classList.add('ring-2', 'ring-amber-400/70', 'shadow-xl');
        }
    }
}

window.handleGachaCardClick = function (idx) {
    const scene = document.getElementById(`flip-card-${idx}`);
    if (!scene) return;

    // Nếu thẻ đã lật -> mở Card Spotlight xem cận cảnh!
    if (scene.classList.contains('is-flipped')) {
        window.openCardSpotlight(idx);
        return;
    }

    // Chặn double-click khi đang chạy hiệu ứng hồi hộp 0.6s
    if (scene.dataset.revealing === 'true') return;
    scene.dataset.revealing = 'true';

    const cardItem = currentDrawnCards[idx];
    const tier = cardItem ? cardItem.tier : 'common';
    const rect = scene.getBoundingClientRect();
    const cx = rect.left + rect.width / 2;
    const cy = rect.top + rect.height / 2;

    const completeFlip = () => {
        scene.classList.remove('is-anticipation');
        scene.classList.add('is-flipped');
        delete scene.dataset.revealing;

        const captionEl = document.getElementById(`flip-caption-${idx}`);
        if (captionEl) {
            captionEl.classList.remove('opacity-0', 'pointer-events-none');
            captionEl.classList.add('opacity-100');
        }

        if (tier === 'ex' || tier === 'hyper') {
            triggerStageFlash(tier);
            triggerStageShake();
            GachaVfxCanvas.burst(cx, cy, tier);
            const holoEl = scene.querySelector('.pk-holo-card');
            if (holoEl) {
                holoEl.classList.add('pk-holo-shimmer');
            }
        } else if (tier === 'rare') {
            GachaVfxCanvas.burst(cx, cy, 'rare', 26);
        }

        checkAllCardsFlippedState();
    };

    if (tier === 'ex' || tier === 'hyper') {
        scene.classList.add('is-anticipation');
        setTimeout(completeFlip, 600);
    } else {
        completeFlip();
    }
};

window.openPurchasedPack = async function (packId) {
    try {
        const res = await fetch(`/api/v1/packs/${packId}/open`, {
            method: 'POST'
        });
        const data = await res.json();
        if (!res.ok) {
            window.showToast('error', 'Không thể mở gói bài', data.message);
            return;
        }

        if (data.positionInBox) {
            window.markPackSlotAsSoldOnUI(data.positionInBox, false, null);
        }
        await window.refreshNavbarWallet();

        const modal = document.getElementById('gacha-result-modal');
        const title = document.getElementById('gacha-modal-title');
        const tearStage = document.getElementById('gacha-tear-stage');
        const tearVisual = document.getElementById('tear-pack-visual');
        const tearStrip = document.getElementById('tear-zone-strip');
        const tearProgressBar = document.getElementById('tear-progress-bar');
        const cardsStage = document.getElementById('gacha-cards-stage');
        const modalFooter = document.getElementById('gacha-modal-footer');
        const flipAllBtn = document.getElementById('btn-flip-all-cards');
        const saveBtn = document.getElementById('btn-gacha-save-continue');
        const container = document.getElementById('gacha-cards-container');

        const posStr = String(data.positionInBox || 1).padStart(2, '0');
        document.getElementById('tear-pack-pos-label').textContent = `#${posStr}`;
        document.getElementById('tear-pack-count-label').textContent = `${data.snapshotCardsPerPack} THẺ`;
        title.textContent = `Gói bài #${posStr} • Bấm để xé vỏ gói bài!`;

        isTearInProgress = false;
        GachaVfxCanvas.clear();
        tearVisual.classList.remove('is-torn', 'is-shaking', 'is-tearing-normal', 'is-tearing-ex', 'is-tearing-hyper');
        tearVisual.style.transform = '';
        if (tearStrip) tearStrip.style.transform = '';
        if (tearProgressBar) tearProgressBar.style.width = '0%';
        tearStage.classList.remove('hidden');
        cardsStage.classList.add('hidden');
        // Ẩn hoàn toàn nút chân trang trong giai đoạn chưa xé gói
        if (modalFooter) {
            modalFooter.classList.add('hidden');
            modalFooter.classList.remove('flex');
        }
        flipAllBtn.classList.add('hidden');
        saveBtn?.classList.remove('ring-2', 'ring-amber-400/70', 'shadow-xl');

        // Tính độ hiếm cao nhất trong 5 lá bài
        let bestTier = 'common';
        currentDrawnCards = (data.drawnCards || []).map(uc => {
            const c = uc.card;
            const tier = resolveRarityTier(c);
            if (TIER_RANK[tier] > TIER_RANK[bestTier]) bestTier = tier;
            return {
                id: c.id,
                name: c.name,
                formattedCardNumber: c.formattedCardNumber,
                rarityName: c.rarityName,
                rarityColorHex: getTierDefaultColor(tier, c.rarityColorHex),
                imageUrl: c.imageUrl,
                isHolo: Boolean(c.rarityIsHolographic) || tier === 'rare' || tier === 'ex' || tier === 'hyper',
                tier
            };
        });
        currentPackHighestTier = bestTier;

        const esc = window.escapeHtml || (v => String(v ?? ''));
        container.innerHTML = currentDrawnCards.map((c, idx) => {
            const safeName = esc(c.name);
            const safeNum = esc(c.formattedCardNumber);
            const safeRarity = esc(c.rarityName);
            const safeImg = esc(c.imageUrl);
            const safeColor = c.rarityColorHex;
            const holoClass = c.isHolo ? 'is-holo-active' : '';
            const auraClass = `pk-aura-${c.tier}`;

            return `
                <div class="flex flex-col gap-2.5 pk-card-deal-in" style="animation-delay: ${idx * 75}ms">
                    <div class="pk-flip-scene" id="flip-card-${idx}" onclick="window.handleGachaCardClick(${idx})">
                        <div class="pk-flip-inner">
                            <!-- Mặt lưng thẻ (Úp) có hào quang màu theo độ hiếm của chính lá bài -->
                            <div class="pk-flip-face pk-flip-back ${auraClass} p-4 flex flex-col items-center justify-between text-center">
                                <span class="pk-num text-[11px] font-bold text-slate-300/80">#${idx + 1}</span>
                                <div class="my-auto flex flex-col items-center space-y-2.5">
                                    <div class="flex h-14 w-14 items-center justify-center rounded-full bg-rose-600/90 text-white shadow-lg ring-2 ring-white/20">
                                        <svg class="h-8 w-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75">
                                            <circle cx="12" cy="12" r="9"></circle>
                                            <circle cx="12" cy="12" r="2.5"></circle>
                                            <line x1="3" y1="12" x2="9.5" y2="12"></line>
                                            <line x1="14.5" y1="12" x2="21" y2="12"></line>
                                        </svg>
                                    </div>
                                    <span class="text-xs font-extrabold text-white tracking-wide">Bấm để lật</span>
                                </div>
                                <span class="text-[10px] font-semibold tracking-widest text-slate-400">DELTA REIGN</span>
                            </div>

                            <!-- Mặt trước lá bài cỡ lớn (Focus on Artwork, 3D Tilt + Holo) -->
                            <div class="pk-flip-face pk-flip-front">
                                <div class="pk-holo-card ${holoClass} h-full w-full rounded-2xl border-2 bg-slate-900/95 p-1.5 shadow-2xl flex items-center justify-center"
                                     style="border-color: ${safeColor}">
                                    <img src="${safeImg}" alt="${safeName}"
                                         onerror="this.src='https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/384.png'"
                                         class="relative z-10 w-full h-full object-contain rounded-xl drop-shadow-xl"/>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Thông tin tối giản dưới mỗi thẻ (ẩn khi đang úp để giữ hồi hộp, hiện ngay khi lật) -->
                    <div id="flip-caption-${idx}" class="px-1 text-center space-y-1 opacity-0 pointer-events-none transition-opacity duration-300">
                        <div class="text-xs sm:text-sm font-extrabold text-white truncate" title="${safeName}">${safeName}</div>
                        <div class="flex items-center justify-center gap-1.5 text-[10px]">
                            <span class="rounded-full border px-2 py-0.5 font-bold truncate max-w-[115px]"
                                  style="border-color:${safeColor}; color:${safeColor}; background:${safeColor}1A">${safeRarity}</span>
                            <span class="pk-num font-bold text-slate-400 shrink-0">${safeNum}</span>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

        modal.classList.remove('hidden');
        modal.classList.add('flex');
        document.body.style.overflow = 'hidden';
        window.bindHoloTiltEffects(container);
    } catch (err) {
        window.showToast('error', 'Lỗi mở gói bài', err.message);
    }
};

window.triggerTearPackAnimation = function () {
    const tearVisual = document.getElementById('tear-pack-visual');
    const tearStage = document.getElementById('gacha-tear-stage');
    const cardsStage = document.getElementById('gacha-cards-stage');
    const flipAllBtn = document.getElementById('btn-flip-all-cards');
    const title = document.getElementById('gacha-modal-title');

    if (!tearVisual || isTearInProgress || tearVisual.classList.contains('is-torn')) return;
    isTearInProgress = true;

    const tier = currentPackHighestTier;
    const isHighTier = tier === 'ex' || tier === 'hyper';
    const rect = tearVisual.getBoundingClientRect();
    const cx = rect.left + rect.width / 2;
    const cy = rect.top + 48;

    const performSplit = () => {
        tearVisual.classList.remove('is-shaking');
        tearVisual.classList.add(
            tier === 'hyper' ? 'is-tearing-hyper' : (tier === 'ex' ? 'is-tearing-ex' : 'is-tearing-normal')
        );
        triggerStageFlash(tier);
        GachaVfxCanvas.burst(cx, cy, tier, isHighTier ? 75 : 26);

        setTimeout(() => {
            tearVisual.classList.add('is-torn');
        }, 120);

        setTimeout(() => {
            tearStage.classList.add('hidden');
            cardsStage.classList.remove('hidden');
            flipAllBtn.classList.remove('hidden');
            const modalFooter = document.getElementById('gacha-modal-footer');
            if (modalFooter) {
                modalFooter.classList.remove('hidden');
                modalFooter.classList.add('flex');
            }
            if (title) title.textContent = 'Bấm vào từng lá bài để lật mở!';
            isTearInProgress = false;
        }, 620);
    };

    if (isHighTier) {
        tearVisual.classList.add('is-shaking');
        setTimeout(performSplit, 420);
    } else {
        performSplit();
    }
};

window.flipAllDrawnCards = function () {
    const scenes = Array.from(document.querySelectorAll('#gacha-cards-container .pk-flip-scene'));
    scenes.forEach((scene, idx) => {
        if (!scene.classList.contains('is-flipped')) {
            setTimeout(() => window.handleGachaCardClick(idx), idx * 140);
        }
    });
};

window.openCardSpotlight = function (idx) {
    const card = currentDrawnCards[idx];
    if (!card) return;

    const modal = document.getElementById('card-spotlight-modal');
    const frame = document.getElementById('spotlight-card-frame');
    const img = document.getElementById('spotlight-card-img');
    const nameEl = document.getElementById('spotlight-card-name');
    const rarityEl = document.getElementById('spotlight-card-rarity');
    const numEl = document.getElementById('spotlight-card-num');
    const detailLink = document.getElementById('spotlight-card-detail-link');

    if (frame) {
        frame.style.borderColor = card.rarityColorHex;
        frame.classList.toggle('is-holo-active', Boolean(card.isHolo));
        frame.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)';
    }
    if (img) {
        img.src = card.imageUrl;
        img.alt = card.name;
    }
    if (nameEl) nameEl.textContent = card.name;
    if (rarityEl) {
        rarityEl.textContent = card.rarityName;
        rarityEl.style.borderColor = card.rarityColorHex;
        rarityEl.style.color = card.rarityColorHex;
        rarityEl.style.backgroundColor = card.rarityColorHex + '20';
    }
    if (numEl) numEl.textContent = card.formattedCardNumber;
    if (detailLink) detailLink.href = `/cards/${Number(card.id)}`;

    modal?.classList.remove('hidden');
    modal?.classList.add('flex');
    if (window.bindHoloTiltEffects && modal) {
        window.bindHoloTiltEffects(modal);
    }
};

window.closeCardSpotlight = function () {
    const modal = document.getElementById('card-spotlight-modal');
    if (!modal) return;
    modal.classList.add('hidden');
    modal.classList.remove('flex');
};

window.closeGachaModal = function () {
    window.closeCardSpotlight();
    GachaVfxCanvas.clear();
    const modal = document.getElementById('gacha-result-modal');
    modal?.classList.add('hidden');
    modal?.classList.remove('flex');
    document.body.style.overflow = '';
};

window.toggleDropRatePopover = function (e) {
    if (e) e.stopPropagation();
    document.getElementById('drop-rate-popover')?.classList.toggle('hidden');
};

window.closeDropRatePopover = function () {
    document.getElementById('drop-rate-popover')?.classList.add('hidden');
};

document.addEventListener('click', (e) => {
    const popover = document.getElementById('drop-rate-popover');
    const btn = document.getElementById('btn-toggle-drop-rates');
    if (popover && !popover.classList.contains('hidden') && !popover.contains(e.target) && !btn?.contains(e.target)) {
        popover.classList.add('hidden');
    }
});

document.addEventListener('keydown', (e) => {
    if (e.key !== 'Escape') return;
    const spotlight = document.getElementById('card-spotlight-modal');
    if (spotlight && !spotlight.classList.contains('hidden')) {
        window.closeCardSpotlight();
        return;
    }
    const gachaModal = document.getElementById('gacha-result-modal');
    if (gachaModal && !gachaModal.classList.contains('hidden')) {
        window.closeGachaModal();
    }
});

// ==================== HERO BOOSTER PACK: 3D PARALLAX TILT + DYNAMIC GLARE + SWIPE-TO-TEAR ====================
function initHeroPackInteractions() {
    const tearVisual = document.getElementById('tear-pack-visual');
    const tearStrip = document.getElementById('tear-zone-strip');
    const progressBar = document.getElementById('tear-progress-bar');
    if (!tearVisual) return;

    // 1. 3D Parallax Tilt + Dynamic Holographic Glare theo vị trí con trỏ chuột
    tearVisual.addEventListener('mousemove', (e) => {
        if (isTearInProgress || tearVisual.classList.contains('is-torn')) return;
        const rect = tearVisual.getBoundingClientRect();
        const pctX = Math.max(0, Math.min(100, ((e.clientX - rect.left) / rect.width) * 100));
        const pctY = Math.max(0, Math.min(100, ((e.clientY - rect.top) / rect.height) * 100));
        const rotateY = ((pctX - 50) / 50) * 12;
        const rotateX = ((50 - pctY) / 50) * 10;

        tearVisual.style.transform = `perspective(1100px) rotateX(${rotateX.toFixed(2)}deg) rotateY(${rotateY.toFixed(2)}deg) scale3d(1.02, 1.02, 1.02)`;
        tearVisual.style.setProperty('--mx', `${pctX.toFixed(1)}%`);
        tearVisual.style.setProperty('--my', `${pctY.toFixed(1)}%`);
        tearVisual.style.setProperty('--glare-opacity', '0.88');
    });

    tearVisual.addEventListener('mouseleave', () => {
        if (isTearInProgress || tearVisual.classList.contains('is-torn')) return;
        tearVisual.style.transform = 'perspective(1100px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)';
        tearVisual.style.setProperty('--glare-opacity', '0.45');
    });

    // 2. Kéo chuột / vuốt cảm ứng ngang qua mép xé (Swipe-to-Tear) + Bấm trực tiếp (Click)
    if (tearStrip) {
        let dragging = false;
        let startX = 0;
        let maxDeltaX = 0;

        const resetDragVisual = () => {
            dragging = false;
            if (!isTearInProgress && !tearVisual.classList.contains('is-torn')) {
                tearStrip.style.transform = '';
                if (progressBar) progressBar.style.width = '0%';
            }
        };

        tearStrip.addEventListener('pointerdown', (e) => {
            if (isTearInProgress || tearVisual.classList.contains('is-torn')) return;
            dragging = true;
            startX = e.clientX;
            maxDeltaX = 0;
            try { tearStrip.setPointerCapture(e.pointerId); } catch (_) {}
        });

        tearStrip.addEventListener('pointermove', (e) => {
            if (!dragging || isTearInProgress) return;
            const deltaX = Math.max(0, e.clientX - startX);
            maxDeltaX = Math.max(maxDeltaX, deltaX);
            const width = tearStrip.offsetWidth || 268;
            const pct = Math.min(100, (deltaX / (width * 0.55)) * 100);

            if (progressBar) progressBar.style.width = `${pct.toFixed(1)}%`;
            const tiltDeg = (pct / 100) * 11;
            const liftY = (pct / 100) * 8;
            tearStrip.style.transform = `translate(${(deltaX * 0.25).toFixed(1)}px, -${liftY.toFixed(1)}px) rotate(${tiltDeg.toFixed(1)}deg)`;

            if (pct >= 80) {
                dragging = false;
                if (progressBar) progressBar.style.width = '100%';
                window.triggerTearPackAnimation();
            }
        });

        tearStrip.addEventListener('pointerup', (e) => {
            if (!dragging) return;
            const deltaX = Math.abs(e.clientX - startX);
            resetDragVisual();
            // Nếu người dùng bấm nhẹ (tap/click < 12px) hoặc kéo đủ xa -> kích hoạt xé gói ngay
            if (deltaX < 12 || maxDeltaX >= 75) {
                window.triggerTearPackAnimation();
            }
        });

        tearStrip.addEventListener('pointercancel', resetDragVisual);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    connectBoxTrayWebSocket();
    initHeroPackInteractions();
});
