/**
 * SpaceRust Website — Launcher Download, Account Registration, Profile & i18n
 */
document.addEventListener('DOMContentLoaded', () => {
    const $ = (id) => document.getElementById(id);

    const STORAGE_CURRENT = 'sr_site_current_user';
    const STORAGE_USERS = 'sr_site_users';
    const GITHUB_EXE_URL = 'https://github.com/DmutroKhludov/spacerust/releases/latest/download/SpaceRust.exe';
    const totp = window.SpaceRustTOTP;
    const i18n = window.SpaceRustI18N;

    const btnRegister = $('btn-register');
    const btnRegisterText = $('btn-register-text');
    const btnDownload = $('btn-download');

    const heroUserBar = $('hero-user-bar');
    const heroUserOpen = $('hero-user-open');
    const heroUserAvatar = $('hero-user-avatar');
    const heroUserNick = $('hero-user-nick');
    const btnLogout = $('btn-logout');

    const authModal = $('auth-modal');
    const formRegister = $('form-register');
    const formLogin = $('form-login');
    const login2faGroup = $('login-2fa-group');
    const login2faCode = $('login-2fa-code');
    const regError = $('reg-error');
    const loginError = $('login-error');
    const authTabs = document.querySelectorAll('.auth-tab');

    const accountModal = $('account-modal');
    const accModalAvatar = $('acc-modal-avatar');
    const accModalNick = $('acc-modal-nick');
    const accModalEmail = $('acc-modal-email');
    const acc2faBadge = $('acc-2fa-badge');
    const accCreatedAt = $('acc-created-at');
    const accLastOnline = $('acc-last-online');
    const accLastServer = $('acc-last-server');
    const emailCurrentDisplay = $('email-current-display');

    const profileTabs = document.querySelectorAll('.profile-tab');
    const profilePanels = {
        overview: $('ptab-overview'),
        email: $('ptab-email'),
        password: $('ptab-password'),
        '2fa': $('ptab-2fa')
    };

    const formChangeEmail = $('ptab-email');
    const changeEmailError = $('change-email-error');
    const formChangePass = $('ptab-password');
    const changePassError = $('change-pass-error');

    const tfaSetupForm = $('tfa-setup-form');
    const tfaActiveBox = $('tfa-active-box');
    const tfaQrWrap = $('tfa-qr-wrap');
    const tfaQrImg = $('tfa-qr-img');
    const tfaSecretDisplay = $('tfa-secret-display');
    const tfaLiveCode = $('tfa-live-code');
    const tfaEnableError = $('tfa-enable-error');
    const tfaEnabledSince = $('tfa-enabled-since');
    const tfaBackupCodes = $('tfa-backup-codes');
    const tfaDisableForm = $('tfa-disable-form');
    const tfaDisableError = $('tfa-disable-error');

    const downloadModal = $('download-modal');
    const dlDirectLink = $('dl-direct-link');
    const docModal = $('doc-modal');
    const docTitle = $('doc-title');
    const docContent = $('doc-content');
    const toastContainer = $('toast-container');

    let pending2faSecret = null;
    let live2faTimer = null;
    let activeDocKey = null;

    function readJson(key, fallback) {
        try {
            const raw = localStorage.getItem(key);
            return raw ? JSON.parse(raw) : fallback;
        } catch {
            return fallback;
        }
    }

    function getUsers() {
        return readJson(STORAGE_USERS, []);
    }

    function saveUsers(users) {
        localStorage.setItem(STORAGE_USERS, JSON.stringify(users));
    }

    function getCurrentSession() {
        return readJson(STORAGE_CURRENT, null);
    }

    function getCurrentUserRecord() {
        const session = getCurrentSession();
        if (!session || !session.nickname) return null;

        const users = getUsers();
        const idx = users.findIndex((u) => u.nickname.toLowerCase() === session.nickname.toLowerCase());
        if (idx === -1) return null;

        const user = users[idx];
        let changed = false;
        if (!user.createdAt) {
            user.createdAt = new Date().toISOString();
            changed = true;
        }
        if (!user.lastOnlineAt) {
            user.lastOnlineAt = user.createdAt;
            changed = true;
        }
        if (changed) {
            users[idx] = user;
            saveUsers(users);
        }
        return user;
    }

    function updateUserRecord(nickname, updater) {
        const users = getUsers();
        const idx = users.findIndex((u) => u.nickname.toLowerCase() === nickname.toLowerCase());
        if (idx === -1) return null;

        const updated = updater({ ...users[idx] });
        users[idx] = updated;
        saveUsers(users);
        setCurrentSession(updated);
        return updated;
    }

    function setCurrentSession(user) {
        if (user) {
            localStorage.setItem(STORAGE_CURRENT, JSON.stringify({
                nickname: user.nickname,
                email: user.email
            }));
        } else {
            localStorage.removeItem(STORAGE_CURRENT);
        }
    }

    function getLocale() {
        const lang = i18n.getLang();
        if (lang === 'uk') return 'uk-UA';
        if (lang === 'en') return 'en-US';
        return 'ru-RU';
    }

    function formatDateTime(isoString) {
        if (!isoString) return '—';
        const d = new Date(isoString);
        if (Number.isNaN(d.getTime())) return '—';
        return d.toLocaleString(getLocale(), {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    function showToast(message, type = 'ok') {
        const el = document.createElement('div');
        el.className = `toast ${type}`;
        el.textContent = message;
        toastContainer.appendChild(el);
        setTimeout(() => {
            el.remove();
        }, 3200);
    }

    function showError(el, msg) {
        el.textContent = msg;
        el.hidden = false;
    }

    function stopLive2faTimer() {
        if (live2faTimer) {
            clearInterval(live2faTimer);
            live2faTimer = null;
        }
    }

    function openModal(modal) {
        modal.classList.add('open');
        modal.setAttribute('aria-hidden', 'false');
    }

    function closeModal(modal) {
        modal.classList.remove('open');
        modal.setAttribute('aria-hidden', 'true');
        if (modal === accountModal) {
            stopLive2faTimer();
        }
    }

    function closeAllModals() {
        document.querySelectorAll('.modal-backdrop.open').forEach(closeModal);
    }

    function switchAuthTab(mode) {
        authTabs.forEach((tab) => {
            tab.classList.toggle('active', tab.dataset.auth === mode);
        });
        const isRegister = mode === 'register';
        formRegister.hidden = !isRegister;
        formLogin.hidden = isRegister;
        regError.hidden = true;
        loginError.hidden = true;
        if (isRegister) {
            login2faGroup.hidden = true;
            login2faCode.value = '';
        }

        const targetInput = isRegister ? $('reg-nick') : $('login-ident');
        setTimeout(() => targetInput && targetInput.focus(), 40);
    }

    function switchProfileTab(tabKey) {
        profileTabs.forEach((tab) => {
            const active = tab.dataset.ptab === tabKey;
            tab.classList.toggle('active', active);
            tab.setAttribute('aria-selected', String(active));
        });

        Object.entries(profilePanels).forEach(([key, panel]) => {
            if (panel) panel.hidden = key !== tabKey;
        });

        changeEmailError.hidden = true;
        changePassError.hidden = true;
        tfaEnableError.hidden = true;
        tfaDisableError.hidden = true;

        if (tabKey === '2fa') {
            refresh2faPanel();
        } else {
            stopLive2faTimer();
        }
    }

    function resolveAvatarUrl(nickname) {
        return `https://mc-heads.net/avatar/${encodeURIComponent(nickname)}/64`;
    }

    function renderProfileDetails(user) {
        if (!user) return;
        const avatarUrl = resolveAvatarUrl(user.nickname);

        accModalNick.textContent = user.nickname;
        accModalEmail.textContent = user.email;
        accModalAvatar.src = avatarUrl;
        accModalAvatar.onerror = () => { accModalAvatar.src = 'assets/logo.png'; };

        const tfaOn = Boolean(user.twoFactorEnabled && user.twoFactorSecret);
        acc2faBadge.textContent = i18n.t(tfaOn ? 'tfaBadgeOn' : 'tfaBadgeOff');
        acc2faBadge.classList.toggle('on', tfaOn);
        acc2faBadge.classList.toggle('off', !tfaOn);

        accCreatedAt.textContent = formatDateTime(user.createdAt);
        accLastOnline.textContent = formatDateTime(user.lastOnlineAt);
        accLastServer.textContent = user.lastServer || i18n.t('neverJoinedServer');
        emailCurrentDisplay.textContent = user.email;

        if (tfaOn) {
            tfaEnabledSince.textContent = user.twoFactorEnabledAt
                ? `${i18n.t('tfaEnabledSincePrefix')}${formatDateTime(user.twoFactorEnabledAt)}`
                : i18n.t('tfaLinkedFallback');
        }
    }

    function refresh2faPanel() {
        stopLive2faTimer();
        const user = getCurrentUserRecord();
        if (!user || !totp) return;

        const isEnabled = Boolean(user.twoFactorEnabled && user.twoFactorSecret);
        tfaSetupForm.hidden = isEnabled;
        tfaActiveBox.hidden = !isEnabled;

        if (isEnabled) {
            tfaEnabledSince.textContent = user.twoFactorEnabledAt
                ? `${i18n.t('tfaEnabledSincePrefix')}${formatDateTime(user.twoFactorEnabledAt)}`
                : i18n.t('tfaLinkedFallback');

            const codes = Array.isArray(user.twoFactorBackupCodes) ? user.twoFactorBackupCodes : [];
            tfaBackupCodes.innerHTML = codes.map((c) => `<code>${c}</code>`).join('');
            return;
        }

        if (!pending2faSecret) {
            pending2faSecret = totp.generateSecret(16);
        }
        tfaSecretDisplay.textContent = pending2faSecret;

        const uri = totp.buildOtpAuthUri(user.nickname, pending2faSecret, 'SpaceRust');
        tfaQrWrap.hidden = true;
        tfaQrImg.onload = () => {
            tfaQrWrap.hidden = false;
        };
        tfaQrImg.onerror = () => {
            tfaQrWrap.hidden = true;
        };
        tfaQrImg.src = `https://api.qrserver.com/v1/create-qr-code/?size=140x140&data=${encodeURIComponent(uri)}`;

        const updatePreviewCode = () => {
            if (tfaLiveCode && pending2faSecret) {
                tfaLiveCode.textContent = totp.generateCode(pending2faSecret);
            }
        };
        updatePreviewCode();
        live2faTimer = setInterval(updatePreviewCode, 1000);
    }

    function renderDocModal() {
        if (!activeDocKey) return;
        const map = {
            terms: ['docTermsTitle', 'docTermsHtml'],
            privacy: ['docPrivacyTitle', 'docPrivacyHtml'],
            rules: ['docRulesTitle', 'docRulesHtml']
        };
        const keys = map[activeDocKey];
        if (!keys) return;
        docTitle.textContent = i18n.t(keys[0]);
        docContent.innerHTML = i18n.t(keys[1]);
    }

    function updateAuthUI() {
        document.title = i18n.t('titleHome');
        const user = getCurrentUserRecord();
        const isAuthed = Boolean(user && user.nickname);

        if (isAuthed) {
            btnRegister.classList.add('authed');
            btnRegisterText.textContent = `${i18n.t('accountBtnPrefix')}${user.nickname}`;

            const avatarUrl = resolveAvatarUrl(user.nickname);
            heroUserBar.hidden = false;
            heroUserNick.textContent = user.nickname;
            heroUserAvatar.src = avatarUrl;
            heroUserAvatar.onerror = () => { heroUserAvatar.src = 'assets/logo.png'; };

            renderProfileDetails(user);
        } else {
            btnRegister.classList.remove('authed');
            btnRegisterText.textContent = i18n.t('registerBtn');
            heroUserBar.hidden = true;
        }
    }

    function openProfileModal(initialTab = 'overview') {
        const user = getCurrentUserRecord();
        if (!user) return;
        renderProfileDetails(user);
        switchProfileTab(initialTab);
        openModal(accountModal);
    }

    async function configureDownloadLink() {
        if (!dlDirectLink) return;
        const host = window.location.hostname;
        const isLocal = window.location.protocol === 'file:' || host === '127.0.0.1' || host === 'localhost';

        if (window.location.protocol === 'file:') {
            dlDirectLink.href = '../SpaceRust.exe';
            dlDirectLink.setAttribute('download', 'SpaceRust.exe');
            return;
        }

        if (isLocal) {
            try {
                const resp = await fetch('../SpaceRust.exe', { method: 'HEAD' });
                if (resp.ok) {
                    dlDirectLink.href = '../SpaceRust.exe';
                    dlDirectLink.setAttribute('download', 'SpaceRust.exe');
                    return;
                }
            } catch {
                // Fall back to GitHub Releases when SpaceRust.exe is not hosted alongside /site
            }
        }

        dlDirectLink.href = GITHUB_EXE_URL;
        dlDirectLink.removeAttribute('download');
        dlDirectLink.setAttribute('target', '_blank');
        dlDirectLink.setAttribute('rel', 'noopener');
    }

    function verifySecondFactor(user, rawCode) {
        const clean = String(rawCode || '').trim().toUpperCase();
        if (!clean) return { ok: false, usedBackup: false };

        if (totp && totp.verifyCode(user.twoFactorSecret, clean, 1)) {
            return { ok: true, usedBackup: false };
        }

        const backups = Array.isArray(user.twoFactorBackupCodes) ? user.twoFactorBackupCodes : [];
        const matchIdx = backups.findIndex((b) => b.toUpperCase() === clean);
        if (matchIdx !== -1) {
            return { ok: true, usedBackup: true, backupIdx: matchIdx };
        }
        return { ok: false, usedBackup: false };
    }

    // Button handlers
    btnRegister.addEventListener('click', () => {
        if (getCurrentUserRecord()) {
            openProfileModal('overview');
        } else {
            switchAuthTab('register');
            openModal(authModal);
        }
    });

    heroUserOpen.addEventListener('click', () => {
        openProfileModal('overview');
    });

    btnDownload.addEventListener('click', () => {
        openModal(downloadModal);
    });

    function logoutUser() {
        setCurrentSession(null);
        pending2faSecret = null;
        updateAuthUI();
        closeModal(accountModal);
        showToast(i18n.t('toastLogout'), 'warn');
    }

    btnLogout.addEventListener('click', (e) => {
        e.stopPropagation();
        logoutUser();
    });
    $('acc-logout').addEventListener('click', logoutUser);
    $('acc-open-download').addEventListener('click', () => {
        closeModal(accountModal);
        openModal(downloadModal);
    });

    // Auth modal tabs
    authTabs.forEach((tab) => {
        tab.addEventListener('click', () => switchAuthTab(tab.dataset.auth));
    });

    // Profile modal tabs
    profileTabs.forEach((tab) => {
        tab.addEventListener('click', () => switchProfileTab(tab.dataset.ptab));
    });

    // Registration submit
    formRegister.addEventListener('submit', (e) => {
        e.preventDefault();
        regError.hidden = true;

        const nickname = $('reg-nick').value.trim();
        const email = $('reg-email').value.trim().toLowerCase();
        const pass = $('reg-pass').value;
        const pass2 = $('reg-pass2').value;
        const agreed = $('reg-agree').checked;

        if (!/^[a-zA-Z0-9_]{3,16}$/.test(nickname)) {
            showError(regError, i18n.t('errRegNick'));
            return;
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            showError(regError, i18n.t('errRegEmail'));
            return;
        }
        if (pass.length < 6) {
            showError(regError, i18n.t('errRegPassLen'));
            return;
        }
        if (pass !== pass2) {
            showError(regError, i18n.t('errRegPassMatch'));
            return;
        }
        if (!agreed) {
            showError(regError, i18n.t('errRegAgree'));
            return;
        }

        const users = getUsers();
        if (users.some((u) => u.nickname.toLowerCase() === nickname.toLowerCase())) {
            showError(regError, i18n.t('errNickTaken'));
            return;
        }
        if (users.some((u) => u.email === email)) {
            showError(regError, i18n.t('errEmailTaken'));
            return;
        }

        const nowIso = new Date().toISOString();
        const newUser = {
            nickname,
            email,
            pass,
            createdAt: nowIso,
            lastOnlineAt: nowIso,
            lastServer: null,
            twoFactorEnabled: false,
            twoFactorSecret: null,
            twoFactorEnabledAt: null,
            twoFactorBackupCodes: []
        };
        users.push(newUser);
        saveUsers(users);
        setCurrentSession(newUser);

        formRegister.reset();
        $('reg-agree').checked = true;
        closeModal(authModal);
        updateAuthUI();
        showToast(i18n.t('toastRegOk', { nick: nickname }));
    });

    // Login submit (supports 2FA when enabled)
    formLogin.addEventListener('submit', (e) => {
        e.preventDefault();
        loginError.hidden = true;

        const ident = $('login-ident').value.trim().toLowerCase();
        const pass = $('login-pass').value;
        const users = getUsers();

        const foundIdx = users.findIndex(
            (u) => (u.nickname.toLowerCase() === ident || u.email === ident) && u.pass === pass
        );

        if (foundIdx === -1) {
            showError(loginError, i18n.t('errLoginCreds'));
            return;
        }

        const found = { ...users[foundIdx] };
        if (found.twoFactorEnabled && found.twoFactorSecret) {
            if (login2faGroup.hidden) {
                login2faGroup.hidden = false;
                login2faCode.focus();
                showError(loginError, i18n.t('errLogin2faPrompt'));
                return;
            }

            const check = verifySecondFactor(found, login2faCode.value);
            if (!check.ok) {
                showError(loginError, i18n.t('errLogin2faBad'));
                return;
            }
            if (check.usedBackup) {
                found.twoFactorBackupCodes = found.twoFactorBackupCodes.filter((_, i) => i !== check.backupIdx);
            }
        }

        found.lastOnlineAt = new Date().toISOString();
        users[foundIdx] = found;
        saveUsers(users);
        setCurrentSession(found);

        formLogin.reset();
        login2faGroup.hidden = true;
        closeModal(authModal);
        updateAuthUI();
        showToast(i18n.t('toastLoginOk', { nick: found.nickname }));
    });

    // Change email submit
    formChangeEmail.addEventListener('submit', (e) => {
        e.preventDefault();
        changeEmailError.hidden = true;

        const user = getCurrentUserRecord();
        if (!user) return;

        const newEmail = $('change-email-new').value.trim().toLowerCase();
        const pass = $('change-email-pass').value;

        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(newEmail)) {
            showError(changeEmailError, i18n.t('errNewEmailBad'));
            return;
        }
        if (newEmail === user.email.toLowerCase()) {
            showError(changeEmailError, i18n.t('errNewEmailSame'));
            return;
        }
        if (pass !== user.pass) {
            showError(changeEmailError, i18n.t('errCurrPassWrong'));
            return;
        }

        const users = getUsers();
        const taken = users.some(
            (u) => u.email.toLowerCase() === newEmail && u.nickname.toLowerCase() !== user.nickname.toLowerCase()
        );
        if (taken) {
            showError(changeEmailError, i18n.t('errEmailUsedOther'));
            return;
        }

        const updated = updateUserRecord(user.nickname, (u) => ({
            ...u,
            email: newEmail
        }));

        formChangeEmail.reset();
        renderProfileDetails(updated);
        showToast(i18n.t('toastEmailChanged'));
    });

    // Change password submit
    formChangePass.addEventListener('submit', (e) => {
        e.preventDefault();
        changePassError.hidden = true;

        const user = getCurrentUserRecord();
        if (!user) return;

        const oldPass = $('change-pass-old').value;
        const newPass = $('change-pass-new').value;
        const newPass2 = $('change-pass-new2').value;

        if (oldPass !== user.pass) {
            showError(changePassError, i18n.t('errCurrPassWrong'));
            return;
        }
        if (newPass.length < 6) {
            showError(changePassError, i18n.t('errNewPassLen'));
            return;
        }
        if (newPass !== newPass2) {
            showError(changePassError, i18n.t('errNewPassMatch'));
            return;
        }
        if (newPass === oldPass) {
            showError(changePassError, i18n.t('errNewPassSame'));
            return;
        }

        updateUserRecord(user.nickname, (u) => ({
            ...u,
            pass: newPass
        }));

        formChangePass.reset();
        showToast(i18n.t('toastPassChanged'));
    });

    // Copy 2FA secret key
    $('tfa-copy-secret').addEventListener('click', async () => {
        if (!pending2faSecret) return;
        try {
            await navigator.clipboard.writeText(pending2faSecret);
            showToast(i18n.t('toastSecretCopied'));
        } catch {
            showToast(i18n.t('toastSecretFallback', { secret: pending2faSecret }));
        }
    });

    // Enable 2FA submit
    tfaSetupForm.addEventListener('submit', (e) => {
        e.preventDefault();
        tfaEnableError.hidden = true;

        const user = getCurrentUserRecord();
        if (!user || !totp || !pending2faSecret) return;

        const code = $('tfa-enable-code').value.trim();
        if (!totp.verifyCode(pending2faSecret, code, 1)) {
            showError(tfaEnableError, i18n.t('err2faEnableCode'));
            return;
        }

        const backupCodes = totp.generateBackupCodes(4);
        const updated = updateUserRecord(user.nickname, (u) => ({
            ...u,
            twoFactorEnabled: true,
            twoFactorSecret: pending2faSecret,
            twoFactorEnabledAt: new Date().toISOString(),
            twoFactorBackupCodes: backupCodes
        }));

        pending2faSecret = null;
        tfaSetupForm.reset();
        renderProfileDetails(updated);
        refresh2faPanel();
        showToast(i18n.t('toast2faEnabled'));
    });

    // Disable 2FA submit
    tfaDisableForm.addEventListener('submit', (e) => {
        e.preventDefault();
        tfaDisableError.hidden = true;

        const user = getCurrentUserRecord();
        if (!user) return;

        const pass = $('tfa-disable-pass').value;
        const code = $('tfa-disable-code').value;

        if (pass !== user.pass) {
            showError(tfaDisableError, i18n.t('errCurrPassWrong'));
            return;
        }

        const check = verifySecondFactor(user, code);
        if (!check.ok) {
            showError(tfaDisableError, i18n.t('err2faDisableCode'));
            return;
        }

        const updated = updateUserRecord(user.nickname, (u) => ({
            ...u,
            twoFactorEnabled: false,
            twoFactorSecret: null,
            twoFactorEnabledAt: null,
            twoFactorBackupCodes: []
        }));

        pending2faSecret = null;
        tfaDisableForm.reset();
        renderProfileDetails(updated);
        refresh2faPanel();
        showToast(i18n.t('toast2faDisabled'), 'warn');
    });

    // Legal docs
    document.querySelectorAll('.footer-link').forEach((btn) => {
        btn.addEventListener('click', () => {
            activeDocKey = btn.dataset.doc;
            renderDocModal();
            openModal(docModal);
        });
    });

    // Modal close bindings
    $('auth-close').addEventListener('click', () => closeModal(authModal));
    $('account-close').addEventListener('click', () => closeModal(accountModal));
    $('dl-close').addEventListener('click', () => closeModal(downloadModal));
    $('doc-close').addEventListener('click', () => closeModal(docModal));
    $('doc-ok').addEventListener('click', () => closeModal(docModal));

    document.querySelectorAll('.modal-backdrop').forEach((backdrop) => {
        backdrop.addEventListener('click', (e) => {
            if (e.target === backdrop) closeModal(backdrop);
        });
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') closeAllModals();
    });

    // Language switcher & initial render
    i18n.applyStaticTranslations();
    i18n.bindSwitcher($('lang-switcher'), () => {
        updateAuthUI();
        renderDocModal();
    });

    configureDownloadLink();
    updateAuthUI();
});
