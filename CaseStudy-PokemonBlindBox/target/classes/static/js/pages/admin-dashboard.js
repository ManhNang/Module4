/* ==================== ADMIN DASHBOARD PAGE (100% AJAX, KHÔNG RELOAD TRANG) ==================== */
(function () {
    const esc = (v) => (window.escapeHtml ? window.escapeHtml(v) : String(v ?? ''));
    const safeHex = (hex, fallback = '#10B981') => (/^#[0-9a-fA-F]{3,8}$/.test(hex || '') ? hex : fallback);
    const fmtPrice = (val) => new Intl.NumberFormat('vi-VN').format(Math.round(Number(val || 0))) + '₫';

    // Trạng thái phân trang Tab 3 (Danh Mục Thẻ Bài — tối đa 13 thẻ/trang)
    const CARDS_PER_PAGE = 13;
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
        ['modal-card-preview', 'modal-user-wallet', 'modal-edit-card', 'modal-species', 'modal-rarity', 'modal-create-set']
            .forEach(id => window.closeModal(id));
    });

    window.openCreateSetModal = function () {
        document.getElementById('form-create-set')?.reset();
        window.openModal('modal-create-set');
    };

    window.openRarityModal = function (rarity, targetSelectId) {
        document.getElementById('rarity-modal-id').value = rarity ? rarity.id : '';
        document.getElementById('rarity-modal-target-select').value = targetSelectId || '';
        document.getElementById('modal-rarity-title').textContent = rarity ? 'Chỉnh Sửa Độ Hiếm' : 'Thêm Độ Hiếm Mới';
        document.getElementById('rarity-modal-name').value = rarity ? rarity.name : '';
        document.getElementById('rarity-modal-weight').value = rarity ? rarity.dropWeight : 30;
        const hex = safeHex(rarity ? rarity.colorHex : '#EC4899', '#EC4899');
        document.getElementById('rarity-modal-color').value = hex;
        document.getElementById('rarity-modal-color-picker').value = /^#[0-9a-fA-F]{6}$/.test(hex) ? hex : '#EC4899';
        document.getElementById('rarity-modal-holo').checked = rarity ? Boolean(rarity.isHolographic) : false;
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

    // ==================== 4. CHUYỂN TAB (5 TAB) ====================
    window.switchAdminTab = function (tab) {
        const tabs = ['sets', 'rarities', 'cards', 'boxes', 'users'];
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
        if (isCurrentActive) tr.className = 'bg-rose-500/5';
        const isActive = set.isActive !== false;
        const statusHtml = isActive
            ? '<span class="inline-flex items-center rounded-full bg-emerald-500/15 px-2.5 py-0.5 text-[11px] font-bold text-emerald-600">Đang mở</span>'
            : '<span class="inline-flex items-center rounded-full bg-slate-500/20 px-2.5 py-0.5 text-[11px] font-bold pk-text-secondary">Đã ẩn</span>';

        tr.innerHTML = `
            <td class="px-4 py-3 font-extrabold text-rose-600">${esc(set.code)}</td>
            <td class="px-4 py-3 font-bold pk-text-primary">
                <a href="/admin/dashboard?setId=${Number(set.id)}" class="hover:underline">${esc(set.name)}</a>
            </td>
            <td class="px-4 py-3 text-center pk-num font-bold pk-text-primary">${Number(set.baseSetTotal)}</td>
            <td class="px-4 py-3 text-center pk-num font-bold text-emerald-600">${Number(set.cardsPerPack)}</td>
            <td class="px-4 py-3 text-center pk-num font-bold pk-text-primary">${Number(set.packsPerBox)}</td>
            <td class="px-4 py-3 text-right pk-num text-xs">
                <div class="font-bold pk-text-primary">${fmtPrice(set.packPrice)}</div>
                <div class="pk-text-secondary">${fmtPrice(set.boxPrice)}</div>
            </td>
            <td class="px-4 py-3 text-center whitespace-nowrap set-status-cell">${statusHtml}</td>
            <td class="px-4 py-3 text-right whitespace-nowrap">
                <button type="button"
                        data-set-id="${Number(set.id)}"
                        data-set-name="${esc(set.name)}"
                        onclick="window.deleteCardSet(parseInt(this.getAttribute('data-set-id'), 10), this.getAttribute('data-set-name'))"
                        class="text-xs font-bold text-rose-600 hover:underline cursor-pointer">
                    Xóa / Ẩn
                </button>
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
            emptyTr.innerHTML = '<td colspan="8" class="px-4 py-8 text-center text-xs pk-text-secondary">Chưa có bộ sưu tập nào. Hãy bấm "+ Tạo Bộ Sưu Tập Mới" để bắt đầu!</td>';
            tbody.appendChild(emptyTr);
        }
    }

    function buildRarityCardElement(r) {
        const div = document.createElement('div');
        div.id = `rarity-card-${r.id}`;
        div.setAttribute('data-rarity-id', String(r.id));
        div.setAttribute('data-drop-weight', String(Number(r.dropWeight || 0)));
        div.className = 'rarity-card-item pk-surface-elevated rounded-2xl p-4 space-y-2';
        const color = safeHex(r.colorHex, '#EC4899');
        const holoBadge = r.isHolographic
            ? '<span class="rounded-full bg-amber-500/20 px-2 py-0.5 text-[10px] font-extrabold text-amber-600 dark:text-amber-300 border border-amber-500/30">HOLO 3D</span>'
            : '';

        div.innerHTML = `
            <div class="flex flex-wrap items-center justify-between gap-3">
                <div class="flex items-center gap-2.5">
                    <span class="h-3.5 w-3.5 rounded-full shrink-0" style="background-color:${color}"></span>
                    <div>
                        <div class="flex items-center gap-2">
                            <span class="text-sm font-extrabold pk-text-primary">${esc(r.name)}</span>
                            ${holoBadge}
                        </div>
                        <div class="text-xs pk-text-secondary">
                            Tỷ lệ rơi thực tế:
                            <strong class="rarity-percent pk-num text-rose-600" data-rarity-id="${Number(r.id)}">0.00%</strong>
                        </div>
                    </div>
                </div>
                <div class="flex flex-wrap items-center gap-2">
                    <label class="text-xs font-semibold pk-text-secondary">Trọng số:</label>
                    <input type="number" min="0" required
                           class="rarity-weight-input pk-input pk-num h-9 w-24 rounded-xl px-3 text-right text-sm font-extrabold"
                           data-rarity-id="${Number(r.id)}"
                           value="${Number(r.dropWeight)}"
                           oninput="window.recalculateRarityPercentages()"/>
                    <button type="button"
                            data-id="${Number(r.id)}"
                            data-name="${esc(r.name)}"
                            data-weight="${Number(r.dropWeight)}"
                            data-color="${color}"
                            data-holo="${Boolean(r.isHolographic)}"
                            onclick="window.openRarityModal({id: parseInt(this.dataset.id,10), name: this.dataset.name, dropWeight: parseInt(this.dataset.weight,10), colorHex: this.dataset.color, isHolographic: this.dataset.holo === 'true'})"
                            class="rarity-edit-btn pk-surface-muted rounded-xl px-3 py-1.5 text-xs font-bold pk-text-primary hover:opacity-80 cursor-pointer">
                        Sửa
                    </button>
                    <button type="button"
                            data-id="${Number(r.id)}"
                            data-name="${esc(r.name)}"
                            onclick="window.deleteRarity(parseInt(this.dataset.id, 10), this.dataset.name)"
                            class="pk-surface-muted rounded-xl px-3 py-1.5 text-xs font-bold text-rose-600 hover:opacity-80 cursor-pointer">
                        Xóa
                    </button>
                </div>
            </div>
            <div class="h-2 w-full rounded-full pk-surface-muted overflow-hidden">
                <div class="rarity-bar h-full rounded-full transition-all duration-300"
                     data-rarity-id="${Number(r.id)}"
                     style="width: 0%; background-color:${color}"></div>
            </div>
        `;
        return div;
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
            ? '<span class="rounded-full bg-emerald-500/15 px-2.5 py-0.5 text-[11px] font-bold text-emerald-600">Đang mở</span>'
            : '<span class="rounded-full bg-slate-500/20 px-2.5 py-0.5 text-[11px] font-bold pk-text-secondary">Đã ẩn</span>';

        tr.innerHTML = `
            <td class="px-4 py-3 pk-num font-extrabold text-emerald-600 whitespace-nowrap">${esc(c.formattedCardNumber)}</td>
            <td class="px-4 py-3">
                <button type="button"
                        onclick="window.openCardPreviewModal(this)"
                        data-name="${esc(c.name)}"
                        data-num="${esc(c.formattedCardNumber)}"
                        data-img="${esc(c.imageUrl)}"
                        data-rarity="${esc(c.rarityName)}"
                        data-color="${color}"
                        data-species="${esc(c.speciesName)}"
                        data-holo="${Boolean(c.rarityIsHolographic)}"
                        class="card-preview-trigger text-left font-bold pk-text-primary hover:underline hover:text-rose-600 cursor-pointer transition">${esc(c.name)}</button>
            </td>
            <td class="px-4 py-3 whitespace-nowrap">
                <span class="card-rarity-badge rounded-full px-2.5 py-0.5 text-xs font-bold border"
                      style="border-color:${color}; color:${color}">${esc(c.rarityName)}</span>
            </td>
            <td class="px-4 py-3 font-semibold pk-text-secondary">${esc(c.speciesName)}</td>
            <td class="px-4 py-3 text-center whitespace-nowrap card-status-cell">${statusHtml}</td>
            <td class="px-4 py-3 text-right whitespace-nowrap space-x-2">
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
                        onclick="window.deleteCardTemplate(parseInt(this.dataset.id, 10), this.dataset.name)"
                        class="font-bold text-rose-600 hover:underline cursor-pointer">
                    Xóa / Ẩn
                </button>
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

        const rows = Array.from(tbody.querySelectorAll('tr[id^="card-row-"]'));
        const total = rows.length;
        if (total === 0) {
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

        rows.forEach((row, idx) => {
            row.style.display = (idx >= startIdx && idx < endIdx) ? '' : 'none';
        });

        bar?.classList.remove('hidden');
        if (infoEl) {
            infoEl.textContent = `Hiển thị ${startIdx + 1}–${endIdx} / ${total} thẻ (Trang ${cardTemplatesPage} / ${totalPages})`;
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
        if (countEl) countEl.textContent = `${rows.length} thẻ bài`;
        if (rows.length === 0 && !document.getElementById('card-templates-empty-row')) {
            const emptyTr = document.createElement('tr');
            emptyTr.id = 'card-templates-empty-row';
            emptyTr.innerHTML = '<td colspan="6" class="px-4 py-8 text-center text-xs pk-text-secondary">Bộ sưu tập này chưa có thẻ bài nào. Hãy thêm thẻ đầu tiên ở biểu mẫu phía trên!</td>';
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

        inputs.forEach(input => {
            const rarityId = input.getAttribute('data-rarity-id');
            const weight = parseInt(input.value || '0', 10) || 0;
            const pctNum = total > 0 ? (weight * 100) / total : 0;
            const label = document.querySelector(`.rarity-percent[data-rarity-id="${rarityId}"]`);
            const bar = document.querySelector(`.rarity-bar[data-rarity-id="${rarityId}"]`);
            if (label) label.textContent = pctNum.toFixed(2) + '%';
            if (bar) bar.style.width = pctNum.toFixed(2) + '%';
        });
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
        previewEl.textContent = cardNum > baseTotal ? `${formatted} ★` : formatted;
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

    // ==================== 7. AJAX DELETE HANDLERS (TAB 1, 2, 3, 4 — KHÔNG RELOAD) ====================
    window.deleteCardSet = async function (setId, setName) {
        if (!confirm(`Xác nhận xóa hoặc ẩn bộ sưu tập "${setName}"?`)) return;
        const res = await fetch(`/api/v1/admin/card-sets/${setId}`, { method: 'DELETE' });
        const data = await res.json();
        if (!res.ok) {
            window.showToast('error', 'Không thể xóa bộ sưu tập', data.message || 'Vui lòng thử lại.');
            return;
        }
        window.showToast('success', 'Cập nhật thành công', data.message);

        const row = document.getElementById(`set-row-${setId}`);
        const switcher = document.getElementById('admin-set-switcher');
        if (data.softDeleted) {
            const statusCell = row?.querySelector('.set-status-cell');
            if (statusCell) {
                statusCell.innerHTML = '<span class="inline-flex items-center rounded-full bg-slate-500/20 px-2.5 py-0.5 text-[11px] font-bold pk-text-secondary">Đã ẩn</span>';
            }
            if (switcher) {
                const opt = Array.from(switcher.options).find(o => o.value === String(setId));
                if (opt && !opt.textContent.includes('[Đã ẩn]')) {
                    opt.textContent = opt.textContent.trim() + ' [Đã ẩn]';
                    syncCustomSelectUI(switcher);
                }
            }
            const activeConfigId = document.getElementById('config-set-id')?.value;
            if (activeConfigId === String(setId)) {
                const cb = document.getElementById('config-set-active');
                if (cb) cb.checked = false;
            }
        } else {
            row?.remove();
            updateCardSetsCountUI();
            removeOptionFromSelects(['admin-set-switcher'], setId);
        }
    };

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
            const emptyDiv = document.createElement('div');
            emptyDiv.id = 'rarities-empty-row';
            emptyDiv.className = 'pk-surface-elevated rounded-2xl p-6 text-center text-xs pk-text-secondary';
            emptyDiv.textContent = 'Chưa có độ hiếm nào. Hãy bấm "+ Thêm Độ Hiếm Mới" để khởi tạo!';
            listEl.appendChild(emptyDiv);
        }
    };

    window.deleteCardTemplate = async function (cardId, cardName) {
        if (!confirm(`Xác nhận xóa/ẩn thẻ bài "${cardName}"? (Nếu người chơi đã sở hữu, thẻ sẽ tự động ẩn khỏi Gacha và vẫn giữ trong Kho đồ)`)) return;
        const setId = document.getElementById('card-set-id').value;
        const res = await fetch(`/api/v1/admin/card-sets/${setId}/cards/${cardId}`, { method: 'DELETE' });
        const data = await res.json();
        if (!res.ok) {
            window.showToast('error', 'Không thể xóa thẻ bài', data.message || 'Vui lòng thử lại.');
            return;
        }
        window.showToast('success', 'Hoàn tất', data.message);

        const row = document.getElementById(`card-row-${cardId}`);
        if (data.softDeleted) {
            const statusCell = row?.querySelector('.card-status-cell');
            if (statusCell) {
                statusCell.innerHTML = '<span class="rounded-full bg-slate-500/20 px-2.5 py-0.5 text-[11px] font-bold pk-text-secondary">Đã ẩn</span>';
            }
            highlightElementBriefly(row);
        } else {
            row?.remove();
            updateCardTemplatesCountUI();
        }
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
            ? '<button type="button" disabled title="Không thể tự khóa tài khoản của chính mình" class="pk-surface rounded-lg px-2.5 py-1 text-[11px] font-bold pk-text-muted opacity-50 cursor-not-allowed">Khóa</button>'
            : `<button type="button"
                       onclick="window.toggleUserLock(${Number(u.id)}, ${!isLocked})"
                       class="pk-surface rounded-lg px-2.5 py-1 text-[11px] font-bold ${isLocked ? 'text-emerald-600' : 'text-rose-600'} hover:opacity-80 cursor-pointer whitespace-nowrap">
                   ${isLocked ? 'Mở khóa' : 'Khóa'}
               </button>`;

        const nextRole = isAdmin ? 'ROLE_USER' : 'ROLE_ADMIN';
        const roleBtn = isSelf
            ? '<button type="button" disabled title="Không thể tự đổi quyền của chính mình" class="pk-surface rounded-lg px-2.5 py-1 text-[11px] font-bold pk-text-muted opacity-50 cursor-not-allowed">Đổi quyền</button>'
            : `<button type="button"
                       onclick="window.toggleUserRole(${Number(u.id)}, '${nextRole}')"
                       class="pk-surface rounded-lg px-2.5 py-1 text-[11px] font-bold pk-text-primary hover:opacity-80 cursor-pointer whitespace-nowrap">
                   ${isAdmin ? 'Hạ USER' : 'Lên ADMIN'}
               </button>`;

        tr.innerHTML = `
            <td class="px-4 py-3">
                <div class="font-extrabold pk-text-primary">${esc(u.username)}${selfBadge}</div>
                <div class="text-[11px] pk-text-secondary">${esc(u.email)}</div>
            </td>
            <td class="px-4 py-3 text-center whitespace-nowrap">${roleBadge}</td>
            <td class="px-4 py-3 text-right pk-num text-xs whitespace-nowrap">
                <div class="font-extrabold text-emerald-600">${window.formatVND(u.balance)}</div>
                <div class="text-[11px] pk-text-secondary">Cọc: ${window.formatVND(u.lockedBalance)}</div>
            </td>
            <td class="px-4 py-3 text-center pk-num font-bold pk-text-primary">${Number(u.totalCardsOwned || 0)}</td>
            <td class="px-4 py-3 text-center whitespace-nowrap">${statusBadge}</td>
            <td class="px-4 py-3 text-right whitespace-nowrap">
                <div class="inline-flex items-center justify-end gap-1.5">
                    ${lockBtn}
                    ${roleBtn}
                    <button type="button"
                            data-id="${Number(u.id)}"
                            data-username="${esc(u.username)}"
                            data-balance="${Number(u.balance || 0)}"
                            data-locked-balance="${Number(u.lockedBalance || 0)}"
                            onclick="window.openUserWalletModal(parseInt(this.dataset.id,10), this.dataset.username, parseFloat(this.dataset.balance), parseFloat(this.dataset.lockedBalance))"
                            class="pk-surface rounded-lg px-2.5 py-1 text-[11px] font-bold text-amber-600 dark:text-amber-400 hover:opacity-80 cursor-pointer whitespace-nowrap">
                        Ví &amp; Lịch sử
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
        const isDebit = ['PURCHASE_PACK', 'PURCHASE_BOX', 'AUCTION_BID_HOLD', 'MARKET_PURCHASE'].includes(tx.type)
            || (tx.type === 'ADMIN_ADJUSTMENT' && (tx.description || '').includes('Trừ bù'));
        const sign = isDebit ? '-' : '+';
        const amountClass = isDebit ? 'text-rose-600' : 'text-emerald-600';

        div.innerHTML = `
            <div class="min-w-0 flex-1 space-y-0.5">
                <div class="flex flex-wrap items-center gap-1.5">
                    <span class="rounded-md pk-surface px-1.5 py-0.5 text-[10px] font-bold pk-text-primary">${esc(tx.type)}</span>
                    <span class="pk-num text-[10px] pk-text-secondary">${esc(timeStr)}</span>
                    <span class="text-[10px] font-bold ${tx.status === 'SUCCESS' ? 'text-emerald-600' : 'text-amber-600'}">${esc(tx.status)}</span>
                </div>
                <div class="text-xs pk-text-primary leading-snug break-words">${esc(tx.description || tx.vnpTxnRef)}</div>
            </div>
            <div class="text-right pk-num shrink-0">
                <div class="text-xs font-extrabold ${amountClass}">${sign}${window.formatVND(tx.amount)}</div>
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
