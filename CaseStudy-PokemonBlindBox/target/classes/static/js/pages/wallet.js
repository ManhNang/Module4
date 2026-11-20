/* ==================== WALLET & VNPAY TOP-UP PAGE ==================== */
window.selectPresetAmount = function (amount, clickedBtn) {
    document.getElementById('deposit-amount').value = amount;
    document.querySelectorAll('.wallet-preset-btn').forEach(btn => {
        btn.classList.remove('border-rose-500', 'ring-2', 'ring-rose-500/25');
        btn.classList.add('border-transparent');
    });
    if (clickedBtn) {
        clickedBtn.classList.remove('border-transparent');
        clickedBtn.classList.add('border-rose-500', 'ring-2', 'ring-rose-500/25');
    }
};

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

    window.showToast('info', 'Đã tạo yêu cầu nạp điểm',
        'Bấm "Xác Nhận Nạp Thử Nghiệm" để hoàn tất cộng tiền vào ví.');
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
            window.showToast('success', 'Nạp điểm thành công!',
                `Giao dịch ${vnpTxnRef} đã được cộng vào số dư ví.`);
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
