/* ==================== MARKETPLACE P2P PAGE ==================== */

// Bộ lọc ngang hợp nhất (Search + Bộ thẻ + Độ hiếm + Loài + Loại giao dịch)
window.applyMarketFilters = function () {
    const searchVal = (document.getElementById('market-filter-search')?.value || '').trim().toLowerCase();
    const setVal = (document.getElementById('market-filter-set')?.value || '').trim().toLowerCase();
    const rarityVal = (document.getElementById('market-filter-rarity')?.value || '').trim().toLowerCase();
    const speciesVal = (document.getElementById('market-filter-species')?.value || '').trim().toLowerCase();
    const typeVal = document.getElementById('market-filter-type')?.value || 'ALL';

    document.getElementById('section-live-auctions')?.classList.toggle('hidden', typeVal === 'FIXED');
    document.getElementById('section-fixed-listings')?.classList.toggle('hidden', typeVal === 'AUCTION');

    document.querySelectorAll('.market-item-card').forEach(card => {
        const cName = (card.getAttribute('data-card-name') || '').toLowerCase();
        const cNum = (card.getAttribute('data-card-num') || '').toLowerCase();
        const cSet = (card.getAttribute('data-set-name') || '').toLowerCase();
        const cRarity = (card.getAttribute('data-rarity-name') || '').toLowerCase();
        const cSpecies = (card.getAttribute('data-species-name') || '').toLowerCase();
        const cType = card.getAttribute('data-market-type');

        let visible = true;
        if (typeVal !== 'ALL' && cType !== typeVal) visible = false;
        if (searchVal && !cName.includes(searchVal) && !cNum.includes(searchVal)) visible = false;
        if (setVal && cSet !== setVal) visible = false;
        if (rarityVal && cRarity !== rarityVal) visible = false;
        if (speciesVal && cSpecies !== speciesVal) visible = false;

        card.style.display = visible ? '' : 'none';
    });
};

window.resetMarketFilters = function () {
    document.getElementById('market-filter-search').value = '';
    document.getElementById('market-filter-set').value = '';
    document.getElementById('market-filter-rarity').value = '';
    document.getElementById('market-filter-species').value = '';
    document.getElementById('market-filter-type').value = 'ALL';
    window.applyMarketFilters();
};

// Đồng hồ đếm ngược trực tiếp cho từng thẻ Đấu giá LIVE
function tickAllAuctionTimers() {
    const now = Date.now();
    document.querySelectorAll('.auction-card-timer').forEach(el => {
        const endStr = el.getAttribute('data-end-time');
        if (!endStr) return;
        const diffSec = Math.floor((new Date(endStr).getTime() - now) / 1000);
        if (diffSec <= 0) {
            el.textContent = 'Đã kết thúc';
            return;
        }
        const h = String(Math.floor(diffSec / 3600)).padStart(2, '0');
        const m = String(Math.floor((diffSec % 3600) / 60)).padStart(2, '0');
        const s = String(diffSec % 60).padStart(2, '0');
        el.textContent = `${h}:${m}:${s}`;
    });
}

document.getElementById('create-listing-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const userCardId = parseInt(document.getElementById('listing-user-card-id').value, 10);
    const price = parseFloat(document.getElementById('listing-price').value);

    const res = await fetch('/api/v1/market/listings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userCardId, price })
    });
    const data = await res.json();
    if (!res.ok) {
        window.showToast('error', 'Không thể đăng bán thẻ', data.message);
        return;
    }
    window.showToast('success', 'Đã đăng bán thẻ bài!', `Thẻ ${data.card.name} (${data.card.formattedCardNumber}) đã lên sàn.`);
    setTimeout(() => window.location.reload(), 900);
});

document.getElementById('create-auction-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const userCardId = parseInt(document.getElementById('auction-user-card-id').value, 10);
    const startingPrice = parseFloat(document.getElementById('auction-start-price').value);
    const minBidIncrement = parseFloat(document.getElementById('auction-min-increment').value);
    const durationMinutes = parseInt(document.getElementById('auction-duration').value, 10);

    const res = await fetch('/api/v1/market/auctions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userCardId, startingPrice, minBidIncrement, durationMinutes })
    });
    const data = await res.json();
    if (!res.ok) {
        window.showToast('error', 'Không thể tạo phiên đấu giá', data.message);
        return;
    }
    window.showToast('success', 'Đã mở Phiên Đấu Giá!', 'Đang chuyển vào phòng đấu giá...');
    setTimeout(() => { window.location.href = `/market/auctions/${data.id}`; }, 800);
});

window.buyFixedListing = async function (listingId) {
    const res = await fetch(`/api/v1/market/listings/${listingId}/buy`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({})
    });
    const data = await res.json();
    if (res.status === 409) {
        window.showToast('conflict', 'Thẻ vừa có người mua', data.message);
        return;
    }
    if (!res.ok) {
        window.showToast('error', 'Không thể mua thẻ', data.message);
        return;
    }
    await window.refreshNavbarWallet();
    window.showToast('success', 'Mua thẻ thành công!', `Thẻ ${data.card.name} đã chuyển vào Kho đồ của bạn.`);
    setTimeout(() => window.location.reload(), 1000);
};

document.addEventListener('DOMContentLoaded', () => {
    tickAllAuctionTimers();
    setInterval(tickAllAuctionTimers, 1000);
});
