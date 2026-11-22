/**
 * SpaceRust Website i18n — Russian (ru), Ukrainian (uk), English (en)
 */
window.SpaceRustI18N = (() => {
    const STORAGE_LANG = 'sr_site_lang';
    const SUPPORTED = ['ru', 'uk', 'en'];

    const DICT = {
        ru: {
            // Page titles
            titleHome: 'SpaceRust — Выживание в стиле Rust на Minecraft 1.16.5',
            titleShop: 'SpaceRust — Донат Магазин (Привилегии, Киты, Кейсы)',

            // Hero & Main buttons
            registerBtn: 'ЗАРЕГИСТРИРОВАТЬСЯ',
            accountBtnPrefix: 'АККАУНТ: ',
            downloadBtn: 'СКАЧАТЬ ЛАУНЧЕР',
            shopBtn: 'ДОНАТ МАГАЗИН',
            profileLabel: 'ПРОФИЛЬ',
            logoutBtn: 'Выйти',
            guestName: 'Гость',

            // Footer
            footerCopy: '© 2026 SpaceRust. Все права защищены.',
            footerDisclaimer1: 'SpaceRust — неофициальный проект Minecraft, не утверждён Mojang и не связан с Mojang.',
            footerDisclaimer2: 'SpaceRust — неофициальный проект Rust, не утверждён Facepunch и не связан с Facepunch.',
            linkTerms: 'Пользовательское соглашение',
            linkPrivacy: 'Политика конфиденциальности',
            linkRules: 'Правила проекта',
            linkHome: 'На главную',
            linkShopTerms: 'Условия выдачи товаров',

            // Auth modal
            tabRegister: 'РЕГИСТРАЦИЯ',
            tabLogin: 'ВХОД',
            regNote: 'Зарегистрируйтесь в SpaceRust, чтобы создать профиль и закрепить свой игровой никнейм.',
            regNickLabel: 'Игровой никнейм (3–16 символов, латиница и цифры)',
            regNickPh: 'Например: Pablus',
            emailLabel: 'Электронная почта',
            regPassLabel: 'Пароль (минимум 6 символов)',
            regPass2Label: 'Повторите пароль',
            regAgree: 'Я принимаю Пользовательское соглашение и Правила проекта',
            regSubmit: 'ЗАРЕГИСТРИРОВАТЬСЯ',
            loginNote: 'Войдите в свой аккаунт SpaceRust.',
            loginIdentLabel: 'Никнейм или Email',
            loginIdentPh: 'Никнейм или Email',
            passLabel: 'Пароль',
            login2faLabel: 'Код двухэтапной защиты (6 цифр или резервный код)',
            loginSubmit: 'ВОЙТИ',

            // Profile modal
            profileTitle: 'ПРОФИЛЬ ИГРОКА',
            tfaBadgeOn: '2FA включена',
            tfaBadgeOff: '2FA не привязана',
            ptabOverview: 'ОБЗОР',
            ptabEmail: 'ПОЧТА',
            ptabPassword: 'ПАРОЛЬ',
            ptab2fa: '2FA ЗАЩИТА',
            statCreated: 'Дата создания аккаунта',
            statLastOnline: 'Последний раз в онлайне',
            statLastServer: 'Последний сервер',
            neverJoinedServer: 'Ещё не заходил на сервер',
            btnDownloadLauncher: 'СКАЧАТЬ ЛАУНЧЕР',
            btnLogoutCaps: 'ВЫЙТИ',

            // Email & password change
            currentEmailLabel: 'Текущая почта',
            newEmailLabel: 'Новая электронная почта',
            confirmCurrPassLabel: 'Текущий пароль для подтверждения',
            saveEmailBtn: 'СОХРАНИТЬ ПОЧТУ',
            currPassLabel: 'Текущий пароль',
            newPassLabel: 'Новый пароль (минимум 6 символов)',
            newPass2Label: 'Повторите новый пароль',
            changePassBtn: 'ИЗМЕНИТЬ ПАРОЛЬ',

            // 2FA
            tfaSetupNote: 'Привяжите приложение-аутентификатор (Google Authenticator, Authy или 2FAS), отсканировав QR-код или введя секретный ключ вручную.',
            tfaSecretLabel: 'Секретный ключ (Base32)',
            copyBtn: 'Копировать',
            tfaLiveLabel: 'Проверочный код сейчас:',
            tfaCodeInputLabel: 'Введите 6-значный код из аутентификатора',
            tfaEnableBtn: 'ВКЛЮЧИТЬ ДВУХЭТАПНУЮ ЗАЩИТУ',
            tfaActiveTitle: 'Двухэтапная защита активна',
            tfaEnabledSincePrefix: 'Включена ',
            tfaLinkedFallback: 'Привязана к аккаунту',
            tfaActiveBadge: 'АКТИВНА',
            tfaBackupLabel: 'Резервные коды восстановления (сохраните в надёжном месте)',
            tfaDisablePassLabel: 'Текущий пароль для отключения 2FA',
            tfaDisableCodeLabel: 'Код из аутентификатора (6 цифр) или резервный код',
            tfaDisableBtn: 'ОТКЛЮЧИТЬ 2FA',

            // Download modal
            dlTitle: 'СКАЧАТЬ ЛАУНЧЕР SPACERUST',
            dlPlatform: 'SpaceRust Launcher (Windows x64)',
            dlSub: 'Версия патча v2.5.0 · Minecraft 1.16.5 Fabric',
            dlNote: 'Нажмите кнопку ниже, чтобы скачать автономный файл <b>SpaceRust.exe</b>. После запуска лаунчер автоматически подготовит клиент и моды.',
            dlBtnExe: 'СКАЧАТЬ SPACERUST.EXE',
            dlAltGit: 'Открыть страницу релизов на GitHub',
            closeBtn: 'ЗАКРЫТЬ',

            // Validation / Toasts (app.js)
            errRegNick: 'Никнейм должен содержать 3–16 символов: латинские буквы, цифры или _.',
            errRegEmail: 'Введите корректный адрес электронной почты.',
            errRegPassLen: 'Пароль должен содержать не менее 6 символов.',
            errRegPassMatch: 'Пароли не совпадают.',
            errRegAgree: 'Необходимо принять Пользовательское соглашение и Правила проекта.',
            errNickTaken: 'Игрок с таким никнеймом уже зарегистрирован.',
            errEmailTaken: 'Эта электронная почта уже зарегистрирована.',
            toastRegOk: 'Регистрация завершена, {nick}!',
            errLoginCreds: 'Неверный никнейм/email или пароль.',
            errLogin2faPrompt: 'Аккаунт защищён 2FA. Введите 6-значный код или резервный код.',
            errLogin2faBad: 'Неверный код двухэтапной защиты.',
            toastLoginOk: 'С возвращением, {nick}!',
            toastLogout: 'Вы вышли из аккаунта',
            errNewEmailBad: 'Введите корректный новый адрес электронной почты.',
            errNewEmailSame: 'Новая почта совпадает с текущей.',
            errCurrPassWrong: 'Неверный текущий пароль.',
            errEmailUsedOther: 'Эта почта уже привязана к другому аккаунту.',
            toastEmailChanged: 'Почта успешно изменена',
            errNewPassLen: 'Новый пароль должен содержать не менее 6 символов.',
            errNewPassMatch: 'Новые пароли не совпадают.',
            errNewPassSame: 'Новый пароль должен отличаться от текущего.',
            toastPassChanged: 'Пароль успешно изменён',
            toastSecretCopied: 'Секретный ключ скопирован',
            toastSecretFallback: 'Ключ: {secret}',
            err2faEnableCode: 'Неверный 6-значный код. Проверьте код и попробуйте снова.',
            toast2faEnabled: 'Двухэтапная защита (2FA) включена',
            err2faDisableCode: 'Неверный код 2FA или резервный код.',
            toast2faDisabled: 'Двухэтапная защита отключена',

            // Shop page
            backHome: 'НА ГЛАВНУЮ',
            shopSub: 'ДОНАТ МАГАЗИН',
            recipientLabel: 'ПОЛУЧАТЕЛЬ',
            ordersBtn: 'ЗАКАЗЫ',
            cartBtn: 'КОРЗИНА',
            shopHeroTitleHtml: 'МАГАЗИН <span>SPACERUST</span>',
            shopHeroDesc: 'Привилегии, наборы выживания (киты), оружейные кейсы и игровые услуги — выдаются сразу на все серверы SpaceRust.',
            shopDeliveryPill: 'Выдача действует на всех серверах',
            catAll: 'ВСЕ ТОВАРЫ',
            catPrivileges: 'ПРИВИЛЕГИИ',
            catKits: 'КИТЫ (НАБОРЫ)',
            catCases: 'КЕЙСЫ И СКИНЫ',
            catServices: 'УСЛУГИ',
            searchPh: 'Поиск: VIP, Кит рейдер, АК-47...',
            shopEmptyText: 'По вашему запросу ничего не найдено.',
            shopResetBtn: 'Сбросить фильтры',
            detailsBtn: 'Подробнее',
            addToCartBtn: 'В корзину',
            detailPerksTitle: 'Что входит:',
            detailPriceLabel: 'Стоимость',
            detailAddBtn: 'ДОБАВИТЬ В КОРЗИНУ',
            cartModalTitle: 'КОРЗИНА ПОКУПОК',
            cartEmptyText: 'Корзина пока пуста. Выберите привилегию, кит или кейс в каталоге.',
            checkoutNickLabel: 'Никнейм получателя (действует на всех серверах)',
            checkoutNickPh: 'Ваш игровой никнейм',
            promoPh: 'Промокод (например: WIPE2026 или RUSTME)',
            promoApplyBtn: 'ПРИМЕНИТЬ',
            cartSubtotalLabel: 'Товары:',
            cartTotalLabel: 'Итого к оплате:',
            checkoutSubmitBtn: 'ОФОРМИТЬ ЗАКАЗ',
            checkoutNote: 'Демо-оформление: привилегии и наборы распространяются на все серверы SpaceRust, заказ сохраняется в истории браузера.',
            ordersModalTitle: 'ИСТОРИЯ ЗАКАЗОВ',
            ordersEmptyText: 'История заказов пока пуста.',
            toastAddedCart: '«{title}» добавлен в корзину',
            promoInvalid: 'Промокод не найден. Попробуйте WIPE2026 или RUSTME.',
            promoValid: 'Промокод {code} применён: скидка {pct}%!',
            discountLabel: 'Скидка ({code} −{pct}%):',
            errCheckoutNick: 'Никнейм должен содержать 3–16 символов (латиница, цифры или _).',
            toastOrderDone: 'Заказ #{id} для {nick} оформлен!',
            orderPrefix: 'Заказ #',
            orderPlayer: 'Игрок:',
            orderScope: 'Действие:',
            orderScopeAll: 'Все серверы SpaceRust',
            orderItems: 'Товары:',
            orderStatusDemo: 'Оформлен (демо-режим)',
            catLabelPrivileges: 'Привилегия',
            catLabelKits: 'Кит-набор',
            catLabelCases: 'Кейс / Скин',
            catLabelServices: 'Услуга',

            // Docs (Home & Shop)
            docTermsTitle: 'Пользовательское соглашение',
            docTermsHtml: `
                <h4>1. Общие положения</h4>
                <p>Настоящее соглашение регулирует использование сайта и игрового лаунчера SpaceRust. Регистрируясь на сайте, вы подтверждаете согласие с данными условиями.</p>
                <h4>2. Аккаунт игрока</h4>
                <p>Пользователь несёт ответственность за сохранность пароля и выбор игрового никнейма. Запрещено использовать никнеймы, нарушающие правила проекта.</p>
                <h4>3. Доступ к лаунчеру</h4>
                <p>Лаунчер SpaceRust предоставляется бесплатно и доступен для скачивания на сайте в любое время.</p>
            `,
            docPrivacyTitle: 'Политика конфиденциальности',
            docPrivacyHtml: `
                <h4>1. Обрабатываемые данные</h4>
                <p>При регистрации сайт сохраняет указанный вами игровой никнейм и адрес электронной почты для авторизации в системе SpaceRust.</p>
                <h4>2. Использование данных</h4>
                <p>Данные применяются исключительно для идентификации игрока, отображения профиля и защиты аккаунта.</p>
                <h4>3. Безопасность</h4>
                <p>Мы не передаём пользовательские данные третьим лицам и не публикуем адреса электронной почты в открытом доступе.</p>
            `,
            docRulesTitle: 'Правила проекта SpaceRust',
            docRulesHtml: `
                <h4>1. Честная игра</h4>
                <p>Запрещено использование сторонних модификаций, читов, макросов и программ, дающих нечестное преимущество над другими игроками.</p>
                <h4>2. Поведение и общение</h4>
                <p>Запрещены оскорбления, спам, реклама сторонних ресурсов и разжигание конфликтов в игровом чате.</p>
                <h4>3. Никнеймы и баги</h4>
                <p>Использование уязвимостей игры или лаунчера для получения выгоды строго запрещено. О найденных ошибках сообщайте администрации.</p>
            `,
            docShopTermsTitle: 'Условия выдачи товаров',
            docShopTermsHtml: `
                <h4>1. Выдача на все серверы</h4>
                <p>Все привилегии, киты и кейсы из донат-магазина SpaceRust действуют сразу на всех серверах проекта и доступны через /claim или автоматически при входе.</p>
                <h4>2. Срок действия привилегий</h4>
                <p>Временные привилегии действуют указанный срок (30 дней) с момента активации. Постоянные привилегии сохраняются на всех последующих вайпах.</p>
                <h4>3. Правила покупок</h4>
                <p>Внимательно проверяйте игровой никнейм перед оформлением заказа. Все цифровые товары привязываются к указанному аккаунту.</p>
            `
        },

        uk: {
            titleHome: 'SpaceRust — Виживання у стилі Rust на Minecraft 1.16.5',
            titleShop: 'SpaceRust — Донат Магазин (Привілеї, Кіти, Кейси)',

            registerBtn: 'ЗАРЕЄСТРУВАТИСЯ',
            accountBtnPrefix: 'АКАУНТ: ',
            downloadBtn: 'ЗАВАНТАЖИТИ ЛАУНЧЕР',
            shopBtn: 'ДОНАТ МАГАЗИН',
            profileLabel: 'ПРОФІЛЬ',
            logoutBtn: 'Вийти',
            guestName: 'Гість',

            footerCopy: '© 2026 SpaceRust. Усі права захищено.',
            footerDisclaimer1: 'SpaceRust — неофіційний проєкт Minecraft, не схвалений Mojang і не повʼязаний із Mojang.',
            footerDisclaimer2: 'SpaceRust — неофіційний проєкт Rust, не схвалений Facepunch і не повʼязаний із Facepunch.',
            linkTerms: 'Угода користувача',
            linkPrivacy: 'Політика конфіденційності',
            linkRules: 'Правила проєкту',
            linkHome: 'На головну',
            linkShopTerms: 'Умови видачі товарів',

            tabRegister: 'РЕЄСТРАЦІЯ',
            tabLogin: 'ВХІД',
            regNote: 'Зареєструйтеся в SpaceRust, щоб створити профіль та закріпити свій ігровий нікнейм.',
            regNickLabel: 'Ігровий нікнейм (3–16 символів, латиниця та цифри)',
            regNickPh: 'Наприклад: Pablus',
            emailLabel: 'Електронна пошта',
            regPassLabel: 'Пароль (мінімум 6 символів)',
            regPass2Label: 'Повторіть пароль',
            regAgree: 'Я приймаю Угоду користувача та Правила проєкту',
            regSubmit: 'ЗАРЕЄСТРУВАТИСЯ',
            loginNote: 'Увійдіть у свій акаунт SpaceRust.',
            loginIdentLabel: 'Нікнейм або Email',
            loginIdentPh: 'Нікнейм або Email',
            passLabel: 'Пароль',
            login2faLabel: 'Код двохетапного захисту (6 цифр або резервний код)',
            loginSubmit: 'УВІЙТИ',

            profileTitle: 'ПРОФІЛЬ ГРАВЦЯ',
            tfaBadgeOn: '2FA увімкнено',
            tfaBadgeOff: '2FA не привʼязано',
            ptabOverview: 'ОГЛЯД',
            ptabEmail: 'ПОШТА',
            ptabPassword: 'ПАРОЛЬ',
            ptab2fa: '2FA ЗАХИСТ',
            statCreated: 'Дата створення акаунту',
            statLastOnline: 'Останній раз в онлайні',
            statLastServer: 'Останній сервер',
            neverJoinedServer: 'Ще не заходив на сервер',
            btnDownloadLauncher: 'ЗАВАНТАЖИТИ ЛАУНЧЕР',
            btnLogoutCaps: 'ВИЙТИ',

            currentEmailLabel: 'Поточна пошта',
            newEmailLabel: 'Нова електронна пошта',
            confirmCurrPassLabel: 'Поточний пароль для підтвердження',
            saveEmailBtn: 'ЗБЕРЕГТИ ПОШТУ',
            currPassLabel: 'Поточний пароль',
            newPassLabel: 'Новий пароль (мінімум 6 символів)',
            newPass2Label: 'Повторіть новий пароль',
            changePassBtn: 'ЗМІНИТИ ПАРОЛЬ',

            tfaSetupNote: 'Привʼяжіть застосунок-автентифікатор (Google Authenticator, Authy або 2FAS), відсканувавши QR-код або ввівши секретний ключ вручну.',
            tfaSecretLabel: 'Секретний ключ (Base32)',
            copyBtn: 'Копіювати',
            tfaLiveLabel: 'Перевірочний код зараз:',
            tfaCodeInputLabel: 'Введіть 6-значний код з автентифікатора',
            tfaEnableBtn: 'УВІМКНУТИ ДВОХЕТАПНИЙ ЗАХИСТ',
            tfaActiveTitle: 'Двохетапний захист активний',
            tfaEnabledSincePrefix: 'Увімкнено ',
            tfaLinkedFallback: 'Привʼязано до акаунту',
            tfaActiveBadge: 'АКТИВНА',
            tfaBackupLabel: 'Резервні коди відновлення (збережіть у надійному місці)',
            tfaDisablePassLabel: 'Поточний пароль для вимкнення 2FA',
            tfaDisableCodeLabel: 'Код з автентифікатора (6 цифр) або резервний код',
            tfaDisableBtn: 'ВИМКНУТИ 2FA',

            dlTitle: 'ЗАВАНТАЖИТИ ЛАУНЧЕР SPACERUST',
            dlPlatform: 'SpaceRust Launcher (Windows x64)',
            dlSub: 'Версія патчу v2.5.0 · Minecraft 1.16.5 Fabric',
            dlNote: 'Натисніть кнопку нижче, щоб завантажити автономний файл <b>SpaceRust.exe</b>. Після запуску лаунчер автоматично підготує клієнт та моди.',
            dlBtnExe: 'ЗАВАНТАЖИТИ SPACERUST.EXE',
            dlAltGit: 'Відкрити сторінку релізів на GitHub',
            closeBtn: 'ЗАКРИТИ',

            errRegNick: 'Нікнейм повинен містити 3–16 символів: латинські літери, цифри або _.',
            errRegEmail: 'Введіть коректну адресу електронної пошти.',
            errRegPassLen: 'Пароль повинен містити щонайменше 6 символів.',
            errRegPassMatch: 'Паролі не збігаються.',
            errRegAgree: 'Необхідно прийняти Угоду користувача та Правила проєкту.',
            errNickTaken: 'Гравець із таким нікнеймом уже зареєстрований.',
            errEmailTaken: 'Ця електронна пошта вже зареєстрована.',
            toastRegOk: 'Реєстрацію завершено, {nick}!',
            errLoginCreds: 'Невірний нікнейм/email або пароль.',
            errLogin2faPrompt: 'Акаунт захищено 2FA. Введіть 6-значний код або резервний код.',
            errLogin2faBad: 'Невірний код двохетапного захисту.',
            toastLoginOk: 'З поверненням, {nick}!',
            toastLogout: 'Ви вийшли з акаунту',
            errNewEmailBad: 'Введіть коректну нову електронну пошту.',
            errNewEmailSame: 'Нова пошта збігається з поточною.',
            errCurrPassWrong: 'Невірний поточний пароль.',
            errEmailUsedOther: 'Ця пошта вже привʼязана до іншого акаунту.',
            toastEmailChanged: 'Пошту успішно змінено',
            errNewPassLen: 'Новий пароль повинен містити щонайменше 6 символів.',
            errNewPassMatch: 'Нові паролі не збігаються.',
            errNewPassSame: 'Новий пароль повинен відрізнятися від поточного.',
            toastPassChanged: 'Пароль успішно змінено',
            toastSecretCopied: 'Секретний ключ скопійовано',
            toastSecretFallback: 'Ключ: {secret}',
            err2faEnableCode: 'Невірний 6-значний код. Перевірте код і спробуйте знову.',
            toast2faEnabled: 'Двохетапний захист (2FA) увімкнено',
            err2faDisableCode: 'Невірний код 2FA або резервний код.',
            toast2faDisabled: 'Двохетапний захист вимкнено',

            backHome: 'НА ГОЛОВНУ',
            shopSub: 'ДОНАТ МАГАЗИН',
            recipientLabel: 'ОТРИМУВАЧ',
            ordersBtn: 'ЗАМОВЛЕННЯ',
            cartBtn: 'КОШИК',
            shopHeroTitleHtml: 'МАГАЗИН <span>SPACERUST</span>',
            shopHeroDesc: 'Привілеї, набори виживання (кіти), збройові кейси та ігрові послуги — діють одразу на всіх серверах SpaceRust.',
            shopDeliveryPill: 'Видача діє на всіх серверах',
            catAll: 'УСІ ТОВАРИ',
            catPrivileges: 'ПРИВІЛЕГІЇ',
            catKits: 'КІТИ (НАБОРИ)',
            catCases: 'КЕЙСИ ТА СКІНИ',
            catServices: 'ПОСЛУГИ',
            searchPh: 'Пошук: VIP, Кіт рейдер, АК-47...',
            shopEmptyText: 'За вашим запитом нічого не знайдено.',
            shopResetBtn: 'Скинути фільтри',
            detailsBtn: 'Детальніше',
            addToCartBtn: 'До кошика',
            detailPerksTitle: 'Що входить:',
            detailPriceLabel: 'Вартість',
            detailAddBtn: 'ДОДАТИ ДО КОШИКА',
            cartModalTitle: 'КОШИК ПОКУПОК',
            cartEmptyText: 'Кошик поки порожній. Оберіть привілей, кіт або кейс у каталозі.',
            checkoutNickLabel: 'Нікнейм отримувача (діє на всіх серверах)',
            checkoutNickPh: 'Ваш ігровий нікнейм',
            promoPh: 'Промокод (наприклад: WIPE2026 або RUSTME)',
            promoApplyBtn: 'ЗАСТОСУВАТИ',
            cartSubtotalLabel: 'Товари:',
            cartTotalLabel: 'Разом до сплати:',
            checkoutSubmitBtn: 'ОФОРМИТИ ЗАМОВЛЕННЯ',
            checkoutNote: 'Демо-оформлення: привілеї та набори діють на всіх серверах SpaceRust, замовлення зберігається в історії браузера.',
            ordersModalTitle: 'ІСТОРІЯ ЗАМОВЛЕНЬ',
            ordersEmptyText: 'Історія замовлень поки порожня.',
            toastAddedCart: '«{title}» додано до кошика',
            promoInvalid: 'Промокод не знайдено. Спробуйте WIPE2026 або RUSTME.',
            promoValid: 'Промокод {code} застосовано: знижка {pct}%!',
            discountLabel: 'Знижка ({code} −{pct}%):',
            errCheckoutNick: 'Нікнейм повинен містити 3–16 символів (латиниця, цифри або _).',
            toastOrderDone: 'Замовлення #{id} для {nick} оформлено!',
            orderPrefix: 'Замовлення #',
            orderPlayer: 'Гравець:',
            orderScope: 'Дія:',
            orderScopeAll: 'Усі сервери SpaceRust',
            orderItems: 'Товари:',
            orderStatusDemo: 'Оформлено (демо-режим)',
            catLabelPrivileges: 'Привілей',
            catLabelKits: 'Кіт-набір',
            catLabelCases: 'Кейс / Скін',
            catLabelServices: 'Послуга',

            docTermsTitle: 'Угода користувача',
            docTermsHtml: `
                <h4>1. Загальні положення</h4>
                <p>Ця угода регулює використання сайту та ігрового лаунчера SpaceRust. Реєструючись на сайті, ви підтверджуєте згоду з цими умовами.</p>
                <h4>2. Акаунт гравця</h4>
                <p>Користувач несе відповідальність за збереження пароля та вибір ігрового нікнейму. Заборонено використовувати нікнейми, що порушують правила проєкту.</p>
                <h4>3. Доступ до лаунчера</h4>
                <p>Лаунчер SpaceRust надається безкоштовно та доступний для завантаження на сайті в будь-який час.</p>
            `,
            docPrivacyTitle: 'Політика конфіденційності',
            docPrivacyHtml: `
                <h4>1. Дані, що обробляються</h4>
                <p>Під час реєстрації сайт зберігає вказаний вами ігровий нікнейм та адресу електронної пошти для авторизації в системі SpaceRust.</p>
                <h4>2. Використання даних</h4>
                <p>Дані використовуються виключно для ідентифікації гравця, відображення профілю та захисту акаунту.</p>
                <h4>3. Безпека</h4>
                <p>Ми не передаємо дані користувачів третім особам і не публікуємо адреси електронної пошти у відкритому доступі.</p>
            `,
            docRulesTitle: 'Правила проєкту SpaceRust',
            docRulesHtml: `
                <h4>1. Чесна гра</h4>
                <p>Заборонено використання сторонніх модифікацій, читів, макросів та програм, що дають нечесну перевагу над іншими гравцями.</p>
                <h4>2. Поведінка та спілкування</h4>
                <p>Заборонені образи, спам, реклама сторонніх ресурсів і розпалювання конфліктів в ігровому чаті.</p>
                <h4>3. Нікнейми та баги</h4>
                <p>Використання вразливостей гри або лаунчера для отримання вигоди суворо заборонено. Про знайдені помилки повідомляйте адміністрацію.</p>
            `,
            docShopTermsTitle: 'Умови видачі товарів',
            docShopTermsHtml: `
                <h4>1. Видача на всі сервери</h4>
                <p>Усі привілеї, кіти та кейси з донат-магазину SpaceRust діють одразу на всіх серверах проєкту та доступні через /claim або автоматично під час входу.</p>
                <h4>2. Термін дії привілеїв</h4>
                <p>Тимчасові привілеї діють зазначений термін (30 днів) з моменту активації. Постійні привілеї зберігаються на всіх наступних вайпах.</p>
                <h4>3. Правила покупок</h4>
                <p>Уважно перевіряйте ігровий нікнейм перед оформленням замовлення. Усі цифрові товари привʼязуються до вказаного акаунту.</p>
            `
        },

        en: {
            titleHome: 'SpaceRust — Rust-Style Survival on Minecraft 1.16.5',
            titleShop: 'SpaceRust — Donate Shop (Ranks, Kits, Cases)',

            registerBtn: 'REGISTER',
            accountBtnPrefix: 'ACCOUNT: ',
            downloadBtn: 'DOWNLOAD LAUNCHER',
            shopBtn: 'DONATE SHOP',
            profileLabel: 'PROFILE',
            logoutBtn: 'Log out',
            guestName: 'Guest',

            footerCopy: '© 2026 SpaceRust. All rights reserved.',
            footerDisclaimer1: 'SpaceRust is an unofficial Minecraft project, not approved by or associated with Mojang.',
            footerDisclaimer2: 'SpaceRust is an unofficial Rust project, not approved by or associated with Facepunch.',
            linkTerms: 'Terms of Service',
            linkPrivacy: 'Privacy Policy',
            linkRules: 'Project Rules',
            linkHome: 'Home',
            linkShopTerms: 'Delivery Terms',

            tabRegister: 'REGISTER',
            tabLogin: 'LOG IN',
            regNote: 'Create a SpaceRust account to reserve your in-game nickname and manage your profile.',
            regNickLabel: 'In-game nickname (3–16 chars, Latin letters & digits)',
            regNickPh: 'e.g. Pablus',
            emailLabel: 'Email address',
            regPassLabel: 'Password (minimum 6 characters)',
            regPass2Label: 'Confirm password',
            regAgree: 'I accept the Terms of Service and Project Rules',
            regSubmit: 'REGISTER',
            loginNote: 'Sign in to your SpaceRust account.',
            loginIdentLabel: 'Nickname or Email',
            loginIdentPh: 'Nickname or Email',
            passLabel: 'Password',
            login2faLabel: 'Two-factor code (6 digits or backup code)',
            loginSubmit: 'LOG IN',

            profileTitle: 'PLAYER PROFILE',
            tfaBadgeOn: '2FA enabled',
            tfaBadgeOff: '2FA not linked',
            ptabOverview: 'OVERVIEW',
            ptabEmail: 'EMAIL',
            ptabPassword: 'PASSWORD',
            ptab2fa: '2FA SECURITY',
            statCreated: 'Account created',
            statLastOnline: 'Last online',
            statLastServer: 'Last server',
            neverJoinedServer: 'Has not joined a server yet',
            btnDownloadLauncher: 'DOWNLOAD LAUNCHER',
            btnLogoutCaps: 'LOG OUT',

            currentEmailLabel: 'Current email',
            newEmailLabel: 'New email address',
            confirmCurrPassLabel: 'Current password to confirm',
            saveEmailBtn: 'SAVE EMAIL',
            currPassLabel: 'Current password',
            newPassLabel: 'New password (minimum 6 characters)',
            newPass2Label: 'Repeat new password',
            changePassBtn: 'CHANGE PASSWORD',

            tfaSetupNote: 'Link an authenticator app (Google Authenticator, Authy, or 2FAS) by scanning the QR code or entering the secret key manually.',
            tfaSecretLabel: 'Secret key (Base32)',
            copyBtn: 'Copy',
            tfaLiveLabel: 'Current verification code:',
            tfaCodeInputLabel: 'Enter the 6-digit code from your authenticator',
            tfaEnableBtn: 'ENABLE TWO-FACTOR AUTH',
            tfaActiveTitle: 'Two-factor authentication is active',
            tfaEnabledSincePrefix: 'Enabled ',
            tfaLinkedFallback: 'Linked to your account',
            tfaActiveBadge: 'ACTIVE',
            tfaBackupLabel: 'Backup recovery codes (keep them in a safe place)',
            tfaDisablePassLabel: 'Current password to disable 2FA',
            tfaDisableCodeLabel: 'Authenticator code (6 digits) or backup code',
            tfaDisableBtn: 'DISABLE 2FA',

            dlTitle: 'DOWNLOAD SPACERUST LAUNCHER',
            dlPlatform: 'SpaceRust Launcher (Windows x64)',
            dlSub: 'Patch v2.5.0 · Minecraft 1.16.5 Fabric',
            dlNote: 'Click the button below to download the standalone <b>SpaceRust.exe</b> file. Once launched, it automatically prepares the client and mods.',
            dlBtnExe: 'DOWNLOAD SPACERUST.EXE',
            dlAltGit: 'Open GitHub Releases page',
            closeBtn: 'CLOSE',

            errRegNick: 'Nickname must be 3–16 characters: Latin letters, digits, or _.',
            errRegEmail: 'Please enter a valid email address.',
            errRegPassLen: 'Password must be at least 6 characters long.',
            errRegPassMatch: 'Passwords do not match.',
            errRegAgree: 'You must accept the Terms of Service and Project Rules.',
            errNickTaken: 'This nickname is already registered.',
            errEmailTaken: 'This email address is already registered.',
            toastRegOk: 'Registration complete, {nick}!',
            errLoginCreds: 'Invalid nickname/email or password.',
            errLogin2faPrompt: 'This account is protected by 2FA. Enter your 6-digit code or backup code.',
            errLogin2faBad: 'Invalid two-factor authentication code.',
            toastLoginOk: 'Welcome back, {nick}!',
            toastLogout: 'You have logged out',
            errNewEmailBad: 'Please enter a valid new email address.',
            errNewEmailSame: 'New email matches your current email.',
            errCurrPassWrong: 'Incorrect current password.',
            errEmailUsedOther: 'This email is already used by another account.',
            toastEmailChanged: 'Email updated successfully',
            errNewPassLen: 'New password must be at least 6 characters.',
            errNewPassMatch: 'New passwords do not match.',
            errNewPassSame: 'New password must differ from the current one.',
            toastPassChanged: 'Password changed successfully',
            toastSecretCopied: 'Secret key copied',
            toastSecretFallback: 'Key: {secret}',
            err2faEnableCode: 'Invalid 6-digit code. Check your code and try again.',
            toast2faEnabled: 'Two-factor authentication (2FA) enabled',
            err2faDisableCode: 'Invalid 2FA code or backup code.',
            toast2faDisabled: 'Two-factor authentication disabled',

            backHome: 'HOME',
            shopSub: 'DONATE SHOP',
            recipientLabel: 'RECIPIENT',
            ordersBtn: 'ORDERS',
            cartBtn: 'CART',
            shopHeroTitleHtml: '<span>SPACERUST</span> STORE',
            shopHeroDesc: 'Privileges, survival kits, weapon cases, and in-game services — valid across all SpaceRust servers.',
            shopDeliveryPill: 'Valid across all servers',
            catAll: 'ALL ITEMS',
            catPrivileges: 'PRIVILEGES',
            catKits: 'KITS',
            catCases: 'CASES & SKINS',
            catServices: 'SERVICES',
            searchPh: 'Search: VIP, Raider kit, AK-47...',
            shopEmptyText: 'No items match your search.',
            shopResetBtn: 'Reset filters',
            detailsBtn: 'Details',
            addToCartBtn: 'Add to cart',
            detailPerksTitle: 'Includes:',
            detailPriceLabel: 'Price',
            detailAddBtn: 'ADD TO CART',
            cartModalTitle: 'SHOPPING CART',
            cartEmptyText: 'Your cart is empty. Pick a privilege, kit, or case from the catalog.',
            checkoutNickLabel: 'Recipient nickname (works on all servers)',
            checkoutNickPh: 'Your in-game nickname',
            promoPh: 'Promo code (e.g. WIPE2026 or RUSTME)',
            promoApplyBtn: 'APPLY',
            cartSubtotalLabel: 'Subtotal:',
            cartTotalLabel: 'Total:',
            checkoutSubmitBtn: 'PLACE ORDER',
            checkoutNote: 'Demo checkout: privileges and kits apply across all SpaceRust servers; orders are saved in browser history.',
            ordersModalTitle: 'ORDER HISTORY',
            ordersEmptyText: 'No orders yet.',
            toastAddedCart: '"{title}" added to cart',
            promoInvalid: 'Promo code not found. Try WIPE2026 or RUSTME.',
            promoValid: 'Promo code {code} applied: {pct}% off!',
            discountLabel: 'Discount ({code} −{pct}%):',
            errCheckoutNick: 'Nickname must be 3–16 characters (Latin letters, digits, or _).',
            toastOrderDone: 'Order #{id} placed for {nick}!',
            orderPrefix: 'Order #',
            orderPlayer: 'Player:',
            orderScope: 'Scope:',
            orderScopeAll: 'All SpaceRust servers',
            orderItems: 'Items:',
            orderStatusDemo: 'Placed (demo mode)',
            catLabelPrivileges: 'Privilege',
            catLabelKits: 'Kit',
            catLabelCases: 'Case / Skin',
            catLabelServices: 'Service',

            docTermsTitle: 'Terms of Service',
            docTermsHtml: `
                <h4>1. General</h4>
                <p>These terms govern the use of the SpaceRust website and launcher. By registering, you agree to these terms.</p>
                <h4>2. Player Account</h4>
                <p>You are responsible for keeping your password secure and choosing a nickname that complies with project rules.</p>
                <h4>3. Launcher Access</h4>
                <p>The SpaceRust launcher is free and available for download at any time.</p>
            `,
            docPrivacyTitle: 'Privacy Policy',
            docPrivacyHtml: `
                <h4>1. Data Collected</h4>
                <p>When registering, the site stores your chosen nickname and email address for SpaceRust authentication.</p>
                <h4>2. Data Usage</h4>
                <p>Your data is used solely for player identification, profile display, and account security.</p>
                <h4>3. Security</h4>
                <p>We never share user data with third parties or expose email addresses publicly.</p>
            `,
            docRulesTitle: 'SpaceRust Project Rules',
            docRulesHtml: `
                <h4>1. Fair Play</h4>
                <p>Cheats, third-party exploits, macros, or software granting an unfair advantage are strictly prohibited.</p>
                <h4>2. Conduct & Chat</h4>
                <p>Harassment, spam, unauthorized advertising, and toxic behavior in game chat are forbidden.</p>
                <h4>3. Bugs & Exploits</h4>
                <p>Abusing game or launcher bugs is forbidden. Report any issues to administration.</p>
            `,
            docShopTermsTitle: 'Delivery Terms',
            docShopTermsHtml: `
                <h4>1. Network-Wide Delivery</h4>
                <p>All privileges, kits, and cases from the SpaceRust store work across every SpaceRust server via /claim or automatic activation on join.</p>
                <h4>2. Duration</h4>
                <p>Timed privileges remain active for the stated duration (30 days) upon activation. Permanent ranks persist across all future wipes.</p>
                <h4>3. Purchase Rules</h4>
                <p>Double-check your nickname before ordering. All digital items are bound to the specified account.</p>
            `
        }
    };

    function getLang() {
        try {
            const saved = localStorage.getItem(STORAGE_LANG);
            if (saved && SUPPORTED.includes(saved)) return saved;
        } catch {
            // ignore storage failures
        }
        return 'ru';
    }

    function setLang(lang) {
        if (!SUPPORTED.includes(lang)) return getLang();
        try {
            localStorage.setItem(STORAGE_LANG, lang);
        } catch {
            // ignore storage failures
        }
        document.documentElement.lang = lang;
        return lang;
    }

    function t(key, vars = null) {
        const lang = getLang();
        let text = (DICT[lang] && DICT[lang][key]) || DICT.ru[key] || key;
        if (vars) {
            Object.entries(vars).forEach(([k, v]) => {
                text = text.replaceAll(`{${k}}`, String(v));
            });
        }
        return text;
    }

    function applyStaticTranslations() {
        const lang = getLang();
        document.documentElement.lang = lang;

        document.querySelectorAll('[data-i18n]').forEach((el) => {
            el.textContent = t(el.dataset.i18n);
        });
        document.querySelectorAll('[data-i18n-html]').forEach((el) => {
            el.innerHTML = t(el.dataset.i18nHtml);
        });
        document.querySelectorAll('[data-i18n-ph]').forEach((el) => {
            el.placeholder = t(el.dataset.i18nPh);
        });
    }

    function bindSwitcher(container, onChange) {
        if (!container) return;
        const buttons = container.querySelectorAll('[data-lang]');
        const sync = () => {
            const cur = getLang();
            buttons.forEach((btn) => {
                const active = btn.dataset.lang === cur;
                btn.classList.toggle('active', active);
                btn.setAttribute('aria-pressed', String(active));
            });
        };

        buttons.forEach((btn) => {
            btn.addEventListener('click', () => {
                setLang(btn.dataset.lang);
                sync();
                applyStaticTranslations();
                if (onChange) onChange(getLang());
            });
        });

        sync();
    }

    return {
        getLang,
        setLang,
        t,
        applyStaticTranslations,
        bindSwitcher
    };
})();
