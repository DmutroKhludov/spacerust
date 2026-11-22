/**
 * SpaceRust Website — Launcher Download & Account Registration
 */
document.addEventListener('DOMContentLoaded', () => {
    const $ = (id) => document.getElementById(id);

    const STORAGE_CURRENT = 'sr_site_current_user';
    const STORAGE_USERS = 'sr_site_users';
    const GITHUB_EXE_URL = 'https://github.com/DmutroKhludov/spacerust/releases/latest/download/SpaceRust.exe';

    const btnRegister = $('btn-register');
    const btnRegisterText = $('btn-register-text');
    const btnDownload = $('btn-download');
    const btnShop = $('btn-shop');

    const heroUserBar = $('hero-user-bar');
    const heroUserAvatar = $('hero-user-avatar');
    const heroUserNick = $('hero-user-nick');
    const btnLogout = $('btn-logout');

    const authModal = $('auth-modal');
    const formRegister = $('form-register');
    const formLogin = $('form-login');
    const regError = $('reg-error');
    const loginError = $('login-error');
    const authTabs = document.querySelectorAll('.auth-tab');

    const accountModal = $('account-modal');
    const accModalAvatar = $('acc-modal-avatar');
    const accModalNick = $('acc-modal-nick');
    const accModalEmail = $('acc-modal-email');

    const downloadModal = $('download-modal');
    const dlDirectLink = $('dl-direct-link');
    const shopModal = $('shop-modal');
    const docModal = $('doc-modal');
    const docTitle = $('doc-title');
    const docContent = $('doc-content');
    const toastContainer = $('toast-container');

    const DOCS = {
        terms: {
            title: 'Пользовательское соглашение',
            html: `
                <h4>1. Общие положения</h4>
                <p>Настоящее соглашение регулирует использование сайта и игрового лаунчера SpaceRust. Регистрируясь на сайте, вы подтверждаете согласие с данными условиями.</p>
                <h4>2. Аккаунт игрока</h4>
                <p>Пользователь несёт ответственность за сохранность пароля и выбор игрового никнейма. Запрещено использовать никнеймы, нарушающие правила проекта.</p>
                <h4>3. Доступ к лаунчеру</h4>
                <p>Лаунчер SpaceRust предоставляется бесплатно и доступен для скачивания на сайте в любое время.</p>
            `
        },
        privacy: {
            title: 'Политика конфиденциальности',
            html: `
                <h4>1. Обрабатываемые данные</h4>
                <p>При регистрации сайт сохраняет указанный вами игровой никнейм и адрес электронной почты для авторизации в системе SpaceRust.</p>
                <h4>2. Использование данных</h4>
                <p>Данные применяются исключительно для идентификации игрока, отображения профиля и предоставления доступа к загрузке лаунчера.</p>
                <h4>3. Безопасность</h4>
                <p>Мы не передаём пользовательские данные третьим лицам и не публикуем адреса электронной почты в открытом доступе.</p>
            `
        },
        rules: {
            title: 'Правила проекта SpaceRust',
            html: `
                <h4>1. Честная игра</h4>
                <p>Запрещено использование сторонних модификаций, читов, макросов и программ, дающих нечестное преимущество над другими игроками.</p>
                <h4>2. Поведение и общение</h4>
                <p>Запрещены оскорбления, спам, реклама сторонних ресурсов и разжигание конфликтов в игровом чате.</p>
                <h4>3. Никнеймы и баги</h4>
                <p>Использование уязвимостей игры или лаунчера для получения выгоды строго запрещено. О найденных ошибках сообщайте администрации.</p>
            `
        }
    };

    function readJson(key, fallback) {
        try {
            const raw = localStorage.getItem(key);
            return raw ? JSON.parse(raw) : fallback;
        } catch {
            return fallback;
        }
    }

    function getCurrentUser() {
        return readJson(STORAGE_CURRENT, null);
    }

    function getUsers() {
        return readJson(STORAGE_USERS, []);
    }

    function setCurrentUser(user) {
        if (user) {
            localStorage.setItem(STORAGE_CURRENT, JSON.stringify({
                nickname: user.nickname,
                email: user.email
            }));
        } else {
            localStorage.removeItem(STORAGE_CURRENT);
        }
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

    function openModal(modal) {
        modal.classList.add('open');
        modal.setAttribute('aria-hidden', 'false');
    }

    function closeModal(modal) {
        modal.classList.remove('open');
        modal.setAttribute('aria-hidden', 'true');
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

        const targetInput = isRegister ? $('reg-nick') : $('login-ident');
        setTimeout(() => targetInput && targetInput.focus(), 40);
    }

    function resolveAvatarUrl(nickname) {
        return `https://mc-heads.net/avatar/${encodeURIComponent(nickname)}/64`;
    }

    function updateAuthUI() {
        const user = getCurrentUser();
        const isAuthed = Boolean(user && user.nickname);

        if (isAuthed) {
            btnRegister.classList.add('authed');
            btnRegisterText.textContent = `АККАУНТ: ${user.nickname}`;

            const avatarUrl = resolveAvatarUrl(user.nickname);
            heroUserBar.hidden = false;
            heroUserNick.textContent = user.nickname;
            heroUserAvatar.src = avatarUrl;
            heroUserAvatar.onerror = () => { heroUserAvatar.src = 'assets/logo.png'; };

            accModalNick.textContent = user.nickname;
            accModalEmail.textContent = user.email || 'Игрок SpaceRust';
            accModalAvatar.src = avatarUrl;
            accModalAvatar.onerror = () => { accModalAvatar.src = 'assets/logo.png'; };
        } else {
            btnRegister.classList.remove('authed');
            btnRegisterText.textContent = 'ЗАРЕГИСТРИРОВАТЬСЯ';

            heroUserBar.hidden = true;
        }
    }

    async function configureDownloadLink() {
        if (!dlDirectLink) return;
        if (window.location.protocol === 'file:') {
            dlDirectLink.href = '../SpaceRust.exe';
            dlDirectLink.setAttribute('download', 'SpaceRust.exe');
            return;
        }

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

        dlDirectLink.href = GITHUB_EXE_URL;
        dlDirectLink.removeAttribute('download');
        dlDirectLink.setAttribute('target', '_blank');
        dlDirectLink.setAttribute('rel', 'noopener');
    }

    function showError(el, msg) {
        el.textContent = msg;
        el.hidden = false;
    }

    // Button handlers
    btnRegister.addEventListener('click', () => {
        if (getCurrentUser()) {
            openModal(accountModal);
        } else {
            switchAuthTab('register');
            openModal(authModal);
        }
    });

    btnDownload.addEventListener('click', () => {
        openModal(downloadModal);
    });

    btnShop.addEventListener('click', () => {
        openModal(shopModal);
    });

    function logoutUser() {
        setCurrentUser(null);
        updateAuthUI();
        closeModal(accountModal);
        showToast('Вы вышли из аккаунта', 'warn');
    }

    btnLogout.addEventListener('click', logoutUser);
    $('acc-logout').addEventListener('click', logoutUser);
    $('acc-open-download').addEventListener('click', () => {
        closeModal(accountModal);
        openModal(downloadModal);
    });

    // Auth modal tabs
    authTabs.forEach((tab) => {
        tab.addEventListener('click', () => switchAuthTab(tab.dataset.auth));
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
            showError(regError, 'Никнейм должен содержать 3–16 символов: латинские буквы, цифры или _.');
            return;
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            showError(regError, 'Введите корректный адрес электронной почты.');
            return;
        }
        if (pass.length < 6) {
            showError(regError, 'Пароль должен содержать не менее 6 символов.');
            return;
        }
        if (pass !== pass2) {
            showError(regError, 'Пароли не совпадают.');
            return;
        }
        if (!agreed) {
            showError(regError, 'Необходимо принять Пользовательское соглашение и Правила проекта.');
            return;
        }

        const users = getUsers();
        if (users.some((u) => u.nickname.toLowerCase() === nickname.toLowerCase())) {
            showError(regError, 'Игрок с таким никнеймом уже зарегистрирован.');
            return;
        }
        if (users.some((u) => u.email === email)) {
            showError(regError, 'Эта электронная почта уже зарегистрирована.');
            return;
        }

        const newUser = { nickname, email, pass };
        users.push(newUser);
        localStorage.setItem(STORAGE_USERS, JSON.stringify(users));
        setCurrentUser(newUser);

        formRegister.reset();
        $('reg-agree').checked = true;
        closeModal(authModal);
        updateAuthUI();
        showToast(`Регистрация завершена, ${nickname}!`);
    });

    // Login submit
    formLogin.addEventListener('submit', (e) => {
        e.preventDefault();
        loginError.hidden = true;

        const ident = $('login-ident').value.trim().toLowerCase();
        const pass = $('login-pass').value;
        const users = getUsers();

        const found = users.find(
            (u) => (u.nickname.toLowerCase() === ident || u.email === ident) && u.pass === pass
        );

        if (!found) {
            showError(loginError, 'Неверный никнейм/email или пароль.');
            return;
        }

        setCurrentUser(found);
        formLogin.reset();
        closeModal(authModal);
        updateAuthUI();
        showToast(`С возвращением, ${found.nickname}!`);
    });

    // Legal docs
    document.querySelectorAll('.footer-link').forEach((btn) => {
        btn.addEventListener('click', () => {
            const doc = DOCS[btn.dataset.doc];
            if (!doc) return;
            docTitle.textContent = doc.title;
            docContent.innerHTML = doc.html;
            openModal(docModal);
        });
    });

    // Modal close bindings
    $('auth-close').addEventListener('click', () => closeModal(authModal));
    $('account-close').addEventListener('click', () => closeModal(accountModal));
    $('dl-close').addEventListener('click', () => closeModal(downloadModal));
    $('shop-close').addEventListener('click', () => closeModal(shopModal));
    $('shop-ok').addEventListener('click', () => closeModal(shopModal));
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

    configureDownloadLink();
    updateAuthUI();
});
