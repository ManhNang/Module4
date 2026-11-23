/**
 * Pokémon Blindbox — Dynamic 6-Tier Card Reveal VFX Engine (GPU-Friendly 60fps)
 * -----------------------------------------------------------------------------
 * Đọc động 100% từ trường `rarityRevealEffect` (Enum từ Database), không hardcode tên Độ hiếm.
 * Hỗ trợ chuỗi: Tụ lực (Anticipation) -> Bùng nổ (Reveal) -> Hào quang giữ lại (Afterglow),
 * kèm tính năng Click-to-Skip và Modal Xem thử cho Admin Dashboard.
 */
(function (global) {
    'use strict';

    const PRESETS = {
        NONE: {
            code: 'NONE',
            rank: 0,
            label: 'Mặc định (Không hiệu ứng đặc biệt)',
            badge: 'Mặc định',
            anticipationMs: 0,
            slowMo: false,
            dimLevel: null,
            tearTier: 'normal',
            accent: '#94A3B8'
        },
        DOUBLE_RARE: {
            code: 'DOUBLE_RARE',
            rank: 1,
            label: 'Double Rare — Lóe xanh lam/tím & lấp lánh',
            badge: 'Double Rare',
            anticipationMs: 180,
            slowMo: false,
            dimLevel: null,
            tearTier: 'ex',
            accent: '#818CF8'
        },
        ILLUSTRATION: {
            code: 'ILLUSTRATION',
            rank: 2,
            label: 'Illustration Rare — Khựng 0.25s & bụi sao tím pastel',
            badge: 'Illustration',
            anticipationMs: 260,
            slowMo: false,
            dimLevel: null,
            tearTier: 'ex',
            accent: '#C084FC'
        },
        ULTRA: {
            code: 'ULTRA',
            rank: 3,
            label: 'Ultra Rare — Tối nền nhẹ, rung tích lực & sóng xung kích cam vàng',
            badge: 'Ultra',
            anticipationMs: 420,
            slowMo: false,
            dimLevel: 'soft',
            tearTier: 'ex',
            accent: '#FB923C'
        },
        SPECIAL_ILLUSTRATION: {
            code: 'SPECIAL_ILLUSTRATION',
            rank: 4,
            label: 'Special Illustration — Tia cầu vồng quét chéo & lật chậm slow-mo',
            badge: 'Special IR',
            anticipationMs: 520,
            slowMo: true,
            dimLevel: 'medium',
            tearTier: 'hyper',
            accent: '#EC4899'
        },
        FUTURISTIC: {
            code: 'FUTURISTIC',
            rank: 5,
            label: 'Futuristic Rare — Cyber Glitch, laser lưới đỏ/cyan & viền neon',
            badge: 'Futuristic',
            anticipationMs: 600,
            slowMo: false,
            dimLevel: 'medium',
            tearTier: 'hyper',
            accent: '#06B6D4'
        },
        MEGA_HYPER: {
            code: 'MEGA_HYPER',
            rank: 6,
            label: 'Mega Hyper Rare — Jackpot tụ năng lượng vàng kim & mưa hạt vàng',
            badge: 'Mega Hyper',
            anticipationMs: 880,
            slowMo: true,
            dimLevel: 'deep',
            tearTier: 'hyper',
            accent: '#F59E0B'
        }
    };

    const PRESET_LIST = Object.values(PRESETS);

    function normalizeEffect(raw) {
        const key = String(raw || 'NONE').trim().toUpperCase();
        return PRESETS[key] ? key : 'NONE';
    }

    function getPreset(raw) {
        return PRESETS[normalizeEffect(raw)];
    }

    function getRank(raw) {
        return getPreset(raw).rank;
    }

    function isReducedMotion() {
        return Boolean(global.matchMedia && global.matchMedia('(prefers-reduced-motion: reduce)').matches);
    }

    // ==================== SHARED GLOBAL OVERLAY LAYER (DIM + FLASH + PARTICLES) ====================
    let overlayRefs = null;
    let activeDimOwners = 0;

    function ensureOverlayLayer() {
        if (overlayRefs && document.body.contains(overlayRefs.root)) {
            return overlayRefs;
        }
        const root = document.createElement('div');
        root.id = 'pk-reveal-vfx-root';
        root.className = 'pk-reveal-vfx-root';
        root.innerHTML = `
            <div class="pk-reveal-backdrop" id="pk-reveal-backdrop"></div>
            <div class="pk-reveal-flash" id="pk-reveal-flash"></div>
            <div class="pk-reveal-particles" id="pk-reveal-particles"></div>
            <div class="pk-reveal-skip-hint" id="pk-reveal-skip-hint">Bấm bất kỳ đâu để bỏ qua hiệu ứng</div>
        `;
        document.body.appendChild(root);
        overlayRefs = {
            root,
            backdrop: root.querySelector('#pk-reveal-backdrop'),
            flash: root.querySelector('#pk-reveal-flash'),
            particles: root.querySelector('#pk-reveal-particles'),
            skipHint: root.querySelector('#pk-reveal-skip-hint')
        };
        return overlayRefs;
    }

    function setBackdropDim(level, onSkip) {
        const refs = ensureOverlayLayer();
        refs.backdrop.classList.remove('is-dim-soft', 'is-dim-medium', 'is-dim-deep', 'is-interactive');
        refs.skipHint.classList.remove('is-visible');
        refs.backdrop.onclick = null;
        if (!level) return;

        activeDimOwners++;
        refs.backdrop.classList.add(`is-dim-${level}`);
        if (typeof onSkip === 'function') {
            refs.backdrop.classList.add('is-interactive');
            refs.skipHint.classList.add('is-visible');
            refs.backdrop.onclick = () => onSkip();
        }
    }

    function clearBackdropDim() {
        if (!overlayRefs) return;
        activeDimOwners = Math.max(0, activeDimOwners - 1);
        if (activeDimOwners === 0) {
            overlayRefs.backdrop.classList.remove('is-dim-soft', 'is-dim-medium', 'is-dim-deep', 'is-interactive');
            overlayRefs.skipHint.classList.remove('is-visible');
            overlayRefs.backdrop.onclick = null;
        }
    }

    function triggerScreenFlash(mode) {
        if (isReducedMotion()) return;
        const refs = ensureOverlayLayer();
        const flash = refs.flash;
        if (mode === 'MEGA_HYPER') {
            flash.style.background = 'radial-gradient(circle, rgba(255,255,255,0.92) 0%, rgba(251,191,36,0.55) 55%, rgba(245,158,11,0.18) 100%)';
        } else if (mode === 'FUTURISTIC') {
            flash.style.background = 'radial-gradient(circle, rgba(224,252,255,0.88) 0%, rgba(6,182,212,0.45) 48%, rgba(244,63,94,0.32) 100%)';
        } else if (mode === 'SPECIAL_ILLUSTRATION') {
            flash.style.background = 'radial-gradient(circle, rgba(255,255,255,0.8) 0%, rgba(236,72,153,0.42) 50%, rgba(56,189,248,0.28) 100%)';
        } else if (mode === 'ULTRA') {
            flash.style.background = 'radial-gradient(circle, rgba(255,251,235,0.75) 0%, rgba(251,146,60,0.4) 55%, transparent 100%)';
        } else {
            return;
        }
        flash.style.opacity = '1';
        setTimeout(() => {
            flash.style.opacity = '0';
        }, mode === 'MEGA_HYPER' ? 220 : 150);
    }

    // ==================== LIGHTWEIGHT GPU PARTICLE SPAWNERS ====================
    function spawnParticlesAroundRect(rect, effectCode) {
        if (isReducedMotion() || !rect) return;
        const refs = ensureOverlayLayer();
        const layer = refs.particles;
        const cx = rect.left + rect.width / 2;
        const cy = rect.top + rect.height / 2;
        const frag = document.createDocumentFragment();
        const nodesToCleanup = [];

        if (effectCode === 'DOUBLE_RARE') {
            // Tia lấp lánh xanh lam/tím xung quanh viền thẻ
            const colors = ['#818CF8', '#60A5FA', '#C4B5FD', '#FFFFFF'];
            for (let i = 0; i < 14; i++) {
                const p = document.createElement('span');
                const angle = (Math.PI * 2 * i) / 14 + (Math.random() - 0.5) * 0.3;
                const dist = 55 + Math.random() * 65;
                p.className = 'pk-vfx-sparkle';
                p.style.left = `${cx}px`;
                p.style.top = `${cy}px`;
                p.style.setProperty('--tx', `${Math.cos(angle) * dist}px`);
                p.style.setProperty('--ty', `${Math.sin(angle) * dist}px`);
                p.style.setProperty('--c', colors[i % colors.length]);
                p.style.animationDelay = `${Math.random() * 80}ms`;
                frag.appendChild(p);
                nodesToCleanup.push(p);
            }
        } else if (effectCode === 'ILLUSTRATION') {
            // Hạt bụi sao tím pastel bay nhẹ lên trên
            const colors = ['#C084FC', '#E9D5FF', '#F0ABFC', '#FFFFFF'];
            for (let i = 0; i < 18; i++) {
                const p = document.createElement('span');
                const sx = rect.left + Math.random() * rect.width;
                const sy = rect.top + rect.height * (0.25 + Math.random() * 0.75);
                p.className = 'pk-vfx-stardust';
                p.style.left = `${sx}px`;
                p.style.top = `${sy}px`;
                p.style.setProperty('--tx', `${(Math.random() - 0.5) * 44}px`);
                p.style.setProperty('--ty', `${-75 - Math.random() * 85}px`);
                p.style.setProperty('--c', colors[i % colors.length]);
                p.style.animationDelay = `${Math.random() * 220}ms`;
                frag.appendChild(p);
                nodesToCleanup.push(p);
            }
        } else if (effectCode === 'ULTRA') {
            // Vòng sóng xung kích cam vàng + tia lửa
            const ring = document.createElement('span');
            ring.className = 'pk-vfx-shockwave';
            ring.style.left = `${cx}px`;
            ring.style.top = `${cy}px`;
            frag.appendChild(ring);
            nodesToCleanup.push(ring);

            const colors = ['#FB923C', '#FBBF24', '#FDE68A', '#FFFFFF'];
            for (let i = 0; i < 22; i++) {
                const p = document.createElement('span');
                const angle = Math.random() * Math.PI * 2;
                const dist = 70 + Math.random() * 95;
                p.className = 'pk-vfx-ember';
                p.style.left = `${cx}px`;
                p.style.top = `${cy}px`;
                p.style.setProperty('--tx', `${Math.cos(angle) * dist}px`);
                p.style.setProperty('--ty', `${Math.sin(angle) * dist}px`);
                p.style.setProperty('--c', colors[i % colors.length]);
                frag.appendChild(p);
                nodesToCleanup.push(p);
            }
        } else if (effectCode === 'SPECIAL_ILLUSTRATION') {
            // Bung hạt sáng đa sắc (hồng / cầu vồng)
            const colors = ['#EC4899', '#F472B6', '#38BDF8', '#A855F7', '#FBBF24', '#34D399', '#FFFFFF'];
            for (let i = 0; i < 30; i++) {
                const p = document.createElement('span');
                const angle = Math.random() * Math.PI * 2;
                const dist = 80 + Math.random() * 140;
                p.className = 'pk-vfx-prism-shard';
                p.style.left = `${cx}px`;
                p.style.top = `${cy}px`;
                p.style.setProperty('--tx', `${Math.cos(angle) * dist}px`);
                p.style.setProperty('--ty', `${Math.sin(angle) * dist - 20}px`);
                p.style.setProperty('--rot', `${Math.round(Math.random() * 540 - 270)}deg`);
                p.style.setProperty('--c', colors[i % colors.length]);
                p.style.animationDelay = `${Math.random() * 90}ms`;
                frag.appendChild(p);
                nodesToCleanup.push(p);
            }
        } else if (effectCode === 'FUTURISTIC') {
            // Cyber shards đỏ-neon / cyan sắc lạnh
            const ring = document.createElement('span');
            ring.className = 'pk-vfx-cyber-pulse';
            ring.style.left = `${cx}px`;
            ring.style.top = `${cy}px`;
            frag.appendChild(ring);
            nodesToCleanup.push(ring);

            const colors = ['#06B6D4', '#22D3EE', '#F43F5E', '#FFFFFF'];
            for (let i = 0; i < 26; i++) {
                const p = document.createElement('span');
                const angle = Math.random() * Math.PI * 2;
                const dist = 75 + Math.random() * 120;
                p.className = 'pk-vfx-cyber-bit';
                p.style.left = `${cx}px`;
                p.style.top = `${cy}px`;
                p.style.setProperty('--tx', `${Math.cos(angle) * dist}px`);
                p.style.setProperty('--ty', `${Math.sin(angle) * dist}px`);
                p.style.setProperty('--c', colors[i % colors.length]);
                frag.appendChild(p);
                nodesToCleanup.push(p);
            }
        } else if (effectCode === 'MEGA_HYPER') {
            // Vòng hào quang vàng kim xoay chậm + mưa hạt vàng rơi toàn màn hình
            const halo = document.createElement('span');
            halo.className = 'pk-vfx-jackpot-halo';
            halo.style.left = `${cx}px`;
            halo.style.top = `${cy}px`;
            frag.appendChild(halo);
            nodesToCleanup.push(halo);

            const colors = ['#F59E0B', '#FBBF24', '#FDE047', '#FEF08A', '#FFFFFF'];
            for (let i = 0; i < 42; i++) {
                const p = document.createElement('span');
                p.className = 'pk-vfx-gold-rain';
                p.style.left = `${8 + Math.random() * 84}vw`;
                p.style.top = `${-15 - Math.random() * 30}px`;
                p.style.setProperty('--tx', `${(Math.random() - 0.5) * 90}px`);
                p.style.setProperty('--rot', `${Math.round(Math.random() * 720 - 360)}deg`);
                p.style.setProperty('--c', colors[i % colors.length]);
                p.style.animationDelay = `${Math.random() * 480}ms`;
                frag.appendChild(p);
                nodesToCleanup.push(p);
            }
        }

        layer.appendChild(frag);
        const cleanupMs = effectCode === 'MEGA_HYPER' ? 2100 : 1200;
        setTimeout(() => {
            nodesToCleanup.forEach(n => n.remove());
        }, cleanupMs);
    }

    // ==================== MAIN REVEAL CHOREOGRAPHER (WITH CLICK-TO-SKIP) ====================
    const activeSessions = new WeakMap();

    function cleanupSceneFxNodes(sceneEl) {
        sceneEl.querySelectorAll('.pk-card-fx-layer').forEach(n => n.remove());
        sceneEl.classList.remove(
            'is-anticipation',
            'pk-pre-double-rare',
            'pk-pre-illustration',
            'pk-pre-ultra',
            'pk-pre-special-illustration',
            'pk-pre-futuristic',
            'pk-pre-mega-hyper',
            'pk-flip-slowmo',
            'pk-jackpot-focus'
        );
    }

    function attachPreRevealOverlay(sceneEl, effectCode) {
        const inner = sceneEl.querySelector('.pk-flip-inner') || sceneEl;
        const fx = document.createElement('div');
        fx.className = `pk-card-fx-layer pk-card-fx-${effectCode.toLowerCase().replace(/_/g, '-')}`;
        if (effectCode === 'FUTURISTIC') {
            fx.innerHTML = '<div class="pk-cyber-grid"></div><div class="pk-cyber-laser"></div>';
        } else if (effectCode === 'SPECIAL_ILLUSTRATION') {
            fx.innerHTML = '<div class="pk-prism-beam"></div>';
        } else if (effectCode === 'MEGA_HYPER') {
            fx.innerHTML = '<div class="pk-gold-converge"></div>';
        } else if (effectCode === 'DOUBLE_RARE') {
            fx.innerHTML = '<div class="pk-edge-flash"></div>';
        }
        inner.appendChild(fx);
        return fx;
    }

    /**
     * Skip ngay lập tức hiệu ứng đang chạy trên 1 lá bài nếu người dùng bấm thêm lần nữa.
     * Trả về true nếu lá bài đang chạy và vừa được skip.
     */
    function skipRevealIfActive(sceneEl) {
        const session = activeSessions.get(sceneEl);
        if (!session) return false;
        session.finish(true);
        return true;
    }

    /**
     * Chạy chuỗi hiệu ứng mở thẻ: Tụ lực (Anticipation) -> Bùng nổ (Reveal) -> Hào quang (Afterglow).
     * @param {HTMLElement} sceneEl phần tử .pk-flip-scene
     * @param {Object} opts { revealEffect, colorHex, isHolo, isBulkFlip, onComplete }
     */
    function playCardReveal(sceneEl, opts) {
        if (!sceneEl) return;
        if (sceneEl.classList.contains('is-flipped')) return;

        // Nếu đang chạy hiệu ứng -> click lần 2 sẽ skip ngay!
        if (skipRevealIfActive(sceneEl)) return;

        const effectCode = normalizeEffect(opts?.revealEffect);
        const preset = PRESETS[effectCode];
        const colorHex = opts?.colorHex || preset.accent;
        const isBulk = Boolean(opts?.isBulkFlip);
        const reduced = isReducedMotion();

        sceneEl.style.setProperty('--reveal-accent', colorHex);
        sceneEl.dataset.revealing = 'true';

        let finished = false;
        let timerId = null;
        let dimActive = false;

        const finish = (skipped) => {
            if (finished) return;
            finished = true;
            if (timerId) {
                clearTimeout(timerId);
                timerId = null;
            }
            activeSessions.delete(sceneEl);

            if (dimActive) {
                dimActive = false;
                clearBackdropDim();
            }

            cleanupSceneFxNodes(sceneEl);

            if (!skipped && !reduced && preset.slowMo && !isBulk) {
                sceneEl.classList.add('pk-flip-slowmo');
                setTimeout(() => sceneEl.classList.remove('pk-flip-slowmo'), 950);
            }

            sceneEl.classList.add('is-flipped');
            delete sceneEl.dataset.revealing;

            // Áp dụng hào quang giữ lại (Afterglow) cho mặt trước theo từng preset
            const frontEl = sceneEl.querySelector('.pk-flip-front');
            if (frontEl && effectCode !== 'NONE') {
                const afterglowClass = `pk-afterglow-${effectCode.toLowerCase().replace(/_/g, '-')}`;
                frontEl.classList.add(afterglowClass);
                if (preset.rank >= 2 || opts?.isHolo) {
                    frontEl.classList.add('pk-holo-shimmer');
                }
            }

            if (!skipped && !reduced && effectCode !== 'NONE') {
                const rect = sceneEl.getBoundingClientRect();
                if (!isBulk || preset.rank >= 4) {
                    triggerScreenFlash(effectCode);
                }
                if (!isBulk || preset.rank >= 3) {
                    spawnParticlesAroundRect(rect, effectCode);
                }
            }

            if (typeof opts?.onComplete === 'function') {
                opts.onComplete({ effectCode, preset, skipped });
            }
        };

        activeSessions.set(sceneEl, { finish });

        // Không hiệu ứng hoặc giảm chuyển động -> lật ngay
        if (effectCode === 'NONE' || reduced) {
            finish(false);
            return;
        }

        // Khi bấm "Lật tất cả" nhiều thẻ, rút ngắn thời gian tụ lực nhưng vẫn giữ kịch tính cho bậc cao
        const waitMs = isBulk
            ? Math.min(preset.anticipationMs, preset.rank >= 5 ? 420 : 180)
            : preset.anticipationMs;

        // 1. Giai đoạn Tụ lực (Anticipation)
        const preClass = `pk-pre-${effectCode.toLowerCase().replace(/_/g, '-')}`;
        sceneEl.classList.add('is-anticipation', preClass);
        if (effectCode === 'MEGA_HYPER' && !isBulk) {
            sceneEl.classList.add('pk-jackpot-focus');
        }
        attachPreRevealOverlay(sceneEl, effectCode);

        if (preset.dimLevel && !isBulk) {
            dimActive = true;
            setBackdropDim(preset.dimLevel, () => finish(true));
        }

        // 2. Giai đoạn Bùng nổ (Reveal) sau thời gian tụ lực
        timerId = setTimeout(() => finish(false), waitMs);
    }

    /**
     * Xác định cấp độ hiệu ứng cao nhất trong danh sách lá bài (dùng cho tia sáng xé vỏ gói).
     */
    function getHighestEffect(drawnCards) {
        let best = 'NONE';
        let bestRank = 0;
        (drawnCards || []).forEach(uc => {
            const c = uc?.card || uc || {};
            const eff = normalizeEffect(c.rarityRevealEffect || c.revealEffect);
            const r = getRank(eff);
            if (r > bestRank) {
                bestRank = r;
                best = eff;
            }
        });
        return best;
    }

    /**
     * Class hào quang mặt lưng thẻ (úc úp) dựa hoàn toàn trên revealEffect + isHolographic (không hardcode tên).
     */
    function getBackAuraClass(revealEffect, isHolo) {
        const eff = normalizeEffect(revealEffect);
        if (eff === 'MEGA_HYPER') return 'pk-aura-mega-hyper';
        if (eff === 'FUTURISTIC') return 'pk-aura-futuristic';
        if (eff === 'SPECIAL_ILLUSTRATION') return 'pk-aura-special-illustration';
        if (eff === 'ULTRA') return 'pk-aura-ultra';
        if (eff === 'ILLUSTRATION') return 'pk-aura-illustration';
        if (eff === 'DOUBLE_RARE') return 'pk-aura-double-rare';
        return isHolo ? 'pk-aura-rare' : 'pk-aura-common';
    }

    // ==================== ADMIN LIVE PREVIEW MODAL ("XEM THỬ" TRONG TAB ĐỘ HIẾM) ====================
    let previewModalEl = null;

    function openPreviewModal(effectRaw, options) {
        const effectCode = normalizeEffect(effectRaw);
        const preset = PRESETS[effectCode];
        const rarityName = options?.rarityName || preset.badge;
        const colorHex = options?.colorHex || preset.accent;
        const isHolo = Boolean(options?.isHolographic || preset.rank >= 1);

        if (!previewModalEl || !document.body.contains(previewModalEl)) {
            previewModalEl = document.createElement('div');
            previewModalEl.id = 'pk-vfx-preview-modal';
            previewModalEl.className = 'fixed inset-0 z-[80] hidden items-center justify-center bg-slate-950/85 backdrop-blur-sm p-4';
            previewModalEl.innerHTML = `
                <div class="relative w-full max-w-md rounded-3xl bg-slate-900 border border-white/15 p-6 shadow-2xl text-center space-y-5">
                    <div class="flex items-center justify-between gap-2 border-b border-white/10 pb-3 text-left">
                        <div>
                            <h4 class="text-sm font-extrabold text-white" id="pk-preview-title">Xem thử hiệu ứng mở thẻ</h4>
                            <p class="text-[11px] text-slate-400" id="pk-preview-subtitle">Bấm vào lá bài để lật hoặc bấm lần nữa để skip</p>
                        </div>
                        <button type="button" id="pk-preview-close-btn"
                                class="h-8 w-8 rounded-full bg-white/10 hover:bg-white/20 text-white text-sm font-bold cursor-pointer">✕</button>
                    </div>
                    <div class="flex items-center justify-center py-4 min-h-[300px]" id="pk-preview-stage"></div>
                    <div class="flex items-center justify-center gap-2.5 pt-1">
                        <button type="button" id="pk-preview-replay-btn"
                                class="rounded-xl bg-amber-400 hover:bg-amber-300 text-slate-950 px-4 py-2 text-xs font-extrabold cursor-pointer transition">
                            ↻ Chạy lại hiệu ứng
                        </button>
                        <button type="button" id="pk-preview-done-btn"
                                class="rounded-xl bg-white/10 hover:bg-white/20 text-slate-200 px-4 py-2 text-xs font-bold cursor-pointer transition">
                            Đóng
                        </button>
                    </div>
                </div>
            `;
            document.body.appendChild(previewModalEl);
            const closeFn = () => {
                previewModalEl.classList.add('hidden');
                previewModalEl.classList.remove('flex');
                activeDimOwners = 0;
                clearBackdropDim();
            };
            previewModalEl.addEventListener('click', (e) => {
                if (e.target === previewModalEl) closeFn();
            });
            previewModalEl.querySelector('#pk-preview-close-btn').onclick = closeFn;
            previewModalEl.querySelector('#pk-preview-done-btn').onclick = closeFn;
        }

        const stage = previewModalEl.querySelector('#pk-preview-stage');
        const titleEl = previewModalEl.querySelector('#pk-preview-title');
        const subEl = previewModalEl.querySelector('#pk-preview-subtitle');
        titleEl.textContent = `Xem thử: ${rarityName}`;
        subEl.textContent = preset.label;

        const mountAndRun = () => {
            const esc = global.escapeHtml || (v => String(v ?? ''));
            const auraClass = getBackAuraClass(effectCode, isHolo);
            stage.innerHTML = `
                <div class="w-[190px]">
                    <div class="pk-flip-scene" id="pk-preview-card-scene">
                        <div class="pk-flip-inner">
                            <div class="pk-flip-face pk-flip-back ${auraClass} flex flex-col items-center justify-between p-4 text-center select-none overflow-hidden">
                                <span class="pk-num text-[10px] font-extrabold tracking-widest text-slate-300/80 uppercase">PREVIEW</span>
                                <div class="flex h-14 w-14 items-center justify-center rounded-full border-2 border-amber-300/60 bg-indigo-950/80">
                                    <svg class="h-8 w-8 text-amber-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75">
                                        <circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="2.5"/>
                                        <line x1="3" y1="12" x2="9.5" y2="12"/><line x1="14.5" y1="12" x2="21" y2="12"/>
                                    </svg>
                                </div>
                                <span class="text-[11px] font-extrabold text-amber-300">${esc(preset.badge)}</span>
                            </div>
                            <div class="pk-flip-face pk-flip-front ${isHolo ? 'pk-holo-card is-holo-active' : ''} relative overflow-hidden bg-slate-900 border-2 shadow-xl flex flex-col items-center justify-between p-3"
                                 style="border-color: ${esc(colorHex)};">
                                <img src="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/384.png"
                                     alt="Rayquaza Preview"
                                     class="w-full h-[78%] object-contain drop-shadow-xl relative z-10"/>
                                <div class="relative z-10 text-center w-full">
                                    <div class="text-xs font-black text-white truncate">Rayquaza Preview</div>
                                    <span class="inline-block mt-0.5 rounded-full border px-2 py-0.5 text-[9.5px] font-extrabold"
                                          style="border-color: ${esc(colorHex)}; color: ${esc(colorHex)}">${esc(rarityName)}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            const scene = stage.querySelector('#pk-preview-card-scene');
            scene.onclick = () => {
                playCardReveal(scene, {
                    revealEffect: effectCode,
                    colorHex,
                    isHolo,
                    isBulkFlip: false
                });
            };
            // Tự động chạy hiệu ứng sau 260ms để Admin xem ngay lập tức
            setTimeout(() => {
                if (document.body.contains(scene) && !scene.classList.contains('is-flipped')) {
                    playCardReveal(scene, {
                        revealEffect: effectCode,
                        colorHex,
                        isHolo,
                        isBulkFlip: false
                    });
                }
            }, 260);
        };

        previewModalEl.querySelector('#pk-preview-replay-btn').onclick = mountAndRun;
        previewModalEl.classList.remove('hidden');
        previewModalEl.classList.add('flex');
        mountAndRun();
    }

    global.PokemonRevealVfx = {
        PRESETS,
        PRESET_LIST,
        normalizeEffect,
        getPreset,
        getRank,
        getHighestEffect,
        getBackAuraClass,
        playCardReveal,
        skipRevealIfActive,
        openPreviewModal
    };
})(window);
