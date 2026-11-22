/**
 * SpaceRust Launcher — Main UI logic (Rust style redesign)
 */
document.addEventListener('DOMContentLoaded', () => {
    const $ = (id) => document.getElementById(id);
    const api = () => (window.pywebview && window.pywebview.api) ? window.pywebview.api : null;
    const sfx = (name) => { if (window.soundEngine && window.soundEngine[name]) window.soundEngine[name](); };

    // ==========================================
    // STATE (no default subscription; promo grants privileges)
    // ==========================================
    if (!localStorage.getItem('sr_sub_migrated')) {
        localStorage.setItem('sr_rank', 'ИГРОК');
        localStorage.removeItem('sr_redeemed_codes');
        localStorage.setItem('sr_sub_migrated', '1');
    }
    localStorage.removeItem('sr_balance');
    localStorage.removeItem('sr_scrap');

    const STATE = {
        ramMB: parseInt(localStorage.getItem('sr_ram_mb') || '6144', 10),
        nickname: localStorage.getItem('sr_nickname') || ('Player_' + Math.floor(100 + Math.random() * 900)),
        rank: localStorage.getItem('sr_rank') || 'ИГРОК',
        redeemedCodes: JSON.parse(localStorage.getItem('sr_redeemed_codes') || '[]'),
        soundEnabled: localStorage.getItem('sr_sound_enabled') !== 'false',
        soundVolume: parseFloat(localStorage.getItem('sr_sound_vol') || '0.6'),
        animations: localStorage.getItem('sr_anim') !== 'false',
        optGC: localStorage.getItem('sr_opt_gc') !== 'false',
        isLaunching: false,
        isPaused: false
    };

    if (window.soundEngine) {
        window.soundEngine.setEnabled(STATE.soundEnabled);
        window.soundEngine.setVolume(STATE.soundVolume);
    }

    // ==========================================
    // DOM
    // ==========================================
    const tabButtons = document.querySelectorAll('.tab-btn');
    const tabViews = document.querySelectorAll('.tab-view');

    const profileCard = $('user-profile-btn');
    const avatarImg = $('user-avatar-img');
    const rankEl = document.querySelector('.user-rank');
    const modalRankEl = $('modal-rank-badge');
    const dockNickEl = $('dock-nick-display');
    const soundBtn = $('sound-toggle-btn');

    const btnPlay = $('btn-play');
    const btnPlayText = $('btn-play-text');
    const loadLine = document.querySelector('.load-line');
    const loadFill = $('launch-progress-bar');
    const loadTitle = $('load-title');
    const loadStage = $('load-stage');
    const loadPct = $('load-pct');
    const btnPause = $('btn-dl-pause');
    const btnResume = $('btn-dl-resume');

    const drawer = $('settings-drawer');
    const drawerBackdrop = $('settings-backdrop');

    const accountModal = $('account-modal');
    const modalAvatar = $('modal-avatar-preview');
    const modalNick = $('modal-display-nick');
    const modalLockedNick = $('modal-locked-nick');
    const modalSubStatus = $('modal-sub-status');

    const avatarUrl = (nick) => `https://mc-heads.net/avatar/${encodeURIComponent(nick || 'Steve')}/64`;
    [avatarImg, modalAvatar].forEach(img => {
        if (img) img.addEventListener('error', () => { img.src = 'assets/logo.png'; });
    });

    // ==========================================
    // TABS
    // ==========================================
    function switchTab(target) {
        tabButtons.forEach(b => b.classList.toggle('active', b.dataset.tab === target));
        tabViews.forEach(v => v.classList.toggle('active-view', v.id === `view-${target}`));
    }

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            if (btn.classList.contains('active')) return;
            sfx('playClick');
            switchTab(btn.dataset.tab);
        });
    });

    $('btn-notify')?.addEventListener('click', () => {
        sfx('playClick');
        const dot = document.querySelector('.notify-dot');
        if (dot) dot.remove();
        switchTab('news');
        showToast('Вышел патч v2.5.0 — Rust-крафт и новый лаунчер', 'success');
    });

    // ==========================================
    // PROFILE / ACCOUNT (nickname is locked)
    // ==========================================
    function updateProfileUI() {
        if (avatarImg) avatarImg.src = avatarUrl(STATE.nickname);
        if (dockNickEl) dockNickEl.textContent = STATE.nickname;
        if (modalNick) modalNick.textContent = STATE.nickname;
        if (modalLockedNick) modalLockedNick.textContent = STATE.nickname;
        if (modalAvatar) modalAvatar.src = avatarUrl(STATE.nickname);
        const rankText = STATE.rank || 'ИГРОК';
        const hasSub = !!STATE.rank && STATE.rank !== 'ИГРОК';
        if (modalSubStatus) {
            modalSubStatus.textContent = hasSub ? `Активна привилегия: ${rankText}` : 'Нет активной подписки';
        }
        [rankEl, modalRankEl].forEach(el => {
            if (!el) return;
            el.textContent = rankText;
            el.classList.toggle('has-sub', hasSub);
        });
    }

    function openAccountModal() {
        sfx('playClick');
        updateProfileUI();
        accountModal.classList.add('show');
    }
    const closeAccountModal = () => accountModal.classList.remove('show');

    profileCard?.addEventListener('click', openAccountModal);
    $('modal-account-close')?.addEventListener('click', closeAccountModal);
    accountModal?.addEventListener('click', (e) => { if (e.target === accountModal) closeAccountModal(); });

    async function checkSystemUsername() {
        if (localStorage.getItem('sr_nickname')) return;
        try {
            const a = api();
            if (a && a.get_system_username) {
                const sysName = await a.get_system_username();
                if (sysName && sysName.length >= 3) {
                    STATE.nickname = sysName;
                    localStorage.setItem('sr_nickname', sysName);
                    updateProfileUI();
                }
            }
        } catch (err) { console.log('username lookup error', err); }
    }
    window.addEventListener('pywebviewready', checkSystemUsername);
    setTimeout(checkSystemUsername, 800);

    // ==========================================
    // PROMO CODES (PRIVILEGES)
    // ==========================================
    const promoInput = $('promo-input');
    function redeemPromo() {
        const code = promoInput.value.trim().toUpperCase();
        if (!code) return showToast('Введите промокод!', 'error');
        if (STATE.redeemedCodes.includes(code)) return showToast('Этот промокод уже активирован!', 'error');

        const promos = {
            'WIPE2026': { rank: 'VIP', label: 'Привилегия VIP' },
            'SPACEVIP': { rank: 'VIP+', label: 'Привилегия VIP+' },
            'RUSTME': { rank: 'PREMIUM', label: 'Привилегия PREMIUM' },
            'SPACERUST': { rank: 'ELITE', label: 'Привилегия ELITE' }
        };
        const reward = promos[code];
        if (!reward) return showToast('Неверный промокод или срок действия истёк', 'error');

        STATE.rank = reward.rank;
        localStorage.setItem('sr_rank', STATE.rank);
        STATE.redeemedCodes.push(code);
        localStorage.setItem('sr_redeemed_codes', JSON.stringify(STATE.redeemedCodes));
        promoInput.value = '';
        sfx('playSuccess');
        updateProfileUI();
        showToast(`Промокод активирован: ${reward.label}!`, 'success');
    }
    $('promo-submit-btn')?.addEventListener('click', redeemPromo);
    promoInput?.addEventListener('keypress', (e) => { if (e.key === 'Enter') redeemPromo(); });

    // ==========================================
    // SETTINGS DRAWER
    // ==========================================
    const openDrawer = () => { sfx('playClick'); drawer.classList.add('show'); drawerBackdrop.classList.add('show'); };
    const closeDrawer = () => { drawer.classList.remove('show'); drawerBackdrop.classList.remove('show'); };
    $('btn-settings')?.addEventListener('click', openDrawer);
    $('settings-close')?.addEventListener('click', closeDrawer);
    drawerBackdrop?.addEventListener('click', closeDrawer);

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') { closeDrawer(); closeAccountModal(); }
    });

    const ramSlider = $('settings-ram-slider');
    const ramLabel = $('settings-ram-label');
    function renderRam() {
        const txt = `${STATE.ramMB} MB`;
        if (ramLabel) ramLabel.textContent = txt;
    }
    if (ramSlider) {
        ramSlider.value = STATE.ramMB;
        ramSlider.addEventListener('input', (e) => {
            STATE.ramMB = parseInt(e.target.value, 10);
            localStorage.setItem('sr_ram_mb', STATE.ramMB);
            renderRam();
        });
    }
    renderRam();

    const volSlider = $('settings-volume-slider');
    const volLabel = $('settings-volume-label');
    if (volSlider) {
        volSlider.value = Math.round(STATE.soundVolume * 100);
        volLabel.textContent = `${volSlider.value}%`;
        volSlider.addEventListener('input', (e) => {
            const v = parseInt(e.target.value, 10);
            STATE.soundVolume = v / 100;
            volLabel.textContent = `${v}%`;
            localStorage.setItem('sr_sound_vol', STATE.soundVolume);
            if (window.soundEngine) window.soundEngine.setVolume(STATE.soundVolume);
        });
    }

    const animToggle = $('setting-gpu-accel');
    const applyAnim = () => document.body.classList.toggle('no-anim', !STATE.animations);
    if (animToggle) {
        animToggle.checked = STATE.animations;
        animToggle.addEventListener('change', () => {
            STATE.animations = animToggle.checked;
            localStorage.setItem('sr_anim', STATE.animations);
            applyAnim();
        });
    }
    applyAnim();

    const gcToggle = $('setting-opt-gc');
    if (gcToggle) {
        gcToggle.checked = STATE.optGC;
        gcToggle.addEventListener('change', () => {
            STATE.optGC = gcToggle.checked;
            localStorage.setItem('sr_opt_gc', STATE.optGC);
        });
    }

    const gamePathInput = $('setting-game-path');

    async function syncGameDir() {
        const a = api();
        if (!a || !a.get_game_dir) return;
        try {
            const res = await a.get_game_dir();
            if (res && res.path && gamePathInput) gamePathInput.value = res.path;
        } catch (err) { console.log('get_game_dir error', err); }
    }
    window.addEventListener('pywebviewready', syncGameDir);
    setTimeout(syncGameDir, 600);

    async function applyGameDir(newPath) {
        if (STATE.isLaunching) {
            showToast('Дождитесь завершения запуска игры', 'error');
            syncGameDir();
            return;
        }
        const a = api();
        if (!a || !a.set_game_dir) return;
        const res = await a.set_game_dir(newPath);
        if (res && res.status === 'ok') {
            if (gamePathInput) gamePathInput.value = res.path;
            sfx('playSuccess');
            showToast(`Путь игры изменён: ${res.path}`, 'success');
        } else {
            showToast((res && res.message) || 'Не удалось изменить путь', 'error');
            syncGameDir();
        }
    }

    if (gamePathInput) {
        gamePathInput.addEventListener('change', () => applyGameDir(gamePathInput.value));
        gamePathInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') gamePathInput.blur();
        });
    }

    $('btn-browse-folder')?.addEventListener('click', async () => {
        sfx('playClick');
        if (STATE.isLaunching) return showToast('Дождитесь завершения запуска игры', 'error');
        const a = api();
        if (!a || !a.browse_game_folder) {
            return showToast('Выбор папки доступен в приложении SpaceRust.exe');
        }
        const res = await a.browse_game_folder();
        if (res && res.status === 'ok') {
            if (gamePathInput) gamePathInput.value = res.path;
            sfx('playSuccess');
            showToast(`Путь игры изменён: ${res.path}`, 'success');
        } else if (res && res.status === 'error') {
            showToast(res.message || 'Ошибка выбора папки', 'error');
        }
    });

    $('btn-reset-folder')?.addEventListener('click', async () => {
        sfx('playClick');
        if (STATE.isLaunching) return showToast('Дождитесь завершения запуска игры', 'error');
        const a = api();
        if (!a || !a.reset_game_dir) return;
        const res = await a.reset_game_dir();
        if (res && res.status === 'ok') {
            if (gamePathInput) gamePathInput.value = res.path;
            sfx('playSuccess');
            showToast('Путь сброшен по умолчанию', 'success');
        }
    });

    $('btn-open-folder')?.addEventListener('click', () => {
        sfx('playClick');
        const a = api();
        if (a && a.open_game_folder) a.open_game_folder();
        else showToast(`Папка клиента: ${gamePathInput ? gamePathInput.value : '%APPDATA%\\.spacerust'}`);
    });

    if (soundBtn) {
        soundBtn.classList.toggle('muted', !STATE.soundEnabled);
        soundBtn.addEventListener('click', () => {
            STATE.soundEnabled = !STATE.soundEnabled;
            localStorage.setItem('sr_sound_enabled', STATE.soundEnabled);
            if (window.soundEngine) {
                window.soundEngine.setEnabled(STATE.soundEnabled);
                if (STATE.soundEnabled) sfx('playClick');
            }
            soundBtn.classList.toggle('muted', !STATE.soundEnabled);
            showToast(STATE.soundEnabled ? 'Звуки интерфейса включены' : 'Звуки интерфейса выключены');
        });
    }

    // ==========================================
    // WINDOW CONTROLS + NATIVE DRAG
    // ==========================================
    $('btn-minimize')?.addEventListener('click', () => {
        const a = api();
        if (a && a.minimize_window) a.minimize_window();
    });
    $('btn-close')?.addEventListener('click', () => {
        const a = api();
        if (a && a.close_window) a.close_window(); else window.close();
    });

    const topbar = $('app-titlebar');
    const triggerDrag = () => {
        const a = api();
        if (a && typeof a.drag_window === 'function') { a.drag_window(); return true; }
        return false;
    };
    topbar?.addEventListener('mousedown', (e) => {
        if (e.button !== 0) return;
        if (e.target.closest('button, a, input')) return;
        e.preventDefault();
        if (!triggerDrag()) setTimeout(triggerDrag, 25);
    });

    // ==========================================
    // GAME LOADING LINE + PLAY
    // ==========================================
    function setIdleLine() {
        loadLine.classList.remove('paused', 'done');
        loadFill.style.width = '0%';
        loadTitle.textContent = '';
        loadStage.textContent = '';
        loadPct.textContent = '';
        btnPause.disabled = true;
        btnResume.disabled = true;
        btnResume.classList.remove('active');
    }

    window.updateLaunchProgress = function (text, percent) {
        const pct = Math.max(0, Math.min(100, Math.round(percent)));
        loadFill.style.width = `${pct}%`;
        loadTitle.textContent = STATE.isPaused ? 'ЗАГРУЗКА НА ПАУЗЕ' : 'ЗАГРУЗКА ИГРЫ';
        loadStage.textContent = text;
        loadPct.textContent = `${pct}%`;
        if (btnPlayText) btnPlayText.textContent = pct >= 100 ? 'ЗАПУСК...' : `ЗАГРУЗКА ${pct}%`;
    };

    function setPaused(paused) {
        STATE.isPaused = paused;
        loadLine.classList.toggle('paused', paused);
        btnPause.disabled = paused;
        btnResume.disabled = !paused;
        btnResume.classList.toggle('active', paused);
        loadTitle.textContent = paused ? 'ЗАГРУЗКА НА ПАУЗЕ' : 'ЗАГРУЗКА ИГРЫ';
    }

    btnPause?.addEventListener('click', async () => {
        if (!STATE.isLaunching || STATE.isPaused) return;
        sfx('playClick');
        setPaused(true);
        const a = api();
        if (a && a.pause_download) await a.pause_download();
        showToast('Загрузка приостановлена');
    });

    btnResume?.addEventListener('click', async () => {
        if (!STATE.isLaunching || !STATE.isPaused) return;
        sfx('playClick');
        setPaused(false);
        const a = api();
        if (a && a.resume_download) await a.resume_download();
        showToast('Загрузка продолжена', 'success');
    });

    function finishLaunch(delay) {
        setTimeout(() => {
            btnPlay.classList.remove('loading-state');
            btnPlayText.textContent = 'ИГРАТЬ';
            STATE.isLaunching = false;
            setPaused(false);
            setIdleLine();
        }, delay);
    }

    // Browser preview fallback (no pywebview) — simulated progress
    function simulateLaunch() {
        return new Promise((resolve) => {
            let p = 5;
            const tick = setInterval(() => {
                if (STATE.isPaused) return;
                p += 4 + Math.random() * 6;
                if (p >= 100) {
                    clearInterval(tick);
                    window.updateLaunchProgress('Запуск Minecraft...', 100);
                    resolve({ status: 'success' });
                } else {
                    const spd = (14 + Math.random() * 12).toFixed(1);
                    window.updateLaunchProgress(`${Math.round(p * 1.6)} / 160 МБ · ${spd} МБ/с`, p);
                }
            }, 180);
        });
    }

    async function handleLaunchGame() {
        if (STATE.isLaunching) return;
        if (!STATE.nickname || STATE.nickname.length < 3) {
            showToast('Укажите никнейм (минимум 3 символа)', 'error');
            return;
        }

        STATE.isLaunching = true;
        sfx('playLaunch');
        btnPlay.classList.add('loading-state');
        loadLine.classList.remove('done', 'paused');
        btnPause.disabled = false;
        btnResume.disabled = true;
        window.updateLaunchProgress('Проверка файлов клиента...', 3);

        try {
            const a = api();
            const res = (a && a.launch_client)
                ? await a.launch_client(STATE.ramMB, STATE.nickname)
                : await simulateLaunch();

            if (res && res.status === 'error') {
                btnPlay.classList.remove('loading-state');
                btnPlayText.textContent = 'ИГРАТЬ';
                STATE.isLaunching = false;
                setPaused(false);
                setIdleLine();
                showToast('Ошибка запуска игры', 'error');
                alert('⚠️ НЕ УДАЛОСЬ ЗАПУСТИТЬ ИГРУ:\n\n' + res.message);
                return;
            }

            loadLine.classList.add('done');
            loadFill.style.width = '100%';
            loadTitle.textContent = 'ИГРА ЗАПУЩЕНА';
            loadStage.textContent = 'Приятной игры на SpaceRust!';
            loadPct.textContent = '100%';
            btnPlayText.textContent = 'ИГРА ЗАПУЩЕНА';
            btnPause.disabled = true;
            btnResume.disabled = true;
            sfx('playSuccess');
            showToast('Игра запущена! Открывается SpaceRust...', 'success');
            finishLaunch(4000);
        } catch (err) {
            console.error('Launch error', err);
            btnPlay.classList.remove('loading-state');
            btnPlayText.textContent = 'ИГРАТЬ';
            STATE.isLaunching = false;
            setPaused(false);
            setIdleLine();
            alert('⚠️ Ошибка соединения с лаунчером:\n\n' + err);
        }
    }
    btnPlay?.addEventListener('click', handleLaunchGame);

    // ==========================================
    // AUDIO HOVER
    // ==========================================
    document.querySelectorAll('button, .account-card, .patch-card').forEach(el => {
        el.addEventListener('mouseenter', () => sfx('playHover'));
    });

    // ==========================================
    // TOASTS
    // ==========================================
    function showToast(message, type = 'info') {
        const container = $('toast-container');
        if (!container) return;
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.textContent = message;
        container.appendChild(toast);
        setTimeout(() => {
            toast.classList.add('hide');
            setTimeout(() => toast.remove(), 260);
        }, 2800);
        while (container.children.length > 3) container.firstChild.remove();
    }

    // ==========================================
    // INIT
    // ==========================================
    updateProfileUI();
    setIdleLine();
});
