/**
 * SpaceRust Launcher — Main UI logic (Rust style redesign)
 */
document.addEventListener('DOMContentLoaded', () => {
    const $ = (id) => document.getElementById(id);
    const api = () => (window.pywebview && window.pywebview.api) ? window.pywebview.api : null;

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
        playtimeSeconds: parseInt(localStorage.getItem('sr_playtime_sec') || '0', 10),
        redeemedCodes: JSON.parse(localStorage.getItem('sr_redeemed_codes') || '[]'),
        animations: localStorage.getItem('sr_anim') !== 'false',
        optGC: localStorage.getItem('sr_opt_gc') !== 'false',
        lang: localStorage.getItem('sr_lang') || 'ru',
        isLaunching: false,
        isPaused: false
    };

    const VALID_LANGS = ['ru', 'uk', 'en'];
    if (!VALID_LANGS.includes(STATE.lang)) STATE.lang = 'ru';

    function t(key, vars) {
        const dicts = window.SPACERUST_I18N || {};
        const table = dicts[STATE.lang] || dicts.ru || {};
        const fallback = dicts.ru || {};
        let str = table[key] !== undefined ? table[key] : (fallback[key] !== undefined ? fallback[key] : key);
        if (vars && typeof str === 'string') {
            Object.keys(vars).forEach((k) => {
                str = str.replace(new RegExp('\\{' + k + '\\}', 'g'), String(vars[k]));
            });
        }
        return str;
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
    const playtimeTextEl = $('playtime-text');
    const modalPlaytimeEl = $('modal-playtime-text');

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
            switchTab(btn.dataset.tab);
        });
    });

    $('btn-notify')?.addEventListener('click', () => {
        const dot = document.querySelector('.notify-dot');
        if (dot) dot.remove();
        switchTab('news');
        showToast(t('topbar.notifyToast'), 'success');
    });

    // ==========================================
    // PROFILE / ACCOUNT (nickname is locked)
    // ==========================================
    function formatPlaytimeHours(seconds) {
        const hrs = Math.max(0, Number(seconds) || 0) / 3600;
        const unit = t('account.hourUnit');
        if (hrs < 0.05) return `0 ${unit}`;
        if (hrs < 100) return `${hrs.toFixed(1).replace(/\.0$/, '')} ${unit}`;
        return `${Math.floor(hrs)} ${unit}`;
    }

    function renderPlaytime() {
        const formatted = formatPlaytimeHours(STATE.playtimeSeconds);
        if (playtimeTextEl) playtimeTextEl.textContent = t('account.playtimeShort', { hours: formatted });
        if (modalPlaytimeEl) modalPlaytimeEl.textContent = t('account.playtimeModal', { hours: formatted });
    }

    window.updatePlaytime = function (seconds) {
        const val = Math.max(0, parseInt(seconds, 10) || 0);
        STATE.playtimeSeconds = val;
        localStorage.setItem('sr_playtime_sec', String(val));
        renderPlaytime();
    };

    async function syncPlaytime() {
        const a = api();
        if (!a || !a.get_playtime) return;
        try {
            const res = await a.get_playtime();
            if (res && res.status === 'ok' && typeof res.seconds === 'number') {
                window.updatePlaytime(res.seconds);
            }
        } catch (err) { console.log('get_playtime error', err); }
    }
    window.addEventListener('pywebviewready', syncPlaytime);
    setTimeout(syncPlaytime, 700);

    function updateProfileUI() {
        if (avatarImg) avatarImg.src = avatarUrl(STATE.nickname);
        if (dockNickEl) dockNickEl.textContent = STATE.nickname;
        if (modalNick) modalNick.textContent = STATE.nickname;
        if (modalLockedNick) modalLockedNick.textContent = STATE.nickname;
        if (modalAvatar) modalAvatar.src = avatarUrl(STATE.nickname);
        const hasSub = !!STATE.rank && STATE.rank !== 'ИГРОК';
        const rankText = hasSub ? STATE.rank : t('account.rankDefault');
        if (modalSubStatus) {
            modalSubStatus.textContent = hasSub ? t('modal.activePriv', { rank: rankText }) : t('modal.noSub');
        }
        [rankEl, modalRankEl].forEach(el => {
            if (!el) return;
            el.textContent = rankText;
            el.classList.toggle('has-sub', hasSub);
        });
        renderPlaytime();
    }

    function openAccountModal() {
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
        if (!code) return showToast(t('promo.empty'), 'error');
        if (STATE.redeemedCodes.includes(code)) return showToast(t('promo.used'), 'error');

        const promos = {
            'WIPE2026': 'VIP',
            'SPACEVIP': 'VIP+',
            'RUSTME': 'PREMIUM',
            'SPACERUST': 'ELITE'
        };
        const rewardRank = promos[code];
        if (!rewardRank) return showToast(t('promo.invalid'), 'error');

        STATE.rank = rewardRank;
        localStorage.setItem('sr_rank', STATE.rank);
        STATE.redeemedCodes.push(code);
        localStorage.setItem('sr_redeemed_codes', JSON.stringify(STATE.redeemedCodes));
        promoInput.value = '';
        updateProfileUI();
        const label = t('promo.privLabel', { rank: rewardRank });
        showToast(t('promo.activated', { label }), 'success');
    }
    $('promo-submit-btn')?.addEventListener('click', redeemPromo);
    promoInput?.addEventListener('keypress', (e) => { if (e.key === 'Enter') redeemPromo(); });

    // ==========================================
    // SETTINGS DRAWER
    // ==========================================
    const openDrawer = () => { drawer.classList.add('show'); drawerBackdrop.classList.add('show'); };
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
            showToast(t('settings.waitLaunch'), 'error');
            syncGameDir();
            return;
        }
        const a = api();
        if (!a || !a.set_game_dir) return;
        const res = await a.set_game_dir(newPath);
        if (res && res.status === 'ok') {
            if (gamePathInput) gamePathInput.value = res.path;
            showToast(t('settings.folderChanged', { path: res.path }), 'success');
        } else {
            showToast((res && res.message) || t('settings.folderChangeErr'), 'error');
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
        if (STATE.isLaunching) return showToast(t('settings.waitLaunch'), 'error');
        const a = api();
        if (!a || !a.browse_game_folder) {
            return showToast(t('settings.folderExeOnly'));
        }
        const res = await a.browse_game_folder();
        if (res && res.status === 'ok') {
            if (gamePathInput) gamePathInput.value = res.path;
            showToast(t('settings.folderChanged', { path: res.path }), 'success');
        } else if (res && res.status === 'error') {
            showToast(res.message || t('settings.folderBrowseErr'), 'error');
        }
    });

    $('btn-reset-folder')?.addEventListener('click', async () => {
        if (STATE.isLaunching) return showToast(t('settings.waitLaunch'), 'error');
        const a = api();
        if (!a || !a.reset_game_dir) return;
        const res = await a.reset_game_dir();
        if (res && res.status === 'ok') {
            if (gamePathInput) gamePathInput.value = res.path;
            showToast(t('settings.folderReset'), 'success');
        }
    });

    $('btn-open-folder')?.addEventListener('click', () => {
        const a = api();
        if (a && a.open_game_folder) a.open_game_folder();
        else showToast(t('settings.clientFolder', { path: gamePathInput ? gamePathInput.value : '%APPDATA%\\.spacerust' }));
    });

    // ==========================================
    // WINDOW CONTROLS + NATIVE DRAG + RESIZE
    // ==========================================
    let isWindowMaximized = false;
    const btnMaximize = $('btn-maximize');

    function updateWindowUIState(maximized) {
        isWindowMaximized = !!maximized;
        document.body.classList.toggle('is-maximized', isWindowMaximized);
        if (btnMaximize) {
            btnMaximize.title = isWindowMaximized ? t('topbar.restore') : t('topbar.maximize');
        }
    }

    window.onWindowStateChange = function (maximized) {
        updateWindowUIState(maximized);
    };

    async function syncWindowState() {
        const a = api();
        if (!a || !a.get_window_state) {
            updateWindowUIState(false);
            return;
        }
        try {
            const res = await a.get_window_state();
            if (res && res.status === 'ok') {
                updateWindowUIState(res.maximized);
            }
        } catch (err) { console.log('syncWindowState error', err); }
    }
    window.addEventListener('pywebviewready', syncWindowState);
    setTimeout(syncWindowState, 500);

    async function handleToggleMaximize() {
        const a = api();
        if (a && a.toggle_maximize_window) {
            const res = await a.toggle_maximize_window();
            if (res && res.status === 'ok') updateWindowUIState(res.maximized);
        } else {
            if (!document.fullscreenElement) {
                document.documentElement.requestFullscreen?.().catch(() => {});
                updateWindowUIState(true);
            } else {
                document.exitFullscreen?.().catch(() => {});
                updateWindowUIState(false);
            }
        }
    }

    $('btn-minimize')?.addEventListener('click', () => {
        const a = api();
        if (a && a.minimize_window) a.minimize_window();
    });
    btnMaximize?.addEventListener('click', handleToggleMaximize);
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
        if (e.button !== 0 || e.detail >= 2) return;
        if (e.target.closest('button, a, input')) return;
        e.preventDefault();
        if (!triggerDrag()) setTimeout(triggerDrag, 25);
    });
    topbar?.addEventListener('dblclick', (e) => {
        if (e.target.closest('button, a, input')) return;
        handleToggleMaximize();
    });

    document.querySelectorAll('.resize-edge[data-edge]').forEach((edgeEl) => {
        edgeEl.addEventListener('mousedown', (e) => {
            if (e.button !== 0 || isWindowMaximized) return;
            e.preventDefault();
            const a = api();
            if (a && typeof a.resize_window === 'function') {
                a.resize_window(edgeEl.dataset.edge);
            }
        });
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

    let lastProgressPct = 0;

    window.updateLaunchProgress = function (text, percent) {
        const pct = Math.max(0, Math.min(100, Math.round(percent)));
        lastProgressPct = pct;
        loadFill.style.width = `${pct}%`;
        loadTitle.textContent = STATE.isPaused ? t('play.loadPaused') : t('play.loadActive');
        loadStage.textContent = text;
        loadPct.textContent = `${pct}%`;
        if (btnPlayText) btnPlayText.textContent = pct >= 100 ? t('play.btnStarting') : t('play.btnProgress', { pct });
    };

    function setPaused(paused) {
        STATE.isPaused = paused;
        loadLine.classList.toggle('paused', paused);
        btnPause.disabled = paused;
        btnResume.disabled = !paused;
        btnResume.classList.toggle('active', paused);
        loadTitle.textContent = paused ? t('play.loadPaused') : t('play.loadActive');
    }

    btnPause?.addEventListener('click', async () => {
        if (!STATE.isLaunching || STATE.isPaused) return;
        setPaused(true);
        const a = api();
        if (a && a.pause_download) await a.pause_download();
        showToast(t('play.toastPaused'));
    });

    btnResume?.addEventListener('click', async () => {
        if (!STATE.isLaunching || !STATE.isPaused) return;
        setPaused(false);
        const a = api();
        if (a && a.resume_download) await a.resume_download();
        showToast(t('play.toastResumed'), 'success');
    });

    function finishLaunch(delay) {
        setTimeout(() => {
            btnPlay.classList.remove('loading-state');
            btnPlayText.textContent = t('play.idle');
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
                    window.updateLaunchProgress(t('play.simStartingMc'), 100);
                    resolve({ status: 'success' });
                } else {
                    const spd = (14 + Math.random() * 12).toFixed(1);
                    window.updateLaunchProgress(t('play.simSpeed', { done: Math.round(p * 1.6), spd }), p);
                }
            }, 180);
        });
    }

    async function handleLaunchGame() {
        if (STATE.isLaunching) return;
        if (!STATE.nickname || STATE.nickname.length < 3) {
            showToast(t('play.nickMin'), 'error');
            return;
        }

        STATE.isLaunching = true;
        btnPlay.classList.add('loading-state');
        loadLine.classList.remove('done', 'paused');
        btnPause.disabled = false;
        btnResume.disabled = true;
        window.updateLaunchProgress(t('play.checkingFiles'), 3);

        try {
            const a = api();
            const res = (a && a.launch_client)
                ? await a.launch_client(STATE.ramMB, STATE.nickname)
                : await simulateLaunch();

            if (res && res.status === 'error') {
                btnPlay.classList.remove('loading-state');
                btnPlayText.textContent = t('play.idle');
                STATE.isLaunching = false;
                setPaused(false);
                setIdleLine();
                showToast(t('play.toastErr'), 'error');
                alert(t('play.alertErr', { msg: res.message }));
                return;
            }

            loadLine.classList.add('done');
            loadFill.style.width = '100%';
            loadTitle.textContent = t('play.stageRunning');
            loadStage.textContent = t('play.msgRunning');
            loadPct.textContent = '100%';
            btnPlayText.textContent = t('play.stageRunning');
            btnPause.disabled = true;
            btnResume.disabled = true;
            showToast(t('play.toastLaunched'), 'success');
            finishLaunch(4000);
        } catch (err) {
            console.error('Launch error', err);
            btnPlay.classList.remove('loading-state');
            btnPlayText.textContent = t('play.idle');
            STATE.isLaunching = false;
            setPaused(false);
            setIdleLine();
            alert(t('play.alertConnErr', { err }));
        }
    }
    btnPlay?.addEventListener('click', handleLaunchGame);

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
    // LANGUAGE SWITCHING (RU / UK / EN)
    // ==========================================
    function applyLanguage(lang, syncBackend = true) {
        if (!VALID_LANGS.includes(lang)) lang = 'ru';
        STATE.lang = lang;
        localStorage.setItem('sr_lang', lang);
        document.documentElement.lang = lang;

        document.querySelectorAll('[data-i18n]').forEach((el) => {
            el.textContent = t(el.dataset.i18n);
        });
        document.querySelectorAll('[data-i18n-html]').forEach((el) => {
            el.innerHTML = t(el.dataset.i18nHtml);
        });
        document.querySelectorAll('[data-i18n-title]').forEach((el) => {
            el.title = t(el.dataset.i18nTitle);
        });
        if (btnMaximize) {
            btnMaximize.title = isWindowMaximized ? t('topbar.restore') : t('topbar.maximize');
        }
        document.querySelectorAll('[data-lang]').forEach((btn) => {
            btn.classList.toggle('active', btn.dataset.lang === lang);
        });

        updateProfileUI();

        if (loadLine.classList.contains('done')) {
            loadTitle.textContent = t('play.stageRunning');
            loadStage.textContent = t('play.msgRunning');
            if (btnPlayText) btnPlayText.textContent = t('play.stageRunning');
        } else if (STATE.isLaunching) {
            loadTitle.textContent = STATE.isPaused ? t('play.loadPaused') : t('play.loadActive');
            if (btnPlayText) {
                btnPlayText.textContent = lastProgressPct >= 100
                    ? t('play.btnStarting')
                    : t('play.btnProgress', { pct: lastProgressPct });
            }
        } else if (btnPlayText) {
            btnPlayText.textContent = t('play.idle');
        }

        if (syncBackend) {
            const a = api();
            if (a && typeof a.set_language === 'function') {
                a.set_language(lang).catch(() => {});
            }
        }
    }

    document.querySelectorAll('[data-lang]').forEach((btn) => {
        btn.addEventListener('click', () => {
            const nextLang = btn.dataset.lang;
            if (!nextLang || nextLang === STATE.lang) return;
            applyLanguage(nextLang, true);
        });
    });

    async function syncLanguage() {
        const a = api();
        if (!a || !a.get_language) return;
        try {
            const hasLocal = !!localStorage.getItem('sr_lang');
            if (hasLocal) {
                if (a.set_language) await a.set_language(STATE.lang);
            } else {
                const res = await a.get_language();
                if (res && res.status === 'ok' && VALID_LANGS.includes(res.lang)) {
                    applyLanguage(res.lang, false);
                }
            }
        } catch (err) { console.log('syncLanguage error', err); }
    }
    window.addEventListener('pywebviewready', syncLanguage);
    setTimeout(syncLanguage, 650);

    // ==========================================
    // INIT
    // ==========================================
    applyLanguage(STATE.lang, false);
    setIdleLine();
});
