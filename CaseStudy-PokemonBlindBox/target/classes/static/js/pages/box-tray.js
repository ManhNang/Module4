/* ==================== SHOP / BOX-TRAY PAGE ==================== */
const activeBoxPanel = document.getElementById('active-box-panel');
const ACTIVE_BOX_ID = activeBoxPanel ? parseInt(activeBoxPanel.getAttribute('data-box-id'), 10) : null;
const TOTAL_PACKS = activeBoxPanel ? parseInt(activeBoxPanel.getAttribute('data-total-packs') || '30', 10) : 30;

// 1. Khóa nút "Mua Nguyên Hộp" ngay trên DOM
window.disableBuyWholeBoxButton = function () {
    const btn = document.getElementById('btn-buy-whole-box');
    const hint = document.getElementById('whole-box-hint');
    if (!btn) return;

    btn.disabled = true;
    btn.className = 'rounded-2xl px-6 py-4 text-sm font-extrabold transition flex items-center justify-center gap-2.5 pk-surface-muted pk-text-secondary cursor-not-allowed opacity-75';
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

// 2. Cập nhật số gói còn lại + thanh Progress Bar
window.updateRemainingPacksDisplay = function (remaining) {
    const remainingEl = document.getElementById('box-remaining-packs');
    const barEl = document.getElementById('box-remaining-progress');
    if (remainingEl && remaining !== undefined) {
        remainingEl.textContent = remaining;
    }
    if (barEl && TOTAL_PACKS > 0 && remaining !== undefined) {
        const pct = Math.max(0, Math.min(100, (remaining * 100.0) / TOTAL_PACKS));
        barEl.style.width = pct + '%';
    }
};

// 3. Làm chìm gói bài đã rút xuống khay (chỉ hiển thị đúng 1 nút "Đã rút khỏi khay" ở đáy gói)
window.markPackSlotAsSoldOnUI = function (positionInBox, boughtByCurrentUser, packId) {
    const slotEl = document.getElementById(`pack-slot-${positionInBox}`);
    const actionsEl = document.getElementById(`pack-actions-${positionInBox}`);
    if (!slotEl) return;

    slotEl.setAttribute('data-status', 'SOLD');

    if (actionsEl) {
        const resolvedPackId = packId || slotEl.getAttribute('data-pack-id');
        if (boughtByCurrentUser && resolvedPackId) {
            slotEl.classList.remove('is-withdrawn');
            slotEl.classList.add('is-available');
            actionsEl.innerHTML = `
                <button type="button"
                        onclick="window.openPurchasedPack(${Number(resolvedPackId)})"
                        class="pk-pack-open-btn rounded-xl bg-amber-500 hover:bg-amber-400 font-extrabold text-slate-950 shadow-md cursor-pointer transition">
                    <span>Xé gói ngay</span>
                </button>
            `;
        } else {
            slotEl.classList.remove('is-available');
            slotEl.classList.add('is-withdrawn');
            actionsEl.innerHTML = `
                <button type="button" disabled
                        class="pk-pack-open-btn pk-surface-muted rounded-xl font-bold pk-text-secondary cursor-not-allowed gap-1.5">
                    <svg class="h-3.5 w-3.5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M20 6 9 17l-5-5"/>
                    </svg>
                    <span>Đã rút khỏi khay</span>
                </button>
            `;
        }
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

// 5. Mua 1 gói lẻ (Chống double-click P3-02, User lấy từ SecurityContext trên Backend)
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

// 6. Mua nguyên hộp (User lấy từ SecurityContext trên Backend)
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

// 7. Mở gói bài Gacha: Hiển thị Modal xé vỏ gói bài 2D -> Lật từng lá bài (kèm 3D Tilt Holographic)
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

        // Cập nhật ngay ô gói bài trên khay sang trạng thái "Đã rút khỏi khay" mà không cần reload trang
        if (data.positionInBox) {
            window.markPackSlotAsSoldOnUI(data.positionInBox, false, null);
        }
        await window.refreshNavbarWallet();

        const modal = document.getElementById('gacha-result-modal');
        const title = document.getElementById('gacha-modal-title');
        const tearStage = document.getElementById('gacha-tear-stage');
        const tearVisual = document.getElementById('tear-pack-visual');
        const cardsStage = document.getElementById('gacha-cards-stage');
        const flipAllBtn = document.getElementById('btn-flip-all-cards');
        const container = document.getElementById('gacha-cards-container');

        const posStr = String(data.positionInBox || 1).padStart(2, '0');
        document.getElementById('tear-pack-pos-label').textContent = `#${posStr}`;
        document.getElementById('tear-pack-count-label').textContent = `${data.snapshotCardsPerPack} THẺ`;
        title.textContent = `Gói bài #${posStr} • Bấm để xé vỏ gói bài 2D!`;

        tearVisual.classList.remove('is-torn');
        tearStage.classList.remove('hidden');
        cardsStage.classList.add('hidden');
        flipAllBtn.classList.add('hidden');

        container.innerHTML = data.drawnCards.map((uc, idx) => {
            const c = uc.card;
            const esc = window.escapeHtml || (v => String(v ?? ''));
            const safeName = esc(c.name);
            const safeNum = esc(c.formattedCardNumber);
            const safeRarity = esc(c.rarityName);
            const safeSpecies = esc(c.speciesName);
            const safeImg = esc(c.imageUrl);
            const safeColor = /^#[0-9a-fA-F]{3,8}$/.test(c.rarityColorHex || '') ? c.rarityColorHex : '#10B981';
            const isHolo = window.isHolographicCard ? window.isHolographicCard(c) : Boolean(c.rarityIsHolographic);
            const holoClass = isHolo ? 'is-holo-active' : '';
            const holoBadge = isHolo
                ? `<span class="rounded-full bg-amber-500/20 px-2 py-0.5 text-[9px] font-extrabold text-amber-600 dark:text-amber-300 border border-amber-500/30">HOLO 3D</span>`
                : '';

            return `
                <div class="pk-flip-scene" id="flip-card-${idx}" onclick="this.classList.add('is-flipped')">
                    <div class="pk-flip-inner">
                        <!-- Mặt lưng lá bài (Úp) -->
                        <div class="pk-flip-face pk-flip-back p-4 flex flex-col items-center justify-between text-center">
                            <span class="pk-num text-[11px] font-bold text-amber-300/80">LÁ BÀI #${idx + 1}</span>
                            <div class="my-auto flex flex-col items-center space-y-2">
                                <div class="flex h-14 w-14 items-center justify-center rounded-full bg-rose-600/90 text-white shadow-lg">
                                    <svg class="h-8 w-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75">
                                        <circle cx="12" cy="12" r="9"></circle>
                                        <circle cx="12" cy="12" r="2.5"></circle>
                                        <line x1="3" y1="12" x2="9.5" y2="12"></line>
                                        <line x1="14.5" y1="12" x2="21" y2="12"></line>
                                    </svg>
                                </div>
                                <span class="text-xs font-extrabold text-white">Bấm để lật mở</span>
                            </div>
                            <span class="text-[10px] font-semibold text-slate-400">DELTA REIGN</span>
                        </div>

                        <!-- Mặt trước lá bài 2D (Có 3D Tilt & Holographic cho độ hiếm đặc biệt) -->
                        <div class="pk-flip-face pk-flip-front">
                            <div class="pk-holo-card ${holoClass} pk-surface-elevated h-full w-full rounded-2xl border-2 p-3 flex flex-col justify-between shadow-xl"
                                 style="border-color: ${safeColor}">
                                <div class="relative z-10 flex items-center justify-between text-[11px]">
                                    <span class="pk-num font-extrabold pk-text-primary">${safeNum}</span>
                                    <span class="rounded-full px-2 py-0.5 font-bold"
                                          style="background:${safeColor}22; color:${safeColor}">${safeRarity}</span>
                                </div>

                                <div class="relative z-10 my-1.5 flex-1 rounded-xl pk-surface-muted flex items-center justify-center overflow-hidden p-2">
                                    <img src="${safeImg}" alt="${safeName}"
                                         onerror="this.src='https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/384.png'"
                                         class="max-h-full max-w-full object-contain drop-shadow"/>
                                </div>

                                <div class="relative z-10 space-y-1">
                                    <div class="flex items-center justify-between gap-1">
                                        <a href="/cards/${Number(c.id)}" onclick="event.stopPropagation()"
                                           class="text-xs font-extrabold pk-text-primary hover:underline truncate">${safeName}</a>
                                        ${holoBadge}
                                    </div>
                                    <div class="flex items-center justify-between text-[10px] pk-text-secondary">
                                        <span class="truncate">${safeSpecies}</span>
                                        <a href="/cards/${Number(c.id)}" onclick="event.stopPropagation()"
                                           class="font-bold text-rose-600 dark:text-amber-400 hover:underline shrink-0">Chi tiết →</a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

        modal.classList.remove('hidden');
        modal.classList.add('flex');
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

    if (!tearVisual || tearVisual.classList.contains('is-torn')) return;
    tearVisual.classList.add('is-torn');

    setTimeout(() => {
        tearStage.classList.add('hidden');
        cardsStage.classList.remove('hidden');
        flipAllBtn.classList.remove('hidden');
        if (title) title.textContent = 'Bấm vào từng lá bài để lật mở!';
    }, 520);
};

window.flipAllDrawnCards = function () {
    document.querySelectorAll('#gacha-cards-container .pk-flip-scene').forEach((el, idx) => {
        setTimeout(() => el.classList.add('is-flipped'), idx * 110);
    });
};

window.closeGachaModal = function () {
    const modal = document.getElementById('gacha-result-modal');
    modal.classList.add('hidden');
    modal.classList.remove('flex');
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

document.addEventListener('DOMContentLoaded', connectBoxTrayWebSocket);
