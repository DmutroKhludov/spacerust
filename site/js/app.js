/**
 * SpaceRust Website — Registration & Launcher Download gating
 */
document.addEventListener('DOMContentLoaded', () => {
    const $ = (id) => document.getElementById(id);

    const STORAGE_CURRENT = 'sr_site_current_user';
    const STORAGE_USERS = 'sr_site_users';

    const btnRegister = $('btn-register');
    const btnRegisterText = $('btn-register-text');
    const btnDownload = $('btn-download');
    const downloadHint = $('download-hint');
    const btnShop = $('btn-shop');

    const heroUserBar = $('hero-user-bar');
    const heroUserAvatar = $('hero-user-avatar');
    const heroUserNick = $('hero-user-nick');
    const btnLogout = $('btn-logout');

    const authModal = $('auth-modal');
    const formRegister = $('form-register');
    const formLogin = $('form-login');
    const authTabs = document.querySelectorAll('.auth-tab');

    const downloadModal = $('download-modal');
    const shopModal = $('shop-modal');
    const docModal = $('doc-modal');

    // Direct launcher download URL (local exe when opened from repo/site, or GitHub Releases)
    const RELEASE_URL = 'https://github.com/DmutroKhludov/spacerust/releases/latest/download/SpaceRust.exe';
    const dlDirectLink = $('dl-direct-link');
    if (dlDirectLink) {
        if (window.location.protocol === 'file:') {
            dlDirectLink.href = '../SpaceRust.exe';
            dlDirectLink.setAttribute('download', 'SpaceRust.exe');
        } else {
            dlDirectLink.href = RELEASE_URL;
        }
    }

    function getCurrentUser() {
        try {
            return JSON.parse(localStorage.getItem(STORAGE_CURRENT) || 'null');
        } catch {
            return null;
        }
    }

    function getUsers() {
        try {
            return JSON.parse(localStorage.getItem(STORAGE_USERS) || '[]');
        } catch {
            return [];
        }
    }

    function updateAuthUI() {
        const user = getCurrentUser();
        const isAuthed = !!(user && user.nickname);

        if (isAuthed) {
            btnRegister.classList.add('authed');
            btnRegisterText.textContent = `АККАУНТ: ${user.nickname}`;
            btnDownload.classList.remove('locked');
            btnDownload.setAttribute('aria-disabled', 'false');
            downloadHint.classList.add('hidden');

            heroUserBar.hidden = false;
            heroUserNick.textContent = user.nickname;
            heroUserAvatar.src = `https://mc-heads.net/avatar/${encodeURIComponent(user.nickname)}/64`;
            heroUserAvatar.onerror = () => { heroUserAvatar.src = 'assets/logo.png'; };
        } else {
            btnRegister.classList.remove('authed');
            btnRegisterText.textContent = 'ЗАРЕГИСТРИРОВАТЬСЯ';
            btnDownload.classList.add('locked');
            btnDownload.setAttribute('aria-disabled', 'true');
            downloadHint.classList.remove('hidden');
            heroUserBar.hidden = true;
        }
    }

    // ==========================================
    // MODALS HELPERS
    // ==========================================
    function openModal(el) { el.classList.add('show'); }
    function closeModal(el) { el.classList.remove('show'); }

    [authModal, downloadModal, shopModal, docModal].forEach(m => {
        m?.addEventListener('click', (e) => { if (e.target === m) closeModal(m); });
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') [authModal, downloadModal, shopModal, docModal].forEach(closeModal);
    });

    function switchAuthTab(mode) {
        authTabs.forEach(t => t.classList.toggle('active', t.dataset.auth === mode));
        formRegister.hidden = mode !== 'register';
        formLogin.hidden = mode !== 'login';
    }

    authTabs.forEach(tab => {
        tab.addEventListener('click', () => switchAuthTab(tab.dataset.auth));
    });

    // ==========================================
    // REGISTER / LOGIN / LOGOUT
    // ==========================================
    btnRegister.addEventListener('click', () => {
        const user = getCurrentUser();
        if (user) {
            showToast(`Вы вошли как ${user.nickname}. Загрузка лаунчера открыта!`, 'success');
            return;
        }
        switchAuthTab('register');
        openModal(authModal);
        setTimeout(() => $('reg-nick').focus(), 50);
    });

    $('auth-close')?.addEventListener('click', () => closeModal(authModal));

    $('reg-nick')?.addEventListener('input', (e) => {
        e.target.value = e.target.value.replace(/[^a-zA-Z0-9_]/g, '');
    });

    formRegister.addEventListener('submit', (e) => {
        e.preventDefault();
        const nickname = $('reg-nick').value.trim();
        const email = $('reg-email').value.trim().toLowerCase();
        const pass = $('reg-pass').value;
        const pass2 = $('reg-pass2').value;

        if (nickname.length < 3 || nickname.length > 16) {
            return showToast('Никнейм должен содержать от 3 до 16 символов', 'error');
        }
        if (!email.includes('@') || !email.includes('.')) {
            return showToast('Введите корректный Email', 'error');
        }
        if (pass.length < 6) {
            return showToast('Пароль должен содержать минимум 6 символов', 'error');
        }
        if (pass !== pass2) {
            return showToast('Пароли не совпадают', 'error');
        }

        const users = getUsers();
        if (users.some(u => u.nickname.toLowerCase() === nickname.toLowerCase())) {
            return showToast('Этот никнейм уже зарегистрирован', 'error');
        }
        if (users.some(u => u.email === email)) {
            return showToast('Этот Email уже используется', 'error');
        }

        const newUser = { nickname, email, pass, createdAt: Date.now() };
        users.push(newUser);
        localStorage.setItem(STORAGE_USERS, JSON.stringify(users));
        localStorage.setItem(STORAGE_CURRENT, JSON.stringify({ nickname, email }));

        formRegister.reset();
        closeModal(authModal);
        updateAuthUI();
        showToast(`Регистрация успешна! Теперь можно скачать лаунчер, ${nickname}.`, 'success');
    });

    formLogin.addEventListener('submit', (e) => {
        e.preventDefault();
        const ident = $('login-ident').value.trim().toLowerCase();
        const pass = $('login-pass').value;

        const users = getUsers();
        const found = users.find(
            u => (u.nickname.toLowerCase() === ident || u.email === ident) && u.pass === pass
        );
        if (!found) {
            return showToast('Неверный никнейм/email или пароль', 'error');
        }

        localStorage.setItem(STORAGE_CURRENT, JSON.stringify({ nickname: found.nickname, email: found.email }));
        formLogin.reset();
        closeModal(authModal);
        updateAuthUI();
        showToast(`С возвращением, ${found.nickname}!`, 'success');
    });

    btnLogout?.addEventListener('click', () => {
        localStorage.removeItem(STORAGE_CURRENT);
        updateAuthUI();
        showToast('Вы вышли из аккаунта');
    });

    // ==========================================
    // DOWNLOAD LAUNCHER (ONLY AFTER REGISTRATION)
    // ==========================================
    btnDownload.addEventListener('click', () => {
        const user = getCurrentUser();
        if (!user) {
            showToast('Сначала зарегистрируйтесь, чтобы скачать лаунчер', 'error');
            switchAuthTab('register');
            openModal(authModal);
            return;
        }
        openModal(downloadModal);
    });
    $('dl-close')?.addEventListener('click', () => closeModal(downloadModal));

    // ==========================================
    // SHOP MODAL
    // ==========================================
    btnShop.addEventListener('click', () => openModal(shopModal));
    $('shop-close')?.addEventListener('click', () => closeModal(shopModal));
    $('shop-ok')?.addEventListener('click', () => closeModal(shopModal));

    // ==========================================
    // FOOTER DOCS
    // ==========================================
    const DOCS = {
        terms: {
            title: 'Пользовательское соглашение',
            html: `
                <p><b>1. Общие положения.</b> Проект SpaceRust является неофициальной игровой модификацией для Minecraft 1.16.5 (Fabric) в стиле выживания Rust.</p>
                <p><b>2. Регистрация аккаунта.</b> Пользователь обязуется указывать корректный игровой никнейм. Администрация оставляет за собой право ограничивать доступ при нарушении правил.</p>
                <p><b>3. Доступ к лаунчеру.</b> Загрузка и использование лаунчера SpaceRust предоставляются бесплатно после регистрации на сайте.</p>
            `
        },
        privacy: {
            title: 'Политика конфиденциальности',
            html: `
                <p><b>1. Данные аккаунта.</b> Сайт сохраняет только минимально необходимые данные для авторизации: игровой никнейм и адрес электронной почты.</p>
                <p><b>2. Защита информации.</b> Персональные данные не передаются третьим лицам и используются исключительно для работы учётной записи игрока.</p>
            `
        },
        rules: {
            title: 'Правила проекта SpaceRust',
            html: `
                <p><b>1. Запрещено использование стороннего ПО:</b> читов, макросов, X-Ray, автоматизации и эксплойтов.</p>
                <p><b>2. Запрещено оскорбление игроков и администрации,</b> разжигание конфликтов и спам в игровом чате.</p>
                <p><b>3. Уважайте честную игру:</b> соблюдайте лимиты команд на серверах и правила конкретного вайпа.</p>
            `
        }
    };

    document.querySelectorAll('.footer-link').forEach(btn => {
        btn.addEventListener('click', () => {
            const doc = DOCS[btn.dataset.doc];
            if (!doc) return;
            $('doc-title').textContent = doc.title;
            $('doc-content').innerHTML = doc.html;
            openModal(docModal);
        });
    });
    $('doc-close')?.addEventListener('click', () => closeModal(docModal));
    $('doc-ok')?.addEventListener('click', () => closeModal(docModal));

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
            setTimeout(() => toast.remove(), 220);
        }, 3000);
        while (container.children.length > 3) container.firstChild.remove();
    }

    updateAuthUI();
});
