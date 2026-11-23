/* ==================== MULTI-SET STORE (DAILY GLOBAL STOCK -> VAULT) ==================== */
(function () {
    'use strict';

    function formatVnd(val) {
        return Math.round(Number(val || 0)).toLocaleString('vi-VN') + ' Xu';
    }

    window.toggleDropRatePopover = function (e) {
        if (e) e.stopPropagation();
        document.getElementById('drop-rate-popover')?.classList.toggle('hidden');
    };

    window.closeDropRatePopover = function () {
        document.getElementById('drop-rate-popover')?.classList.add('hidden');
    };

    document.addEventListener('click', (e) => {
        const pop = document.getElementById('drop-rate-popover');
        const btn = document.getElementById('btn-toggle-drop-rates');
        if (pop && !pop.classList.contains('hidden') && !pop.contains(e.target) && !btn?.contains(e.target)) {
            pop.classList.add('hidden');
        }
    });

    window.stepStoreQty = function (kind, btn, delta) {
        const setId = btn.getAttribute('data-set-id');
        const input = document.getElementById(`${kind}-qty-${setId}`);
        if (!input) return;
        const maxCap = Math.min(kind === 'pack' ? 20 : 5, parseInt(input.getAttribute('data-max') || '20', 10) || 1);
        const cur = parseInt(input.value || '1', 10) || 1;
        const next = Math.max(1, Math.min(maxCap, cur + delta));
        input.value = String(next);
        window.syncStoreTotal(kind, input);
    };

    window.syncStoreTotal = function (kind, input) {
        if (!input) return;
        const setId = input.id.replace(`${kind}-qty-`, '');
        const maxCap = Math.min(kind === 'pack' ? 20 : 5, parseInt(input.getAttribute('data-max') || '20', 10) || 1);
        let qty = parseInt(input.value || '1', 10);
        if (!Number.isFinite(qty) || qty < 1) qty = 1;
        if (qty > maxCap) qty = maxCap;
        input.value = String(qty);

        const minusBtn = document.getElementById(`${kind}-minus-${setId}`);
        const plusBtn = document.getElementById(`${kind}-plus-${setId}`);
        if (minusBtn) minusBtn.disabled = qty <= 1;
        if (plusBtn) plusBtn.disabled = qty >= maxCap;

        const unitPrice = parseFloat(input.getAttribute('data-unit-price') || '0');
        const total = unitPrice * qty;
        const labelEl = document.getElementById(`${kind}-btn-total-${setId}`);
        if (labelEl) {
            const prefix = kind === 'pack' ? `Mua ${qty > 1 ? qty + ' ' : ''}Pack` : `Mua ${qty > 1 ? qty + ' ' : ''}Box`;
            labelEl.textContent = `${prefix} • ${formatVnd(total)}`;
        }
    };

    function applyStockBarColor(barEl, textEl, pct) {
        if (barEl) {
            barEl.classList.remove(
                'from-emerald-500', 'to-teal-400',
                'from-amber-400', 'to-amber-500',
                'from-orange-500', 'to-rose-600'
            );
            if (pct >= 50) {
                barEl.classList.add('from-emerald-500', 'to-teal-400');
            } else if (pct >= 20) {
                barEl.classList.add('from-amber-400', 'to-amber-500');
            } else {
                barEl.classList.add('from-orange-500', 'to-rose-600');
            }
        }
        if (textEl) {
            textEl.classList.toggle('text-emerald-600', pct >= 20);
            textEl.classList.toggle('dark:text-emerald-400', pct >= 20);
            textEl.classList.toggle('text-rose-600', pct < 20);
            textEl.classList.toggle('dark:text-rose-400', pct < 20);
        }
    }

    window.buyStoreItem = async function (kind, btn) {
        const setId = parseInt(btn.getAttribute('data-set-id'), 10);
        const limit = parseInt(btn.getAttribute('data-limit') || '1', 10);
        const input = document.getElementById(`${kind}-qty-${setId}`);
        const quantity = Math.max(1, parseInt(input?.value || '1', 10) || 1);

        const endpoint = kind === 'pack' ? '/api/v1/store/buy-pack' : '/api/v1/store/buy-box';
        const labelEl = document.getElementById(`${kind}-btn-total-${setId}`);
        const origLabelText = labelEl ? labelEl.textContent : btn.textContent;
        btn.disabled = true;
        btn.classList.add('opacity-70', 'cursor-wait');
        if (labelEl) {
            labelEl.textContent = 'Đang xử lý...';
        } else {
            btn.textContent = 'Đang xử lý...';
        }

        try {
            const res = await fetch(endpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ setId, quantity })
            });
            const data = await res.json();
            if (!res.ok) {
                window.showToast('error', 'Không thể mua vật phẩm', data.message || 'Vui lòng kiểm tra số dư hoặc hạn mức hôm nay.');
                return;
            }

            // Cập nhật số dư ví trên Navbar
            if (data.newWalletBalance != null) {
                const navBal = document.getElementById('nav-wallet-balance');
                if (navBal) navBal.textContent = formatVnd(data.newWalletBalance);
            }

            // Cập nhật hạn mức còn lại ngay lập tức không cần reload
            const remaining = kind === 'pack' ? Number(data.packsRemainingToday) : Number(data.boxesRemainingToday);
            const stockText = document.getElementById(`${kind}-stock-text-${setId}`);
            const stockBar = document.getElementById(`${kind}-stock-bar-${setId}`);
            const pct = limit > 0 ? Math.max(0, Math.min(100, (remaining * 100) / limit)) : 0;
            if (stockText) {
                stockText.textContent = `${remaining} / ${limit}`;
            }
            if (stockBar) {
                stockBar.style.width = `${pct}%`;
            }
            applyStockBarColor(stockBar, stockText, pct);

            if (remaining <= 0) {
                document.getElementById(`${kind}-buy-controls-${setId}`)?.classList.add('hidden');
                document.getElementById(`${kind}-soldout-${setId}`)?.classList.remove('hidden');
            } else if (input) {
                input.setAttribute('data-max', String(remaining));
                input.value = '1';
                window.syncStoreTotal(kind, input);
            }

            // Hiển thị banner thành công + nút tắt "Đi tới Kho mở ngay"
            const banner = document.getElementById('store-purchase-success-banner');
            const titleEl = document.getElementById('store-purchase-success-title');
            const descEl = document.getElementById('store-purchase-success-desc');
            if (titleEl) titleEl.textContent = data.message || 'Mua thành công!';
            if (descEl) {
                descEl.textContent = `Đã thanh toán ${formatVnd(data.totalCost)} • Vật phẩm đang chờ trong Kho đồ (Tab Pack & Box).`;
            }
            if (banner) {
                banner.classList.remove('hidden');
                banner.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }

            window.showToast('success', 'Đã lưu vào Kho đồ!', data.message);
        } catch (err) {
            window.showToast('error', 'Lỗi kết nối', err.message || 'Không thể kết nối máy chủ.');
        } finally {
            btn.disabled = false;
            btn.classList.remove('opacity-70', 'cursor-wait');
            if (labelEl && labelEl.textContent === 'Đang xử lý...') {
                labelEl.textContent = origLabelText;
            } else if (!labelEl) {
                btn.textContent = origLabelText;
            }
        }
    };
    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('input[id^="pack-qty-"]').forEach((inp) => window.syncStoreTotal('pack', inp));
        document.querySelectorAll('input[id^="box-qty-"]').forEach((inp) => window.syncStoreTotal('box', inp));
    });
})();
