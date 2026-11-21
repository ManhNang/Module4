/* ==================== MARKETPLACE (CHỢ ĐẤU GIÁ TRỰC TIẾP: THẺ BÀI, PACK & BOX) ==================== */

let currentSellItemType = 'CARD';     // 'CARD' | 'PACK' | 'BOX'
let activeMarketStatusTab = 'ACTIVE'; // 'ACTIVE' | 'MINE' | 'ENDED'
let selectedItemBuybackPrice = 0;
let selectedItemPriceLabel = 'giá bán nhanh';

function isAuctionCardEnded(card, nowMs) {
    const status = (card.getAttribute('data-status') || 'ACTIVE').toUpperCase();
    if (status !== 'ACTIVE') return true;
    const endStr = card.getAttribute('data-end-time');
    if (!endStr) return false;
    return new Date(endStr).getTime() <= nowMs;
}

function applyEndedStyleToCard(card, ended) {
    card.dataset.computedEnded = ended ? 'true' : 'false';
    const badge = card.querySelector('.auction-status-badge');
    const dot = card.querySelector('.auction-live-dot');
    const statusText = card.querySelector('.auction-status-text');
    const ctaBtn = card.querySelector('.auction-cta-btn');

    if (ended) {
        card.classList.add('opacity-75', 'saturate-[0.8]', 'border-slate-500/25');
        card.classList.remove('pk-rarity-glow-card', 'border-slate-300/40', 'dark:border-slate-700/60');
        if (badge) {
            badge.className = 'auction-status-badge inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[10px] font-extrabold bg-slate-500/20 text-slate-400 border border-slate-500/30';
        }
        dot?.classList.add('hidden');
        if (statusText) statusText.textContent = 'ĐÃ KẾT THÚC';
        if (ctaBtn) {
            ctaBtn.className = 'auction-cta-btn w-full rounded-xl py-2.5 text-center text-xs font-extrabold transition pk-surface-muted border border-slate-500/25 pk-text-secondary hover:pk-text-primary hover:border-slate-400/40';
            ctaBtn.textContent = 'Xem kết quả →';
        }
    } else {
        card.classList.remove('opacity-75', 'saturate-[0.8]', 'border-slate-500/25');
        card.classList.add('pk-rarity-glow-card', 'border-slate-300/40', 'dark:border-slate-700/60');
        if (badge) {
            badge.className = 'auction-status-badge inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[10px] font-extrabold bg-rose-600 text-white';
        }
        dot?.classList.remove('hidden');
        if (statusText) statusText.textContent = 'LIVE';
        if (ctaBtn) {
            ctaBtn.className = 'auction-cta-btn w-full rounded-xl py-2.5 text-center text-xs font-extrabold transition bg-amber-500/12 border border-amber-500/45 text-amber-600 dark:text-amber-400 hover:bg-amber-500 hover:text-slate-950 hover:border-amber-500';
            ctaBtn.textContent = 'Vào Phòng Đấu Giá →';
        }
    }
}

window.switchMarketStatusTab = function (tab) {
    activeMarketStatusTab = ['ACTIVE', 'MINE', 'ENDED'].includes(tab) ? tab : 'ACTIVE';
    ['ACTIVE', 'MINE', 'ENDED'].forEach(t => {
        const btn = document.getElementById(`market-status-tab-${t}`);
        if (!btn) return;
        const isActive = t === activeMarketStatusTab;
        btn.className = isActive
            ? 'market-status-tab rounded-lg px-3 py-1.5 text-xs font-extrabold cursor-pointer transition bg-amber-500 text-slate-950 shadow-sm whitespace-nowrap'
            : 'market-status-tab rounded-lg px-3 py-1.5 text-xs font-bold pk-text-secondary hover:pk-text-primary cursor-pointer transition whitespace-nowrap';
    });
    window.applyMarketFilters();
};

// Bộ lọc ngang hợp nhất (Tab trạng thái + Search + Loại vật phẩm + Bộ thẻ + Độ hiếm + Sắp xếp)
window.applyMarketFilters = function () {
    const grid = document.getElementById('market-auctions-grid');
    if (!grid) return;
    const nowMs = Date.now();
    const myUserId = String(window.CURRENT_USER_ID || '');
    const searchVal = (document.getElementById('market-filter-search')?.value || '').trim().toLowerCase();
    const itemTypeVal = document.getElementById('market-filter-item-type')?.value || 'ALL';
    const setVal = (document.getElementById('market-filter-set')?.value || '').trim().toLowerCase();
    const rarityVal = (document.getElementById('market-filter-rarity')?.value || '').trim().toLowerCase();
    const sortVal = document.getElementById('market-filter-sort')?.value || 'ending-asc';

    const cards = Array.from(grid.querySelectorAll('.market-item-card'));

    // Sắp xếp: luôn đẩy phiên đã kết thúc xuống dưới phiên đang diễn ra
    cards.sort((a, b) => {
        const aEnded = isAuctionCardEnded(a, nowMs) ? 1 : 0;
        const bEnded = isAuctionCardEnded(b, nowMs) ? 1 : 0;
        if (aEnded !== bEnded) return aEnded - bEnded;

        const priceA = parseFloat(a.getAttribute('data-price') || '0') || 0;
        const priceB = parseFloat(b.getAttribute('data-price') || '0') || 0;
        const endA = new Date(a.getAttribute('data-end-time') || 0).getTime();
        const endB = new Date(b.getAttribute('data-end-time') || 0).getTime();
        const startA = new Date(a.getAttribute('data-start-time') || 0).getTime();
        const startB = new Date(b.getAttribute('data-start-time') || 0).getTime();

        if (sortVal === 'price-asc') return priceA - priceB;
        if (sortVal === 'price-desc') return priceB - priceA;
        if (sortVal === 'newest') return startB - startA;
        // ending-asc (với phiên đã kết thúc, mới kết thúc gần nhất lên trước)
        return aEnded ? (endB - endA) : (endA - endB);
    });

    let countActive = 0;
    let countMine = 0;
    let countEnded = 0;
    let visibleCount = 0;

    cards.forEach(card => {
        const ended = isAuctionCardEnded(card, nowMs);
        applyEndedStyleToCard(card, ended);

        const cName = (card.getAttribute('data-card-name') || '').toLowerCase();
        const cNum = (card.getAttribute('data-card-num') || '').toLowerCase();
        const cSet = (card.getAttribute('data-set-name') || '').toLowerCase();
        const cRarity = (card.getAttribute('data-rarity-name') || '').toLowerCase();
        const cItemType = card.getAttribute('data-item-type') || 'CARD';
        const sellerId = String(card.getAttribute('data-seller-id') || '');
        const isMine = myUserId && sellerId === myUserId;

        let matchFilter = true;
        if (itemTypeVal !== 'ALL' && cItemType !== itemTypeVal) matchFilter = false;
        if (searchVal && !cName.includes(searchVal) && !cNum.includes(searchVal) && !cSet.includes(searchVal)) matchFilter = false;
        if (setVal && cSet !== setVal) matchFilter = false;
        if (rarityVal && cRarity !== rarityVal) matchFilter = false;

        if (matchFilter) {
            if (!ended) countActive++;
            if (isMine) countMine++;
            if (ended) countEnded++;
        }

        let matchTab = true;
        if (activeMarketStatusTab === 'ACTIVE' && ended) matchTab = false;
        if (activeMarketStatusTab === 'ENDED' && !ended) matchTab = false;
        if (activeMarketStatusTab === 'MINE' && !isMine) matchTab = false;

        const visible = matchFilter && matchTab;
        card.style.display = visible ? '' : 'none';
        if (visible) visibleCount++;
        grid.appendChild(card);
    });

    const setTxt = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = String(val); };
    setTxt('market-tab-count-ACTIVE', countActive);
    setTxt('market-tab-count-MINE', countMine);
    setTxt('market-tab-count-ENDED', countEnded);
    setTxt('market-visible-count', visibleCount);

    const emptyEl = document.getElementById('market-empty-state');
    if (emptyEl) {
        emptyEl.classList.toggle('hidden', visibleCount > 0);
        if (visibleCount === 0) {
            emptyEl.textContent = activeMarketStatusTab === 'MINE'
                ? 'Bạn chưa mở phiên đấu giá nào phù hợp với bộ lọc hiện tại.'
                : (activeMarketStatusTab === 'ENDED'
                    ? 'Chưa có phiên đấu giá nào đã kết thúc.'
                    : 'Chưa có phiên đấu giá trực tiếp nào đang diễn ra.');
        }
    }
};

window.resetMarketFilters = function () {
    const setEl = (id, v) => { const el = document.getElementById(id); if (el) el.value = v; };
    setEl('market-filter-search', '');
    setEl('market-filter-item-type', 'ALL');
    setEl('market-filter-set', '');
    setEl('market-filter-rarity', '');
    setEl('market-filter-sort', 'ending-asc');
    window.applyMarketFilters();
};

function tickAllAuctionTimers() {
    const now = Date.now();
    let statusFlipped = false;

    document.querySelectorAll('.market-item-card').forEach(card => {
        const timerEl = card.querySelector('.auction-card-timer');
        if (!timerEl) return;
        const endStr = timerEl.getAttribute('data-end-time');
        const wasEnded = card.dataset.computedEnded === 'true';
        const serverEnded = (card.getAttribute('data-status') || 'ACTIVE').toUpperCase() !== 'ACTIVE';
        const diffSec = endStr ? Math.floor((new Date(endStr).getTime() - now) / 1000) : 0;

        if (serverEnded || diffSec <= 0) {
            timerEl.textContent = 'Đã kết thúc';
            timerEl.className = 'auction-card-timer pk-num tabular-nums text-sm sm:text-base font-extrabold pk-text-secondary';
            if (!wasEnded) statusFlipped = true;
            return;
        }

        const h = String(Math.floor(diffSec / 3600)).padStart(2, '0');
        const m = String(Math.floor((diffSec % 3600) / 60)).padStart(2, '0');
        const s = String(diffSec % 60).padStart(2, '0');
        timerEl.textContent = `${h}:${m}:${s}`;

        // Chỉ chuyển đỏ + nhấp nháy nhẹ khi còn dưới 10 phút (< 600 giây)
        if (diffSec < 600) {
            timerEl.className = 'auction-card-timer pk-num tabular-nums text-sm sm:text-base font-extrabold text-rose-600 dark:text-rose-400 animate-pulse';
        } else {
            timerEl.className = 'auction-card-timer pk-num tabular-nums text-sm sm:text-base font-extrabold pk-text-primary';
        }
    });

    if (statusFlipped) {
        window.applyMarketFilters();
    }
}

// ============================================================================
// MODAL MỞ PHIÊN ĐẤU GIÁ: THẺ BÀI / PACK / BOX (CHỈ ĐẤU GIÁ LIVE)
// ============================================================================
window.openSellModal = function (preselectCardId, preselectItemType, preselectSealedId) {
    const modal = document.getElementById('modal-market-sell');
    if (!modal) return;
    window.switchSellItemType(preselectItemType || 'CARD');

    const searchInput = document.getElementById('sell-picker-search');
    if (searchInput) searchInput.value = '';
    window.filterSellPickerCards();

    if (preselectSealedId && (preselectItemType === 'PACK' || preselectItemType === 'BOX')) {
        const target = document.querySelector(`.sell-picker-sealed[data-sealed-id="${preselectSealedId}"]`);
        if (target) {
            window.selectSellCard(target);
            target.scrollIntoView({ block: 'nearest' });
        }
    } else if (preselectCardId) {
        const targetCard = document.querySelector(`.sell-picker-card[data-user-card-id="${preselectCardId}"]`);
        if (targetCard) {
            window.selectSellCard(targetCard);
            targetCard.scrollIntoView({ block: 'nearest' });
        }
    }

    window.updatePricePreview('auction-start-price', 'preview-auction-start');
    window.updatePricePreview('auction-min-increment', 'preview-auction-increment', true);
    window.syncDurationButtons();
    syncSubmitAuctionButtonState();

    modal.classList.remove('hidden');
};

window.closeSellModal = function () {
    document.getElementById('modal-market-sell')?.classList.add('hidden');
};

function syncSubmitAuctionButtonState() {
    const btn = document.getElementById('btn-submit-auction');
    if (!btn) return;
    const cardId = (document.getElementById('sell-selected-card-id')?.value || '').trim();
    const sealedId = (document.getElementById('sell-selected-sealed-id')?.value || '').trim();
    const hasSelection = currentSellItemType === 'CARD' ? Boolean(cardId) : Boolean(sealedId);
    btn.disabled = !hasSelection;
}

function resetSellPreviewPanel() {
    document.getElementById('sell-preview-empty')?.classList.remove('hidden');
    document.getElementById('sell-preview-content')?.classList.add('hidden');
}

function updateSellPreviewPanel(cardEl) {
    const emptyEl = document.getElementById('sell-preview-empty');
    const contentEl = document.getElementById('sell-preview-content');
    if (!cardEl || !contentEl) {
        resetSellPreviewPanel();
        return;
    }

    const itemType = cardEl.getAttribute('data-item-type') || 'CARD';
    const cardName = cardEl.getAttribute('data-card-name') || 'Vật phẩm';
    const rarityName = cardEl.getAttribute('data-rarity-name') || '';
    const rarityColor = cardEl.getAttribute('data-rarity-color') || '#94A3B8';
    const isBox = cardEl.getAttribute('data-is-box') === 'true' || itemType === 'BOX';
    const refPrice = Math.max(0, Math.round(parseFloat(cardEl.getAttribute('data-buyback-price') || '0') || 0));
    const ownedQty = Math.max(1, parseInt(cardEl.getAttribute('data-qty') || '1', 10) || 1);

    const nameEl = document.getElementById('sell-preview-name');
    const rarityEl = document.getElementById('sell-preview-rarity');
    const refPriceEl = document.getElementById('sell-preview-ref-price');
    const ownedQtyEl = document.getElementById('sell-preview-owned-qty');

    if (nameEl) nameEl.textContent = cardName;
    if (rarityEl) {
        rarityEl.textContent = itemType === 'CARD' ? (rarityName || 'Common') : (isBox ? 'Box Nguyên Seal' : 'Pack Lẻ');
        rarityEl.style.borderColor = rarityColor;
        rarityEl.style.color = rarityColor;
    }
    if (refPriceEl) {
        const labelTxt = itemType === 'CARD' ? 'Bán nhanh' : 'Giá shop';
        refPriceEl.innerHTML = `${labelTxt}: <span class="font-extrabold text-amber-600 dark:text-amber-400">${refPrice.toLocaleString('vi-VN')} Xu</span>`;
    }
    if (ownedQtyEl) {
        ownedQtyEl.innerHTML = `Đang có: <span class="font-bold pk-text-primary">x${ownedQty}</span>`;
    }

    emptyEl?.classList.add('hidden');
    contentEl.classList.remove('hidden');
}

window.switchSellItemType = function (itemType) {
    currentSellItemType = ['CARD', 'PACK', 'BOX'].includes(itemType) ? itemType : 'CARD';
    ['CARD', 'PACK', 'BOX'].forEach(t => {
        const btn = document.getElementById(`sell-item-tab-${t}`);
        if (!btn) return;
        const active = t === currentSellItemType;
        btn.className = active
            ? 'rounded-lg px-3 py-1.5 text-xs font-extrabold cursor-pointer transition pk-btn-primary'
            : 'rounded-lg px-3 py-1.5 text-xs font-bold pk-text-secondary hover:pk-text-primary cursor-pointer transition';
    });

    // Reset selection, preview & start price when switching item type
    selectedItemBuybackPrice = 0;
    selectedItemPriceLabel = currentSellItemType === 'CARD' ? 'giá bán nhanh' : 'giá shop';
    const cardInput = document.getElementById('sell-selected-card-id');
    const sealedInput = document.getElementById('sell-selected-sealed-id');
    if (cardInput) cardInput.value = '';
    if (sealedInput) sealedInput.value = '';
    const startInput = document.getElementById('auction-start-price');
    if (startInput) startInput.value = '';
    document.getElementById('auction-start-warning')?.classList.add('hidden');
    resetSellPreviewPanel();

    document.querySelectorAll('.sell-picker-card, .sell-picker-sealed').forEach(el => {
        el.classList.remove('border-amber-500', 'ring-2', 'ring-amber-500/35');
        el.classList.add('border-transparent');
        el.querySelector('.sell-picker-check')?.classList.add('hidden');
    });

    window.filterSellPickerCards();
    window.updatePricePreview('auction-start-price', 'preview-auction-start');
    syncSubmitAuctionButtonState();
};

window.selectSellCard = function (cardEl) {
    if (!cardEl) return;
    const itemType = cardEl.getAttribute('data-item-type') || 'CARD';
    const cardId = cardEl.getAttribute('data-user-card-id') || '';
    const sealedId = cardEl.getAttribute('data-sealed-id') || '';
    const buybackRaw = parseFloat(cardEl.getAttribute('data-buyback-price') || '0') || 0;
    selectedItemBuybackPrice = Math.max(0, Math.round(buybackRaw));
    selectedItemPriceLabel = itemType === 'CARD' ? 'giá bán nhanh' : 'giá shop';

    document.querySelectorAll('.sell-picker-card, .sell-picker-sealed').forEach(el => {
        el.classList.remove('border-amber-500', 'ring-2', 'ring-amber-500/35');
        el.classList.add('border-transparent');
        el.querySelector('.sell-picker-check')?.classList.add('hidden');
    });

    cardEl.classList.remove('border-transparent');
    cardEl.classList.add('border-amber-500', 'ring-2', 'ring-amber-500/35');
    cardEl.querySelector('.sell-picker-check')?.classList.remove('hidden');

    document.getElementById('sell-selected-card-id').value = itemType === 'CARD' ? cardId : '';
    document.getElementById('sell-selected-sealed-id').value = itemType !== 'CARD' ? sealedId : '';

    // Cập nhật cột Xem trước (Preview) bên phải
    updateSellPreviewPanel(cardEl);

    // Chống bán hớ: tự động điền Giá khởi điểm mặc định bằng Giá bán nhanh (Thẻ) / Giá shop (Pack & Box)
    const startInput = document.getElementById('auction-start-price');
    if (startInput && selectedItemBuybackPrice > 0) {
        startInput.value = String(selectedItemBuybackPrice);
    }
    window.updatePricePreview('auction-start-price', 'preview-auction-start');
    syncSubmitAuctionButtonState();
};

window.filterSellPickerCards = function () {
    const query = (document.getElementById('sell-picker-search')?.value || '').trim().toLowerCase();
    const allItems = document.querySelectorAll('.sell-picker-card, .sell-picker-sealed');
    let visibleCount = 0;

    allItems.forEach(item => {
        const type = item.getAttribute('data-item-type') || 'CARD';
        const name = (item.getAttribute('data-card-name') || '').toLowerCase();
        const rarity = (item.getAttribute('data-rarity-name') || '').toLowerCase();
        const matchType = type === currentSellItemType;
        const matchQuery = !query || name.includes(query) || rarity.includes(query);
        const show = matchType && matchQuery;
        item.classList.toggle('hidden', !show);
        if (show) visibleCount++;
    });

    document.getElementById('sell-picker-empty')?.classList.toggle('hidden', visibleCount > 0);
};

function initSellPickerCards() {
    const grid = document.getElementById('sell-picker-grid');
    if (!grid) return;
    const preselectId = document.getElementById('modal-market-sell')?.getAttribute('data-preselect-card-id') || '';
    const rawCards = Array.from(grid.querySelectorAll('.sell-picker-card'));
    const grouped = new Map();

    rawCards.forEach(card => {
        const tplId = card.getAttribute('data-template-id') || card.getAttribute('data-user-card-id');
        const ucId = card.getAttribute('data-user-card-id');
        if (!grouped.has(tplId)) {
            grouped.set(tplId, { el: card, count: 1 });
        } else {
            const entry = grouped.get(tplId);
            entry.count += 1;
            if (preselectId && ucId === preselectId) {
                entry.el.remove();
                entry.el = card;
            } else {
                card.remove();
            }
        }
    });

    const uniqueEntries = Array.from(grouped.values());
    uniqueEntries.sort((a, b) => {
        const priceB = parseFloat(b.el.getAttribute('data-buyback-price') || '0');
        const priceA = parseFloat(a.el.getAttribute('data-buyback-price') || '0');
        if (priceB !== priceA) return priceB - priceA;
        return (a.el.getAttribute('data-card-name') || '').localeCompare(b.el.getAttribute('data-card-name') || '');
    });

    uniqueEntries.forEach(({ el, count }) => {
        el.setAttribute('data-qty', String(count));
        const qtyBadge = el.querySelector('.sell-picker-qty');
        if (qtyBadge) {
            if (count > 1) {
                qtyBadge.textContent = `x${count}`;
                qtyBadge.classList.remove('hidden');
            } else {
                qtyBadge.classList.add('hidden');
            }
        }
        grid.appendChild(el);
    });
}

window.syncDurationButtons = function () {
    const val = String(parseInt(document.getElementById('auction-duration')?.value || '0', 10));
    document.querySelectorAll('.auction-dur-btn').forEach(btn => {
        const isActive = btn.getAttribute('data-dur') === val;
        btn.classList.toggle('bg-amber-500', isActive);
        btn.classList.toggle('text-slate-950', isActive);
        btn.classList.toggle('font-extrabold', isActive);
        btn.classList.toggle('pk-surface-muted', !isActive);
        btn.classList.toggle('pk-text-secondary', !isActive);
    });
};

window.setAuctionDuration = function (minutes) {
    const input = document.getElementById('auction-duration');
    if (input) input.value = String(minutes);
    window.syncDurationButtons();
};

window.updatePricePreview = function (inputId, previewId, isStep) {
    const rawStr = (document.getElementById(inputId)?.value || '').trim();
    const val = parseFloat(rawStr || '0');
    const previewEl = document.getElementById(previewId);
    if (previewEl) {
        if (!rawStr && inputId === 'auction-start-price') {
            previewEl.textContent = 'Chưa chọn vật phẩm';
            previewEl.className = 'pk-num text-[11px] font-bold pk-text-secondary px-1';
        } else if (!Number.isFinite(val) || val <= 0) {
            previewEl.textContent = '0 Xu';
            if (inputId === 'auction-start-price') {
                previewEl.className = 'pk-num text-[11px] font-bold pk-text-secondary px-1';
            }
        } else {
            const formatted = Math.round(val).toLocaleString('vi-VN') + ' Xu';
            previewEl.textContent = isStep ? `+${formatted} / bước` : formatted;
            if (inputId === 'auction-start-price') {
                previewEl.className = 'pk-num text-[11px] font-bold text-amber-600 dark:text-amber-400 px-1';
            }
        }
    }

    if (inputId === 'auction-start-price') {
        const warnEl = document.getElementById('auction-start-warning');
        if (warnEl) {
            if (selectedItemBuybackPrice > 0 && Number.isFinite(val) && val > 0 && val < selectedItemBuybackPrice) {
                warnEl.textContent = `⚠ Thấp hơn ${selectedItemPriceLabel} (${selectedItemBuybackPrice.toLocaleString('vi-VN')} Xu)`;
                warnEl.classList.remove('hidden');
            } else {
                warnEl.classList.add('hidden');
            }
        }
    }
};

document.getElementById('unified-sell-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const userCardId = parseInt(document.getElementById('sell-selected-card-id')?.value || '0', 10) || null;
    const sealedItemId = parseInt(document.getElementById('sell-selected-sealed-id')?.value || '0', 10) || null;

    if (currentSellItemType === 'CARD' && !userCardId) {
        window.showToast('error', 'Chưa chọn thẻ bài', 'Vui lòng bấm chọn 1 thẻ bài trước khi mở phiên đấu giá.');
        return;
    }
    if (currentSellItemType !== 'CARD' && !sealedItemId) {
        window.showToast('error', 'Chưa chọn Pack / Box', 'Vui lòng bấm chọn 1 Pack hoặc Box trong Kho đồ.');
        return;
    }

    const startingPrice = parseFloat(document.getElementById('auction-start-price')?.value || '0');
    const minBidIncrement = parseFloat(document.getElementById('auction-min-increment')?.value || '0');
    const durationMinutes = parseInt(document.getElementById('auction-duration')?.value || '0', 10);
    if (!Number.isFinite(startingPrice) || startingPrice < 1 || !Number.isFinite(minBidIncrement) || minBidIncrement < 1 || durationMinutes < 5) {
        window.showToast('error', 'Thông số đấu giá không hợp lệ', 'Giá khởi điểm & bước giá tối thiểu 1 Xu, thời lượng tối thiểu 5 phút.');
        return;
    }
    const btn = document.getElementById('btn-submit-auction');
    if (btn) btn.disabled = true;
    try {
        const res = await fetch('/api/v1/market/auctions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                itemType: currentSellItemType,
                userCardId,
                sealedItemId,
                startingPrice,
                minBidIncrement,
                durationMinutes
            })
        });
        const data = await res.json();
        if (!res.ok) {
            window.showToast('error', 'Không thể tạo phiên đấu giá', data.message);
            return;
        }
        window.closeSellModal();
        window.showToast('success', 'Đã mở Phiên Đấu Giá!', 'Đang chuyển vào phòng đấu giá...');
        setTimeout(() => { window.location.href = `/market/auctions/${data.id}`; }, 750);
    } finally {
        syncSubmitAuctionButtonState();
    }
});

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        const modal = document.getElementById('modal-market-sell');
        if (modal && !modal.classList.contains('hidden')) {
            window.closeSellModal();
        }
    }
});

document.addEventListener('DOMContentLoaded', () => {
    initSellPickerCards();
    window.syncDurationButtons();
    window.applyMarketFilters();
    tickAllAuctionTimers();
    setInterval(tickAllAuctionTimers, 1000);

    const modal = document.getElementById('modal-market-sell');
    const preselectCardId = modal?.getAttribute('data-preselect-card-id');
    const preselectSealedId = modal?.getAttribute('data-preselect-sealed-id');
    const preselectItemType = modal?.getAttribute('data-preselect-item-type');
    if (preselectSealedId && (preselectItemType === 'PACK' || preselectItemType === 'BOX')) {
        window.openSellModal(null, preselectItemType, preselectSealedId);
    } else if (preselectCardId) {
        window.openSellModal(preselectCardId, 'CARD', null);
    }
});
