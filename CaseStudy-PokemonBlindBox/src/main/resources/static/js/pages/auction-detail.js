/* ==================== AUCTION DETAIL PAGE (LIVE WEBSOCKET) ==================== */
const rootEl = document.getElementById('auction-detail-root');
const AUCTION_ID = rootEl ? parseInt(rootEl.getAttribute('data-auction-id'), 10) : null;
const MIN_INCREMENT = rootEl ? parseFloat(rootEl.getAttribute('data-min-increment') || '10000') : 10000;
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
        return;
    }
    const hrs = String(Math.floor(diffSeconds / 3600)).padStart(2, '0');
    const mins = String(Math.floor((diffSeconds % 3600) / 60)).padStart(2, '0');
    const secs = String(diffSeconds % 60).padStart(2, '0');
    timerEl.textContent = `${hrs}:${mins}:${secs}`;
}

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
                if (historyList) {
                    const safeBidder = window.escapeHtml ? window.escapeHtml(event.maskedBidderName) : event.maskedBidderName;
                    const row = document.createElement('div');
                    row.className = 'pk-surface-elevated rounded-xl px-4 py-2.5 flex items-center justify-between text-xs';
                    row.innerHTML = `
                        <div>
                            <span class="font-bold pk-text-primary">${safeBidder}</span>
                            <span class="ml-2 text-emerald-600 dark:text-emerald-400 font-semibold">Vừa đặt giá</span>
                        </div>
                        <span class="pk-num font-extrabold text-emerald-600 dark:text-emerald-400">${window.formatVND(event.newHighestBid)}</span>
                    `;
                    historyList.prepend(row);
                }

                window.showToast('info', 'Bước giá mới!',
                    `${event.maskedBidderName} vừa nâng giá lên ${window.formatVND(event.newHighestBid)}.`);
            });
        }
    });
    client.activate();
}

document.getElementById('place-bid-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!AUCTION_ID) return;
    document.getElementById('auction-conflict-banner')?.classList.add('hidden');

    const bidderDisplayName = document.getElementById('bidder-display-name').value.trim();
    const bidAmount = parseFloat(document.getElementById('bid-amount-input').value);

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
            window.showToast('error', 'Không thể đặt giá', data.message);
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
    setInterval(updateCountdownTimer, 1000);
    connectAuctionWebSocket();
});
