/**
 * SpaceRust Launcher - Main Application Logic
 */

document.addEventListener('DOMContentLoaded', () => {
    // --- STATE INITIALIZATION ---
    const STATE = {
        ramMB: parseInt(localStorage.getItem('sr_ram_mb') || '6144', 10),
        nickname: localStorage.getItem('sr_nickname') || ('Player_' + Math.floor(100 + Math.random() * 900)),
        balance: parseInt(localStorage.getItem('sr_balance') || '1450', 10),
        rank: localStorage.getItem('sr_rank') || 'VIP+',
        soundEnabled: localStorage.getItem('sr_sound_enabled') !== 'false',
        soundVolume: parseFloat(localStorage.getItem('sr_sound_vol') || '0.6'),
        redeemedCodes: JSON.parse(localStorage.getItem('sr_redeemed_codes') || '[]'),
        isLaunching: false
    };

    // Initialize Audio Engine Settings
    if (window.soundEngine) {
        window.soundEngine.setEnabled(STATE.soundEnabled);
        window.soundEngine.setVolume(STATE.soundVolume);
    }

    // --- DOM REFERENCES ---
    const tabButtons = document.querySelectorAll('.tab-btn');
    const tabViews = document.querySelectorAll('.tab-view');
    
    // Header & Profile
    const profileBtn = document.getElementById('user-profile-btn');
    const profileNickEl = document.getElementById('profile-nickname');
    const profileBalEl = document.getElementById('profile-balance');
    const profileAvatarImg = document.getElementById('user-avatar-img');
    const soundToggleBtn = document.getElementById('sound-toggle-btn');
    const homePlayerNick = document.getElementById('home-player-nick');
    const homeRamVal = document.getElementById('home-ram-val');
    
    // Bottom Dock
    const dockRamStat = document.getElementById('dock-ram-stat');
    const dockNickInput = document.getElementById('dock-nick-input');
    const btnPlay = document.getElementById('btn-play');
    const btnPlayText = document.getElementById('btn-play-text');
    const launchProgressBar = document.getElementById('launch-progress-bar');
    
    // Settings elements
    const settingsRamSlider = document.getElementById('settings-ram-slider');
    const settingsRamLabel = document.getElementById('settings-ram-label');
    const settingsVolSlider = document.getElementById('settings-volume-slider');
    const settingsVolLabel = document.getElementById('settings-volume-label');
    const btnOpenFolder = document.getElementById('btn-open-folder');

    // Promo elements
    const promoInput = document.getElementById('promo-input');
    const promoSubmitBtn = document.getElementById('promo-submit-btn');

    // Account Modal elements
    const accountModal = document.getElementById('account-modal');
    const modalCloseBtn = document.getElementById('modal-account-close');
    const modalInputNick = document.getElementById('modal-input-nick');
    const modalAvatarPreview = document.getElementById('modal-avatar-preview');
    const modalDisplayNick = document.getElementById('modal-display-nick');
    const modalSaveBtn = document.getElementById('modal-save-btn');

    // Window controls
    const btnMinimize = document.getElementById('btn-minimize');
    const btnClose = document.getElementById('btn-close');

    // --- INITIAL UI POPULATION ---
    updateProfileUI();
    syncSettingsUI();
    attachAudioListeners();

    // ==========================================
    // TABS SWITCHING
    // ==========================================
    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetTab = btn.dataset.tab;
            if (btn.classList.contains('active')) return;

            if (window.soundEngine) window.soundEngine.playClick();

            tabButtons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            tabViews.forEach(view => {
                view.classList.remove('active-view');
                if (view.id === `view-${targetTab}`) {
                    view.classList.add('active-view');
                }
            });
        });
    });

    // ==========================================
    // SETTINGS LOGIC
    // ==========================================
    function syncSettingsUI() {
        if (settingsRamSlider && settingsRamLabel && dockRamStat) {
            settingsRamSlider.value = STATE.ramMB;
            settingsRamLabel.textContent = `${STATE.ramMB} MB`;
            dockRamStat.textContent = `${STATE.ramMB} MB`;
            if (homeRamVal) homeRamVal.textContent = `${STATE.ramMB} MB`;

            settingsRamSlider.addEventListener('input', (e) => {
                const val = parseInt(e.target.value, 10);
                STATE.ramMB = val;
                settingsRamLabel.textContent = `${val} MB`;
                dockRamStat.textContent = `${val} MB`;
                if (homeRamVal) homeRamVal.textContent = `${val} MB`;
                localStorage.setItem('sr_ram_mb', val);
            });
        }

        if (settingsVolSlider && settingsVolLabel) {
            settingsVolSlider.value = Math.round(STATE.soundVolume * 100);
            settingsVolLabel.textContent = `${settingsVolSlider.value}%`;

            settingsVolSlider.addEventListener('input', (e) => {
                const val = parseInt(e.target.value, 10);
                const floatVal = val / 100;
                STATE.soundVolume = floatVal;
                settingsVolLabel.textContent = `${val}%`;
                localStorage.setItem('sr_sound_vol', floatVal);
                if (window.soundEngine) window.soundEngine.setVolume(floatVal);
            });
        }

        if (btnOpenFolder) {
            btnOpenFolder.addEventListener('click', () => {
                if (window.soundEngine) window.soundEngine.playClick();
                if (window.pywebview && window.pywebview.api && window.pywebview.api.open_game_folder) {
                    window.pywebview.api.open_game_folder();
                } else {
                    showToast('Папка клиента: %APPDATA%\\.spacerust', 'info');
                }
            });
        }
    }

    // Sound toggle in top titlebar
    if (soundToggleBtn) {
        soundToggleBtn.addEventListener('click', () => {
            STATE.soundEnabled = !STATE.soundEnabled;
            localStorage.setItem('sr_sound_enabled', STATE.soundEnabled);
            if (window.soundEngine) {
                window.soundEngine.setEnabled(STATE.soundEnabled);
                if (STATE.soundEnabled) window.soundEngine.playClick();
            }
            soundToggleBtn.style.opacity = STATE.soundEnabled ? '1' : '0.4';
            showToast(STATE.soundEnabled ? 'Звуки интерфейса включены' : 'Звуки интерфейса выключены');
        });
    }

    // ==========================================
    // PROMO CODE ACTIVATION
    // ==========================================
    if (promoSubmitBtn && promoInput) {
        promoSubmitBtn.addEventListener('click', redeemPromo);
        promoInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') redeemPromo();
        });
    }

    function redeemPromo() {
        const code = promoInput.value.trim().toUpperCase();
        if (!code) {
            showToast('Введите промокод!', 'error');
            return;
        }

        if (STATE.redeemedCodes.includes(code)) {
            showToast('Этот промокод уже был активирован ранее!', 'error');
            return;
        }

        const validPromos = {
            'WIPE2026': { bonus: 500, label: '+500 ₽ на баланс' },
            'RUSTME': { bonus: 300, label: '+300 ₽' },
            'SPACERUST': { bonus: 1000, label: '+1,000 ₽ на баланс' }
        };

        if (validPromos[code]) {
            const reward = validPromos[code];
            if (reward.bonus) {
                STATE.balance += reward.bonus;
                localStorage.setItem('sr_balance', STATE.balance);
            }

            STATE.redeemedCodes.push(code);
            localStorage.setItem('sr_redeemed_codes', JSON.stringify(STATE.redeemedCodes));

            promoInput.value = '';
            if (window.soundEngine) window.soundEngine.playSuccess();
            updateProfileUI();
            showToast(`Успешно! Промокод активирован: ${reward.label}`, 'success');
        } else {
            showToast('Неверный промокод или срок действия истек', 'error');
        }
    }

    // ==========================================
    // ACCOUNT / NICKNAME MODAL
    // ==========================================
    if (profileBtn && accountModal) {
        profileBtn.addEventListener('click', () => {
            if (window.soundEngine) window.soundEngine.playClick();
            modalInputNick.value = STATE.nickname;
            modalDisplayNick.textContent = STATE.nickname;
            modalAvatarPreview.src = `https://mc-heads.net/avatar/${encodeURIComponent(STATE.nickname)}/64`;
            accountModal.classList.add('show');
        });
    }

    if (modalCloseBtn && accountModal) {
        modalCloseBtn.addEventListener('click', () => {
            accountModal.classList.remove('show');
        });
    }

    if (modalInputNick) {
        modalInputNick.addEventListener('input', (e) => {
            const raw = e.target.value.replace(/[^a-zA-Z0-9_]/g, '');
            e.target.value = raw;
            const previewNick = raw || 'Steve';
            modalDisplayNick.textContent = raw || '...';
            modalAvatarPreview.src = `https://mc-heads.net/avatar/${encodeURIComponent(previewNick)}/64`;
        });
    }

    if (modalSaveBtn) {
        modalSaveBtn.addEventListener('click', () => {
            const newNick = modalInputNick.value.trim();
            if (newNick.length < 3) {
                showToast('Никнейм должен содержать минимум 3 символа!', 'error');
                return;
            }

            STATE.nickname = newNick;
            localStorage.setItem('sr_nickname', newNick);
            updateProfileUI();
            accountModal.classList.remove('show');
            if (window.soundEngine) window.soundEngine.playSuccess();
            showToast(`Никнейм успешно сохранен: ${newNick}`, 'success');
        });
    }

    function updateProfileUI() {
        if (profileNickEl) profileNickEl.textContent = STATE.nickname;
        if (profileBalEl) profileBalEl.textContent = `${STATE.balance.toLocaleString('ru-RU')} ₽`;
        if (profileAvatarImg) {
            profileAvatarImg.src = `https://mc-heads.net/avatar/${encodeURIComponent(STATE.nickname)}/64`;
        }
        if (homePlayerNick) homePlayerNick.textContent = STATE.nickname;
        if (dockNickInput && document.activeElement !== dockNickInput) {
            dockNickInput.value = STATE.nickname;
        }
        if (modalInputNick && document.activeElement !== modalInputNick) {
            modalInputNick.value = STATE.nickname;
        }
        if (modalDisplayNick) modalDisplayNick.textContent = STATE.nickname;
        if (modalAvatarPreview) {
            modalAvatarPreview.src = `https://mc-heads.net/avatar/${encodeURIComponent(STATE.nickname)}/64`;
        }
        const rankEl = document.querySelector('.user-rank');
        if (rankEl) rankEl.textContent = STATE.rank;
    }

    // Direct Nickname Input in Bottom Dock
    if (dockNickInput) {
        dockNickInput.value = STATE.nickname;
        dockNickInput.addEventListener('input', (e) => {
            const raw = e.target.value.replace(/[^a-zA-Z0-9_]/g, '');
            e.target.value = raw;
            if (raw.length >= 3) {
                STATE.nickname = raw;
                localStorage.setItem('sr_nickname', raw);
                if (profileNickEl) profileNickEl.textContent = raw;
                if (homePlayerNick) homePlayerNick.textContent = raw;
                if (profileAvatarImg) profileAvatarImg.src = `https://mc-heads.net/avatar/${encodeURIComponent(raw)}/64`;
                if (modalDisplayNick) modalDisplayNick.textContent = raw;
                if (modalInputNick && document.activeElement !== modalInputNick) modalInputNick.value = raw;
                if (modalAvatarPreview) modalAvatarPreview.src = `https://mc-heads.net/avatar/${encodeURIComponent(raw)}/64`;
            }
        });
        dockNickInput.addEventListener('change', (e) => {
            const raw = e.target.value.trim();
            if (raw.length < 3) {
                showToast('Никнейм должен содержать минимум 3 символа!', 'error');
                e.target.value = STATE.nickname;
            } else {
                showToast(`Никнейм обновлен: ${raw}`, 'success');
            }
        });
    }

    if (homePlayerNick) {
        homePlayerNick.style.cursor = 'pointer';
        homePlayerNick.title = 'Нажмите для смены ника';
        homePlayerNick.addEventListener('click', () => {
            if (profileBtn) profileBtn.click();
        });
    }

    // Auto-detect Windows username on fresh start
    async function checkSystemUsername() {
        if (!localStorage.getItem('sr_nickname')) {
            try {
                if (window.pywebview && window.pywebview.api && window.pywebview.api.get_system_username) {
                    const sysName = await window.pywebview.api.get_system_username();
                    if (sysName && sysName.length >= 3) {
                        STATE.nickname = sysName;
                        localStorage.setItem('sr_nickname', sysName);
                        updateProfileUI();
                    }
                }
            } catch (err) {
                console.log('System username lookup error:', err);
            }
        }
    }
    window.addEventListener('pywebviewready', checkSystemUsername);
    setTimeout(checkSystemUsername, 800);

    // ==========================================
    // WINDOW CONTROLS (PYWEBVIEW / BROWSER)
    // ==========================================
    if (btnMinimize) {
        btnMinimize.addEventListener('click', () => {
            if (window.pywebview && window.pywebview.api && window.pywebview.api.minimize_window) {
                window.pywebview.api.minimize_window();
            } else {
                showToast('Сворачивание доступно в окне приложения');
            }
        });
    }

    if (btnClose) {
        btnClose.addEventListener('click', () => {
            if (window.pywebview && window.pywebview.api && window.pywebview.api.close_window) {
                window.pywebview.api.close_window();
            } else {
                window.close();
            }
        });
    }

    // ==========================================
    // LAUNCH GAME ACTION
    // ==========================================
    if (btnPlay) {
        btnPlay.addEventListener('click', handleLaunchGame);
    }

    async function handleLaunchGame() {
        if (STATE.isLaunching) return;

        STATE.isLaunching = true;
        if (window.soundEngine) window.soundEngine.playLaunch();

        btnPlay.classList.add('loading-state');
        if (launchProgressBar) launchProgressBar.style.width = '20%';
        if (btnPlayText) btnPlayText.textContent = 'ПРОВЕРКА ФАЙЛОВ...';

        try {
            if (launchProgressBar) launchProgressBar.style.width = '50%';
            if (btnPlayText) btnPlayText.textContent = 'ЗАПУСК КЛИЕНТА...';

            let res = { status: 'success' };
            if (window.pywebview && window.pywebview.api && window.pywebview.api.launch_client) {
                res = await window.pywebview.api.launch_client(STATE.ramMB, STATE.nickname);
            }

            if (res && res.status === 'error') {
                if (launchProgressBar) launchProgressBar.style.width = '0%';
                btnPlay.classList.remove('loading-state');
                if (btnPlayText) btnPlayText.textContent = 'ЗАПУСТИТЬ ИГРУ';
                STATE.isLaunching = false;

                showToast('Ошибка запуска! Проверьте сообщение на экране', 'error');
                alert('⚠️ НЕ УДАЛОСЬ ЗАПУСТИТЬ ИГРУ:\n\n' + res.message);
                return;
            }

            if (launchProgressBar) launchProgressBar.style.width = '100%';
            if (btnPlayText) btnPlayText.textContent = 'ИГРА ЗАПУСКАЕТСЯ...';
            if (window.soundEngine) window.soundEngine.playSuccess();
            showToast('🚀 Игра запущена! Открывается SpaceRust...', 'success');

            setTimeout(() => {
                btnPlay.classList.remove('loading-state');
                if (launchProgressBar) launchProgressBar.style.width = '0%';
                if (btnPlayText) btnPlayText.textContent = 'ЗАПУСТИТЬ ИГРУ';
                STATE.isLaunching = false;
            }, 3500);

        } catch (err) {
            console.error('Launch Error:', err);
            btnPlay.classList.remove('loading-state');
            if (launchProgressBar) launchProgressBar.style.width = '0%';
            if (btnPlayText) btnPlayText.textContent = 'ЗАПУСТИТЬ ИГРУ';
            STATE.isLaunching = false;
            alert('⚠️ Ошибка соединения с лаунчером:\n\n' + err);
        }
    }

    // ==========================================
    // AUDIO HOVER LISTENER HELPER
    // ==========================================
    function attachAudioListeners() {
        const interactiveElements = document.querySelectorAll('button, .tab-btn, .social-btn, .user-profile-badge');
        interactiveElements.forEach(el => {
            if (!el.dataset.audioBound) {
                el.dataset.audioBound = 'true';
                el.addEventListener('mouseenter', () => {
                    if (window.soundEngine) window.soundEngine.playHover();
                });
            }
        });
    }

    // ==========================================
    // TOAST NOTIFICATIONS HELPER
    // ==========================================
    function showToast(message, type = 'info') {
        const container = document.getElementById('toast-container');
        if (!container) return;

        const toast = document.createElement('div');
        toast.className = `toast ${type === 'success' ? 'success' : ''}`;
        
        let iconSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/></svg>';
        if (type === 'success') {
            iconSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="var(--rust-green-bright)"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>';
        } else if (type === 'error') {
            iconSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="#e81123"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>';
        }

        toast.innerHTML = `${iconSvg}<span>${message}</span>`;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(40px)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3500);
    }
});
