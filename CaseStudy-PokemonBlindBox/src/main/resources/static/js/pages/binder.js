/* ==================== BINDER / INVENTORY PAGE ==================== */

// Gom nhóm các bản sao trùng nhau thành 1 thẻ duy nhất kèm badge xN (ưu tiên giữ bản sao chưa khóa để có thể đem bán)
(function deduplicateInventoryCards() {
    const grid = document.getElementById('my-cards-grid');
    if (!grid) return;
    const items = Array.from(grid.querySelectorAll('.my-card-item'));
    const keptByTemplate = new Map();
    items.forEach(el => {
        const tplId = el.getAttribute('data-template-id');
        const isLocked = el.getAttribute('data-is-locked') === 'true';
        if (!keptByTemplate.has(tplId)) {
            keptByTemplate.set(tplId, { el, isLocked });
        } else {
            const existing = keptByTemplate.get(tplId);
            if (existing.isLocked && !isLocked) {
                existing.el.remove();
                keptByTemplate.set(tplId, { el, isLocked });
            } else {
                el.remove();
            }
        }
    });
})();

const binderMainEl = document.getElementById('binder-main');
const INITIAL_TAB = (binderMainEl && binderMainEl.dataset.initialTab) ? binderMainEl.dataset.initialTab : 'inventory';

window.switchInventoryTab = function (tabName) {
    const invPanel = document.getElementById('panel-tab-inventory');
    const msPanel = document.getElementById('panel-tab-masterset');
    const invBtn = document.getElementById('btn-tab-inventory');
    const msBtn = document.getElementById('btn-tab-masterset');

    const isInventory = tabName === 'inventory';
    invPanel?.classList.toggle('hidden', !isInventory);
    msPanel?.classList.toggle('hidden', isInventory);

    if (invBtn && msBtn) {
        invBtn.className = isInventory
            ? 'pk-btn-primary rounded-xl px-4 py-2.5 text-xs sm:text-sm font-extrabold transition flex items-center gap-2 cursor-pointer'
            : 'rounded-xl px-4 py-2.5 text-xs sm:text-sm font-bold pk-text-secondary hover:pk-text-primary transition flex items-center gap-2 cursor-pointer';
        msBtn.className = !isInventory
            ? 'pk-btn-primary rounded-xl px-4 py-2.5 text-xs sm:text-sm font-extrabold transition flex items-center gap-2 cursor-pointer'
            : 'rounded-xl px-4 py-2.5 text-xs sm:text-sm font-bold pk-text-secondary hover:pk-text-primary transition flex items-center gap-2 cursor-pointer';
    }
};

window.filterBinderGridClientSide = function () {
    const nameVal = (document.getElementById('filter-name')?.value || '').trim().toLowerCase();
    const numVal = (document.getElementById('filter-number')?.value || '').trim();
    const rarityVal = (document.getElementById('filter-rarity')?.value || '').trim().toLowerCase();
    const speciesVal = (document.getElementById('filter-species')?.value || '').trim().toLowerCase();
    const onlyConfigured = document.getElementById('toggle-only-configured')?.checked || false;

    document.querySelectorAll('.binder-slot-item').forEach(slot => {
        const cNum = slot.getAttribute('data-card-number') || '';
        const cName = (slot.getAttribute('data-card-name') || '').toLowerCase();
        const cRarity = (slot.getAttribute('data-rarity') || '').toLowerCase();
        const cSpecies = (slot.getAttribute('data-species') || '').toLowerCase();
        const hasTemplate = slot.getAttribute('data-has-template') === 'true';

        let visible = true;
        if (onlyConfigured && !hasTemplate) visible = false;
        if (numVal && cNum !== numVal) visible = false;
        if (nameVal && !cName.includes(nameVal)) visible = false;
        if (rarityVal && cRarity !== rarityVal) visible = false;
        if (speciesVal && cSpecies !== speciesVal) visible = false;

        slot.style.display = visible ? '' : 'none';
    });
};

window.applyPresetFilter = function (name, num, rarity, setName, species) {
    document.getElementById('filter-name').value = name;
    document.getElementById('filter-number').value = num;
    document.getElementById('filter-rarity').value = rarity;
    document.getElementById('filter-species').value = species;
    window.filterBinderGridClientSide();
    document.getElementById('binder-filter-form')?.requestSubmit();
};

window.resetBinderFilter = function () {
    document.getElementById('filter-name').value = '';
    document.getElementById('filter-number').value = '';
    document.getElementById('filter-rarity').value = '';
    document.getElementById('filter-species').value = '';
    if (document.getElementById('toggle-only-configured')) {
        document.getElementById('toggle-only-configured').checked = false;
    }
    document.getElementById('api-search-results-box')?.classList.add('hidden');
    window.filterBinderGridClientSide();
};

document.getElementById('binder-filter-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const params = new URLSearchParams();
    const name = document.getElementById('filter-name').value.trim();
    const cardNumber = document.getElementById('filter-number').value.trim();
    const rarityName = document.getElementById('filter-rarity').value;
    const setName = document.getElementById('filter-set').value;
    const speciesName = document.getElementById('filter-species').value;

    if (name) params.set('name', name);
    if (cardNumber) params.set('cardNumber', cardNumber);
    if (rarityName) params.set('rarityName', rarityName);
    if (setName) params.set('setName', setName);
    if (speciesName) params.set('speciesName', speciesName);

    const res = await fetch(`/api/v1/binder/cards?${params.toString()}`);
    const pageData = await res.json();
    const box = document.getElementById('api-search-results-box');
    const summary = document.getElementById('api-search-summary');
    const listEl = document.getElementById('api-search-cards-list');
    const esc = window.escapeHtml || (v => String(v ?? ''));

    const cards = pageData.content || [];
    summary.textContent = `Tìm thấy ${cards.length} thẻ bài phù hợp:`;
    listEl.innerHTML = cards.map(c => {
        const safeColor = /^#[0-9a-fA-F]{3,8}$/.test(c.rarityColorHex || '') ? c.rarityColorHex : '#10B981';
        return `
            <div class="pk-surface rounded-xl p-3.5 flex items-center justify-between gap-3">
                <div>
                    <div class="pk-num text-xs font-extrabold text-emerald-600 dark:text-emerald-400">${esc(c.formattedCardNumber)}</div>
                    <div class="text-sm font-bold pk-text-primary">${esc(c.name)}</div>
                    <div class="text-xs pk-text-secondary">
                        Độ hiếm: <strong style="color:${safeColor}">${esc(c.rarityName)}</strong> •
                        Loài: <strong class="pk-text-primary">${esc(c.speciesName)}</strong>
                    </div>
                </div>
                <a href="/market?speciesName=${encodeURIComponent(c.speciesName || '')}"
                   class="pk-btn-primary rounded-lg px-3 py-1.5 text-[11px] font-bold">
                    Tìm trên Sàn →
                </a>
            </div>
        `;
    }).join('');
    box.classList.remove('hidden');
});

document.addEventListener('DOMContentLoaded', () => {
    if (INITIAL_TAB === 'masterset') {
        window.switchInventoryTab('masterset');
    }
});
