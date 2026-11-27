/* ==================== WALLET & VNPAY TOP-UP PAGE ==================== */
const WALLET_TX_PER_PAGE = 10;
let walletTxPage = 1;
let walletTxFilter = 'ALL'; // 'ALL' | 'CREDIT' | 'DEBIT'

window.updateDepositXuPreview = function () {
    const raw = parseFloat(document.getElementById('deposit-amount')?.value || '0') || 0;
    const xu = Math.max(0, Math.floor(raw / 10));
    const el = document.getElementById('deposit-xu-preview');
    if (el) {
        el.textContent = `Nhận: +${xu.toLocaleString('vi-VN')} Xu`;
    }
};

window.handleDepositAmountInput = function (displayInput) {
    const digits = String(displayInput.value || '').replace(/\D/g, '');
    const numericVal = digits ? parseInt(digits, 10) : 0;
    const hiddenEl = document.getElementById('deposit-amount');
    if (hiddenEl) hiddenEl.value = String(numericVal);
    displayInput.value = numericVal > 0 ? numericVal.toLocaleString('vi-VN') : '';
    window.updateDepositXuPreview();
};

window.selectPresetAmount = function (amount, clickedBtn) {
    const hiddenEl = document.getElementById('deposit-amount');
    const displayEl = document.getElementById('deposit-amount-display');
    if (hiddenEl) hiddenEl.value = String(amount);
    if (displayEl) displayEl.value = Number(amount).toLocaleString('vi-VN');
    window.updateDepositXuPreview();

    document.querySelectorAll('.wallet-preset-btn').forEach(btn => {
        btn.classList.remove('border-amber-500', 'ring-2', 'ring-amber-500/25');
        btn.classList.add('border-transparent');
        const vndEl = btn.querySelector('.wallet-preset-vnd');
        if (vndEl) {
            vndEl.classList.remove('text-amber-600', 'dark:text-amber-400');
            vndEl.classList.add('pk-text-primary');
        }
    });
    if (clickedBtn) {
        clickedBtn.classList.remove('border-transparent');
        clickedBtn.classList.add('border-amber-500', 'ring-2', 'ring-amber-500/25');
        const vndEl = clickedBtn.querySelector('.wallet-preset-vnd');
        if (vndEl) {
            vndEl.classList.remove('pk-text-primary');
            vndEl.classList.add('text-amber-600', 'dark:text-amber-400');
        }
    }
};

window.setWalletTxFilter = function (mode) {
    walletTxFilter = ['ALL', 'CREDIT', 'DEBIT'].includes(mode) ? mode : 'ALL';
    ['ALL', 'CREDIT', 'DEBIT'].forEach(m => {
        const btn = document.getElementById(`wallet-filter-${m}`);
        if (!btn) return;
        const active = m === walletTxFilter;
        btn.className = active
            ? 'wallet-tx-filter-btn pk-btn-primary rounded-lg px-3 py-1 text-xs font-extrabold cursor-pointer transition'
            : 'wallet-tx-filter-btn rounded-lg px-3 py-1 text-xs font-bold pk-text-secondary hover:pk-text-primary cursor-pointer transition';
    });
    renderWalletTxPagination(1);
};

function renderWalletTxPagination(targetPage) {
    const allRows = Array.from(document.querySelectorAll('#wallet-tx-tbody .wallet-tx-row'));
    const bar = document.getElementById('wallet-tx-pagination');
    const infoEl = document.getElementById('wallet-tx-page-info');
    const controlsEl = document.getElementById('wallet-tx-page-controls');

    const matchedRows = allRows.filter(row => {
        const isCredit = row.getAttribute('data-is-credit') === 'true';
        if (walletTxFilter === 'CREDIT') return isCredit;
        if (walletTxFilter === 'DEBIT') return !isCredit;
        return true;
    });

    const total = matchedRows.length;
    const totalPages = Math.max(1, Math.ceil(total / WALLET_TX_PER_PAGE));
    if (typeof targetPage === 'number' && !Number.isNaN(targetPage)) {
        walletTxPage = targetPage;
    }
    walletTxPage = Math.max(1, Math.min(walletTxPage, totalPages));

    const startIdx = (walletTxPage - 1) * WALLET_TX_PER_PAGE;
    const endIdx = Math.min(startIdx + WALLET_TX_PER_PAGE, total);
    const visibleSet = new Set(matchedRows.slice(startIdx, endIdx));

    allRows.forEach(r => {
        r.style.display = visibleSet.has(r) ? '' : 'none';
    });

    if (bar) {
        bar.classList.toggle('hidden', total === 0 || totalPages <= 1);
    }
    if (infoEl && total > 0) {
        infoEl.textContent = `Hiển thị ${startIdx + 1}–${endIdx} trên tổng ${total} giao dịch`;
    }
    if (controlsEl && totalPages > 1) {
        const buttons = [];
        const prevDis = walletTxPage <= 1 ? 'disabled' : '';
        const nextDis = walletTxPage >= totalPages ? 'disabled' : '';
        buttons.push(
            `<button type="button" onclick="window.goToWalletTxPage(${walletTxPage - 1})" ${prevDis}
                     class="pk-surface-muted rounded-xl px-2.5 py-1 text-xs font-bold pk-text-primary disabled:opacity-40 cursor-pointer disabled:cursor-not-allowed">‹</button>`
        );
        const pages = new Set([1, totalPages]);
        for (let p = Math.max(1, walletTxPage - 2); p <= Math.min(totalPages, walletTxPage + 2); p++) {
            pages.add(p);
        }
        const sorted = Array.from(pages).sort((a, b) => a - b);
        let prevP = 0;
        sorted.forEach(p => {
            if (prevP && p - prevP > 1) {
                buttons.push('<span class="px-1 text-xs pk-text-secondary select-none">…</span>');
            }
            const cls = p === walletTxPage
                ? 'pk-btn-primary rounded-xl px-2.5 py-1 text-xs font-extrabold cursor-pointer pk-num'
                : 'pk-surface-muted rounded-xl px-2.5 py-1 text-xs font-bold pk-text-primary hover:opacity-80 cursor-pointer pk-num';
            buttons.push(`<button type="button" onclick="window.goToWalletTxPage(${p})" class="${cls}">${p}</button>`);
            prevP = p;
        });
        buttons.push(
            `<button type="button" onclick="window.goToWalletTxPage(${walletTxPage + 1})" ${nextDis}
                     class="pk-surface-muted rounded-xl px-2.5 py-1 text-xs font-bold pk-text-primary disabled:opacity-40 cursor-pointer disabled:cursor-not-allowed">›</button>`
        );
        controlsEl.innerHTML = buttons.join('');
    }
}

window.goToWalletTxPage = function (page) {
    renderWalletTxPagination(page);
};

window.copyModalTxRef = async function () {
    const text = (document.getElementById('modal-tx-ref')?.textContent || '').trim();
    if (!text || text === '---') return;
    try {
        await navigator.clipboard.writeText(text);
        window.showToast('success', 'Đã sao chép', `Mã giao dịch ${text} đã được lưu vào bộ nhớ tạm.`);
    } catch (e) {
        window.showToast('info', 'Mã giao dịch', text);
    }
};

document.addEventListener('DOMContentLoaded', () => {
    window.updateDepositXuPreview();
    renderWalletTxPagination(1);
});

document.getElementById('vnpay-deposit-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    await window.createVietQrDeposit();
});

window.openTxDetailModal = function (rowEl) {
    if (!rowEl) return;
    const txnRef = rowEl.getAttribute('data-txn-ref') || '---';
    const friendlyType = rowEl.getAttribute('data-friendly-type') || 'Giao dịch';
    const signedAmount = rowEl.getAttribute('data-signed-amount') || '0 ₫';
    const isCredit = rowEl.getAttribute('data-is-credit') === 'true';
    const balanceAfter = rowEl.getAttribute('data-balance-after') || '---';
    const status = rowEl.getAttribute('data-status') || 'SUCCESS';
    const time = rowEl.getAttribute('data-time') || '---';
    const desc = rowEl.getAttribute('data-desc') || 'Không có ghi chú thêm.';

    document.getElementById('modal-tx-type').textContent = friendlyType;
    const amountEl = document.getElementById('modal-tx-amount');
    amountEl.textContent = signedAmount;
    amountEl.className = 'pk-num text-2xl sm:text-3xl font-extrabold ' +
        (isCredit ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400');

    const badgeEl = document.getElementById('modal-tx-status-badge');
    if (status === 'SUCCESS') {
        badgeEl.innerHTML = '<span class="inline-flex items-center whitespace-nowrap rounded-full bg-emerald-500/15 px-3 py-0.5 text-xs font-bold text-emerald-600 dark:text-emerald-400">Hoàn tất</span>';
    } else if (status === 'PENDING') {
        badgeEl.innerHTML = '<span class="inline-flex items-center whitespace-nowrap rounded-full bg-amber-500/15 px-3 py-0.5 text-xs font-bold text-amber-600 dark:text-amber-400">Đang chờ</span>';
    } else {
        badgeEl.innerHTML = '<span class="inline-flex items-center whitespace-nowrap rounded-full bg-rose-500/15 px-3 py-0.5 text-xs font-bold text-rose-600 dark:text-rose-400">Thất bại</span>';
    }

    document.getElementById('modal-tx-ref').textContent = txnRef;
    document.getElementById('modal-tx-balance-after').textContent = balanceAfter;
    document.getElementById('modal-tx-time').textContent = time;
    document.getElementById('modal-tx-desc').textContent = desc;

    const confirmBtn = document.getElementById('modal-tx-confirm-btn');
    if (confirmBtn) {
        confirmBtn.classList.add('hidden');
        confirmBtn.onclick = null;
    }

    const modal = document.getElementById('tx-detail-modal');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
};

window.closeTxDetailModal = function () {
    const modal = document.getElementById('tx-detail-modal');
    if (!modal) return;
    modal.classList.add('hidden');
    modal.classList.remove('flex');
};

// ==================== VIETQR + SEPAY AUTO-DEPOSIT (24/7 PERSONAL BANK) ====================
let vietQrPollTimer = null;
let activeVietQrCode = null;

function stopVietQrPolling() {
    if (vietQrPollTimer) {
        clearInterval(vietQrPollTimer);
        vietQrPollTimer = null;
    }
}

window.closeVietQrBox = function () {
    stopVietQrPolling();
    activeVietQrCode = null;
    document.getElementById('generated-vietqr-box')?.classList.add('hidden');
};

window.copyVietQrField = async function (elementId, label) {
    const text = (document.getElementById(elementId)?.textContent || '').trim();
    if (!text || text === '---') return;
    try {
        await navigator.clipboard.writeText(text);
        window.showToast('success', 'Đã sao chép', `${label}: ${text}`);
    } catch (e) {
        window.showToast('info', label, text);
    }
};

window.createVietQrDeposit = async function () {
    const amount = parseFloat(document.getElementById('deposit-amount')?.value || '0') || 0;
    if (amount < 10000) {
        window.showToast('error', 'Số tiền chưa hợp lệ', 'Số tiền nạp tối thiểu là 10.000 ₫.');
        return;
    }

    const btn = document.getElementById('btn-create-vietqr');
    const origText = btn ? btn.textContent : '';
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Đang tạo mã QR...';
    }

    try {
        const res = await fetch('/api/v1/wallets/vietqr/create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ amount })
        });
        const data = await res.json();
        if (!res.ok) {
            window.showToast('error', 'Không thể tạo mã VietQR', data.message || 'Vui lòng thử lại.');
            return;
        }

        document.getElementById('generated-vnpay-box')?.classList.add('hidden');
        const box = document.getElementById('generated-vietqr-box');
        const alertEl = document.getElementById('vietqr-not-configured-alert');
        const imgWrap = document.getElementById('vietqr-image-wrap');
        const imgEl = document.getElementById('vietqr-img');
        const bankEl = document.getElementById('vietqr-bank-code');
        const accEl = document.getElementById('vietqr-account-number');
        const nameEl = document.getElementById('vietqr-account-name');
        const amtEl = document.getElementById('vietqr-amount-display');
        const codeEl = document.getElementById('vietqr-transfer-code');
        const statusEl = document.getElementById('vietqr-status-text');

        activeVietQrCode = data.transferCode;
        if (bankEl) bankEl.textContent = data.bankCode || 'MB';
        if (accEl) accEl.textContent = data.accountNumber || 'Chưa cấu hình';
        if (nameEl) nameEl.textContent = data.accountName || 'Chưa cấu hình';
        if (amtEl) {
            const vndStr = Number(data.amountVnd || 0).toLocaleString('vi-VN');
            const xuStr = Number(data.creditedXu || 0).toLocaleString('vi-VN');
            amtEl.textContent = `${vndStr} ₫ (+${xuStr} Xu)`;
        }
        if (codeEl) codeEl.textContent = data.transferCode;
        if (alertEl) alertEl.classList.toggle('hidden', Boolean(data.configured));
        if (imgWrap) imgWrap.classList.toggle('hidden', !data.configured);
        if (imgEl && data.qrImageUrl) imgEl.src = data.qrImageUrl;
        if (statusEl) {
            statusEl.innerHTML = '<span class="inline-block h-2 w-2 rounded-full bg-emerald-500 animate-pulse shrink-0"></span><span>Đang chờ chuyển khoản... tự động cộng Xu sau 1–3 giây</span>';
        }
        if (box) {
            box.classList.remove('hidden');
        }

        stopVietQrPolling();
        if (data.configured) {
            vietQrPollTimer = setInterval(async () => {
                if (!activeVietQrCode || document.hidden) return;
                try {
                    const stRes = await fetch(`/api/v1/wallets/vietqr/status/${encodeURIComponent(activeVietQrCode)}`);
                    if (!stRes.ok) return;
                    const st = await stRes.json();
                    if (st.status === 'SUCCESS') {
                        stopVietQrPolling();
                        if (statusEl) {
                            statusEl.innerHTML = '<span class="inline-block h-2 w-2 rounded-full bg-emerald-500 shrink-0"></span><span class="text-emerald-600 dark:text-emerald-400 font-extrabold">Đã nhận tiền & cộng Xu thành công!</span>';
                        }
                        if (window.updateNavbarWallet && st.walletBalance != null) {
                            window.updateNavbarWallet(st.walletBalance);
                        }
                        const balEl = document.getElementById('wallet-page-balance');
                        if (balEl && st.walletBalance != null) {
                            balEl.textContent = `${Number(st.walletBalance).toLocaleString('vi-VN')} Xu`;
                        }
                        window.showToast('success', 'Nạp Xu thành công!', `Đã cộng +${Number(st.creditedXu || 0).toLocaleString('vi-VN')} Xu vào Ví của bạn.`);
                        setTimeout(() => {
                            window.closeVietQrBox();
                            window.location.reload();
                        }, 1100);
                    }
                } catch (e) {}
            }, 3000);
        }
    } catch (err) {
        window.showToast('error', 'Lỗi kết nối', err.message || 'Không thể tạo mã VietQR.');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.textContent = origText;
        }
    }
};

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        window.closeVietQrBox();
        window.closeTxDetailModal();
    }
});

window.addEventListener('pagehide', stopVietQrPolling);
