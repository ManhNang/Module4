/* ==================== COMMON SCRIPTS (DÙNG CHUNG TOÀN HỆ THỐNG) ==================== */
(function () {
    const userContextEl = document.getElementById('app-user-context');
    const rawUserId = userContextEl ? userContextEl.dataset.userId : null;
    const parsedUserId = rawUserId ? parseInt(rawUserId, 10) : 1;
    window.CURRENT_USER_ID = Number.isNaN(parsedUserId) ? 1 : parsedUserId;
    window.CURRENT_USERNAME = (userContextEl && userContextEl.dataset.username) ? userContextEl.dataset.username : 'satoshi';
})();

// P1-08: Escape HTML chống DOM-based XSS
window.escapeHtml = function (value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
};

// P0-05: Tự động đính kèm X-XSRF-TOKEN vào mọi yêu cầu fetch POST/PUT/PATCH/DELETE cùng nguồn
(function () {
    if (window.__pkCsrfFetchPatched) return;
    window.__pkCsrfFetchPatched = true;
    const originalFetch = window.fetch.bind(window);
    function getCookie(name) {
        const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
        return match ? decodeURIComponent(match[1]) : null;
    }
    window.fetch = function (input, init) {
        const opts = init ? { ...init } : {};
        const method = (opts.method || 'GET').toUpperCase();
        if (method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS') {
            const token = getCookie('XSRF-TOKEN');
            if (token) {
                const headers = new Headers(opts.headers || {});
                if (!headers.has('X-XSRF-TOKEN')) {
                    headers.set('X-XSRF-TOKEN', token);
                }
                opts.headers = headers;
            }
        }
        return originalFetch(input, opts);
    };
})();

window.formatVND = function (amount) {
    const num = Math.round(Number(amount || 0));
    return new Intl.NumberFormat('vi-VN').format(num) + ' Xu';
};
window.formatXu = window.formatVND;
window.formatVndCurrency = function (amount) {
    const num = Math.round(Number(amount || 0));
    return new Intl.NumberFormat('vi-VN').format(num) + ' ₫';
};

window.updateNavbarWallet = function (newBalance) {
    const balanceEl = document.getElementById('nav-wallet-balance');
    if (balanceEl && newBalance !== undefined && newBalance !== null) {
        balanceEl.textContent = window.formatVND(newBalance);
    }
};

window.syncThemeIcons = function () {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    const moon = document.getElementById('theme-icon-moon');
    const sun = document.getElementById('theme-icon-sun');
    if (moon && sun) {
        moon.classList.toggle('hidden', isDark);
        sun.classList.toggle('hidden', !isDark);
    }
};

window.toggleBlindboxTheme = function () {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    if (isDark) {
        document.documentElement.removeAttribute('data-theme');
        localStorage.setItem('pk_blindbox_theme', 'light');
    } else {
        document.documentElement.setAttribute('data-theme', 'dark');
        localStorage.setItem('pk_blindbox_theme', 'dark');
    }
    window.syncThemeIcons();
};

window.showToast = function (type, title, message) {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const borderColors = {
        success: 'border-emerald-500/60',
        conflict: 'border-rose-500 ring-2 ring-rose-500/30',
        error: 'border-rose-500/70',
        info: 'border-amber-500/60'
    };
    const badgeColors = {
        success: 'text-emerald-600 dark:text-emerald-400',
        conflict: 'text-rose-600 dark:text-rose-400',
        error: 'text-rose-600 dark:text-rose-400',
        info: 'text-amber-600 dark:text-amber-400'
    };

    const safeTitle = window.escapeHtml(title);
    const safeMessage = window.escapeHtml(message);
    const toast = document.createElement('div');
    toast.className = `pointer-events-auto toast-enter pk-surface rounded-2xl border-2 p-4 shadow-2xl ${borderColors[type] || borderColors.info}`;
    toast.innerHTML = `
        <div class="flex items-start justify-between gap-3">
            <div>
                <div class="text-xs font-extrabold uppercase tracking-wider ${badgeColors[type] || badgeColors.info}">${safeTitle}</div>
                <p class="mt-1 text-sm pk-text-primary leading-relaxed">${safeMessage}</p>
            </div>
            <button onclick="this.closest('.toast-enter').remove()" class="pk-text-secondary hover:pk-text-primary text-sm font-bold cursor-pointer">✕</button>
        </div>
    `;
    container.appendChild(toast);
    setTimeout(() => {
        if (toast.parentNode) toast.remove();
    }, 5500);
};

window.refreshNavbarWallet = async function () {
    try {
        const res = await fetch('/api/v1/wallets/me');
        if (!res.ok) return;
        const data = await res.json();
        const balanceEl = document.getElementById('nav-wallet-balance');
        if (balanceEl && data.balance !== undefined) {
            balanceEl.textContent = window.formatVND(data.balance);
        }
    } catch (e) {}
};

/**
 * Kiểm tra cờ isHolographic từ DB trên DTO thẻ bài (thay thế hardcode tên độ hiếm).
 */
window.isHolographicCard = function (card) {
    return Boolean(card && card.rarityIsHolographic === true);
};
window.isHolographicRarity = window.isHolographicCard;

/**
 * Gắn hiệu ứng 3D Tilt (perspective(1000px) rotateX/Y) & ánh kim Holographic theo tọa độ chuột
 * cho mọi phần tử có class .pk-holo-card.
 */
window.bindHoloTiltEffects = function (rootContainer) {
    const scope = rootContainer || document;
    scope.querySelectorAll('.pk-holo-card').forEach(card => {
        if (card.getAttribute('data-holo-bound') === 'true') return;
        card.setAttribute('data-holo-bound', 'true');

        card.addEventListener('mousemove', (e) => {
            const rect = card.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;
            const pctX = Math.max(0, Math.min(100, (x / rect.width) * 100));
            const pctY = Math.max(0, Math.min(100, (y / rect.height) * 100));

            const rotateY = ((pctX - 50) / 50) * 13;
            const rotateX = ((50 - pctY) / 50) * 13;

            card.style.transform = `perspective(1000px) rotateX(${rotateX.toFixed(2)}deg) rotateY(${rotateY.toFixed(2)}deg) scale3d(1.03, 1.03, 1.03)`;
            card.style.setProperty('--mx', `${pctX.toFixed(1)}%`);
            card.style.setProperty('--my', `${pctY.toFixed(1)}%`);
            card.style.setProperty('--bgx', `${(50 + (pctX - 50) * 0.6).toFixed(1)}%`);
            card.style.setProperty('--bgy', `${(50 + (pctY - 50) * 0.6).toFixed(1)}%`);
            card.style.setProperty('--holo-opacity', '0.92');
        });

        card.addEventListener('mouseleave', () => {
            card.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)';
            card.style.setProperty('--holo-opacity', '0.52');
        });
    });
};

window.toggleMobileNavDrawer = function (forceClose) {
    const drawer = document.getElementById('mobile-nav-drawer');
    const backdrop = document.getElementById('mobile-nav-backdrop');
    const toggleBtn = document.getElementById('mobile-menu-toggle');
    if (!drawer || !backdrop) return;

    const isCurrentlyOpen = !drawer.classList.contains('-translate-x-full');
    const shouldClose = forceClose === true || isCurrentlyOpen;

    if (shouldClose) {
        drawer.classList.add('-translate-x-full');
        backdrop.classList.add('hidden');
        document.body.style.overflow = '';
        if (toggleBtn) toggleBtn.setAttribute('aria-expanded', 'false');
    } else {
        drawer.classList.remove('-translate-x-full');
        backdrop.classList.remove('hidden');
        document.body.style.overflow = 'hidden';
        if (toggleBtn) toggleBtn.setAttribute('aria-expanded', 'true');
    }
};

/**
 * Custom Dropdown Component dùng chung — tự động nâng cấp mọi thẻ <select>
 * Giữ nguyên 100% id, name, onchange, required, Thymeleaf và đồng bộ 2 chiều giá trị.
 */
window.initCustomSelects = function (rootContainer) {
    const scope = rootContainer || document;
    const selects = scope.querySelectorAll('select');
    if (!window.__pkActiveDropdown) window.__pkActiveDropdown = null;

    const closeActiveDropdown = () => {
        if (!window.__pkActiveDropdown) return;
        const { trigger, menu } = window.__pkActiveDropdown;
        trigger.classList.remove('is-open');
        trigger.setAttribute('aria-expanded', 'false');
        menu.classList.remove('is-open');
        window.__pkActiveDropdown = null;
    };
    window.closeAllCustomSelects = closeActiveDropdown;

    if (!window.__pkDropdownGlobalBound) {
        window.__pkDropdownGlobalBound = true;
        document.addEventListener('click', (e) => {
            if (!window.__pkActiveDropdown) return;
            const { trigger, menu } = window.__pkActiveDropdown;
            if (!trigger.contains(e.target) && !menu.contains(e.target)) {
                closeActiveDropdown();
            }
        });
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && window.__pkActiveDropdown) {
                const { trigger } = window.__pkActiveDropdown;
                closeActiveDropdown();
                trigger.focus();
            }
        });
        window.addEventListener('resize', closeActiveDropdown);
        window.addEventListener('scroll', (e) => {
            if (!window.__pkActiveDropdown) return;
            if (e.target && window.__pkActiveDropdown.menu.contains(e.target)) return;
            closeActiveDropdown();
        }, true);
    }

    const nativeValueDescriptor = Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype, 'value');

    selects.forEach((select) => {
        if (select.getAttribute('data-pk-select-enhanced') === 'true') return;
        select.setAttribute('data-pk-select-enhanced', 'true');

        // Giữ nguyên thẻ <select> gốc trong DOM nhưng ẩn đi
        select.style.display = 'none';

        // Wrapper & Trigger Button
        const wrap = document.createElement('div');
        wrap.className = 'pk-select-wrap';

        const trigger = document.createElement('button');
        trigger.type = 'button';
        trigger.className = 'pk-select-trigger';
        trigger.setAttribute('aria-haspopup', 'listbox');
        trigger.setAttribute('aria-expanded', 'false');

        // Kế thừa kích thước chữ / độ cao đồng bộ với các input xung quanh
        if (select.classList.contains('h-10')) {
            trigger.style.height = '40px';
            trigger.style.paddingTop = '0';
            trigger.style.paddingBottom = '0';
        }
        if (select.classList.contains('text-xs') && !select.classList.contains('sm:text-sm')) {
            trigger.style.fontSize = '0.75rem';
        } else {
            trigger.style.fontSize = '0.8125rem';
        }
        if (select.classList.contains('font-bold')) {
            trigger.style.fontWeight = '700';
        } else if (select.classList.contains('font-semibold')) {
            trigger.style.fontWeight = '600';
        } else {
            trigger.style.fontWeight = '500';
        }

        const textSpan = document.createElement('span');
        textSpan.className = 'pk-select-trigger-text';

        const chevronSvg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        chevronSvg.setAttribute('class', 'pk-select-chevron');
        chevronSvg.setAttribute('viewBox', '0 0 24 24');
        chevronSvg.setAttribute('fill', 'none');
        chevronSvg.setAttribute('stroke', 'currentColor');
        chevronSvg.setAttribute('stroke-width', '2');
        chevronSvg.setAttribute('stroke-linecap', 'round');
        chevronSvg.setAttribute('stroke-linejoin', 'round');
        chevronSvg.innerHTML = '<path d="m6 9 6 6 6-6"/>';

        trigger.appendChild(textSpan);
        trigger.appendChild(chevronSvg);
        wrap.appendChild(trigger);
        select.parentNode.insertBefore(wrap, select.nextSibling);

        // Dropdown Menu Portal gắn trực tiếp lên document.body để tránh bị cắt hoặc che bởi thẻ/bảng
        const menu = document.createElement('div');
        menu.className = 'pk-select-menu';
        menu.setAttribute('role', 'listbox');
        document.body.appendChild(menu);

        const isSearchable = select.dataset.searchable === 'true';
        let searchInput = null;
        let optionsContainer = menu;

        if (isSearchable) {
            const searchWrap = document.createElement('div');
            searchWrap.className = 'pk-select-search-wrap';
            searchWrap.addEventListener('click', (e) => e.stopPropagation());

            searchInput = document.createElement('input');
            searchInput.type = 'text';
            searchInput.className = 'pk-select-search-input';
            searchInput.placeholder = select.dataset.searchPlaceholder || 'Tìm kiếm...';
            searchInput.autocomplete = 'off';
            searchInput.addEventListener('click', (e) => e.stopPropagation());
            searchInput.addEventListener('keydown', (e) => {
                if (e.key === 'Escape') {
                    e.stopPropagation();
                    closeActiveDropdown();
                    trigger.focus();
                }
            });

            searchWrap.appendChild(searchInput);
            menu.appendChild(searchWrap);

            optionsContainer = document.createElement('div');
            optionsContainer.className = 'pk-select-options-list';
            menu.appendChild(optionsContainer);
        }

        const normalizeText = (str) => String(str || '')
            .toLowerCase()
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .replace(/đ/g, 'd');

        const filterMenuOptions = () => {
            if (!isSearchable || !searchInput) return;
            const rawQuery = searchInput.value.trim().toLowerCase();
            const normQuery = normalizeText(rawQuery);
            let visibleCount = 0;

            optionsContainer.querySelectorAll('.pk-select-option').forEach((btn) => {
                const rawLabel = (btn.dataset.searchLabel || '').toLowerCase();
                const normLabel = btn.dataset.searchNorm || '';
                const matched = !rawQuery || rawLabel.includes(rawQuery) || normLabel.includes(normQuery);
                btn.style.display = matched ? '' : 'none';
                if (matched) visibleCount++;
            });

            let emptyEl = optionsContainer.querySelector('.pk-select-empty');
            if (visibleCount === 0) {
                if (!emptyEl) {
                    emptyEl = document.createElement('div');
                    emptyEl.className = 'pk-select-empty';
                    emptyEl.textContent = 'Không tìm thấy kết quả phù hợp';
                    optionsContainer.appendChild(emptyEl);
                }
            } else if (emptyEl) {
                emptyEl.remove();
            }
        };

        if (searchInput) {
            searchInput.addEventListener('input', filterMenuOptions);
        }

        const syncTriggerAndMenu = () => {
            const selectedOption = select.options[select.selectedIndex] || select.options[0];
            if (selectedOption) {
                textSpan.textContent = selectedOption.textContent.trim();
                textSpan.classList.toggle('is-placeholder', selectedOption.disabled && selectedOption.value === '');
            } else {
                textSpan.textContent = '-- Chọn --';
                textSpan.classList.add('is-placeholder');
            }

            optionsContainer.innerHTML = '';
            Array.from(select.options).forEach((opt, idx) => {
                const labelText = opt.textContent.trim();
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'pk-select-option';
                btn.setAttribute('role', 'option');
                btn.dataset.searchLabel = labelText;
                btn.dataset.searchNorm = normalizeText(labelText);
                const isSelected = idx === select.selectedIndex;
                if (isSelected) {
                    btn.classList.add('is-selected');
                    btn.setAttribute('aria-selected', 'true');
                }
                if (opt.disabled) {
                    btn.disabled = true;
                }

                btn.innerHTML = `
                    <span class="pk-select-option-label">${window.escapeHtml(labelText)}</span>
                    <svg class="pk-select-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M20 6 9 17l-5-5"/>
                    </svg>
                `;

                btn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    if (opt.disabled) return;
                    const changed = select.selectedIndex !== idx;
                    select.selectedIndex = idx;
                    syncTriggerAndMenu();
                    closeActiveDropdown();
                    if (changed) {
                        select.dispatchEvent(new Event('change', { bubbles: true }));
                    }
                });

                optionsContainer.appendChild(btn);
            });

            if (isSearchable && searchInput && searchInput.value.trim()) {
                filterMenuOptions();
            }
        };

        const positionMenu = () => {
            const rect = trigger.getBoundingClientRect();
            const menuMaxH = isSearchable ? 300 : 260;
            const spaceBelow = window.innerHeight - rect.bottom - 12;
            const spaceAbove = rect.top - 12;
            const openUpward = spaceBelow < 180 && spaceAbove > spaceBelow;

            menu.style.minWidth = `${rect.width}px`;
            menu.style.maxWidth = `${Math.min(window.innerWidth - 24, Math.max(rect.width, 280))}px`;

            let left = rect.left;
            const estimatedWidth = Math.max(rect.width, menu.offsetWidth || rect.width);
            if (left + estimatedWidth > window.innerWidth - 12) {
                left = Math.max(12, window.innerWidth - estimatedWidth - 12);
            }
            menu.style.left = `${left}px`;

            if (openUpward) {
                menu.classList.add('is-upward');
                menu.style.top = 'auto';
                menu.style.bottom = `${window.innerHeight - rect.top + 6}px`;
                menu.style.maxHeight = `${Math.min(menuMaxH, spaceAbove)}px`;
            } else {
                menu.classList.remove('is-upward');
                menu.style.bottom = 'auto';
                menu.style.top = `${rect.bottom + 6}px`;
                menu.style.maxHeight = `${Math.min(menuMaxH, Math.max(140, spaceBelow))}px`;
            }
        };

        trigger.addEventListener('click', (e) => {
            e.stopPropagation();
            const isSameOpen = window.__pkActiveDropdown && window.__pkActiveDropdown.trigger === trigger;
            closeActiveDropdown();
            if (!isSameOpen) {
                if (searchInput) searchInput.value = '';
                syncTriggerAndMenu();
                positionMenu();
                trigger.classList.add('is-open');
                trigger.setAttribute('aria-expanded', 'true');
                menu.classList.add('is-open');
                window.__pkActiveDropdown = { trigger, menu, select };

                if (searchInput) {
                    setTimeout(() => searchInput.focus(), 20);
                } else {
                    const activeItem = optionsContainer.querySelector('.pk-select-option.is-selected');
                    if (activeItem) {
                        activeItem.scrollIntoView({ block: 'nearest' });
                    }
                }
            }
        });

        // Hỗ trợ mở nhanh bằng phím mũi tên xuống / Space / Enter
        trigger.addEventListener('keydown', (e) => {
            if (e.key === 'ArrowDown' || e.key === 'Enter' || e.key === ' ') {
                if (!trigger.classList.contains('is-open')) {
                    e.preventDefault();
                    trigger.click();
                }
            }
        });

        // Đồng bộ 2 chiều khi JS bên ngoài thay đổi select.value trực tiếp (VD: resetFilter / applyPresetFilter)
        if (nativeValueDescriptor && nativeValueDescriptor.set && nativeValueDescriptor.get) {
            Object.defineProperty(select, 'value', {
                configurable: true,
                get() {
                    return nativeValueDescriptor.get.call(this);
                },
                set(val) {
                    nativeValueDescriptor.set.call(this, val);
                    syncTriggerAndMenu();
                }
            });
        }

        select.addEventListener('change', syncTriggerAndMenu);

        // Xử lý validation HTML5 required khi người dùng chưa chọn mục hợp lệ
        select.addEventListener('invalid', (e) => {
            e.preventDefault();
            trigger.focus();
            if (!trigger.classList.contains('is-open')) {
                trigger.click();
            }
            if (window.showToast) {
                window.showToast('info', 'Vui lòng chọn một mục', 'Hãy chọn giá trị hợp lệ từ danh sách.');
            }
        });

        syncTriggerAndMenu();
    });
};

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        window.toggleMobileNavDrawer(true);
    }
});

document.addEventListener('DOMContentLoaded', () => {
    // Đảm bảo Drawer & Backdrop luôn nằm trực tiếp trước thẻ đóng </body>
    const backdrop = document.getElementById('mobile-nav-backdrop');
    const drawer = document.getElementById('mobile-nav-drawer');
    if (backdrop && backdrop.parentElement !== document.body) {
        document.body.appendChild(backdrop);
    }
    if (drawer && drawer.parentElement !== document.body) {
        document.body.appendChild(drawer);
    }
    window.syncThemeIcons();
    window.initCustomSelects(document);
    window.bindHoloTiltEffects(document);
});
