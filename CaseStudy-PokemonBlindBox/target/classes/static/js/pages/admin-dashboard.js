/* ==================== ADMIN DASHBOARD PAGE (100% AJAX, KHÔNG RELOAD TRANG) ==================== */
(function () {
    const esc = (v) => (window.escapeHtml ? window.escapeHtml(v) : String(v ?? ''));
    const safeHex = (hex, fallback = '#10B981') => (/^#[0-9a-fA-F]{3,8}$/.test(hex || '') ? hex : fallback);
    const fmtPrice = (val) => new Intl.NumberFormat('vi-VN').format(Math.round(Number(val || 0))) + ' Xu';

    // Trạng thái phân trang Tab 3 (Danh Mục Thẻ Bài — tối đa 15 thẻ/trang)
    const CARDS_PER_PAGE = 15;
    let cardTemplatesPage = 1;

    // Trạng thái phân trang Tab 5 (Quản Lý Người Dùng)
    let adminUsersLoaded = false;
    let adminUsersPage = 0;
    let adminUsersTotalPages = 1;

    // ==================== 1. MODAL HELPERS & ESCAPE KEY ====================
    window.openModal = function (id) {
        const el = document.getElementById(id);
        if (!el) return;
        el.classList.remove('hidden');
        el.classList.add('flex');
    };

    window.closeModal = function (id) {
        const el = document.getElementById(id);
        if (!el) return;
        el.classList.add('hidden');
        el.classList.remove('flex');
    };

    document.addEventListener('keydown', (e) => {
        if (e.key !== 'Escape') return;
        ['modal-card-preview', 'modal-user-wallet', 'modal-edit-card', 'modal-create-card', 'modal-species', 'modal-rarity', 'modal-create-set']
            .forEach(id => window.closeModal(id));
    });

    let rarityInitialSnapshot = null;

    window.updateNewSetBoxHint = function () {
        const packs = parseInt(document.getElementById('new-set-packs-per-box')?.value || '0', 10) || 0;
        const packPrice = parseFloat(document.getElementById('new-set-pack-price')?.value || '0') || 0;
        const boxPrice = parseFloat(document.getElementById('new-set-box-price')?.value || '0') || 0;
        const hintEl = document.getElementById('new-set-box-hint');
        if (!hintEl) return;
        const fullPrice = packs * packPrice;
        if (fullPrice <= 0) {
            hintEl.textContent = '';
            return;
        }
        const diffPct = Math.round(((fullPrice - boxPrice) / fullPrice) * 100);
        const fullStr = Math.round(fullPrice).toLocaleString('vi-VN') + ' Xu';
        if (diffPct > 0) {
            hintEl.textContent = `Giá lẻ ${packs} gói: ${fullStr} — Đang giảm ${diffPct}%`;
        } else if (diffPct < 0) {
            hintEl.textContent = `Giá lẻ ${packs} gói: ${fullStr} — Cao hơn ${Math.abs(diffPct)}%`;
        } else {
            hintEl.textContent = `Giá lẻ ${packs} gói: ${fullStr} — Bằng giá lẻ`;
        }
    };

    window.validateCreateSetForm = function () {
        const btn = document.getElementById('btn-create-set-submit');
        if (!btn) return;
        const name = (document.getElementById('new-set-name')?.value || '').trim();
        const code = (document.getElementById('new-set-code')?.value || '').trim();
        const total = parseInt(document.getElementById('new-set-total')?.value || '0', 10);
        const cpp = parseInt(document.getElementById('new-set-cards-per-pack')?.value || '0', 10);
        const ppb = parseInt(document.getElementById('new-set-packs-per-box')?.value || '0', 10);
        const pp = parseFloat(document.getElementById('new-set-pack-price')?.value || '0');
        const bp = parseFloat(document.getElementById('new-set-box-price')?.value || '0');
        btn.disabled = !(name && code && total >= 1 && cpp >= 1 && ppb >= 1 && pp >= 1 && bp >= 1);
    };

    window.updateRarityModalPreview = function () {
        const name = (document.getElementById('rarity-modal-name')?.value || '').trim() || 'Tên độ hiếm';
        const color = safeHex(document.getElementById('rarity-modal-color')?.value?.trim(), '#EC4899');
        const isHolo = Boolean(document.getElementById('rarity-modal-holo')?.checked);
        const badge = document.getElementById('rarity-modal-preview-badge');
        const holoEl = document.getElementById('rarity-modal-preview-holo');
        if (badge) {
            badge.textContent = name;
            badge.style.borderColor = color;
            badge.style.color = color;
        }
        if (holoEl) holoEl.classList.toggle('hidden', !isHolo);
        window.validateRarityForm();
    };

    window.validateRarityForm = function () {
        const btn = document.getElementById('btn-upsert-rarity-submit');
        if (!btn) return;
        const name = (document.getElementById('rarity-modal-name')?.value || '').trim();
        const weightRaw = document.getElementById('rarity-modal-weight')?.value;
        const weight = parseInt(weightRaw || '-1', 10);
        const color = (document.getElementById('rarity-modal-color')?.value || '').trim().toUpperCase();
        const isHolo = Boolean(document.getElementById('rarity-modal-holo')?.checked);
        const valid = Boolean(name && weightRaw !== '' && !Number.isNaN(weight) && weight >= 0 && /^#[0-9A-F]{6}$/.test(color));
        if (!valid) {
            btn.disabled = true;
            return;
        }
        if (rarityInitialSnapshot) {
            const changed = name !== rarityInitialSnapshot.name
                || weight !== rarityInitialSnapshot.dropWeight
                || color !== rarityInitialSnapshot.colorHex
                || isHolo !== rarityInitialSnapshot.isHolographic;
            btn.disabled = !changed;
        } else {
            btn.disabled = false;
        }
    };

    window.validateCreateCardForm = function () {
        const btn = document.getElementById('btn-create-card-submit');
        if (!btn) return;
        const name = (document.getElementById('card-name')?.value || '').trim();
        const num = parseInt(document.getElementById('card-number')?.value || '0', 10);
        const rarityId = document.getElementById('card-rarity-id')?.value || '';
        const speciesId = document.getElementById('card-species-id')?.value || '';
        btn.disabled = !(name && num >= 1 && rarityId && speciesId);
    };

    window.validateAdjustWalletForm = function () {
        const btn = document.getElementById('btn-adjust-wallet-submit');
        if (!btn) return;
        const amount = parseFloat(document.getElementById('adjust-wallet-amount')?.value || '0');
        const reason = (document.getElementById('adjust-wallet-reason')?.value || '').trim();
        btn.disabled = !(Number.isFinite(amount) && amount >= 1 && reason.length > 0);
    };

    window.syncCreateCardImagePreview = function () {
        const url = (document.getElementById('card-image-url')?.value || '').trim();
        const imgEl = document.getElementById('create-card-img-preview');
        const phEl = document.getElementById('create-card-img-placeholder');
        if (!imgEl || !phEl) return;
        if (url) {
            imgEl.src = url;
            imgEl.classList.remove('hidden');
            phEl.classList.add('hidden');
        } else {
            imgEl.src = '';
            imgEl.classList.add('hidden');
            phEl.classList.remove('hidden');
        }
    };

    window.handleAutoUploadCardImage = async function (fileInput) {
        if (!fileInput || !fileInput.files || fileInput.files.length === 0) return;
        const labelEl = document.getElementById('card-upload-btn-label');
        if (labelEl) labelEl.textContent = 'Đang tải lên...';
        try {
            await window.uploadSelectedImageToCloudinary('card-image-file', 'card-image-url', 'card-number');
            window.syncCreateCardImagePreview();
        } finally {
            if (labelEl) labelEl.textContent = 'Chọn ảnh & Tải lên';
        }
    };

    window.openCreateSetModal = function () {
        document.getElementById('form-create-set')?.reset();
        window.updateNewSetBoxHint();
        window.validateCreateSetForm();
        window.openModal('modal-create-set');
    };

    window.openRarityModal = function (rarity, targetSelectId) {
        document.getElementById('rarity-modal-id').value = rarity ? rarity.id : '';
        document.getElementById('rarity-modal-target-select').value = targetSelectId || '';
        document.getElementById('modal-rarity-title').textContent = rarity ? 'Chỉnh Sửa Độ Hiếm' : 'Thêm Độ Hiếm Mới';
        document.getElementById('rarity-modal-name').value = rarity ? rarity.name : '';
        document.getElementById('rarity-modal-weight').value = rarity ? rarity.dropWeight : 30;
        const hex = safeHex(rarity ? rarity.colorHex : '#EC4899', '#EC4899').toUpperCase();
        document.getElementById('rarity-modal-color').value = hex;
        document.getElementById('rarity-modal-color-picker').value = /^#[0-9A-F]{6}$/.test(hex) ? hex : '#EC4899';
        document.getElementById('rarity-modal-holo').checked = rarity ? Boolean(rarity.isHolographic) : false;

        const submitBtn = document.getElementById('btn-upsert-rarity-submit');
        if (submitBtn) submitBtn.textContent = rarity ? 'Lưu thay đổi' : 'Lưu Độ Hiếm';
        rarityInitialSnapshot = rarity
            ? { name: rarity.name, dropWeight: Number(rarity.dropWeight), colorHex: hex, isHolographic: Boolean(rarity.isHolographic) }
            : null;

        window.updateRarityModalPreview();
        window.openModal('modal-rarity');
    };

    window.openSpeciesModal = function (species, targetSelectId) {
        document.getElementById('species-modal-target-select').value = targetSelectId || '';
        document.getElementById('species-modal-name').value = species ? species.name : '';
        document.getElementById('species-modal-beta').checked = species ? Boolean(species.isBeta) : true;
        window.openModal('modal-species');
    };

    window.openEditCardModal = function (btn) {
        document.getElementById('edit-card-id').value = btn.dataset.id;
        document.getElementById('edit-card-name').value = btn.dataset.name;
        document.getElementById('edit-card-number').value = btn.dataset.num;
        document.getElementById('edit-card-image-url').value = btn.dataset.img;
        document.getElementById('edit-card-rarity-id').value = btn.dataset.rarity;
        document.getElementById('edit-card-species-id').value = btn.dataset.species;
        window.openModal('modal-edit-card');
    };

    // ==================== 2. MINI POPUP XEM TRƯỚC ẢNH THẺ (HẠNG MỤC 2) ====================
    window.openCardPreviewModal = function (btn) {
        if (!btn) return;
        const name = btn.dataset.name || '---';
        const num = btn.dataset.num || '---';
        const imgUrl = btn.dataset.img || '';
        const rarity = btn.dataset.rarity || '---';
        const color = safeHex(btn.dataset.color, '#10B981');
        const species = btn.dataset.species || '---';
        const isHolo = btn.dataset.holo === 'true';

        const numEl = document.getElementById('preview-card-num');
        const nameEl = document.getElementById('preview-card-name');
        const rarityEl = document.getElementById('preview-card-rarity');
        const holoEl = document.getElementById('preview-card-holo');
        const speciesEl = document.getElementById('preview-card-species');
        const rawLinkEl = document.getElementById('preview-card-open-raw');
        const imgEl = document.getElementById('preview-card-img');
        const errEl = document.getElementById('preview-card-img-error');

        if (numEl) numEl.textContent = num;
        if (nameEl) nameEl.textContent = name;
        if (rarityEl) {
            rarityEl.textContent = rarity;
            rarityEl.style.borderColor = color;
            rarityEl.style.color = color;
        }
        if (holoEl) holoEl.classList.toggle('hidden', !isHolo);
        if (speciesEl) speciesEl.textContent = species;
        if (rawLinkEl) rawLinkEl.href = imgUrl || '#';

        if (imgEl && errEl) {
            errEl.classList.add('hidden');
            imgEl.classList.remove('hidden');
            imgEl.alt = name;
            imgEl.onerror = () => {
                imgEl.classList.add('hidden');
                errEl.classList.remove('hidden');
            };
            if (!imgUrl) {
                imgEl.classList.add('hidden');
                errEl.classList.remove('hidden');
            } else {
                imgEl.src = imgUrl;
            }
        }

        window.openModal('modal-card-preview');
    };

    // ==================== 3. ĐỒNG BỘ CUSTOM SELECT KHÔNG RELOAD ====================
    function syncCustomSelectUI(selectEl) {
        if (!selectEl) return;
        // Gọi setter value custom trong common.js để đồng bộ trigger & menu mà KHÔNG fire sự kiện change (tránh redirect Set Switcher)
        selectEl.value = selectEl.value;
    }

    function upsertOptionInSelects(selectIds, value, label, autoSelectId) {
        const strVal = String(value);
        selectIds.forEach(id => {
            const sel = document.getElementById(id);
            if (!sel) return;
            let opt = Array.from(sel.options).find(o => o.value === strVal);
            if (!opt) {
                opt = document.createElement('option');
                opt.value = strVal;
                sel.appendChild(opt);
            }
            opt.textContent = label;
            if (id === autoSelectId) {
                sel.value = strVal;
            } else {
                syncCustomSelectUI(sel);
            }
        });
    }

    function removeOptionFromSelects(selectIds, value) {
        const strVal = String(value);
        selectIds.forEach(id => {
            const sel = document.getElementById(id);
            if (!sel) return;
            const opt = Array.from(sel.options).find(o => o.value === strVal);
            if (opt) opt.remove();
            syncCustomSelectUI(sel);
        });
    }

    function highlightElementBriefly(el) {
        if (!el) return;
        el.classList.add('bg-emerald-500/10');
        setTimeout(() => el.classList.remove('bg-emerald-500/10'), 1400);
    }

    // ==================== 4. CHUYỂN TAB (4 TAB) ====================
    window.switchAdminTab = function (tab) {
        const tabs = ['sets', 'rarities', 'cards', 'users'];
        if (!tabs.includes(tab)) tab = 'sets';
        tabs.forEach(t => {
            const panel = document.getElementById(`admin-panel-${t}`);
            const btn = document.getElementById(`admin-tab-btn-${t}`);
            const isActive = t === tab;
            panel?.classList.toggle('hidden', !isActive);
            if (btn) {
                btn.className = isActive
                    ? 'pk-btn-primary rounded-xl px-3.5 py-2.5 text-xs font-extrabold cursor-pointer transition text-center'
                    : 'rounded-xl px-3.5 py-2.5 text-xs font-bold pk-text-secondary hover:pk-text-primary cursor-pointer transition text-center';
            }
        });
        const setToolbar = document.getElementById('admin-set-toolbar');
        if (setToolbar) {
            setToolbar.classList.toggle('hidden', tab === 'users');
        }
        try {
            sessionStorage.setItem('pk_admin_active_tab', tab);
        } catch (e) {}
        if (tab === 'users' && !adminUsersLoaded) {
            adminUsersLoaded = true;
            window.loadAdminUsers(0);
        }
    };

    // ==================== 5. DOM BUILDERS CHO TAB 1, 2, 3, 4 ====================
    function buildCardSetRow(set, isCurrentActive) {
        const tr = document.createElement('tr');
        tr.id = `set-row-${set.id}`;
        if (isCurrentActive) tr.className = 'border-l-4 border-l-amber-500 bg-amber-500/[0.06]';
        const isActive = set.isActive !== false;
        const statusHtml = isActive
            ? '<span class="inline-flex items-center rounded-full bg-emerald-500/15 px-2.5 py-0.5 text-[11px] font-bold text-emerald-600 dark:text-emerald-400">Đang mở</span>'
            : '<span class="inline-flex items-center rounded-full bg-slate-500/20 px-2.5 py-0.5 text-[11px] font-bold pk-text-secondary">Đã ẩn</span>';

        tr.innerHTML = `
            <td class="px-4 py-3">
                <span class="inline-flex items-center rounded-lg bg-slate-900/90 dark:bg-slate-800 border border-amber-500/35 px-2 py-0.5 text-xs font-extrabold text-amber-400 tracking-wider">${esc(set.code)}</span>
            </td>
            <td class="px-4 py-3 font-bold pk-text-primary">
                <a href="/admin/dashboard?setId=${Number(set.id)}" class="hover:underline">${esc(set.name)}</a>
            </td>
            <td class="px-4 py-3 text-center pk-num tabular-nums font-bold pk-text-primary">${Number(set.baseSetTotal)}</td>
            <td class="px-4 py-3 text-center pk-num tabular-nums font-bold pk-text-primary">${Number(set.cardsPerPack)} thẻ / ${Number(set.packsPerBox)} gói</td>
            <td class="px-4 py-3 text-right pk-num tabular-nums text-xs whitespace-nowrap">
                <div class="font-extrabold text-amber-600 dark:text-amber-400">${fmtPrice(set.packPrice)}</div>
                <div class="font-semibold text-amber-600/80 dark:text-amber-400/80">${fmtPrice(set.boxPrice)}</div>
            </td>
            <td class="px-4 py-3 text-center whitespace-nowrap set-status-cell">${statusHtml}</td>
            <td class="px-4 py-3 text-center whitespace-nowrap">
                <div class="inline-flex items-center justify-center gap-2">
                    <button type="button"
                            data-set-id="${Number(set.id)}"
                            data-set-name="${esc(set.name)}"
                            data-active="${isActive}"
                            onclick="window.toggleCardSetVisibility(parseInt(this.getAttribute('data-set-id'), 10), this.getAttribute('data-set-name'), this.getAttribute('data-active') !== 'true')"
                            class="set-toggle-btn text-xs font-bold ${isActive ? 'text-rose-500' : 'text-emerald-500'} hover:underline cursor-pointer">
                        ${isActive ? 'Ẩn bộ' : 'Hiện lại'}
                    </button>
                </div>
            </td>
        `;
        return tr;
    }

    function updateCardSetsCountUI() {
        const tbody = document.getElementById('card-sets-tbody');
        const countEl = document.getElementById('card-sets-count');
        if (!tbody) return;
        const rows = tbody.querySelectorAll('tr[id^="set-row-"]');
        if (countEl) countEl.textContent = `${rows.length} bộ sưu tập`;
        if (rows.length === 0 && !document.getElementById('card-sets-empty-row')) {
            const emptyTr = document.createElement('tr');
            emptyTr.id = 'card-sets-empty-row';
            emptyTr.innerHTML = '<td colspan="7" class="px-4 py-8 text-center text-xs pk-text-secondary">Chưa có bộ sưu tập nào. Hãy bấm "+ Tạo Bộ Sưu Tập Mới" để bắt đầu!</td>';
            tbody.appendChild(emptyTr);
        }
    }

    function buildRarityCardElement(r) {
        const tr = document.createElement('tr');
        tr.id = `rarity-card-${r.id}`;
        tr.setAttribute('data-rarity-id', String(r.id));
        tr.setAttribute('data-drop-weight', String(Number(r.dropWeight || 0)));
        tr.className = 'rarity-card-item';
        const color = safeHex(r.colorHex, '#EC4899');
        const holoBadge = r.isHolographic
            ? '<span class="rounded-full bg-amber-500/20 px-2 py-0.5 text-[10px] font-extrabold text-amber-600 dark:text-amber-300 border border-amber-500/30">HOLO 3D</span>'
            : '';

        tr.innerHTML = `
            <td class="px-4 py-3">
                <div class="flex items-center gap-2">
                    <span class="h-3 w-3 rounded-full shrink-0" style="background-color:${color}"></span>
                    <span class="font-extrabold pk-text-primary">${esc(r.name)}</span>
                    ${holoBadge}
                </div>
            </td>
            <td class="px-4 py-3 text-left">
                <div class="flex items-center justify-start gap-3">
                    <div class="hidden sm:block w-32 h-1.5 rounded-full bg-slate-200/80 dark:bg-slate-800 overflow-hidden shrink-0">
                        <div class="rarity-bar h-full rounded-full transition-all duration-300"
                             data-rarity-id="${Number(r.id)}"
                             style="width: 0%; background-color:${color}"></div>
                    </div>
                    <span class="rarity-percent pk-num tabular-nums font-bold pk-text-primary w-14 text-right" data-rarity-id="${Number(r.id)}">0.00%</span>
                </div>
            </td>
            <td class="px-4 py-3 text-center">
                <input type="number" min="0" step="1" required
                       class="rarity-weight-input pk-input pk-num tabular-nums h-9 w-24 mx-auto rounded-xl px-3 text-center text-sm font-extrabold"
                       data-rarity-id="${Number(r.id)}"
                       data-initial-weight="${Number(r.dropWeight)}"
                       value="${Number(r.dropWeight)}"
                       oninput="window.recalculateRarityPercentages()"/>
            </td>
            <td class="px-4 py-3 text-center whitespace-nowrap">
                <div class="inline-flex items-center justify-center gap-3">
                    <button type="button"
                            data-id="${Number(r.id)}"
                            data-name="${esc(r.name)}"
                            data-weight="${Number(r.dropWeight)}"
                            data-color="${color}"
                            data-holo="${Boolean(r.isHolographic)}"
                            onclick="window.openRarityModal({id: parseInt(this.dataset.id,10), name: this.dataset.name, dropWeight: parseInt(this.dataset.weight,10), colorHex: this.dataset.color, isHolographic: this.dataset.holo === 'true'})"
                            class="rarity-edit-btn font-bold pk-text-primary hover:underline cursor-pointer">
                        Sửa
                    </button>
                    <button type="button"
                            data-id="${Number(r.id)}"
                            data-name="${esc(r.name)}"
                            onclick="window.deleteRarity(parseInt(this.dataset.id, 10), this.dataset.name)"
                            class="font-bold text-rose-500 hover:underline cursor-pointer">
                        Xóa
                    </button>
                </div>
            </td>
        `;
        return tr;
    }

    // Sắp xếp danh sách Độ hiếm (Tab 2) theo Trọng số từ cao xuống thấp
    function sortRaritiesListByWeightDesc() {
        const listEl = document.getElementById('rarities-list');
        if (!listEl) return;
        const cards = Array.from(listEl.querySelectorAll('.rarity-card-item'));
        cards.sort((a, b) => {
            const wA = parseInt(a.querySelector('.rarity-weight-input')?.value || a.getAttribute('data-drop-weight') || '0', 10) || 0;
            const wB = parseInt(b.querySelector('.rarity-weight-input')?.value || b.getAttribute('data-drop-weight') || '0', 10) || 0;
            if (wB !== wA) return wB - wA;
            const idA = parseInt(a.getAttribute('data-rarity-id') || '0', 10) || 0;
            const idB = parseInt(b.getAttribute('data-rarity-id') || '0', 10) || 0;
            return idA - idB;
        });
        cards.forEach(card => {
            const w = parseInt(card.querySelector('.rarity-weight-input')?.value || '0', 10) || 0;
            card.setAttribute('data-drop-weight', String(w));
            const editBtn = card.querySelector('button[data-weight]');
            if (editBtn) editBtn.dataset.weight = String(w);
            listEl.appendChild(card);
        });
    }

    // Đồng bộ màu sắc, tên độ hiếm & cờ Holo 3D cho TẤT CẢ các thẻ cũ đang hiển thị trong bảng Tab 3
    function syncExistingCardsForUpdatedRarity(rarity) {
        if (!rarity || !rarity.id) return;
        const color = safeHex(rarity.colorHex, '#10B981');
        const rows = document.querySelectorAll(`#card-templates-tbody tr[data-rarity-id="${Number(rarity.id)}"]`);
        rows.forEach(row => {
            const badge = row.querySelector('.card-rarity-badge');
            if (badge) {
                badge.textContent = rarity.name;
                badge.style.borderColor = color;
                badge.style.color = color;
            }
            row.querySelectorAll('.card-preview-trigger').forEach(btn => {
                btn.dataset.rarity = rarity.name;
                btn.dataset.color = color;
                btn.dataset.holo = String(Boolean(rarity.isHolographic));
            });
        });
    }

    function buildCardRowElement(c) {
        const tr = document.createElement('tr');
        tr.id = `card-row-${c.id}`;
        tr.setAttribute('data-card-number', String(c.cardNumber));
        tr.setAttribute('data-rarity-id', String(Number(c.rarityId)));
        const color = safeHex(c.rarityColorHex, '#10B981');
        const isActive = c.isActive !== false;
        const statusHtml = isActive
            ? '<span class="inline-flex items-center gap-1.5 text-[11px] font-bold text-emerald-600 dark:text-emerald-400"><span class="h-2 w-2 rounded-full bg-emerald-500"></span><span>Đang hiện</span></span>'
            : '<span class="inline-flex items-center gap-1.5 text-[11px] font-bold pk-text-secondary"><span class="h-2 w-2 rounded-full bg-slate-400"></span><span>Đã ẩn</span></span>';

        tr.innerHTML = `
            <td class="px-4 py-2.5 pk-num font-bold text-slate-700 dark:text-slate-200 whitespace-nowrap">${esc(c.formattedCardNumber)}</td>
            <td class="px-4 py-2.5">
                <button type="button"
                        onclick="window.openCardPreviewModal(this)"
                        data-name="${esc(c.name)}"
                        data-num="${esc(c.formattedCardNumber)}"
                        data-img="${esc(c.imageUrl)}"
                        data-rarity="${esc(c.rarityName)}"
                        data-color="${color}"
                        data-species="${esc(c.speciesName)}"
                        data-holo="${Boolean(c.rarityIsHolographic)}"
                        class="card-preview-trigger group inline-flex items-center gap-2.5 text-left font-bold pk-text-primary hover:text-amber-500 cursor-pointer transition">
                    <img src="${esc(c.imageUrl)}" alt="" class="w-6 h-[34px] rounded object-cover shrink-0 bg-slate-800/30 transition-transform duration-200 group-hover:scale-150 group-hover:z-20 group-hover:shadow-lg"/>
                    <span>${esc(c.name)}</span>
                </button>
            </td>
            <td class="px-4 py-2.5 whitespace-nowrap">
                <span class="card-rarity-badge pk-rarity-badge px-2.5 py-0.5 text-xs"
                      style="border-color:${color}; color:${color}">${esc(c.rarityName)}</span>
            </td>
            <td class="px-4 py-2.5 font-semibold pk-text-secondary">${esc(c.speciesName)}</td>
            <td class="px-4 py-2.5 text-center whitespace-nowrap card-status-cell">${statusHtml}</td>
            <td class="px-4 py-2.5 text-center whitespace-nowrap">
                <div class="inline-flex items-center justify-center gap-3">
                    <button type="button"
                            data-id="${Number(c.id)}"
                            data-name="${esc(c.name)}"
                            data-num="${Number(c.cardNumber)}"
                            data-img="${esc(c.imageUrl)}"
                            data-rarity="${Number(c.rarityId)}"
                            data-species="${Number(c.speciesId)}"
                            onclick="window.openEditCardModal(this)"
                            class="font-bold pk-text-primary hover:underline cursor-pointer">
                        Sửa
                    </button>
                    <button type="button"
                            data-id="${Number(c.id)}"
                            data-name="${esc(c.name)}"
                            data-active="${isActive}"
                            onclick="window.toggleCardTemplateVisibility(parseInt(this.dataset.id, 10), this.dataset.name, this.dataset.active !== 'true')"
                            class="card-toggle-btn font-bold ${isActive ? 'text-rose-500' : 'text-emerald-500'} hover:underline cursor-pointer">
                        ${isActive ? 'Ẩn thẻ' : 'Hiện lại'}
                    </button>
                </div>
            </td>
        `;
        return tr;
    }

    function renderCardTemplatesPagination(targetPage) {
        const tbody = document.getElementById('card-templates-tbody');
        const bar = document.getElementById('cards-pagination-bar');
        const infoEl = document.getElementById('cards-pagination-info');
        const controlsEl = document.getElementById('cards-pagination-controls');
        if (!tbody) return;

        const searchQ = (document.getElementById('admin-cards-search')?.value || '').trim().toLowerCase().replace(/^#/, '');
        const rarityFilter = (document.getElementById('admin-cards-filter-rarity')?.value || '').trim();

        const allRows = Array.from(tbody.querySelectorAll('tr[id^="card-row-"]'));
        const matchedRows = allRows.filter(row => {
            const cNum = (row.getAttribute('data-card-number') || '').padStart(3, '0');
            const cRarityId = row.getAttribute('data-rarity-id') || '';
            const trigger = row.querySelector('.card-preview-trigger');
            const cName = (trigger?.dataset?.name || trigger?.textContent || '').toLowerCase();
            const cFormattedNum = (trigger?.dataset?.num || '').toLowerCase();

            const matchSearch = !searchQ || cName.includes(searchQ) || cNum.includes(searchQ) || cFormattedNum.includes(searchQ);
            const matchRarity = !rarityFilter || cRarityId === rarityFilter;
            return matchSearch && matchRarity;
        });

        const total = matchedRows.length;
        if (allRows.length === 0 || total === 0) {
            allRows.forEach(r => { r.style.display = 'none'; });
            bar?.classList.add('hidden');
            return;
        }

        const totalPages = Math.max(1, Math.ceil(total / CARDS_PER_PAGE));
        if (typeof targetPage === 'number' && !Number.isNaN(targetPage)) {
            cardTemplatesPage = targetPage;
        }
        cardTemplatesPage = Math.max(1, Math.min(cardTemplatesPage, totalPages));

        const startIdx = (cardTemplatesPage - 1) * CARDS_PER_PAGE;
        const endIdx = Math.min(startIdx + CARDS_PER_PAGE, total);

        const visibleSet = new Set(matchedRows.slice(startIdx, endIdx));
        allRows.forEach(row => {
            row.style.display = visibleSet.has(row) ? '' : 'none';
        });

        bar?.classList.remove('hidden');
        if (infoEl) {
            infoEl.textContent = `${startIdx + 1}–${endIdx} / ${total}`;
        }

        if (controlsEl) {
            const buttons = [];
            const prevDisabled = cardTemplatesPage <= 1 ? 'disabled' : '';
            const nextDisabled = cardTemplatesPage >= totalPages ? 'disabled' : '';

            buttons.push(
                `<button type="button" onclick="window.goToCardTemplatesPage(${cardTemplatesPage - 1})" ${prevDisabled}
                         class="pk-surface-muted rounded-xl px-3 py-1.5 text-xs font-bold pk-text-primary disabled:opacity-40 cursor-pointer disabled:cursor-not-allowed">← Trước</button>`
            );

            // Tạo dãy số trang gọn gàng (tối đa 7 nút quanh trang hiện tại)
            const pagesToShow = new Set([1, totalPages]);
            for (let p = Math.max(1, cardTemplatesPage - 2); p <= Math.min(totalPages, cardTemplatesPage + 2); p++) {
                pagesToShow.add(p);
            }
            const sortedPages = Array.from(pagesToShow).sort((a, b) => a - b);
            let prevP = 0;
            sortedPages.forEach(p => {
                if (prevP && p - prevP > 1) {
                    buttons.push('<span class="px-1 text-xs pk-text-secondary select-none">…</span>');
                }
                const cls = p === cardTemplatesPage
                    ? 'pk-btn-primary rounded-xl px-3 py-1.5 text-xs font-extrabold cursor-pointer pk-num'
                    : 'pk-surface-muted rounded-xl px-3 py-1.5 text-xs font-bold pk-text-primary hover:opacity-80 cursor-pointer pk-num';
                buttons.push(`<button type="button" onclick="window.goToCardTemplatesPage(${p})" class="${cls}">${p}</button>`);
                prevP = p;
            });

            buttons.push(
                `<button type="button" onclick="window.goToCardTemplatesPage(${cardTemplatesPage + 1})" ${nextDisabled}
                         class="pk-surface-muted rounded-xl px-3 py-1.5 text-xs font-bold pk-text-primary disabled:opacity-40 cursor-pointer disabled:cursor-not-allowed">Sau →</button>`
            );

            controlsEl.innerHTML = buttons.join('');
        }
    }

    window.filterAdminCardsTable = function () {
        renderCardTemplatesPagination(1);
    };

    window.goToCardTemplatesPage = function (page) {
        renderCardTemplatesPagination(page);
    };

    function insertCardRowSorted(rowEl, cardNumber) {
        const tbody = document.getElementById('card-templates-tbody');
        if (!tbody) return;
        document.getElementById('card-templates-empty-row')?.remove();

        const targetNum = Number(cardNumber);
        const existingRows = Array.from(tbody.querySelectorAll('tr[data-card-number]'));
        const nextRow = existingRows.find(r => Number(r.getAttribute('data-card-number')) > targetNum);
        if (nextRow) {
            tbody.insertBefore(rowEl, nextRow);
        } else {
            tbody.appendChild(rowEl);
        }

        const allRows = Array.from(tbody.querySelectorAll('tr[id^="card-row-"]'));
        const insertedIdx = allRows.indexOf(rowEl);
        const pageOfInserted = insertedIdx >= 0 ? Math.floor(insertedIdx / CARDS_PER_PAGE) + 1 : 1;

        updateCardTemplatesCountUI(pageOfInserted);
        highlightElementBriefly(rowEl);
    }

    function updateCardTemplatesCountUI(targetPage) {
        const tbody = document.getElementById('card-templates-tbody');
        const countEl = document.getElementById('card-templates-count');
        if (!tbody) return;
        const rows = tbody.querySelectorAll('tr[id^="card-row-"]');
        if (countEl) countEl.textContent = `${rows.length} thẻ`;
        if (rows.length === 0 && !document.getElementById('card-templates-empty-row')) {
            const emptyTr = document.createElement('tr');
            emptyTr.id = 'card-templates-empty-row';
            emptyTr.innerHTML = '<td colspan="6" class="px-4 py-8 text-center text-xs pk-text-secondary">Bộ sưu tập này chưa có thẻ bài nào. Hãy bấm "+ Thêm thẻ bài" để thêm thẻ đầu tiên!</td>';
            tbody.appendChild(emptyTr);
        }
        renderCardTemplatesPagination(targetPage);
    }

    function buildBoosterBoxRowElement(box) {
        const div = document.createElement('div');
        div.id = `box-row-${box.id}`;
        const isSealed = box.status === 'AVAILABLE' && Number(box.remainingPacks) === Number(box.totalPacks);
        div.setAttribute('data-box-code', box.boxCode);
        div.setAttribute('data-sealed', String(isSealed));
        div.className = 'box-list-item p-3.5 flex flex-wrap items-center justify-between gap-2 text-xs';

        const statusBadge = isSealed
            ? '<span class="rounded-full bg-emerald-500/15 px-2.5 py-0.5 text-[10px] font-bold text-emerald-600 whitespace-nowrap">Nguyên seal</span>'
            : `<span class="rounded-full pk-surface px-2.5 py-0.5 text-[10px] font-semibold pk-text-secondary whitespace-nowrap">${box.status === 'SOLD_OUT' ? 'Đã bán hết' : 'Đang bán lẻ'}</span>`;

        const deleteBtn = isSealed
            ? `<button type="button"
                       data-box-id="${Number(box.id)}"
                       data-box-code="${esc(box.boxCode)}"
                       onclick="window.deleteSingleSealedBox(parseInt(this.dataset.boxId, 10), this.dataset.boxCode)"
                       class="pk-surface rounded-lg px-2.5 py-1 text-[11px] font-bold text-rose-600 hover:opacity-80 cursor-pointer whitespace-nowrap">
                   Xóa / Thu hồi
               </button>`
            : '';

        div.innerHTML = `
            <div>
                <div class="pk-num font-bold pk-text-primary">${esc(box.boxCode)}</div>
                <div class="pk-num text-[11px] pk-text-secondary">
                    <span>${Number(box.snapshotCardsPerPack)} thẻ/gói</span>
                    • Còn <strong class="pk-text-primary">${Number(box.remainingPacks)}/${Number(box.totalPacks)}</strong> gói
                </div>
            </div>
            <div class="flex items-center gap-2">
                ${statusBadge}
                ${deleteBtn}
            </div>
        `;
        return div;
    }

    function updateBoxCountsUI() {
        const listEl = document.getElementById('booster-boxes-list');
        if (!listEl) return;
        const items = listEl.querySelectorAll('.box-list-item');
        const sealedItems = listEl.querySelectorAll('.box-list-item[data-sealed="true"]');
        const totalEl = document.getElementById('total-box-count');
        const sealedEl = document.getElementById('sealed-box-count');
        if (totalEl) totalEl.textContent = `${items.length} hộp`;
        if (sealedEl) sealedEl.textContent = `${sealedItems.length} hộp`;

        const reduceInput = document.getElementById('reduce-box-quantity');
        const reduceBtn = document.getElementById('btn-reduce-box-submit');
        const noSealed = sealedItems.length === 0;
        if (reduceInput) reduceInput.disabled = noSealed;
        if (reduceBtn) reduceBtn.disabled = noSealed;
        if (items.length === 0 && !document.getElementById('booster-boxes-empty-row')) {
            const emptyDiv = document.createElement('div');
            emptyDiv.id = 'booster-boxes-empty-row';
            emptyDiv.className = 'p-6 text-center text-xs pk-text-secondary';
            emptyDiv.textContent = 'Bộ sưu tập này chưa có hộp bài nào. Hãy phát hành lô hộp bài đầu tiên!';
            listEl.appendChild(emptyDiv);
        }
    }

    // ==================== 6. TÍNH TỶ LỆ ĐỘ HIẾM, PREVIEW SỐ THẺ & UPLOAD CLOUDINARY ====================
    window.recalculateRarityPercentages = function () {
        const inputs = Array.from(document.querySelectorAll('.rarity-weight-input'));
        const total = inputs.reduce((acc, el) => acc + (parseInt(el.value || '0', 10) || 0), 0);
        const totalEl = document.getElementById('total-weight-display');
        const totalPctEl = document.getElementById('total-percent-display');
        if (totalEl) totalEl.textContent = total;
        if (totalPctEl) totalPctEl.textContent = total > 0 ? '100.00%' : '0.00%';

        let hasChanged = false;
        inputs.forEach(input => {
            const rarityId = input.getAttribute('data-rarity-id');
            const weight = parseInt(input.value || '0', 10) || 0;
            const initialWeight = parseInt(input.getAttribute('data-initial-weight') ?? String(weight), 10);
            if (weight !== initialWeight) hasChanged = true;
            const pctNum = total > 0 ? (weight * 100) / total : 0;
            const label = document.querySelector(`.rarity-percent[data-rarity-id="${rarityId}"]`);
            const bar = document.querySelector(`.rarity-bar[data-rarity-id="${rarityId}"]`);
            if (label) label.textContent = pctNum.toFixed(2) + '%';
            if (bar) bar.style.width = pctNum.toFixed(2) + '%';
        });

        const saveBtn = document.getElementById('btn-save-rarity-weights');
        if (saveBtn) saveBtn.disabled = !hasChanged;
    };

    window.updateFormattedNumberPreview = function () {
        const cardNum = parseInt(document.getElementById('card-number')?.value || '0', 10);
        const baseTotal = parseInt(document.getElementById('card-base-total')?.value || '103', 10);
        const previewEl = document.getElementById('formatted-number-preview');
        if (!previewEl) return;
        if (!cardNum || cardNum < 1) {
            previewEl.textContent = '--- / ' + String(baseTotal).padStart(3, '0');
            return;
        }
        const formatted = String(cardNum).padStart(3, '0') + ' / ' + String(baseTotal).padStart(3, '0');
        previewEl.innerHTML = cardNum > baseTotal
            ? `<span>${formatted}</span><span class="text-amber-400 ml-1">★</span>`
            : formatted;
    };

    window.uploadSelectedImageToCloudinary = async function (fileInputId, urlInputId, cardNumberInputId) {
        const fileInput = document.getElementById(fileInputId);
        const setCode = document.getElementById('card-set-code')?.value || 'DLR';
        const cardNumber = document.getElementById(cardNumberInputId)?.value || 1;
        if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
            window.showToast('error', 'Chưa chọn file ảnh', 'Vui lòng chọn file ảnh thẻ bài (PNG, JPG, WEBP).');
            return;
        }
        const formData = new FormData();
        formData.append('file', fileInput.files[0]);
        formData.append('setCode', setCode);
        formData.append('cardNumber', cardNumber);

        const res = await fetch('/api/v1/admin/cards/upload-image', { method: 'POST', body: formData });
        const data = await res.json();
        if (!res.ok) {
            window.showToast('error', 'Không thể tải ảnh', data.message || 'Vui lòng thử lại.');
            return;
        }
        document.getElementById(urlInputId).value = data.imageUrl;
        window.showToast('success', 'Tải ảnh thành công', 'Đã cập nhật đường dẫn hình ảnh thẻ bài.');
    };

    // ==================== 7. AJAX ẨN / HIỆN & XÓA HANDLERS (TAB 1, 2, 3, 4 — KHÔNG RELOAD) ====================
    window.toggleCardSetVisibility = async function (setId, setName, nextActive) {
        const confirmMsg = nextActive
            ? `Xác nhận mở bán hiện lại bộ sưu tập "${setName}" trên Cửa hàng?`
            : `Xác nhận ẩn bộ sưu tập "${setName}" khỏi Cửa hàng?`;
        if (!confirm(confirmMsg)) return;

        const res = await fetch(`/api/v1/admin/card-sets/${setId}/active`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ active: nextActive })
        });
        const data = await res.json();
        if (!res.ok) {
            window.showToast('error', 'Không thể cập nhật bộ sưu tập', data.message || 'Vui lòng thử lại.');
            return;
        }
        window.showToast('success', nextActive ? 'Đã hiện lại bộ sưu tập' : 'Đã ẩn bộ sưu tập',
            nextActive ? `Bộ "${setName}" đã mở bán trở lại.` : `Bộ "${setName}" đã được ẩn khỏi Cửa hàng.`);

        const row = document.getElementById(`set-row-${setId}`);
        const switcher = document.getElementById('admin-set-switcher');
        const statusCell = row?.querySelector('.set-status-cell');
        if (statusCell) {
            statusCell.innerHTML = nextActive
                ? '<span class="inline-flex items-center rounded-full bg-emerald-500/15 px-2.5 py-0.5 text-[11px] font-bold text-emerald-600 dark:text-emerald-400">Đang mở</span>'
                : '<span class="inline-flex items-center rounded-full bg-slate-500/20 px-2.5 py-0.5 text-[11px] font-bold pk-text-secondary">Đã ẩn</span>';
        }
        const toggleBtn = row?.querySelector('.set-toggle-btn');
        if (toggleBtn) {
            toggleBtn.setAttribute('data-active', String(nextActive));
            toggleBtn.textContent = nextActive ? 'Ẩn bộ' : 'Hiện lại';
            toggleBtn.className = `set-toggle-btn text-xs font-bold ${nextActive ? 'text-rose-500' : 'text-emerald-500'} hover:underline cursor-pointer`;
        }
        if (switcher) {
            const opt = Array.from(switcher.options).find(o => o.value === String(setId));
            if (opt) {
                const cleanText = opt.textContent.replace(/\s*\[Đã ẩn\]\s*$/, '').trim();
                opt.textContent = nextActive ? cleanText : `${cleanText} [Đã ẩn]`;
                syncCustomSelectUI(switcher);
            }
        }
        const activeConfigId = document.getElementById('config-set-id')?.value;
        if (activeConfigId === String(setId)) {
            const cb = document.getElementById('config-set-active');
            if (cb) cb.checked = nextActive;
        }
        highlightElementBriefly(row);
    };

    window.deleteCardSet = window.toggleCardSetVisibility;

    window.deleteRarity = async function (rarityId, rarityName) {
        if (!confirm(`Xác nhận xóa độ hiếm "${rarityName}"?`)) return;
        const res = await fetch(`/api/v1/admin/rarities/${rarityId}`, { method: 'DELETE' });
        const data = await res.json();
        if (!res.ok) {
            window.showToast('error', 'Không thể xóa độ hiếm', data.message || 'Vui lòng thử lại.');
            return;
        }
        window.showToast('success', 'Đã xóa độ hiếm', data.message);
        document.getElementById(`rarity-card-${rarityId}`)?.remove();
        window.recalculateRarityPercentages();
        removeOptionFromSelects(['card-rarity-id', 'edit-card-rarity-id'], rarityId);

        const listEl = document.getElementById('rarities-list');
        if (listEl && listEl.querySelectorAll('[id^="rarity-card-"]').length === 0 && !document.getElementById('rarities-empty-row')) {
            const emptyTr = document.createElement('tr');
            emptyTr.id = 'rarities-empty-row';
            emptyTr.innerHTML = '<td colspan="4" class="px-4 py-6 text-center text-xs pk-text-secondary">Chưa có độ hiếm nào. Hãy bấm "+ Thêm Độ Hiếm" để khởi tạo!</td>';
            listEl.appendChild(emptyTr);
        }
    };

    window.toggleCardTemplateVisibility = async function (cardId, cardName, nextActive) {
        const confirmMsg = nextActive
            ? `Xác nhận hiện lại thẻ bài "${cardName}" trong danh sách Gacha?`
            : `Xác nhận ẩn thẻ bài "${cardName}" khỏi Gacha? (Các bản sao người chơi đã sở hữu vẫn được giữ nguyên trong Kho đồ)`;
        if (!confirm(confirmMsg)) return;
        const setId = document.getElementById('card-set-id').value;
        const res = await fetch(`/api/v1/admin/card-sets/${setId}/cards/${cardId}/active`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ active: nextActive })
        });
        const data = await res.json();
        if (!res.ok) {
            window.showToast('error', 'Không thể cập nhật trạng thái thẻ', data.message || 'Vui lòng thử lại.');
            return;
        }
        window.showToast('success', nextActive ? 'Đã hiện lại thẻ bài' : 'Đã ẩn thẻ bài',
            nextActive ? `Thẻ "${cardName}" đã xuất hiện trở lại trong Gacha.` : `Thẻ "${cardName}" đã được ẩn khỏi Gacha.`);

        const row = document.getElementById(`card-row-${cardId}`);
        const statusCell = row?.querySelector('.card-status-cell');
        if (statusCell) {
            statusCell.innerHTML = nextActive
                ? '<span class="inline-flex items-center gap-1.5 text-[11px] font-bold text-emerald-600 dark:text-emerald-400"><span class="h-2 w-2 rounded-full bg-emerald-500"></span><span>Đang hiện</span></span>'
                : '<span class="inline-flex items-center gap-1.5 text-[11px] font-bold pk-text-secondary"><span class="h-2 w-2 rounded-full bg-slate-400"></span><span>Đã ẩn</span></span>';
        }
        const toggleBtn = row?.querySelector('.card-toggle-btn');
        if (toggleBtn) {
            toggleBtn.dataset.active = String(nextActive);
            toggleBtn.textContent = nextActive ? 'Ẩn thẻ' : 'Hiện lại';
            toggleBtn.className = `card-toggle-btn font-bold ${nextActive ? 'text-rose-500' : 'text-emerald-500'} hover:underline cursor-pointer`;
        }
        highlightElementBriefly(row);
    };

    window.deleteCardTemplate = window.toggleCardTemplateVisibility;

    window.resetSetDailySales = async function (setId) {
        if (!confirm('Xác nhận reset toàn bộ lượt bán Pack & Box hôm nay của bộ này về 0? Thao tác này sẽ khôi phục đầy đủ hạn mức bán trong ngày.')) return;
        const res = await fetch(`/api/v1/admin/card-sets/${setId}/reset-daily-sales`, { method: 'POST' });
        const data = await res.json();
        if (!res.ok) {
            window.showToast('error', 'Không thể reset hạn mức', data.message || 'Vui lòng thử lại.');
            return;
        }
        const pEl = document.getElementById('admin-packs-sold-today');
        const bEl = document.getElementById('admin-boxes-sold-today');
        if (pEl) pEl.textContent = '0';
        if (bEl) bEl.textContent = '0';
        window.showToast('success', 'Đã reset hạn mức hôm nay!', data.message);
    };

    window.deleteSingleSealedBox = async function (boxId, boxCode) {
        if (!confirm(`Xác nhận thu hồi và xóa hộp nguyên seal "${boxCode}"?`)) return;
        const res = await fetch(`/api/v1/admin/booster-boxes/${boxId}`, { method: 'DELETE' });
        const data = await res.json();
        if (!res.ok) {
            window.showToast('error', 'Không thể thu hồi hộp', data.message || 'Vui lòng thử lại.');
            return;
        }
        window.showToast('success', 'Đã thu hồi hộp bài', data.message);
        document.getElementById(`box-row-${boxId}`)?.remove();
        updateBoxCountsUI();
    };

    // ==================== 8. TAB 5 — QUẢN LÝ NGƯỜI DÙNG (100% AJAX) ====================
    function buildAdminUserRow(u) {
        const tr = document.createElement('tr');
        tr.id = `user-row-${u.id}`;
        const isSelf = Number(u.id) === Number(window.CURRENT_USER_ID);
        const isAdmin = u.role === 'ROLE_ADMIN';
        const isLocked = Boolean(u.isLocked);

        const roleBadge = isAdmin
            ? '<span class="rounded-full bg-rose-500/15 px-2.5 py-0.5 text-[11px] font-extrabold text-rose-600 dark:text-amber-400">ADMIN</span>'
            : '<span class="rounded-full pk-surface px-2.5 py-0.5 text-[11px] font-bold pk-text-secondary">USER</span>';

        const statusBadge = isLocked
            ? '<span class="rounded-full bg-rose-500/15 px-2.5 py-0.5 text-[11px] font-bold text-rose-600">Đã khóa</span>'
            : '<span class="rounded-full bg-emerald-500/15 px-2.5 py-0.5 text-[11px] font-bold text-emerald-600">Hoạt động</span>';

        const selfBadge = isSelf
            ? '<span class="ml-1.5 rounded-full bg-amber-500/20 px-1.5 py-0.5 text-[9px] font-extrabold text-amber-600">Bạn</span>'
            : '';

        const lockBtn = isSelf
            ? '<span title="Bạn không thể tự khóa tài khoản của chính mình" class="inline-block cursor-not-allowed"><button type="button" disabled class="pointer-events-none pk-surface rounded-lg px-2.5 py-1 text-[11px] font-bold pk-text-muted opacity-45">Khóa</button></span>'
            : `<button type="button"
                       onclick="window.toggleUserLock(${Number(u.id)}, ${!isLocked})"
                       class="pk-surface rounded-lg px-2.5 py-1 text-[11px] font-bold ${isLocked ? 'text-emerald-600' : 'text-rose-600'} hover:opacity-80 cursor-pointer whitespace-nowrap">
                   ${isLocked ? 'Mở khóa' : 'Khóa'}
               </button>`;

        const nextRole = isAdmin ? 'ROLE_USER' : 'ROLE_ADMIN';
        const roleBtn = isSelf
            ? '<span title="Bạn không thể tự hạ quyền tài khoản của chính mình" class="inline-block cursor-not-allowed"><button type="button" disabled class="pointer-events-none pk-surface rounded-lg px-2.5 py-1 text-[11px] font-bold pk-text-muted opacity-45">Đổi quyền</button></span>'
            : `<button type="button"
                       onclick="window.toggleUserRole(${Number(u.id)}, '${nextRole}')"
                       class="pk-surface rounded-lg px-2.5 py-1 text-[11px] font-bold pk-text-primary hover:opacity-80 cursor-pointer whitespace-nowrap">
                   ${isAdmin ? 'Hạ USER' : 'Lên ADMIN'}
               </button>`;

        const lockedAmount = Number(u.lockedBalance || 0);
        const lockedHtml = lockedAmount > 0
            ? `<div class="text-[11px] pk-text-secondary">Cọc: ${window.formatVND(lockedAmount)}</div>`
            : '';

        tr.innerHTML = `
            <td class="px-4 py-3">
                <div class="font-extrabold pk-text-primary">${esc(u.username)}${selfBadge}</div>
                <div class="text-[11px] pk-text-secondary">${esc(u.email)}</div>
            </td>
            <td class="px-4 py-3 text-center whitespace-nowrap">${roleBadge}</td>
            <td class="px-4 py-3 text-right pk-num text-xs whitespace-nowrap">
                <div class="font-extrabold text-amber-600 dark:text-amber-400">${window.formatVND(u.balance)}</div>
                ${lockedHtml}
            </td>
            <td class="px-4 py-3 text-center pk-num font-bold pk-text-primary">${Number(u.totalCardsOwned || 0)}</td>
            <td class="px-4 py-3 text-center whitespace-nowrap">${statusBadge}</td>
            <td class="px-4 py-3 text-center whitespace-nowrap">
                <div class="inline-flex items-center justify-center gap-1.5">
                    ${lockBtn}
                    ${roleBtn}
                    <button type="button"
                            data-id="${Number(u.id)}"
                            data-username="${esc(u.username)}"
                            data-balance="${Number(u.balance || 0)}"
                            data-locked-balance="${lockedAmount}"
                            onclick="window.openUserWalletModal(parseInt(this.dataset.id,10), this.dataset.username, parseFloat(this.dataset.balance), parseFloat(this.dataset.lockedBalance))"
                            class="pk-surface inline-flex items-center gap-1 rounded-lg px-2.5 py-1 text-[11px] font-bold text-amber-600 dark:text-amber-400 hover:opacity-80 cursor-pointer whitespace-nowrap">
                        <svg class="h-3 w-3 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1"/><path d="M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4"/></svg>
                        <span>± Ví</span>
                    </button>
                </div>
            </td>
        `;
        return tr;
    }

    window.loadAdminUsers = async function (page = 0) {
        const keyword = (document.getElementById('user-search-keyword')?.value || '').trim();
        const role = document.getElementById('user-filter-role')?.value || '';
        const locked = document.getElementById('user-filter-locked')?.value || '';

        const params = new URLSearchParams();
        params.set('page', String(Math.max(0, page)));
        params.set('size', '20');
        if (keyword) params.set('keyword', keyword);
        if (role) params.set('role', role);
        if (locked) params.set('locked', locked);

        const tbody = document.getElementById('admin-users-tbody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="6" class="px-4 py-8 text-center text-xs pk-text-secondary">Đang tải danh sách người dùng...</td></tr>';
        }

        try {
            const res = await fetch(`/api/v1/admin/users?${params.toString()}`);
            const data = await res.json();
            if (!res.ok) {
                window.showToast('error', 'Không thể tải danh sách người dùng', data.message || 'Vui lòng thử lại.');
                return;
            }

            adminUsersPage = data.number || 0;
            adminUsersTotalPages = Math.max(1, data.totalPages || 1);
            const totalElements = data.totalElements || 0;
            const users = data.content || [];

            const totalEl = document.getElementById('admin-users-total');
            if (totalEl) totalEl.textContent = `${totalElements} người dùng`;

            const paginationBar = document.getElementById('users-pagination-bar');
            if (paginationBar) {
                paginationBar.classList.toggle('hidden', adminUsersTotalPages <= 1);
            }

            const pageInfoEl = document.getElementById('users-pagination-info');
            if (pageInfoEl) pageInfoEl.textContent = `Trang ${adminUsersPage + 1} / ${adminUsersTotalPages}`;

            const prevBtn = document.getElementById('btn-users-prev');
            const nextBtn = document.getElementById('btn-users-next');
            if (prevBtn) prevBtn.disabled = adminUsersPage <= 0;
            if (nextBtn) nextBtn.disabled = adminUsersPage >= adminUsersTotalPages - 1;

            if (tbody) {
                tbody.innerHTML = '';
                if (users.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="6" class="px-4 py-8 text-center text-xs pk-text-secondary">Không tìm thấy tài khoản nào phù hợp với bộ lọc.</td></tr>';
                } else {
                    users.forEach(u => tbody.appendChild(buildAdminUserRow(u)));
                }
            }
        } catch (err) {
            window.showToast('error', 'Lỗi kết nối', err.message);
        }
    };

    window.changeAdminUsersPage = function (delta) {
        const next = adminUsersPage + delta;
        if (next < 0 || next >= adminUsersTotalPages) return;
        window.loadAdminUsers(next);
    };

    window.toggleUserLock = async function (userId, nextLocked) {
        const label = nextLocked ? 'khóa' : 'mở khóa';
        if (!confirm(`Xác nhận ${label} tài khoản người dùng #${userId}?`)) return;
        const res = await fetch(`/api/v1/admin/users/${userId}/lock`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ locked: nextLocked })
        });
        const data = await res.json();
        if (!res.ok) {
            window.showToast('error', 'Thao tác thất bại', data.message || 'Không thể cập nhật trạng thái khóa.');
            return;
        }
        const existingRow = document.getElementById(`user-row-${userId}`);
        if (existingRow) {
            const updatedRow = buildAdminUserRow(data);
            existingRow.replaceWith(updatedRow);
            highlightElementBriefly(updatedRow);
        }
        window.showToast('success', 'Đã cập nhật trạng thái',
            `Tài khoản "${data.username}" đã được ${data.isLocked ? 'khóa' : 'mở khóa'}.`);
    };

    window.toggleUserRole = async function (userId, nextRole) {
        const label = nextRole === 'ROLE_ADMIN' ? 'Quản trị viên (ADMIN)' : 'Người chơi (USER)';
        if (!confirm(`Xác nhận chuyển quyền tài khoản #${userId} sang ${label}?`)) return;
        const res = await fetch(`/api/v1/admin/users/${userId}/role`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ role: nextRole })
        });
        const data = await res.json();
        if (!res.ok) {
            window.showToast('error', 'Không thể đổi quyền', data.message || 'Vui lòng thử lại.');
            return;
        }
        const existingRow = document.getElementById(`user-row-${userId}`);
        if (existingRow) {
            const updatedRow = buildAdminUserRow(data);
            existingRow.replaceWith(updatedRow);
            highlightElementBriefly(updatedRow);
        }
        window.showToast('success', 'Đã đổi quyền thành công', `Tài khoản "${data.username}" hiện có quyền ${data.role}.`);
    };

    function buildUserTransactionRow(tx) {
        const div = document.createElement('div');
        div.className = 'p-3 flex flex-wrap items-start justify-between gap-2';
        const timeStr = tx.createdAt ? String(tx.createdAt).replace('T', ' ').substring(0, 19) : '---';
        const isSettlement = tx.type === 'AUCTION_SETTLEMENT';
        const isDebit = ['PURCHASE_PACK', 'PURCHASE_BOX', 'AUCTION_BID_HOLD', 'MARKET_PURCHASE'].includes(tx.type)
            || (tx.type === 'ADMIN_ADJUSTMENT' && (tx.description || '').includes('Trừ bù'));

        let amountClass = 'text-emerald-600 dark:text-emerald-400';
        let amountText = `+${window.formatVND(tx.amount)}`;
        if (isSettlement) {
            amountClass = 'text-slate-500 dark:text-slate-300';
            amountText = `${window.formatVND(tx.amount)} (từ cọc)`;
        } else if (isDebit) {
            amountClass = 'text-rose-600 dark:text-rose-400';
            amountText = `-${window.formatVND(tx.amount)}`;
        }

        div.innerHTML = `
            <div class="min-w-0 flex-1 space-y-0.5">
                <div class="flex flex-wrap items-center gap-1.5">
                    <span class="rounded-md pk-surface px-1.5 py-0.5 text-[10px] font-bold pk-text-primary">${esc(tx.type)}</span>
                    <span class="pk-num text-[10px] pk-text-secondary">${esc(timeStr)}</span>
                    <span class="text-[10px] font-bold ${tx.status === 'SUCCESS' ? 'text-emerald-600 dark:text-emerald-400' : 'text-amber-600 dark:text-amber-400'}">${esc(tx.status)}</span>
                </div>
                <div class="text-xs pk-text-primary leading-snug break-words">${esc(tx.description || tx.vnpTxnRef)}</div>
            </div>
            <div class="text-right pk-num tabular-nums shrink-0">
                <div class="text-xs font-extrabold ${amountClass}">${amountText}</div>
                <div class="text-[10px] pk-text-secondary">Sau GD: ${window.formatVND(tx.balanceAfter)}</div>
            </div>
        `;
        return div;
    }

    window.openUserWalletModal = async function (userId, username, balance, lockedBalance) {
        document.getElementById('adjust-wallet-user-id').value = String(userId);
        document.getElementById('wallet-modal-username').textContent = username;
        document.getElementById('wallet-modal-balance').textContent = window.formatVND(balance);
        document.getElementById('wallet-modal-locked').textContent = window.formatVND(lockedBalance);
        document.getElementById('adjust-wallet-amount').value = '';
        document.getElementById('adjust-wallet-reason').value = '';
        window.validateAdjustWalletForm();

        const listEl = document.getElementById('user-transactions-list');
        const countEl = document.getElementById('wallet-modal-tx-count');
        if (listEl) listEl.innerHTML = '<div class="p-4 text-center pk-text-secondary">Đang tải lịch sử giao dịch...</div>';
        if (countEl) countEl.textContent = '...';

        window.openModal('modal-user-wallet');

        try {
            const res = await fetch(`/api/v1/admin/users/${userId}/transactions?limit=50`);
            const txList = await res.json();
            if (!res.ok) {
                if (listEl) listEl.innerHTML = `<div class="p-4 text-center text-rose-600">${esc(txList.message || 'Lỗi tải lịch sử')}</div>`;
                return;
            }
            if (countEl) countEl.textContent = `${txList.length} giao dịch`;
            if (listEl) {
                listEl.innerHTML = '';
                if (txList.length === 0) {
                    listEl.innerHTML = '<div class="p-4 text-center pk-text-secondary">Người dùng này chưa có giao dịch ví nào.</div>';
                } else {
                    txList.forEach(tx => listEl.appendChild(buildUserTransactionRow(tx)));
                }
            }
        } catch (err) {
            if (listEl) listEl.innerHTML = `<div class="p-4 text-center text-rose-600">${esc(err.message)}</div>`;
        }
    };

    // ==================== 9. FORM SUBMIT HANDLERS (100% AJAX — KHÔNG RELOAD) ====================
    function initAdminDashboard() {
        window.recalculateRarityPercentages();
        window.updateFormattedNumberPreview();
        renderCardTemplatesPagination(1);

        // Bind realtime validation cho các Modal forms
        document.getElementById('form-create-set')?.addEventListener('input', () => {
            window.updateNewSetBoxHint();
            window.validateCreateSetForm();
        });
        document.getElementById('form-upsert-rarity')?.addEventListener('input', window.updateRarityModalPreview);
        document.getElementById('form-upsert-rarity')?.addEventListener('change', window.updateRarityModalPreview);
        document.getElementById('create-card-template-form')?.addEventListener('input', window.validateCreateCardForm);
        document.getElementById('create-card-template-form')?.addEventListener('change', window.validateCreateCardForm);
        document.getElementById('form-adjust-user-wallet')?.addEventListener('input', window.validateAdjustWalletForm);
        window.validateCreateSetForm();
        window.validateCreateCardForm();
        window.validateAdjustWalletForm();
        try {
            const savedTab = sessionStorage.getItem('pk_admin_active_tab');
            if (savedTab && savedTab !== 'sets') {
                window.switchAdminTab(savedTab);
            }
        } catch (e) {}

        // 1. Tạo Bộ Sưu Tập mới (AJAX)
        document.getElementById('form-create-set')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const payload = {
                name: document.getElementById('new-set-name').value.trim(),
                code: document.getElementById('new-set-code').value.trim().toUpperCase(),
                baseSetTotal: parseInt(document.getElementById('new-set-total').value, 10),
                cardsPerPack: parseInt(document.getElementById('new-set-cards-per-pack').value, 10),
                packsPerBox: parseInt(document.getElementById('new-set-packs-per-box').value, 10),
                packPrice: parseFloat(document.getElementById('new-set-pack-price').value),
                boxPrice: parseFloat(document.getElementById('new-set-box-price').value)
            };
            const res = await fetch('/api/v1/admin/card-sets', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (!res.ok) {
                window.showToast(res.status === 409 ? 'conflict' : 'error', 'Không thể tạo bộ sưu tập', data.message || 'Kiểm tra lại thông tin.');
                return;
            }
            window.closeModal('modal-create-set');

            const tbody = document.getElementById('card-sets-tbody');
            const hadNoSets = tbody && tbody.querySelectorAll('tr[id^="set-row-"]').length === 0;
            document.getElementById('card-sets-empty-row')?.remove();
            if (tbody) {
                const newRow = buildCardSetRow(data, hadNoSets);
                tbody.appendChild(newRow);
                highlightElementBriefly(newRow);
                updateCardSetsCountUI();
            }

            const label = `${data.name} (${data.code})`;
            upsertOptionInSelects(['admin-set-switcher'], data.id, label, hadNoSets ? 'admin-set-switcher' : null);

            // Nếu đây là bộ đầu tiên trong DB trống, đồng bộ hidden inputs để thêm thẻ/box ngay được
            if (hadNoSets) {
                const setIdInput = document.getElementById('card-set-id');
                const setCodeInput = document.getElementById('card-set-code');
                const baseTotalInput = document.getElementById('card-base-total');
                if (setIdInput) setIdInput.value = String(data.id);
                if (setCodeInput) setCodeInput.value = data.code;
                if (baseTotalInput) baseTotalInput.value = String(data.baseSetTotal);
                window.updateFormattedNumberPreview();
            }

            window.showToast('success', 'Tạo bộ sưu tập thành công!', `Đã thêm bộ "${data.name}" (${data.code}) vào danh sách.`);
        });

        // 2. Cập nhật Bộ Sưu Tập đang chọn (AJAX)
        document.getElementById('update-set-config-form')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const setId = document.getElementById('config-set-id').value;
            const payload = {
                name: document.getElementById('config-set-name').value.trim(),
                code: document.getElementById('config-set-code').value.trim().toUpperCase(),
                baseSetTotal: parseInt(document.getElementById('config-base-total').value, 10),
                cardsPerPack: parseInt(document.getElementById('config-cards-per-pack').value, 10),
                packsPerBox: parseInt(document.getElementById('config-packs-per-box').value, 10),
                packPrice: parseFloat(document.getElementById('config-pack-price').value),
                boxPrice: parseFloat(document.getElementById('config-box-price').value),
                isActive: document.getElementById('config-set-active').checked
            };
            const res = await fetch(`/api/v1/admin/card-sets/${setId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const dailyPackLimit = parseInt(document.getElementById('config-daily-pack-limit')?.value || '200', 10);
            const dailyBoxLimit = parseInt(document.getElementById('config-daily-box-limit')?.value || '15', 10);
            if (res.ok) {
                await fetch(`/api/v1/admin/card-sets/${setId}/daily-limits`, {
                    method: 'PATCH',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ dailyPackLimit, dailyBoxLimit })
                });
            }
            const data = await res.json();
            if (!res.ok) {
                window.showToast(res.status === 409 ? 'conflict' : 'error', 'Cập nhật thất bại', data.message);
                return;
            }

            const existingRow = document.getElementById(`set-row-${data.id}`);
            if (existingRow) {
                const updatedRow = buildCardSetRow(data, true);
                existingRow.replaceWith(updatedRow);
                highlightElementBriefly(updatedRow);
            }

            const switcherLabel = `${data.name} (${data.code})${data.isActive === false ? ' [Đã ẩn]' : ''}`;
            upsertOptionInSelects(['admin-set-switcher'], data.id, switcherLabel, null);

            const setCodeInput = document.getElementById('card-set-code');
            const baseTotalInput = document.getElementById('card-base-total');
            if (setCodeInput) setCodeInput.value = data.code;
            if (baseTotalInput) baseTotalInput.value = String(data.baseSetTotal);
            window.updateFormattedNumberPreview();

            window.showToast('success', 'Đã lưu thông tin bộ thẻ', `Bộ ${data.name} (${data.code}) đã được cập nhật.`);
        });

        // 3. Lưu trọng số Độ hiếm (AJAX)
        document.getElementById('update-rarity-weights-form')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const items = Array.from(document.querySelectorAll('.rarity-weight-input')).map(input => ({
                rarityId: parseInt(input.getAttribute('data-rarity-id'), 10),
                dropWeight: parseInt(input.value, 10)
            }));
            const res = await fetch('/api/v1/admin/rarities/drop-weights', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ items })
            });
            const data = await res.json();
            if (!res.ok) {
                window.showToast('error', 'Lỗi cập nhật tỷ lệ', data.message);
                return;
            }
            document.querySelectorAll('.rarity-weight-input').forEach(input => {
                input.setAttribute('data-initial-weight', String(parseInt(input.value || '0', 10) || 0));
            });
            sortRaritiesListByWeightDesc();
            window.recalculateRarityPercentages();
            window.showToast('success', 'Đã lưu tỷ lệ độ hiếm', 'Tỷ lệ rơi thẻ mới đã được áp dụng và sắp xếp theo trọng số giảm dần.');
        });

        // 4. Tạo / Sửa Độ hiếm (100% AJAX — cập nhật Tab 2, sắp xếp trọng số giảm dần & đồng bộ màu cho tất cả thẻ cũ ở Tab 3)
        document.getElementById('form-upsert-rarity')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const rarityId = document.getElementById('rarity-modal-id').value;
            const targetSelectId = document.getElementById('rarity-modal-target-select').value;
            const payload = {
                name: document.getElementById('rarity-modal-name').value.trim(),
                dropWeight: parseInt(document.getElementById('rarity-modal-weight').value, 10),
                colorHex: document.getElementById('rarity-modal-color').value.trim().toUpperCase(),
                isHolographic: document.getElementById('rarity-modal-holo').checked
            };
            const url = rarityId ? `/api/v1/admin/rarities/${rarityId}` : '/api/v1/admin/rarities';
            const method = rarityId ? 'PUT' : 'POST';
            const res = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (!res.ok) {
                window.showToast(res.status === 409 ? 'conflict' : 'error', 'Không thể lưu độ hiếm', data.message);
                return;
            }
            window.closeModal('modal-rarity');

            const listEl = document.getElementById('rarities-list');
            document.getElementById('rarities-empty-row')?.remove();
            const cardEl = buildRarityCardElement(data);
            if (rarityId) {
                const existing = document.getElementById(`rarity-card-${data.id}`);
                if (existing) existing.replaceWith(cardEl);
                else listEl?.appendChild(cardEl);
                syncExistingCardsForUpdatedRarity(data);
            } else {
                listEl?.appendChild(cardEl);
            }
            sortRaritiesListByWeightDesc();
            highlightElementBriefly(cardEl);
            window.recalculateRarityPercentages();
            upsertOptionInSelects(['card-rarity-id', 'edit-card-rarity-id'], data.id, data.name, targetSelectId || null);

            window.showToast('success', rarityId ? 'Đã cập nhật độ hiếm' : 'Đã thêm độ hiếm mới', `"${data.name}" đã được đồng bộ màu sắc cho toàn bộ thẻ.`);
        });

        // 5. Tạo nhanh Loài (AJAX — đồng bộ dropdown trực tiếp & sắp xếp theo tên A-Z)
        document.getElementById('form-create-species')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const targetSelectId = document.getElementById('species-modal-target-select').value;
            const payload = {
                name: document.getElementById('species-modal-name').value.trim(),
                isBeta: document.getElementById('species-modal-beta').checked
            };
            const res = await fetch('/api/v1/admin/species', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (!res.ok) {
                window.showToast(res.status === 409 ? 'conflict' : 'error', 'Không thể tạo loài mới', data.message);
                return;
            }
            window.closeModal('modal-species');
            upsertOptionInSelects(['card-species-id', 'edit-card-species-id'], data.id, data.name, targetSelectId || 'card-species-id');
            ['card-species-id', 'edit-card-species-id'].forEach(selId => {
                const sel = document.getElementById(selId);
                if (!sel) return;
                const currentVal = sel.value;
                const placeholderOpts = Array.from(sel.options).filter(o => o.value === '' && o.disabled);
                const dataOpts = Array.from(sel.options).filter(o => !(o.value === '' && o.disabled));
                dataOpts.sort((a, b) => a.textContent.trim().localeCompare(b.textContent.trim(), 'vi'));
                sel.innerHTML = '';
                placeholderOpts.forEach(o => sel.appendChild(o));
                dataOpts.forEach(o => sel.appendChild(o));
                sel.value = currentVal;
            });
            window.showToast('success', 'Đã thêm loài mới', `"${data.name}" đã được chọn sẵn.`);
        });

        // 6. Thêm mới Thẻ bài (AJAX — chèn đúng thứ tự tăng dần, tự tính số thứ tự tiếp theo từ max + 1)
        document.getElementById('create-card-template-form')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const setId = document.getElementById('card-set-id').value;
            const alertBox = document.getElementById('card-conflict-alert');
            const alertMsg = document.getElementById('card-conflict-message');
            alertBox.classList.add('hidden');

            const payload = {
                name: document.getElementById('card-name').value.trim(),
                cardNumber: parseInt(document.getElementById('card-number').value, 10),
                imageUrl: document.getElementById('card-image-url').value.trim(),
                rarityId: parseInt(document.getElementById('card-rarity-id').value, 10),
                speciesId: parseInt(document.getElementById('card-species-id').value, 10)
            };
            const res = await fetch(`/api/v1/admin/card-sets/${setId}/cards`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (res.status === 409) {
                alertMsg.textContent = data.message || 'Số thứ tự thẻ đã tồn tại trong bộ này!';
                alertBox.classList.remove('hidden');
                window.showToast('conflict', 'Trùng số thứ tự thẻ', data.message);
                return;
            }
            if (!res.ok) {
                window.showToast('error', 'Không thể thêm thẻ bài', data.message);
                return;
            }

            const newRow = buildCardRowElement(data);
            insertCardRowSorted(newRow, data.cardNumber);
            window.closeModal('modal-create-card');

            const nameInput = document.getElementById('card-name');
            const fileInput = document.getElementById('card-image-file');
            const urlInput = document.getElementById('card-image-url');
            const numInput = document.getElementById('card-number');
            if (nameInput) nameInput.value = '';
            if (fileInput) fileInput.value = '';
            if (urlInput) urlInput.value = '';
            if (numInput) {
                const maxNum = Array.from(document.querySelectorAll('#card-templates-tbody tr[data-card-number]'))
                    .reduce((acc, r) => Math.max(acc, parseInt(r.getAttribute('data-card-number') || '0', 10) || 0), 0);
                numInput.value = String(maxNum + 1);
            }
            window.updateFormattedNumberPreview();

            window.showToast('success', 'Thêm thẻ bài thành công!', `Đã thêm "${data.name}" (${data.formattedCardNumber}).`);
        });

        // 7. Chỉnh sửa Thẻ bài (AJAX — thay thế dòng & sắp xếp lại nếu đổi cardNumber)
        document.getElementById('form-edit-card')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const setId = document.getElementById('card-set-id').value;
            const cardId = document.getElementById('edit-card-id').value;
            const payload = {
                name: document.getElementById('edit-card-name').value.trim(),
                cardNumber: parseInt(document.getElementById('edit-card-number').value, 10),
                imageUrl: document.getElementById('edit-card-image-url').value.trim(),
                rarityId: parseInt(document.getElementById('edit-card-rarity-id').value, 10),
                speciesId: parseInt(document.getElementById('edit-card-species-id').value, 10)
            };
            const res = await fetch(`/api/v1/admin/card-sets/${setId}/cards/${cardId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (!res.ok) {
                window.showToast(res.status === 409 ? 'conflict' : 'error', 'Không thể cập nhật thẻ bài', data.message);
                return;
            }
            window.closeModal('modal-edit-card');

            document.getElementById(`card-row-${data.id}`)?.remove();
            const updatedRow = buildCardRowElement(data);
            insertCardRowSorted(updatedRow, data.cardNumber);

            window.showToast('success', 'Đã cập nhật thẻ bài', `"${data.name}" (${data.formattedCardNumber}) đã được lưu.`);
        });

        // 8. Phát hành lô Booster Box mới (AJAX)
        document.getElementById('create-box-batch-form')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const setId = document.getElementById('card-set-id').value;
            const quantity = parseInt(document.getElementById('batch-box-quantity').value, 10);
            const res = await fetch(`/api/v1/admin/card-sets/${setId}/booster-boxes/batch`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ quantity })
            });
            const data = await res.json();
            if (!res.ok) {
                window.showToast('error', 'Không thể phát hành hộp bài', data.message);
                return;
            }
            const listEl = document.getElementById('booster-boxes-list');
            document.getElementById('booster-boxes-empty-row')?.remove();
            if (listEl && Array.isArray(data)) {
                data.forEach(box => {
                    const row = buildBoosterBoxRowElement(box);
                    listEl.prepend(row);
                    highlightElementBriefly(row);
                });
                updateBoxCountsUI();
            }
            window.showToast('success', `Đã phát hành ${data.length} hộp bài mới!`, 'Lô hộp bài đã sẵn sàng tại Cửa hàng.');
        });

        // 9. Thu hồi nhanh N hộp nguyên seal (AJAX)
        document.getElementById('reduce-box-batch-form')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const setId = document.getElementById('card-set-id').value;
            const quantity = parseInt(document.getElementById('reduce-box-quantity').value, 10);
            if (!confirm(`Xác nhận thu hồi ${quantity} hộp bài nguyên seal mới nhất trong bộ này?`)) return;
            const res = await fetch(`/api/v1/admin/card-sets/${setId}/booster-boxes/reduce`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ quantity })
            });
            const data = await res.json();
            if (!res.ok) {
                window.showToast('error', 'Không thể thu hồi hộp bài', data.message);
                return;
            }
            const removedCodes = data.removedBoxCodes || [];
            removedCodes.forEach(code => {
                const el = document.querySelector(`.box-list-item[data-box-code="${CSS.escape(code)}"]`);
                el?.remove();
            });
            updateBoxCountsUI();
            window.showToast('success', 'Đã thu hồi hộp bài', data.message);
        });

        // 10. Lọc & Tìm kiếm Người dùng (Tab 5 — AJAX)
        document.getElementById('admin-users-filter-form')?.addEventListener('submit', (e) => {
            e.preventDefault();
            window.loadAdminUsers(0);
        });

        // 11. Cộng / Trừ bù ví thủ công (Tab 5 Modal — AJAX)
        document.getElementById('form-adjust-user-wallet')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const userId = parseInt(document.getElementById('adjust-wallet-user-id').value, 10);
            const credit = document.getElementById('adjust-wallet-credit').value === 'true';
            const amount = parseFloat(document.getElementById('adjust-wallet-amount').value);
            const reason = document.getElementById('adjust-wallet-reason').value.trim();

            const res = await fetch(`/api/v1/admin/users/${userId}/wallet/adjust`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ amount, credit, reason })
            });
            const data = await res.json();
            if (!res.ok) {
                window.showToast('error', 'Không thể điều chỉnh ví', data.message || 'Vui lòng kiểm tra lại số tiền và lý do.');
                return;
            }

            if (data.user) {
                document.getElementById('wallet-modal-balance').textContent = window.formatVND(data.user.balance);
                document.getElementById('wallet-modal-locked').textContent = window.formatVND(data.user.lockedBalance);
                const existingRow = document.getElementById(`user-row-${data.user.id}`);
                if (existingRow) {
                    const updatedRow = buildAdminUserRow(data.user);
                    existingRow.replaceWith(updatedRow);
                    highlightElementBriefly(updatedRow);
                }
            }
            if (data.transaction) {
                const listEl = document.getElementById('user-transactions-list');
                if (listEl) {
                    if (listEl.children.length === 1 && listEl.textContent.includes('chưa có giao dịch')) {
                        listEl.innerHTML = '';
                    }
                    const txRow = buildUserTransactionRow(data.transaction);
                    listEl.prepend(txRow);
                    highlightElementBriefly(txRow);
                    const countEl = document.getElementById('wallet-modal-tx-count');
                    if (countEl) countEl.textContent = `${listEl.children.length} giao dịch`;
                }
            }
            if (Number(userId) === Number(window.CURRENT_USER_ID) && window.refreshNavbarWallet) {
                window.refreshNavbarWallet();
            }

            document.getElementById('adjust-wallet-amount').value = '';
            document.getElementById('adjust-wallet-reason').value = '';
            window.showToast('success', 'Điều chỉnh ví thành công', data.message);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAdminDashboard);
    } else {
        initAdminDashboard();
    }
})();
