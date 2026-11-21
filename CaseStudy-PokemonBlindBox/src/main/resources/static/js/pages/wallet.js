/* ==================== WALLET & VNPAY TOP-UP PAGE ==================== */
const WALLET_TX_PER_PAGE = 10;
let walletTxPage = 1;
let walletTxFilter = 'ALL'; // 'ALL' | 'CREDIT' | 'DEBIT'

window.updateDepositXuPreview = function () {
    const raw = parseFloat(document.getElementById('deposit-amount')?.value || '0') || 0;
    const xu = Math.max(0, Math.floor(raw / 100));
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

async function requestVNPayUrl() {
    const userId = parseInt(document.getElementById('deposit-user-id').value, 10);
    const amount = parseFloat(document.getElementById('deposit-amount').value);
    const bankCode = document.getElementById('deposit-bank-code').value;

    const res = await fetch('/api/v1/wallets/vnpay/create-url', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId, amount, bankCode, locale: 'vn' })
    });
    const data = await res.json();
    if (!res.ok) {
        window.showToast('error', 'Không thể tạo giao dịch', data.message || 'Vui lòng kiểm tra số tiền nạp');
        return null;
    }
    return data;
}

document.getElementById('vnpay-deposit-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const data = await requestVNPayUrl();
    if (!data) return;

    window.showToast('success', 'Đang chuyển sang cổng thanh toán', 'Vui lòng hoàn tất thanh toán trên VNPay.');
    setTimeout(() => {
        window.location.href = data.paymentUrl;
    }, 700);
});

window.createUrlAndStayForTest = async function () {
    const data = await requestVNPayUrl();
    if (!data) return;

    const box = document.getElementById('generated-vnpay-box');
    document.getElementById('generated-txn-ref').textContent = data.vnpTxnRef;
    document.getElementById('btn-open-vnpay-url').href = data.paymentUrl;
    document.getElementById('btn-simulate-ipn').onclick = () => window.triggerSimulateIpn(data.vnpTxnRef);
    box.classList.remove('hidden');

    window.showToast('info', 'Đã tạo yêu cầu nạp Xu',
        'Bấm "Xác Nhận Nạp Thử" để hoàn tất cộng Xu vào ví.');
};

window.triggerSimulateIpn = async function (vnpTxnRef) {
    try {
        const res = await fetch(`/api/v1/admin/dev/simulate-ipn/${encodeURIComponent(vnpTxnRef)}`, { method: 'POST' });
        if (res.status === 403) {
            window.showToast('error', 'Không có quyền', 'Chức năng giả lập IPN chỉ dành cho Quản trị viên ở môi trường Dev.');
            return;
        }
        const data = await res.json();
        if (data.RspCode === '00') {
            window.showToast('success', 'Nạp Xu thành công!',
                `Giao dịch ${vnpTxnRef} đã được cộng Xu vào số dư ví.`);
            await window.refreshNavbarWallet();
            setTimeout(() => window.location.reload(), 1100);
        } else if (data.RspCode === '02') {
            window.showToast('conflict', 'Giao dịch đã được xử lý',
                `Mã giao dịch ${vnpTxnRef} đã được xác nhận trước đó.`);
        } else {
            window.showToast('error', 'Không thể xác nhận giao dịch', data.Message || 'Vui lòng thử lại');
        }
    } catch (err) {
        window.showToast('error', 'Lỗi kết nối', err.message);
    }
};

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
    if (status === 'PENDING' && txnRef && txnRef !== '---') {
        confirmBtn.classList.remove('hidden');
        confirmBtn.onclick = () => {
            window.closeTxDetailModal();
            window.triggerSimulateIpn(txnRef);
        };
    } else {
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
