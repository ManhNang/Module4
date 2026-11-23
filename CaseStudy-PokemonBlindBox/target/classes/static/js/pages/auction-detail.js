/* ==================== AUCTION DETAIL PAGE (LIVE WEBSOCKET) ==================== */
const rootEl = document.getElementById('auction-detail-root');
const AUCTION_ID = rootEl ? parseInt(rootEl.getAttribute('data-auction-id'), 10) : null;
const MIN_INCREMENT = rootEl ? parseFloat(rootEl.getAttribute('data-min-increment') || '10') : 10;
let auctionEndTime = rootEl ? new Date(rootEl.getAttribute('data-end-time')) : new Date();

window.bumpBidAmount = function (delta) {
    const input = document.getElementById('bid-amount-input');
    if (!input) return;
    const current = parseFloat(input.value || '0');
    input.value = current + delta;
};

function updateCountdownTimer() {
    const timerEl = document.getElementById('live-countdown-timer');
    if (!timerEl) return;

    const now = new Date();
    const diffSeconds = Math.floor((auctionEndTime.getTime() - now.getTime()) / 1000);
    if (diffSeconds <= 0) {
        timerEl.textContent = 'ĐÃ KẾT THÚC';
        timerEl.className = 'mt-1.5 pk-num tabular-nums text-3xl font-extrabold pk-text-secondary';
        return;
    }
    const hrs = String(Math.floor(diffSeconds / 3600)).padStart(2, '0');
    const mins = String(Math.floor((diffSeconds % 3600) / 60)).padStart(2, '0');
    const secs = String(diffSeconds % 60).padStart(2, '0');
    timerEl.textContent = `${hrs}:${mins}:${secs}`;

    // Chỉ chuyển màu đỏ (kèm nhấp nháy nhẹ) khi còn dưới 10 phút (< 600 giây)
    if (diffSeconds < 600) {
        timerEl.className = 'mt-1.5 pk-num tabular-nums text-3xl font-extrabold text-rose-600 dark:text-rose-400 animate-pulse';
    } else {
        timerEl.className = 'mt-1.5 pk-num tabular-nums text-3xl font-extrabold pk-text-primary';
    }
}

let auctionStompClient = null;
let auctionTimerId = null;

function connectAuctionWebSocket() {
    if (!AUCTION_ID || typeof SockJS === 'undefined' || typeof StompJs === 'undefined') return;

    const client = new StompJs.Client({
        webSocketFactory: () => new SockJS('/ws-blindbox'),
        reconnectDelay: 4000,
        onConnect: () => {
            const indicator = document.getElementById('ws-auction-indicator');
            const statusText = document.getElementById('ws-auction-status-text');
            if (indicator) indicator.className = 'h-2 w-2 rounded-full bg-emerald-500 animate-pulse';
            if (statusText) statusText.textContent = 'Trực tuyến';

            client.subscribe(`/topic/auctions/${AUCTION_ID}`, (message) => {
                const event = JSON.parse(message.body);
                const bidEl = document.getElementById('live-highest-bid');
                const leaderEl = document.getElementById('live-leader-name');
                const boxEl = document.getElementById('highest-bid-box');

                if (bidEl) bidEl.textContent = window.formatVND(event.newHighestBid);
                if (leaderEl) leaderEl.textContent = event.maskedBidderName;

                const inputEl = document.getElementById('bid-amount-input');
                if (inputEl) {
                    inputEl.value = Number(event.newHighestBid) + MIN_INCREMENT;
                }

                if (event.endTime) {
                    auctionEndTime = new Date(event.endTime);
                    updateCountdownTimer();
                }

                if (boxEl) {
                    boxEl.classList.add('ring-2', 'ring-amber-500');
                    setTimeout(() => boxEl.classList.remove('ring-2', 'ring-amber-500'), 1000);
                }

                const historyList = document.getElementById('live-bid-history-list');
                const emptyState = document.getElementById('bid-history-empty');
                const countEl = document.getElementById('live-bid-count');
                if (emptyState) emptyState.classList.add('hidden');

                if (historyList) {
                    // Hạ cấp dòng dẫn đầu cũ
                    historyList.querySelectorAll('.bid-leading-row').forEach(oldRow => {
                        oldRow.classList.remove('bid-leading-row', 'border-l-2', 'border-amber-400', 'bg-amber-500/5');
                        oldRow.querySelector('.bid-leader-pill')?.remove();
                    });

                    const safeBidder = window.escapeHtml ? window.escapeHtml(event.maskedBidderName) : event.maskedBidderName;
                    const nowTime = new Date().toLocaleTimeString('vi-VN', { hour12: false });
                    const row = document.createElement('div');
                    row.className = 'bid-leading-row border-l-2 border-amber-400 bg-amber-500/5 pk-surface-elevated rounded-xl px-4 py-2.5 flex items-center justify-between gap-2 text-xs';
                    row.innerHTML = `
                        <div class="flex items-center gap-2 min-w-0">
                            <span class="font-bold pk-text-primary truncate">${safeBidder}</span>
                            <span class="bid-leader-pill rounded-full bg-amber-500/20 border border-amber-500/35 px-2 py-0.5 text-[10px] font-extrabold text-amber-600 dark:text-amber-400 shrink-0">Dẫn đầu</span>
                            <span class="pk-num tabular-nums pk-text-secondary shrink-0">${nowTime}</span>
                        </div>
                        <span class="pk-num tabular-nums font-extrabold text-amber-600 dark:text-amber-400 shrink-0">${window.formatVND(event.newHighestBid)}</span>
                    `;
                    historyList.prepend(row);
                }

                if (countEl) {
                    const prevCount = parseInt(countEl.textContent || '0', 10) || 0;
                    countEl.textContent = String(prevCount + 1);
                }

                window.showToast('info', 'Bước giá mới!',
                    `${event.maskedBidderName} vừa nâng giá lên ${window.formatVND(event.newHighestBid)}.`);
            });
        }
    });
    auctionStompClient = client;
    client.activate();
}

document.getElementById('place-bid-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!AUCTION_ID) return;
    document.getElementById('auction-conflict-banner')?.classList.add('hidden');

    const bidderDisplayName = (document.getElementById('bidder-display-name')?.value || '').trim();
    const rawBidStr = (document.getElementById('bid-amount-input')?.value || '').trim();
    const bidAmount = Number(rawBidStr);

    if (!rawBidStr || !Number.isFinite(bidAmount) || bidAmount < 1 || !Number.isInteger(bidAmount)) {
        window.showToast('error', 'Mức giá đặt không hợp lệ', 'Vui lòng nhập số nguyên Xu lớn hơn hoặc bằng 1.');
        return;
    }
    if (bidderDisplayName.length > 60) {
        window.showToast('error', 'Biệt danh quá dài', 'Biệt danh hiển thị tối đa 60 ký tự.');
        return;
    }

    try {
        const res = await fetch(`/api/v1/market/auctions/${AUCTION_ID}/bids`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                bidderId: window.CURRENT_USER_ID,
                bidderDisplayName,
                bidAmount
            })
        });
        const data = await res.json();

        if (res.status === 409) {
            const banner = document.getElementById('auction-conflict-banner');
            const bannerText = document.getElementById('auction-conflict-text');
            if (bannerText) bannerText.textContent = data.message;
            banner?.classList.remove('hidden');
            window.showToast('conflict', 'Mức giá vừa thay đổi', data.message);
            return;
        }
        if (!res.ok) {
            const fieldMsg = data.fieldErrors ? Object.values(data.fieldErrors).join(' • ') : '';
            window.showToast('error', 'Không thể đặt giá', fieldMsg || data.message || 'Vui lòng kiểm tra lại thông tin đặt giá.');
            return;
        }

        await window.refreshNavbarWallet();
        window.showToast('success', 'Đặt giá thành công!',
            `Bạn đang dẫn đầu với mức giá ${window.formatVND(data.bidAmount)}.`);
    } catch (err) {
        window.showToast('error', 'Lỗi kết nối', err.message);
    }
});

document.addEventListener('DOMContentLoaded', () => {
    updateCountdownTimer();
    auctionTimerId = setInterval(updateCountdownTimer, 1000);
    connectAuctionWebSocket();

    window.addEventListener('pagehide', () => {
        if (auctionTimerId != null) {
            clearInterval(auctionTimerId);
            auctionTimerId = null;
        }
        if (auctionStompClient) {
            auctionStompClient.deactivate();
            auctionStompClient = null;
        }
    });
});
