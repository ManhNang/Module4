/* ==================== BINDER / INVENTORY PAGE (3 TABS: CARDS, PACK & BOX, MASTER SET) ==================== */

// Tập hợp ID các loại thẻ đang sở hữu (dùng để gắn nhãn MỚI khi mở Pack)
const ownedTemplateIds = new Set();

function updateInventoryQuickStats() {
    const grid = document.getElementById('my-cards-grid');
    const statsEl = document.getElementById('inventory-quick-stats');
    if (!grid || !statsEl) return;
    const cards = Array.from(grid.querySelectorAll('.my-card-item')).filter(el => el.dataset.removing !== 'true');
    const uniqueTypes = cards.length;
    let totalCopies = 0;
    cards.forEach(el => {
        totalCopies += parseInt(el.getAttribute('data-total-count') || '1', 10) || 1;
    });
    statsEl.textContent = `${uniqueTypes} loại thẻ • ${totalCopies} bản sao`;
}

// Gom nhóm các bản sao trùng nhau thành 1 thẻ duy nhất kèm badge xN
// Đồng thời đếm chính xác tổng số bản sao (data-total-count) và số bản sao chưa khóa có thể bán (data-sellable-count)
(function deduplicateInventoryCards() {
    const grid = document.getElementById('my-cards-grid');
    if (!grid) return;
    const items = Array.from(grid.querySelectorAll('.my-card-item'));
    const keptByTemplate = new Map();
    items.forEach(el => {
        const tplId = el.getAttribute('data-template-id');
        const isLocked = el.getAttribute('data-is-locked') === 'true';
        if (!keptByTemplate.has(tplId)) {
            keptByTemplate.set(tplId, {
                el,
                isLocked,
                totalCount: 1,
                sellableCount: isLocked ? 0 : 1
            });
        } else {
            const entry = keptByTemplate.get(tplId);
            entry.totalCount += 1;
            if (!isLocked) entry.sellableCount += 1;

            if (entry.isLocked && !isLocked) {
                entry.el.remove();
                entry.el = el;
                entry.isLocked = false;
            } else {
                el.remove();
            }
        }
    });

    keptByTemplate.forEach((entry, tplId) => {
        if (tplId) ownedTemplateIds.add(String(tplId));
        entry.el.setAttribute('data-total-count', String(entry.totalCount));
        entry.el.setAttribute('data-sellable-count', String(entry.sellableCount));
        const badge = entry.el.querySelector('.card-copy-badge');
        if (badge) {
            badge.textContent = `x${entry.totalCount}`;
            badge.classList.toggle('hidden', entry.totalCount <= 1);
        }
    });

    document.querySelectorAll('.binder-slot-item[data-is-owned="true"]').forEach(slot => {
        const id = slot.getAttribute('data-template-id');
        if (id) ownedTemplateIds.add(String(id));
    });

    updateInventoryQuickStats();
})();

// Tìm kiếm, Lọc Bộ sưu tập, Lọc Độ hiếm, Sắp xếp & Phân trang Tab 1 (Thẻ Bài)
const INV_CARDS_PER_PAGE = 25;
let invCardsPage = 1;

function renderInventoryPagination(totalMatched, startIdx, endIdx, totalPages) {
    const bar = document.getElementById('inv-cards-pagination');
    const infoEl = document.getElementById('inv-cards-page-info');
    const controlsEl = document.getElementById('inv-cards-page-controls');
    if (!bar || !infoEl || !controlsEl) return;

    if (totalMatched <= INV_CARDS_PER_PAGE) {
        bar.classList.add('hidden');
        return;
    }

    bar.classList.remove('hidden');
    infoEl.textContent = `Hiển thị ${startIdx + 1}–${endIdx} trên tổng ${totalMatched} loại thẻ`;

    const pages = [];
    for (let p = 1; p <= totalPages; p++) {
        if (p === 1 || p === totalPages || Math.abs(p - invCardsPage) <= 1) {
            pages.push(p);
        } else if (pages[pages.length - 1] !== '...') {
            pages.push('...');
        }
    }

    const baseBtn = 'rounded-lg px-2.5 py-1.5 text-xs font-bold transition cursor-pointer';
    const inactiveBtn = `${baseBtn} pk-surface-muted pk-text-secondary hover:pk-text-primary`;
    const activeBtn = `${baseBtn} bg-amber-500 text-slate-950 font-extrabold shadow-sm`;
    const disabledBtn = `${baseBtn} pk-surface-muted pk-text-secondary opacity-40 cursor-not-allowed`;

    let html = `<button type="button" ${invCardsPage <= 1 ? 'disabled' : `onclick="window.goToInventoryCardsPage(${invCardsPage - 1})"`} class="${invCardsPage <= 1 ? disabledBtn : inactiveBtn}">‹ Trước</button>`;
    pages.forEach(p => {
        if (p === '...') {
            html += `<span class="px-1.5 text-xs pk-text-secondary">…</span>`;
        } else {
            html += `<button type="button" onclick="window.goToInventoryCardsPage(${p})" class="${p === invCardsPage ? activeBtn : inactiveBtn}">${p}</button>`;
        }
    });
    html += `<button type="button" ${invCardsPage >= totalPages ? 'disabled' : `onclick="window.goToInventoryCardsPage(${invCardsPage + 1})"`} class="${invCardsPage >= totalPages ? disabledBtn : inactiveBtn}">Sau ›</button>`;
    controlsEl.innerHTML = html;
}

window.goToInventoryCardsPage = function (page) {
    window.filterAndSortInventoryCards(page);
    document.getElementById('panel-tab-inventory')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

window.filterAndSortInventoryCards = function (targetPage) {
    const grid = document.getElementById('my-cards-grid');
    if (!grid) return;
    invCardsPage = typeof targetPage === 'number' && !Number.isNaN(targetPage) ? targetPage : 1;

    const searchVal = (document.getElementById('inv-filter-search')?.value || '').trim().toLowerCase().replace(/^#/, '');
    const setVal = (document.getElementById('inv-filter-set')?.value || '').trim().toLowerCase();
    const rarityVal = (document.getElementById('inv-filter-rarity')?.value || '').trim().toLowerCase();
    const sortVal = document.getElementById('inv-filter-sort')?.value || 'rarity-desc';

    const cards = Array.from(grid.querySelectorAll('.my-card-item')).filter(el => el.dataset.removing !== 'true');
    cards.sort((a, b) => {
        if (sortVal === 'number-asc') {
            return (parseInt(a.dataset.cardNumber || '0', 10) || 0) - (parseInt(b.dataset.cardNumber || '0', 10) || 0);
        }
        if (sortVal === 'name-asc') {
            return (a.dataset.cardName || '').localeCompare(b.dataset.cardName || '');
        }
        const pDiff = (parseFloat(b.dataset.buybackPrice || '0') || 0) - (parseFloat(a.dataset.buybackPrice || '0') || 0);
        if (pDiff !== 0) return pDiff;
        return (parseInt(a.dataset.cardNumber || '0', 10) || 0) - (parseInt(b.dataset.cardNumber || '0', 10) || 0);
    });

    const matched = [];
    cards.forEach(card => {
        const name = (card.dataset.cardName || '').toLowerCase();
        const setName = (card.dataset.setName || '').toLowerCase();
        const num = (card.dataset.cardFormattedNum || card.dataset.cardNumber || '').toLowerCase();
        const rarity = (card.dataset.rarityName || '').toLowerCase();
        const matchSearch = !searchVal || name.includes(searchVal) || num.includes(searchVal);
        const matchSet = !setVal || setName === setVal;
        const matchRarity = !rarityVal || rarity === rarityVal;
        const isMatch = matchSearch && matchSet && matchRarity;
        card.dataset.filterMatch = isMatch ? 'true' : 'false';
        if (isMatch) matched.push(card);
        grid.appendChild(card);
    });

    const totalMatched = matched.length;
    const totalPages = Math.max(1, Math.ceil(totalMatched / INV_CARDS_PER_PAGE));
    invCardsPage = Math.min(totalPages, Math.max(1, invCardsPage));
    const startIdx = (invCardsPage - 1) * INV_CARDS_PER_PAGE;
    const endIdx = Math.min(startIdx + INV_CARDS_PER_PAGE, totalMatched);
    const visibleSet = new Set(matched.slice(startIdx, endIdx));

    cards.forEach(card => {
        card.style.display = visibleSet.has(card) ? '' : 'none';
    });

    renderInventoryPagination(totalMatched, startIdx, endIdx, totalPages);
};

const binderMainEl = document.getElementById('binder-main');
const INITIAL_TAB = (binderMainEl && binderMainEl.dataset.initialTab) ? binderMainEl.dataset.initialTab : 'inventory';

const TAB_TO_URL_PARAM = {
    inventory: 'cards',
    sealed: 'sealed',
    masterset: 'progress'
};

function normalizeInventoryTab(tabName) {
    const t = (tabName || '').trim().toLowerCase();
    if (t === 'sealed') return 'sealed';
    if (t === 'progress' || t === 'masterset') return 'masterset';
    return 'inventory';
}

let activeInventoryTab = 'inventory';

window.switchInventoryTab = function (tabName, syncUrl = true) {
    const tabs = ['inventory', 'sealed', 'masterset'];
    const active = normalizeInventoryTab(tabName);
    activeInventoryTab = active;

    // Tự động tắt chế độ Dọn kho khi rời khỏi tab Thẻ Bài
    if (active !== 'inventory' && typeof window.toggleBulkSellMode === 'function' && isBulkSellMode) {
        window.toggleBulkSellMode(false);
    }

    tabs.forEach(t => {
        const panel = document.getElementById(`panel-tab-${t}`);
        const btn = document.getElementById(`btn-tab-${t}`);
        const isActive = t === active;
        panel?.classList.toggle('hidden', !isActive);
        if (btn) {
            btn.className = isActive
                ? 'pk-btn-primary justify-center rounded-xl px-3.5 py-2.5 text-xs sm:text-sm font-extrabold transition flex items-center gap-1.5 cursor-pointer whitespace-nowrap'
                : 'justify-center rounded-xl px-3.5 py-2.5 text-xs sm:text-sm font-bold pk-text-secondary hover:pk-text-primary transition flex items-center gap-1.5 cursor-pointer whitespace-nowrap';
        }
    });

    if (syncUrl && window.history && typeof window.history.replaceState === 'function') {
        const url = new URL(window.location.href);
        url.searchParams.set('tab', TAB_TO_URL_PARAM[active] || 'cards');
        window.history.replaceState(null, '', url.pathname + url.search + url.hash);
    }
};

let progressOwnershipFilter = 'ALL';

window.setProgressOwnershipFilter = function (mode) {
    progressOwnershipFilter = mode || 'ALL';
    ['ALL', 'OWNED', 'MISSING'].forEach(m => {
        const btn = document.getElementById(`progress-filter-${m}`);
        if (!btn) return;
        const isActive = m === progressOwnershipFilter;
        btn.className = isActive
            ? 'progress-filter-btn rounded-lg px-2.5 py-1.5 text-xs font-extrabold transition cursor-pointer bg-amber-500 text-slate-950 shadow-sm'
            : 'progress-filter-btn rounded-lg px-2.5 py-1.5 text-xs font-bold pk-text-secondary hover:pk-text-primary transition cursor-pointer';
    });
    window.filterBinderGridClientSide();
};

window.filterBinderGridClientSide = function () {
    const nameVal = (document.getElementById('filter-name')?.value || '').trim().toLowerCase().replace(/^#/, '');

    document.querySelectorAll('.binder-slot-item').forEach(slot => {
        const cNum = slot.getAttribute('data-card-number') || '';
        const paddedNum = cNum.padStart(3, '0');
        const cName = (slot.getAttribute('data-card-name') || '').toLowerCase();
        const isOwned = slot.getAttribute('data-is-owned') === 'true';

        let visible = true;
        if (progressOwnershipFilter === 'OWNED' && !isOwned) visible = false;
        if (progressOwnershipFilter === 'MISSING' && isOwned) visible = false;
        if (nameVal && !cName.includes(nameVal) && !cNum.includes(nameVal) && !paddedNum.includes(nameVal)) visible = false;

        slot.style.display = visible ? '' : 'none';
    });
};

// ==================== TAB 2: KHO PACK & BOX (BÓC SEAL, MỞ 1 PACK, MỞ NHANH 5 PACK) ====================
let activeVaultSetId = null;
let activeVaultSetName = '';
let activeVaultSetCode = 'DLR';
let activeVaultCardsPerPack = 6;
let remainingVaultPacks = 0;
let hasOpenedInSession = false;
let isOpeningVaultPacks = false;
let tearDragState = null;

async function parseJsonSafe(res) {
    try {
        return await res.json();
    } catch (e) {
        return { message: `Lỗi máy chủ (HTTP ${res.status}). Vui lòng thử lại sau!` };
    }
}

function classifyRarityTier(rarityName, isHolo) {
    const n = (rarityName || '').toLowerCase();
    if (/(hyper|secret|gold|special illustration)/.test(n)) return 'hyper';
    if (/(\bex\b|double rare|ultra rare|full art|illustration rare)/.test(n)) return 'ex';
    if (/(uncommon|không phổ biến)/.test(n)) return 'uncommon';
    if (/(\brare\b|hiếm)/.test(n) || isHolo) return 'rare';
    return 'common';
}

function resolveStandardRarityColor(rarityName, fallbackHex) {
    const n = (rarityName || '').toLowerCase();
    if (n.includes('special illustration')) return '#ec4899';
    if (/(hyper|secret|gold)/.test(n)) return '#f59e0b';
    if (n.includes('illustration')) return '#c084fc';
    if (n.includes('ultra')) return '#fb923c';
    if (n.includes('double')) return '#818cf8';
    if (n.includes('uncommon') || n.includes('không phổ biến')) return '#34d399';
    if (n.includes('rare') || n.includes('hiếm')) return '#38bdf8';
    if (n.includes('common') || n.includes('phổ biến')) return '#94a3b8';
    return fallbackHex || '#94a3b8';
}

function triggerIsolatedShake(targetEl) {
    if (!targetEl) return;
    targetEl.classList.remove('pk-screen-shake');
    void targetEl.offsetWidth;
    targetEl.classList.add('pk-screen-shake');
    setTimeout(() => targetEl.classList.remove('pk-screen-shake'), 420);
}

function setGachaStageScrollMode(isMultiPack) {
    const stage = document.getElementById('vault-gacha-stage');
    if (!stage) return;
    stage.classList.toggle('is-multi-pack', Boolean(isMultiPack));
    stage.classList.toggle('is-single-pack', !isMultiPack);
}

function triggerStageFlash(tier) {
    const flash = document.getElementById('vault-flash-overlay');
    if (!flash) return;
    flash.style.background = tier === 'hyper'
        ? 'radial-gradient(circle, rgba(255,255,255,0.95), rgba(168,85,247,0.55), rgba(56,189,248,0.35))'
        : (tier === 'ex'
            ? 'radial-gradient(circle, rgba(255,255,255,0.95), rgba(251,191,36,0.55))'
            : '#ffffff');
    flash.style.opacity = '0.88';
    setTimeout(() => { flash.style.opacity = '0'; }, 180);
}

function spawnGachaConfetti(tier) {
    const layer = document.getElementById('vault-confetti-layer');
    if (!layer) return;
    const count = tier === 'hyper' ? 42 : 26;
    const colors = tier === 'hyper'
        ? ['#ff2a85', '#fbbf24', '#34d399', '#38bdf8', '#a855f7', '#ffffff']
        : ['#fbbf24', '#fde047', '#f59e0b', '#ffffff', '#fb7185'];

    for (let i = 0; i < count; i++) {
        const piece = document.createElement('div');
        const size = 6 + Math.random() * 7;
        const startX = 25 + Math.random() * 50;
        const startY = 30 + Math.random() * 25;
        const dx = (Math.random() - 0.5) * 420;
        const dy = -120 - Math.random() * 220;
        const rot = Math.round(Math.random() * 720 - 360);
        piece.style.cssText = `
            position:absolute; left:${startX}%; top:${startY}%;
            width:${size}px; height:${size * 0.65}px;
            background:${colors[i % colors.length]};
            border-radius:2px; box-shadow:0 0 8px ${colors[i % colors.length]};
            transition: transform 1.15s cubic-bezier(0.12, 0.82, 0.28, 1), opacity 1.15s ease-out;
            transform: translate3d(0,0,0) rotate(0deg); opacity: 1;
        `;
        layer.appendChild(piece);
        requestAnimationFrame(() => {
            piece.style.transform = `translate3d(${dx}px, ${dy + 340}px, 0) rotate(${rot}deg)`;
            piece.style.opacity = '0';
        });
        setTimeout(() => piece.remove(), 1250);
    }
}

function resetTearPackVisual() {
    const wrap = document.getElementById('vault-tear-wrapper');
    const prog = document.getElementById('vault-tear-progress');
    if (wrap) {
        wrap.classList.remove('is-shaking', 'is-torn', 'is-tearing-normal', 'is-tearing-ex', 'is-tearing-hyper');
        wrap.style.transform = '';
    }
    if (prog) prog.style.width = '0%';
}

function setupTearPackInteractions() {
    const wrap = document.getElementById('vault-tear-wrapper');
    const flap = document.getElementById('vault-tear-flap');
    const body = document.getElementById('vault-tear-body');
    const glare = document.getElementById('vault-hero-glare');
    const prog = document.getElementById('vault-tear-progress');
    if (!wrap || !flap || !body) return;

    // 3D Parallax Tilt + Glare on hover
    wrap.addEventListener('mousemove', (e) => {
        if (wrap.classList.contains('is-torn') || wrap.classList.contains('is-shaking')) return;
        const r = wrap.getBoundingClientRect();
        const px = (e.clientX - r.left) / r.width;
        const py = (e.clientY - r.top) / r.height;
        const rotY = (px - 0.5) * 18;
        const rotX = (0.5 - py) * 14;
        wrap.style.transform = `rotateX(${rotX.toFixed(1)}deg) rotateY(${rotY.toFixed(1)}deg)`;
        if (glare) {
            glare.style.setProperty('--mx', `${(px * 100).toFixed(1)}%`);
            glare.style.setProperty('--my', `${(py * 100).toFixed(1)}%`);
        }
    });
    wrap.addEventListener('mouseleave', () => {
        if (!wrap.classList.contains('is-torn')) wrap.style.transform = '';
    });

    // Click body or flap -> tear immediately
    body.addEventListener('click', () => window.executeTearSinglePack());

    // Drag-to-tear across top flap (or simple tap)
    flap.addEventListener('pointerdown', (e) => {
        if (isOpeningVaultPacks) return;
        tearDragState = { startX: e.clientX, moved: false };
        flap.setPointerCapture?.(e.pointerId);
    });
    flap.addEventListener('pointermove', (e) => {
        if (!tearDragState || isOpeningVaultPacks) return;
        const dx = Math.max(0, e.clientX - tearDragState.startX);
        if (dx > 6) tearDragState.moved = true;
        const pct = Math.min(100, (dx / 150) * 100);
        if (prog) prog.style.width = `${pct}%`;
        if (pct >= 72) {
            tearDragState = null;
            window.executeTearSinglePack();
        }
    });
    flap.addEventListener('pointerup', () => {
        if (!tearDragState) return;
        const wasClick = !tearDragState.moved;
        tearDragState = null;
        if (prog) prog.style.width = '0%';
        if (wasClick) window.executeTearSinglePack();
    });
}

window.unsealVaultBox = async function (btn) {
    if (isOpeningVaultPacks) return;
    const setId = parseInt(btn.getAttribute('data-set-id'), 10);
    const setName = btn.getAttribute('data-set-name') || '';
    const packsPerBox = btn.getAttribute('data-packs-per-box') || '20';
    if (!confirm(`Bóc seal 1 Box "${setName}" để nhận +${packsPerBox} Pack lẻ vào Kho?`)) return;

    isOpeningVaultPacks = true;
    const origHtml = btn.innerHTML;
    btn.disabled = true;
    btn.textContent = 'Đang bóc seal...';
    try {
        const res = await fetch('/api/v1/inventory/sealed/unseal-box', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ setId })
        });
        const data = await parseJsonSafe(res);
        if (!res.ok) {
            window.showToast('error', 'Không thể bóc seal Box', data.message || 'Vui lòng thử lại.');
            return;
        }
        window.showToast('success', 'Bóc seal thành công!', data.message);
        setTimeout(() => { window.location.href = '/inventory?tab=sealed'; }, 750);
    } catch (err) {
        window.showToast('error', 'Lỗi kết nối', err.message);
    } finally {
        isOpeningVaultPacks = false;
        btn.disabled = false;
        btn.innerHTML = origHtml;
    }
};

let activeDrawnCards = [];
let lastHitVfxAt = 0;
let vaultRareOnlyFilter = false;

function lockBodyScrollForGacha(locked) {
    document.documentElement.classList.toggle('pk-modal-lock', locked);
    document.body.classList.toggle('pk-modal-lock', locked);
}

function showTearStageForActiveSet() {
    resetTearPackVisual();
    setGachaStageScrollMode(false);
    vaultRareOnlyFilter = false;
    document.getElementById('vault-open-title').textContent = `Mở 1 Pack • ${activeVaultSetName}`;
    document.getElementById('vault-open-subtitle').textContent = `Bấm hoặc kéo sang phải tại mép trên để xé vỏ và khám phá ${activeVaultCardsPerPack} thẻ bài!`;
    document.getElementById('vault-tear-set-code').textContent = activeVaultSetCode;
    document.getElementById('vault-tear-set-name').textContent = activeVaultSetName;
    document.getElementById('vault-tear-cards-count').textContent = `${activeVaultCardsPerPack} THẺ`;

    document.getElementById('vault-flip-progress')?.classList.add('hidden');
    document.getElementById('vault-scroll-fade')?.classList.add('hidden');
    document.getElementById('btn-vault-filter-rare')?.classList.add('hidden');

    document.getElementById('vault-stage-tear').classList.remove('hidden');
    document.getElementById('vault-stage-cards').classList.add('hidden');
    document.getElementById('modal-vault-open-pack').classList.remove('hidden');
    lockBodyScrollForGacha(true);
}

window.openVaultPackInteractive = function (btn) {
    if (isOpeningVaultPacks) return;
    activeVaultSetId = parseInt(btn.getAttribute('data-set-id'), 10);
    activeVaultSetName = btn.getAttribute('data-set-name') || '';
    activeVaultSetCode = btn.getAttribute('data-set-code') || 'PKM';
    activeVaultCardsPerPack = parseInt(btn.getAttribute('data-cards-per-pack') || '6', 10) || 6;
    remainingVaultPacks = parseInt(btn.getAttribute('data-available-packs') || '1', 10) || 1;
    hasOpenedInSession = false;
    showTearStageForActiveSet();
};

window.openNextVaultPack = function () {
    if (isOpeningVaultPacks || remainingVaultPacks <= 0) return;
    showTearStageForActiveSet();
};

window.executeTearSinglePack = async function () {
    if (!activeVaultSetId || isOpeningVaultPacks) return;
    const wrap = document.getElementById('vault-tear-wrapper');
    isOpeningVaultPacks = true;

    // Giai đoạn rung lắc build-up ngay khi bắt đầu xé
    if (wrap) {
        wrap.style.transform = '';
        wrap.classList.add('is-shaking');
    }

    try {
        const res = await fetch('/api/v1/inventory/sealed/open-packs', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ setId: activeVaultSetId, count: 1 })
        });
        const data = await parseJsonSafe(res);
        if (!res.ok) {
            resetTearPackVisual();
            window.showToast('error', 'Không thể mở Pack', data.message || 'Vui lòng thử lại.');
            return;
        }

        hasOpenedInSession = true;
        remainingVaultPacks = Math.max(0, remainingVaultPacks - 1);
        const drawnCards = data.drawnCards || [];

        // Xác định độ hiếm cao nhất trong gói để chọn màu tia sáng xé gói
        let highestTier = 'normal';
        drawnCards.forEach(uc => {
            const t = classifyRarityTier(uc.card?.rarityName, uc.card?.rarityIsHolographic);
            if (t === 'hyper') highestTier = 'hyper';
            else if (t === 'ex' && highestTier !== 'hyper') highestTier = 'ex';
        });

        // Cao trào: Tia sáng đường cắt + Rung cô lập trên chính gói bài (nếu có EX/Hyper) + Xé toạc vỏ + Lóe sáng
        if (wrap) {
            wrap.classList.remove('is-shaking');
            wrap.classList.add(`is-tearing-${highestTier}`);
            if (highestTier === 'ex' || highestTier === 'hyper') {
                triggerIsolatedShake(wrap);
            }
        }

        setTimeout(() => {
            wrap?.classList.add('is-torn');
            triggerStageFlash(highestTier);
        }, 260);

        setTimeout(() => {
            document.getElementById('vault-open-subtitle').textContent =
                'Bấm vào từng lá bài để lật 3D hoặc bấm "Lật tất cả thẻ"!';
            renderDrawnCardsResult(drawnCards, true);
            updateOpenAnotherButton();
            isOpeningVaultPacks = false;
        }, 680);
    } catch (err) {
        resetTearPackVisual();
        window.showToast('error', 'Lỗi kết nối', err.message);
        isOpeningVaultPacks = false;
    }
};

window.openVaultQuickFivePacks = async function (btn) {
    if (isOpeningVaultPacks) return;
    const setId = parseInt(btn.getAttribute('data-set-id'), 10);
    const setName = btn.getAttribute('data-set-name') || '';
    activeVaultSetId = setId;
    activeVaultSetName = setName;
    remainingVaultPacks = 0;
    hasOpenedInSession = false;

    isOpeningVaultPacks = true;
    const origText = btn.textContent;
    btn.disabled = true;
    btn.textContent = 'Đang mở 5 Pack...';
    try {
        const res = await fetch('/api/v1/inventory/sealed/open-packs', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ setId, count: 5 })
        });
        const data = await parseJsonSafe(res);
        if (!res.ok) {
            window.showToast('error', 'Không thể mở nhanh 5 Pack', data.message || 'Vui lòng thử lại.');
            return;
        }
        hasOpenedInSession = true;
        const drawnCards = data.drawnCards || [];
        document.getElementById('vault-open-title').textContent = `Mở Nhanh 5 Pack • ${setName}`;
        document.getElementById('vault-open-subtitle').textContent =
            `Đã bóc ${drawnCards.length} thẻ bài — bấm từng lá để lật 3D hoặc bấm "Lật tất cả thẻ"!`;
        document.getElementById('modal-vault-open-pack').classList.remove('hidden');
        lockBodyScrollForGacha(true);
        renderDrawnCardsResult(drawnCards, true);
        updateOpenAnotherButton();
        window.showToast('success', 'Mở nhanh 5 Pack thành công!', data.message);
    } catch (err) {
        window.showToast('error', 'Lỗi kết nối', err.message);
    } finally {
        isOpeningVaultPacks = false;
        btn.disabled = false;
        btn.textContent = origText;
    }
};

function updateOpenAnotherButton() {
    const btn = document.getElementById('btn-open-another-pack');
    if (!btn) return;
    if (remainingVaultPacks > 0) {
        btn.textContent = `Mở tiếp 1 Pack (Còn ${remainingVaultPacks})`;
        btn.classList.remove('hidden');
    } else {
        btn.classList.add('hidden');
    }
}

function updateVaultFlipProgress() {
    const items = Array.from(document.querySelectorAll('#vault-drawn-cards-grid .vault-flip-item'));
    const total = items.length;
    const flipped = items.filter(el => el.classList.contains('is-flipped')).length;

    const progressEl = document.getElementById('vault-flip-progress');
    if (progressEl && total > 0) {
        progressEl.textContent = `Đã lật: ${flipped} / ${total}`;
        progressEl.classList.remove('hidden');
    }

    const finishBtn = document.getElementById('btn-vault-finish');
    const flipAllBtn = document.getElementById('btn-flip-all-vault-cards');
    const filterRareBtn = document.getElementById('btn-vault-filter-rare');

    if (total > 0 && flipped >= total) {
        flipAllBtn?.classList.add('hidden');
        if (finishBtn) {
            finishBtn.className = 'pk-btn-primary rounded-xl px-5 py-2.5 text-xs font-extrabold cursor-pointer shadow-md';
        }
        if (filterRareBtn && total > 12) {
            filterRareBtn.classList.remove('hidden');
        }
        // Tóm tắt kết quả mở pack theo độ hiếm (ưu tiên độ hiếm cao trước)
        const rarityOrder = (name) => {
            const n = (name || '').toLowerCase();
            if (n.includes('special illustration')) return 8;
            if (/(hyper|secret|gold)/.test(n)) return 7;
            if (n.includes('illustration')) return 6;
            if (n.includes('ultra')) return 5;
            if (n.includes('double')) return 4;
            if (n.includes('rare') && !n.includes('uncommon')) return 3;
            if (n.includes('uncommon')) return 2;
            return 1;
        };
        const counts = new Map();
        activeDrawnCards.forEach(uc => {
            const rName = uc.card?.rarityName || 'Common';
            counts.set(rName, (counts.get(rName) || 0) + 1);
        });
        const summaryParts = Array.from(counts.entries())
            .sort((a, b) => rarityOrder(b[0]) - rarityOrder(a[0]))
            .map(([rName, cnt]) => `${cnt} ${rName}`);
        const subEl = document.getElementById('vault-open-subtitle');
        if (subEl && summaryParts.length > 0) {
            subEl.textContent = `Đã mở xong ${total}/${total} lá • ${summaryParts.join(', ')}`;
        }
    } else if (finishBtn) {
        finishBtn.className = 'rounded-xl bg-white/10 hover:bg-white/15 border border-white/20 text-slate-200 px-5 py-2.5 text-xs font-extrabold cursor-pointer transition';
    }
}

window.toggleVaultRareFilter = function () {
    vaultRareOnlyFilter = !vaultRareOnlyFilter;
    const btn = document.getElementById('btn-vault-filter-rare');
    const items = document.querySelectorAll('#vault-drawn-cards-grid .vault-flip-item');
    items.forEach(el => {
        const tier = el.getAttribute('data-tier') || 'common';
        const isRarePlus = tier !== 'common' && tier !== 'uncommon';
        el.style.display = (vaultRareOnlyFilter && !isRarePlus) ? 'none' : '';
    });
    if (btn) {
        if (vaultRareOnlyFilter) {
            btn.className = 'rounded-xl bg-amber-500 text-slate-950 px-3.5 py-2.5 text-xs font-extrabold transition cursor-pointer shadow-sm';
            btn.textContent = '★ Đang lọc thẻ hiếm (Rare+)';
        } else {
            btn.className = 'rounded-xl bg-amber-500/15 hover:bg-amber-500/25 border border-amber-400/40 px-3.5 py-2.5 text-xs font-bold text-amber-300 transition cursor-pointer';
            btn.textContent = '★ Chỉ xem thẻ hiếm (Rare+)';
        }
    }
};

function renderDrawnCardsResult(drawnCards, faceDown) {
    const esc = window.escapeHtml || (v => String(v ?? ''));
    const grid = document.getElementById('vault-drawn-cards-grid');
    document.getElementById('vault-stage-tear').classList.add('hidden');
    document.getElementById('vault-stage-cards').classList.remove('hidden');
    document.getElementById('btn-flip-all-vault-cards').classList.toggle('hidden', !faceDown);

    vaultRareOnlyFilter = false;
    const filterRareBtn = document.getElementById('btn-vault-filter-rare');
    if (filterRareBtn) {
        filterRareBtn.classList.add('hidden');
        filterRareBtn.className = 'hidden rounded-xl bg-amber-500/15 hover:bg-amber-500/25 border border-amber-400/40 px-3.5 py-2.5 text-xs font-bold text-amber-300 transition cursor-pointer';
        filterRareBtn.textContent = '★ Chỉ xem thẻ hiếm (Rare+)';
    }

    const list = drawnCards.slice();
    activeDrawnCards = list;
    const isMultiPack = list.length > 12;
    setGachaStageScrollMode(isMultiPack);
    document.getElementById('vault-scroll-fade')?.classList.toggle('hidden', !isMultiPack);

    grid.scrollTop = 0;
    grid.innerHTML = list.map((uc, idx) => {
        const c = uc.card || {};
        const tier = classifyRarityTier(c.rarityName, c.rarityIsHolographic);
        const color = resolveStandardRarityColor(c.rarityName, c.rarityColorHex);
        const isHighHit = tier === 'ex' || tier === 'hyper';
        const isRarePlus = tier !== 'common' && tier !== 'uncommon';
        const auraClass = `pk-aura-${tier}`;
        const holoClass = (isHighHit || c.rarityIsHolographic) ? 'pk-holo-card is-holo-active' : '';
        const dealDelay = Math.min(idx * (isMultiPack ? 25 : 75), 520);
        const glowStyle = isRarePlus ? `box-shadow: 0 0 18px -3px ${color}88;` : '';

        const tplKey = c.id != null ? String(c.id) : '';
        const isNewCard = Boolean(tplKey && !ownedTemplateIds.has(tplKey));
        if (isNewCard) ownedTemplateIds.add(tplKey);

        return `
            <div class="vault-flip-item pk-flip-scene pk-card-deal-in ${faceDown ? '' : 'is-flipped'}"
                 data-tier="${tier}"
                 data-card-index="${idx}"
                 style="animation-delay: ${dealDelay}ms"
                 onclick="window.handleVaultCardClick(this)">
                <div class="pk-flip-inner">
                    <!-- Mặt sau thẻ bài Pokémon ánh kim + Hào quang độ hiếm -->
                    <div class="pk-flip-face pk-flip-back ${auraClass} flex flex-col items-center justify-between p-3 text-center select-none overflow-hidden">
                        <span class="pk-num text-[9px] font-extrabold tracking-widest text-slate-300/80 uppercase">Lá #${idx + 1}</span>
                        <div class="relative flex h-14 w-14 items-center justify-center rounded-full border-2 border-amber-300/60 bg-indigo-950/80 shadow-inner">
                            <svg class="h-8 w-8 text-amber-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75">
                                <circle cx="12" cy="12" r="9"/>
                                <circle cx="12" cy="12" r="2.5"/>
                                <line x1="3" y1="12" x2="9.5" y2="12"/>
                                <line x1="14.5" y1="12" x2="21" y2="12"/>
                            </svg>
                        </div>
                        <span class="text-[10px] font-bold ${isHighHit ? 'text-amber-300' : 'text-slate-300'}">
                            ${isHighHit ? 'Ánh sáng hiếm!' : 'Bấm để lật'}
                        </span>
                    </div>

                    <!-- Mặt trước thẻ bài + Hiệu ứng Holo 3D -->
                    <div class="pk-flip-face pk-flip-front ${holoClass} relative overflow-hidden bg-slate-900 border-2 shadow-xl"
                         style="border-color: ${color}; ${glowStyle}">
                        ${isNewCard ? '<span class="absolute top-1.5 left-1.5 z-20 rounded-md bg-amber-400 text-slate-950 px-1.5 py-0.5 text-[9px] font-black tracking-wide shadow">MỚI</span>' : ''}
                        <img src="${esc(c.imageUrl)}" alt="${esc(c.name)}" class="w-full h-full object-cover"/>
                    </div>
                </div>
                <div class="vault-card-caption ${faceDown ? 'opacity-0' : 'opacity-100'} transition-opacity duration-300 mt-1.5 text-center space-y-1">
                    <div class="text-[11px] font-extrabold text-white truncate">${esc(c.name)}</div>
                    <span class="pk-rarity-badge text-[9px] px-2 py-0.5 max-w-full truncate"
                          style="border-color: ${color}; color: ${color}">${esc(c.rarityName)}</span>
                </div>
            </div>
        `;
    }).join('');

    // Kích hoạt Holo 3D Tilt khi di chuột lên các thẻ hiếm
    grid.querySelectorAll('.pk-holo-card.is-holo-active').forEach(cardEl => {
        cardEl.addEventListener('mousemove', (e) => {
            const rect = cardEl.getBoundingClientRect();
            const px = (e.clientX - rect.left) / rect.width;
            const py = (e.clientY - rect.top) / rect.height;
            cardEl.style.setProperty('--mx', `${(px * 100).toFixed(1)}%`);
            cardEl.style.setProperty('--my', `${(py * 100).toFixed(1)}%`);
        });
    });

    updateVaultFlipProgress();
}

window.handleVaultCardClick = function (sceneEl) {
    if (!sceneEl) return;
    if (sceneEl.classList.contains('is-flipped')) {
        const idx = parseInt(sceneEl.getAttribute('data-card-index') || '-1', 10);
        if (idx >= 0) window.openVaultCardLightbox(idx);
        return;
    }
    window.flipSingleVaultCard(sceneEl);
};

window.flipSingleVaultCard = function (sceneEl) {
    if (!sceneEl || sceneEl.classList.contains('is-flipped') || sceneEl.classList.contains('is-anticipation')) return;
    const tier = sceneEl.getAttribute('data-tier') || 'common';
    const isHighHit = tier === 'ex' || tier === 'hyper';

    const finishFlip = () => {
        sceneEl.classList.remove('is-anticipation');
        sceneEl.classList.add('is-flipped');
        sceneEl.querySelector('.vault-card-caption')?.classList.replace('opacity-0', 'opacity-100');

        if (isHighHit) {
            const frontEl = sceneEl.querySelector('.pk-flip-front');
            frontEl?.classList.add('pk-holo-shimmer');
            triggerIsolatedShake(sceneEl);
            const now = Date.now();
            if (now - lastHitVfxAt > 260) {
                lastHitVfxAt = now;
                spawnGachaConfetti(tier);
            }
        }

        // Cập nhật tiến độ lật & trạng thái nút Hoàn tất
        updateVaultFlipProgress();
    };

    if (isHighHit) {
        // Rung nhẹ hồi hộp trước khi lật thẻ EX / Hyper Rare
        sceneEl.classList.add('is-anticipation');
        setTimeout(finishFlip, 420);
    } else {
        finishFlip();
    }
};

window.flipAllVaultCards = function () {
    const unflipped = Array.from(document.querySelectorAll('#vault-drawn-cards-grid .vault-flip-item:not(.is-flipped)'));
    document.getElementById('btn-flip-all-vault-cards')?.classList.add('hidden');
    // Nhịp lật dây chuyền mượt mà (75ms cho 30 thẻ mở nhanh 5 Pack, 160ms cho 6 thẻ mở 1 Pack)
    const stepDelay = unflipped.length > 12 ? 75 : 160;
    unflipped.forEach((el, idx) => {
        setTimeout(() => window.flipSingleVaultCard(el), idx * stepDelay);
    });
};

// ==================== CARD ZOOM / LIGHTBOX POPUP ====================
window.openVaultCardLightbox = function (cardIndex) {
    const uc = activeDrawnCards[cardIndex];
    if (!uc || !uc.card) return;
    const c = uc.card;
    const tier = classifyRarityTier(c.rarityName, c.rarityIsHolographic);
    const color = resolveStandardRarityColor(c.rarityName, c.rarityColorHex);
    const isHolo = tier === 'ex' || tier === 'hyper' || tier === 'rare' || Boolean(c.rarityIsHolographic);

    const frame = document.getElementById('vault-lightbox-card-frame');
    const img = document.getElementById('vault-lightbox-card-img');
    const nameEl = document.getElementById('vault-lightbox-card-name');
    const rarityEl = document.getElementById('vault-lightbox-card-rarity');
    const metaEl = document.getElementById('vault-lightbox-card-meta');
    const valueEl = document.getElementById('vault-lightbox-card-value');
    const modal = document.getElementById('modal-vault-card-lightbox');

    if (img) {
        img.src = c.imageUrl || '';
        img.alt = c.name || 'Pokémon Card';
    }
    if (frame) {
        frame.style.borderColor = color;
        frame.style.transform = '';
        frame.classList.toggle('pk-holo-card', isHolo);
        frame.classList.toggle('is-holo-active', isHolo);
    }
    if (nameEl) nameEl.textContent = c.name || 'Thẻ bài Pokémon';
    if (rarityEl) {
        rarityEl.textContent = c.rarityName || 'Common';
        rarityEl.style.backgroundColor = '';
        rarityEl.style.borderColor = color;
        rarityEl.style.color = color;
    }
    if (metaEl) {
        const parts = [];
        if (c.setName) parts.push(c.setName);
        if (c.formattedCardNumber) parts.push(`#${c.formattedCardNumber}`);
        metaEl.textContent = parts.join(' • ');
    }
    if (valueEl) {
        const refVal = Number(c.rarityBuybackPrice || 0);
        valueEl.textContent = window.formatVND ? window.formatVND(refVal) : `${refVal.toLocaleString('vi-VN')} Xu`;
    }

    modal?.classList.remove('hidden');
};

window.closeVaultCardLightbox = function () {
    const modal = document.getElementById('modal-vault-card-lightbox');
    const frame = document.getElementById('vault-lightbox-card-frame');
    if (frame) frame.style.transform = '';
    modal?.classList.add('hidden');
};

function setupVaultLightboxInteractions() {
    const frame = document.getElementById('vault-lightbox-card-frame');
    if (frame) {
        frame.addEventListener('mousemove', (e) => {
            const r = frame.getBoundingClientRect();
            const px = (e.clientX - r.left) / r.width;
            const py = (e.clientY - r.top) / r.height;
            const rotY = (px - 0.5) * 20;
            const rotX = (0.5 - py) * 16;
            frame.style.transform = `rotateX(${rotX.toFixed(1)}deg) rotateY(${rotY.toFixed(1)}deg)`;
            frame.style.setProperty('--mx', `${(px * 100).toFixed(1)}%`);
            frame.style.setProperty('--my', `${(py * 100).toFixed(1)}%`);
            frame.style.setProperty('--bgx', `${(px * 100).toFixed(1)}%`);
            frame.style.setProperty('--bgy', `${(py * 100).toFixed(1)}%`);
        });
        frame.addEventListener('mouseleave', () => {
            frame.style.transform = '';
        });
    }

    // Phím ESC chỉ đóng Lightbox xem thẻ (không đóng Modal mở Pack bên dưới)
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            const lb = document.getElementById('modal-vault-card-lightbox');
            if (lb && !lb.classList.contains('hidden')) {
                e.preventDefault();
                e.stopPropagation();
                window.closeVaultCardLightbox();
            }
        }
    });
}

window.closeVaultOpenModal = function (reloadIfOpened) {
    window.closeVaultCardLightbox();
    document.getElementById('modal-vault-open-pack')?.classList.add('hidden');
    lockBodyScrollForGacha(false);
    resetTearPackVisual();
    if (reloadIfOpened && hasOpenedInSession) {
        window.location.href = '/inventory?tab=sealed';
    }
};

// ==================== BÁN NHANH THẺ BÀI (CHỌN SỐ LƯỢNG x2+) & DỌN KHO HÀNG LOẠT ====================
let pendingBuybackCardId = null;
let pendingBuybackTemplateId = null;
let pendingBuybackUnitPrice = 0;
let pendingBuybackMaxQty = 1;
let pendingBuybackQty = 1;

function renderSingleSellModalQty() {
    const qtyDisplay = document.getElementById('buyback-modal-qty-display');
    const priceEl = document.getElementById('buyback-modal-price');
    const qtySection = document.getElementById('buyback-modal-qty-section');
    if (qtySection) {
        qtySection.classList.toggle('hidden', pendingBuybackMaxQty < 2);
    }
    if (qtyDisplay) {
        qtyDisplay.textContent = `${pendingBuybackQty} / ${pendingBuybackMaxQty}`;
    }
    if (priceEl) {
        priceEl.textContent = '+' + window.formatVND(pendingBuybackUnitPrice * pendingBuybackQty);
    }
}

window.openSellToShopModal = function (btn) {
    if (!btn) return;
    const cardItem = btn.closest('.my-card-item');
    pendingBuybackCardId = parseInt(btn.dataset.userCardId, 10);
    pendingBuybackTemplateId = parseInt(btn.dataset.templateId || cardItem?.dataset.templateId || '0', 10);
    pendingBuybackUnitPrice = parseFloat(btn.dataset.price || '0') || 0;
    pendingBuybackMaxQty = Math.max(1, parseInt(cardItem?.getAttribute('data-sellable-count') || '1', 10) || 1);
    pendingBuybackQty = 1;

    const nameEl = document.getElementById('buyback-modal-card-name');
    const rarityEl = document.getElementById('buyback-modal-card-rarity');
    if (nameEl) nameEl.textContent = btn.dataset.cardName || 'Thẻ bài';
    if (rarityEl) rarityEl.textContent = btn.dataset.rarity || 'Common';
    renderSingleSellModalQty();

    const modal = document.getElementById('modal-sell-to-shop');
    modal?.classList.remove('hidden');
    modal?.classList.add('flex');
};

window.adjustSingleSellQty = function (delta) {
    pendingBuybackQty = Math.min(pendingBuybackMaxQty, Math.max(1, pendingBuybackQty + delta));
    renderSingleSellModalQty();
};

window.setSingleSellQuickQty = function (mode) {
    if (mode === 'keep1') {
        pendingBuybackQty = Math.max(1, pendingBuybackMaxQty - 1);
    } else if (mode === 'all') {
        pendingBuybackQty = pendingBuybackMaxQty;
    }
    renderSingleSellModalQty();
};

window.closeSellToShopModal = function () {
    pendingBuybackCardId = null;
    pendingBuybackTemplateId = null;
    const modal = document.getElementById('modal-sell-to-shop');
    modal?.classList.add('hidden');
    modal?.classList.remove('flex');
};

// Cập nhật DOM trực tiếp tại chỗ sau khi Bán nhanh / Bán hàng loạt thành công (KHÔNG reload / KHÔNG chuyển trang)
function applyBuybackDomUpdate(data) {
    if (!data) return;

    // 1. Cập nhật số dư ví trên Header
    const newBal = data.newBalance !== undefined ? data.newBalance : data.newWalletBalance;
    if (newBal !== undefined && newBal !== null) {
        const balEl = document.getElementById('nav-wallet-balance');
        if (balEl) balEl.textContent = window.formatVND(newBal);
    }

    // 2. Cập nhật các bộ đếm tổng số thẻ trong kho
    if (typeof data.totalCards === 'number') {
        const tabLabel = document.getElementById('tab-inventory-count-label');
        const statTotal = document.getElementById('stat-total-cards-count');
        const headingTotal = document.getElementById('heading-owned-cards-count');
        if (tabLabel) tabLabel.textContent = `Thẻ Bài (${data.totalCards})`;
        if (statTotal) statTotal.textContent = `${data.totalCards} thẻ`;
        if (headingTotal) headingTotal.textContent = `Thẻ Bài Đang Sở Hữu (${data.totalCards})`;
    }

    // 3. Cập nhật từng thẻ vừa bán trong lưới DOM (giữ nguyên vị trí cuộn & bộ lọc)
    const items = Array.isArray(data.updatedItems) ? data.updatedItems : [];
    let removedAny = false;
    items.forEach(item => {
        const tplId = item.cardTemplateId;
        const remQty = Number(item.remainingQuantity ?? 0);
        const remSellable = Number(item.remainingSellableQuantity ?? remQty);
        const nextUnlockedId = item.nextUnlockedUserCardId;
        const cardEl = document.querySelector(`#my-cards-grid .my-card-item[data-template-id="${tplId}"]`);
        if (!cardEl) return;

        if (remQty <= 0) {
            removedAny = true;
            cardEl.dataset.removing = 'true';
            ownedTemplateIds.delete(String(tplId));
            // Hiệu ứng mờ dần ~200ms rồi xóa hẳn khỏi DOM
            cardEl.style.transition = 'opacity 200ms ease, transform 200ms ease';
            cardEl.style.opacity = '0';
            cardEl.style.transform = 'scale(0.92)';
            cardEl.style.pointerEvents = 'none';
            setTimeout(() => {
                cardEl.remove();
                updateInventoryQuickStats();
                window.filterAndSortInventoryCards(invCardsPage);
            }, 210);
        } else {
            cardEl.setAttribute('data-total-count', String(remQty));
            cardEl.setAttribute('data-sellable-count', String(remSellable));

            const badge = cardEl.querySelector('.card-copy-badge');
            if (badge) {
                badge.textContent = `x${remQty}`;
                badge.classList.toggle('hidden', remQty <= 1);
            }

            const quickSellBtn = cardEl.querySelector('.btn-quick-sell-card');
            const marketLink = cardEl.querySelector('.btn-list-market-card');

            if (remSellable > 0 && nextUnlockedId) {
                if (quickSellBtn) quickSellBtn.dataset.userCardId = String(nextUnlockedId);
                if (marketLink) marketLink.setAttribute('href', `/market?sellUserCardId=${nextUnlockedId}`);
            } else if (remSellable <= 0) {
                // Toàn bộ bản sao còn lại đang bị khóa trên Chợ / Thám hiểm
                cardEl.setAttribute('data-is-locked', 'true');
                const actionsWrap = cardEl.querySelector('.normal-card-actions');
                if (actionsWrap) {
                    actionsWrap.innerHTML = `
                        <a href="/market"
                           class="pk-surface-muted w-full rounded-xl py-2.5 px-3 text-xs font-bold text-amber-600 dark:text-amber-400 flex items-center justify-center text-center hover:opacity-85 transition">
                            <span>Đang khóa • Chợ / Thám hiểm</span>
                        </a>`;
                }
            }
        }
    });
    updateInventoryQuickStats();
    if (!removedAny) {
        window.filterAndSortInventoryCards(invCardsPage);
    }
}

window.confirmSellToShop = async function () {
    if (!pendingBuybackCardId) return;
    const confirmBtn = document.getElementById('btn-confirm-sell-to-shop');
    if (confirmBtn) confirmBtn.disabled = true;

    try {
        let res;
        if (pendingBuybackQty > 1 && pendingBuybackTemplateId > 0) {
            res = await fetch('/api/inventory/sell-to-shop/bulk', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    items: [{ cardTemplateId: pendingBuybackTemplateId, quantity: pendingBuybackQty }]
                })
            });
        } else {
            res = await fetch(`/api/inventory/${pendingBuybackCardId}/sell-to-shop`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' }
            });
        }
        const data = await parseJsonSafe(res);
        if (!res.ok) {
            window.showToast('error', 'Không thể bán nhanh thẻ', data.message || 'Vui lòng thử lại.');
            return;
        }
        window.closeSellToShopModal();
        applyBuybackDomUpdate(data);
        const earnedText = data.earnedAmount !== undefined
            ? `Đã bán thành công +${window.formatVND(data.earnedAmount)}`
            : (data.message || 'Đã bán thẻ thành công!');
        window.showToast('success', 'Bán nhanh thành công!', earnedText);
    } catch (err) {
        window.showToast('error', 'Lỗi kết nối', err.message);
    } finally {
        if (confirmBtn) confirmBtn.disabled = false;
    }
};

// -------------------- CHẾ ĐỘ DỌN KHO / BÁN NHANH HÀNG LOẠT (BULK QUICK SELL) --------------------
let isBulkSellMode = false;
const bulkSelectedMap = new Map(); // templateId -> { qty, maxQty, unitPrice }

window.handleInventoryCardLinkClick = function (event, linkEl) {
    if (!isBulkSellMode) return true;
    event.preventDefault();
    const cardItem = linkEl.closest('.my-card-item');
    window.toggleBulkCardSelection(cardItem);
    return false;
};

function syncBulkCardVisual(cardItem) {
    if (!cardItem) return;
    const tplId = parseInt(cardItem.getAttribute('data-template-id') || '0', 10);
    const sellable = parseInt(cardItem.getAttribute('data-sellable-count') || '0', 10) || 0;
    const unitPrice = parseFloat(cardItem.getAttribute('data-buyback-price') || '0') || 0;
    const selected = bulkSelectedMap.get(tplId);
    const qty = selected ? selected.qty : 0;
    const isSel = qty > 0;

    cardItem.classList.toggle('pk-bulk-selected', isBulkSellMode && isSel);
    cardItem.classList.toggle('opacity-50', isBulkSellMode && sellable <= 0);

    const badge = cardItem.querySelector('.bulk-select-badge');
    const checkIcon = cardItem.querySelector('.bulk-check-icon');
    const normalActions = cardItem.querySelector('.normal-card-actions');
    const bulkControls = cardItem.querySelector('.bulk-card-controls');
    const stepperRow = cardItem.querySelector('.bulk-stepper-row');
    const qtyLabel = cardItem.querySelector('.bulk-qty-label');
    const minusBtn = cardItem.querySelector('.bulk-minus-btn');
    const plusBtn = cardItem.querySelector('.bulk-plus-btn');
    const unitPriceHint = cardItem.querySelector('.bulk-unit-price-hint');
    const singleToggleBtn = cardItem.querySelector('.bulk-single-toggle-btn');

    if (badge) {
        badge.classList.toggle('hidden', !isBulkSellMode || sellable <= 0);
        badge.classList.toggle('bg-amber-400', isSel);
        badge.classList.toggle('border-amber-300', isSel);
        badge.classList.toggle('bg-slate-950/75', !isSel);
    }
    if (checkIcon) {
        checkIcon.classList.toggle('hidden', !isSel);
    }
    if (normalActions) {
        normalActions.classList.toggle('hidden', isBulkSellMode);
    }
    if (bulkControls) {
        bulkControls.classList.toggle('hidden', !isBulkSellMode);
    }
    if (isBulkSellMode) {
        if (sellable <= 0) {
            stepperRow?.classList.add('hidden');
            stepperRow?.classList.remove('flex');
            unitPriceHint?.classList.add('hidden');
            if (singleToggleBtn) {
                singleToggleBtn.classList.remove('hidden');
                singleToggleBtn.disabled = true;
                singleToggleBtn.textContent = 'Đang khóa';
            }
        } else if (sellable >= 2) {
            stepperRow?.classList.remove('hidden');
            stepperRow?.classList.add('flex');
            if (qtyLabel) qtyLabel.textContent = `${qty} / ${sellable}`;
            if (minusBtn) minusBtn.disabled = qty <= 0;
            if (plusBtn) plusBtn.disabled = qty >= sellable;
            if (unitPriceHint) {
                unitPriceHint.classList.remove('hidden');
                unitPriceHint.textContent = qty > 0
                    ? `Đang chọn: +${window.formatVND(qty * unitPrice)}`
                    : `+${window.formatVND(unitPrice)} / thẻ`;
            }
            if (singleToggleBtn) {
                singleToggleBtn.classList.add('hidden');
            }
        } else {
            stepperRow?.classList.add('hidden');
            stepperRow?.classList.remove('flex');
            unitPriceHint?.classList.add('hidden');
            if (singleToggleBtn) {
                singleToggleBtn.classList.remove('hidden');
                singleToggleBtn.disabled = false;
                singleToggleBtn.textContent = isSel
                    ? `Đã chọn • +${window.formatVND(unitPrice)}`
                    : `Chọn bán • +${window.formatVND(unitPrice)}`;
                singleToggleBtn.classList.toggle('bg-amber-500/20', isSel);
                singleToggleBtn.classList.toggle('text-amber-600', isSel);
                singleToggleBtn.classList.toggle('dark:text-amber-300', isSel);
            }
        }
    }
}

function updateBulkSummaryBar() {
    let totalCards = 0;
    let totalCoins = 0;
    let rareCardsCount = 0;
    bulkSelectedMap.forEach((item, tplId) => {
        if (item.qty > 0) {
            totalCards += item.qty;
            totalCoins += item.qty * item.unitPrice;
            const cardEl = document.querySelector(`#my-cards-grid .my-card-item[data-template-id="${tplId}"]`);
            if (cardEl && cardEl.getAttribute('data-is-high-rarity') === 'true') {
                rareCardsCount += item.qty;
            }
        }
    });

    const bar = document.getElementById('bulk-sell-sticky-bar');
    const badgeEl = document.getElementById('bulk-summary-count-badge');
    const countEl = document.getElementById('bulk-summary-count');
    const totalEl = document.getElementById('bulk-summary-total');
    const rareWarnEl = document.getElementById('bulk-rare-warning');
    const confirmBtn = document.getElementById('btn-confirm-bulk-sell');

    const showBar = isBulkSellMode && activeInventoryTab === 'inventory';
    bar?.classList.toggle('hidden', !showBar);
    if (badgeEl) badgeEl.textContent = String(totalCards);
    if (countEl) countEl.textContent = String(totalCards);
    if (totalEl) totalEl.textContent = '+' + window.formatVND(totalCoins);
    if (rareWarnEl) {
        rareWarnEl.classList.toggle('hidden', rareCardsCount <= 0);
        if (rareCardsCount > 0) {
            rareWarnEl.textContent = `⚠ Có ${rareCardsCount} thẻ hiếm`;
        }
    }
    if (confirmBtn) confirmBtn.disabled = totalCards <= 0;
}

window.toggleBulkSellMode = function (forceState) {
    isBulkSellMode = typeof forceState === 'boolean' ? forceState : !isBulkSellMode;
    if (!isBulkSellMode) {
        bulkSelectedMap.clear();
    }

    const toolbar = document.getElementById('bulk-sell-toolbar');
    const toggleBtn = document.getElementById('btn-toggle-bulk-sell');
    toolbar?.classList.toggle('hidden', !isBulkSellMode);
    if (toggleBtn) {
        toggleBtn.classList.toggle('bg-amber-500', isBulkSellMode);
        toggleBtn.classList.toggle('text-slate-950', isBulkSellMode);
        toggleBtn.classList.toggle('dark:text-slate-950', isBulkSellMode);
    }

    document.querySelectorAll('#my-cards-grid .my-card-item').forEach(syncBulkCardVisual);
    updateBulkSummaryBar();
};

window.toggleBulkCardSelection = function (cardItem) {
    if (!isBulkSellMode || !cardItem) return;
    const tplId = parseInt(cardItem.getAttribute('data-template-id') || '0', 10);
    const sellable = parseInt(cardItem.getAttribute('data-sellable-count') || '0', 10) || 0;
    const unitPrice = parseFloat(cardItem.getAttribute('data-buyback-price') || '0') || 0;
    if (tplId <= 0 || sellable <= 0) return;

    if (bulkSelectedMap.has(tplId) && bulkSelectedMap.get(tplId).qty > 0) {
        bulkSelectedMap.delete(tplId);
    } else {
        // Mặc định khi tick chọn thẻ x2+: chọn giữ lại 1 (hoặc 1 nếu chỉ có 1)
        const defaultQty = sellable >= 2 ? (sellable - 1) : 1;
        bulkSelectedMap.set(tplId, { qty: defaultQty, maxQty: sellable, unitPrice });
    }
    syncBulkCardVisual(cardItem);
    updateBulkSummaryBar();
};

window.adjustBulkCardQty = function (cardItem, delta) {
    if (!isBulkSellMode || !cardItem) return;
    const tplId = parseInt(cardItem.getAttribute('data-template-id') || '0', 10);
    const sellable = parseInt(cardItem.getAttribute('data-sellable-count') || '0', 10) || 0;
    const unitPrice = parseFloat(cardItem.getAttribute('data-buyback-price') || '0') || 0;
    if (tplId <= 0 || sellable <= 0) return;

    const current = bulkSelectedMap.get(tplId)?.qty || 0;
    const next = Math.min(sellable, Math.max(0, current + delta));
    if (next <= 0) {
        bulkSelectedMap.delete(tplId);
    } else {
        bulkSelectedMap.set(tplId, { qty: next, maxQty: sellable, unitPrice });
    }
    syncBulkCardVisual(cardItem);
    updateBulkSummaryBar();
};

window.bulkSelectDuplicates = function () {
    if (!isBulkSellMode) return;
    bulkSelectedMap.clear();
    document.querySelectorAll('#my-cards-grid .my-card-item').forEach(cardItem => {
        if (cardItem.dataset.filterMatch === 'false' || cardItem.dataset.removing === 'true') {
            syncBulkCardVisual(cardItem);
            return;
        }
        const tplId = parseInt(cardItem.getAttribute('data-template-id') || '0', 10);
        const total = parseInt(cardItem.getAttribute('data-total-count') || '1', 10) || 1;
        const sellable = parseInt(cardItem.getAttribute('data-sellable-count') || '0', 10) || 0;
        const unitPrice = parseFloat(cardItem.getAttribute('data-buyback-price') || '0') || 0;
        // Giữ lại đúng 1 bản sao cho mỗi loại thẻ trùng (x2 trở lên)
        const excess = Math.min(sellable, Math.max(0, total - 1));
        if (tplId > 0 && excess > 0) {
            bulkSelectedMap.set(tplId, { qty: excess, maxQty: sellable, unitPrice });
        }
        syncBulkCardVisual(cardItem);
    });
    updateBulkSummaryBar();
};

window.bulkSelectCommonUncommon = function () {
    if (!isBulkSellMode) return;
    bulkSelectedMap.clear();
    document.querySelectorAll('#my-cards-grid .my-card-item').forEach(cardItem => {
        if (cardItem.dataset.filterMatch === 'false' || cardItem.dataset.removing === 'true') {
            syncBulkCardVisual(cardItem);
            return;
        }
        const tplId = parseInt(cardItem.getAttribute('data-template-id') || '0', 10);
        const sellable = parseInt(cardItem.getAttribute('data-sellable-count') || '0', 10) || 0;
        const unitPrice = parseFloat(cardItem.getAttribute('data-buyback-price') || '0') || 0;
        const rarity = (cardItem.getAttribute('data-rarity-name') || '').trim().toLowerCase();
        const isCommonOrUncommon = rarity === 'common' || rarity === 'uncommon' || rarity.includes('phổ biến');
        if (tplId > 0 && sellable > 0 && isCommonOrUncommon) {
            bulkSelectedMap.set(tplId, { qty: sellable, maxQty: sellable, unitPrice });
        }
        syncBulkCardVisual(cardItem);
    });
    updateBulkSummaryBar();
};

window.bulkClearSelection = function () {
    bulkSelectedMap.clear();
    document.querySelectorAll('#my-cards-grid .my-card-item').forEach(syncBulkCardVisual);
    updateBulkSummaryBar();
};

window.confirmBulkSellToShop = async function () {
    const items = [];
    let totalCards = 0;
    let totalCoins = 0;
    bulkSelectedMap.forEach((val, tplId) => {
        if (val.qty > 0) {
            items.push({ cardTemplateId: tplId, quantity: val.qty });
            totalCards += val.qty;
            totalCoins += val.qty * val.unitPrice;
        }
    });
    if (items.length === 0) return;
    if (!confirm(`Xác nhận bán nhanh ${totalCards} thẻ bài để nhận ngay +${window.formatVND(totalCoins)}? Hành động này không thể hoàn tác.`)) {
        return;
    }

    const btn = document.getElementById('btn-confirm-bulk-sell');
    const origText = btn?.textContent || 'Xác nhận bán nhanh';
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Đang xử lý...';
    }
    try {
        const res = await fetch('/api/inventory/sell-to-shop/bulk', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ items })
        });
        const data = await parseJsonSafe(res);
        if (!res.ok) {
            window.showToast('error', 'Không thể bán hàng loạt', data.message || 'Vui lòng thử lại.');
            return;
        }
        applyBuybackDomUpdate(data);
        window.toggleBulkSellMode(false);
        const earnedText = data.earnedAmount !== undefined
            ? `Đã bán thành công +${window.formatVND(data.earnedAmount)}`
            : (data.message || 'Dọn kho thành công!');
        window.showToast('success', 'Dọn kho thành công!', earnedText);
    } catch (err) {
        window.showToast('error', 'Lỗi kết nối', err.message);
    } finally {
        if (btn) {
            btn.textContent = origText;
            btn.disabled = false;
        }
    }
};

document.addEventListener('DOMContentLoaded', () => {
    setupTearPackInteractions();
    setupVaultLightboxInteractions();
    window.filterAndSortInventoryCards();
    const urlTab = new URLSearchParams(window.location.search).get('tab');
    window.switchInventoryTab(urlTab || INITIAL_TAB, false);
    if (window.bindHoloTiltEffects) window.bindHoloTiltEffects();
});
