/**
 * SpaceRust Donate Shop — Privileges, Kits, Cases, Services & i18n (All Servers)
 */
document.addEventListener('DOMContentLoaded', () => {
    const $ = (id) => document.getElementById(id);

    const STORAGE_CURRENT = 'sr_site_current_user';
    const STORAGE_CART = 'sr_shop_cart';
    const STORAGE_ORDERS = 'sr_shop_orders';
    const i18n = window.SpaceRustI18N;

    const PROMO_CODES = {
        WIPE2026: 15,
        RUSTME: 10
    };

    const CATEGORY_KEYS = {
        privileges: 'catLabelPrivileges',
        kits: 'catLabelKits',
        cases: 'catLabelCases',
        services: 'catLabelServices'
    };

    const ICONS = {
        crown: '<svg viewBox="0 0 24 24"><path d="M5 16L3 5l5.5 5L12 4l3.5 6L21 5l-2 11H5zm14 3c0 .6-.4 1-1 1H6c-.6 0-1-.4-1-1v-1h14v1z"/></svg>',
        kit: '<svg viewBox="0 0 24 24"><path d="M20 6h-4V4c0-1.11-.89-2-2-2h-4c-1.11 0-2 .89-2 2v2H4c-1.11 0-2 .89-2 2v11c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2zm-10-2h4v2h-4V4zm10 15H4V8h16v11z"/></svg>',
        gun: '<svg viewBox="0 0 24 24"><path d="M21 7h-9l-2-2H3v4h2v2H3v4h4l2 4h3l-1.5-4H15l1 2h3v-4h2V7z"/></svg>',
        case: '<svg viewBox="0 0 24 24"><path d="M21 16.5c0 .38-.21.71-.53.88l-7.9 4.44c-.16.12-.36.18-.57.18s-.41-.06-.57-.18l-7.9-4.44A.991.991 0 0 1 3 16.5v-9c0-.38.21-.71.53-.88l7.9-4.44c.16-.12.36-.18.57-.18s.41.06.57.18l7.9 4.44c.32.17.53.5.53.88v9zM12 4.15L6.04 7.5 12 10.85l5.96-3.35L12 4.15zM5 15.91l6 3.38v-6.71L5 9.21v6.7zm14 0v-6.7l-6 3.37v6.71l6-3.38z"/></svg>',
        shield: '<svg viewBox="0 0 24 24"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z"/></svg>'
    };

    const PRODUCTS = [
        // PRIVILEGES
        {
            id: 'priv-vip',
            category: 'privileges',
            price: 199,
            tone: 'tone-green',
            icon: 'crown',
            bgImage: 'assets/shop/priv-vip.jpg',
            badgeTone: '',
            i18n: {
                ru: {
                    title: 'VIP',
                    duration: '30 дней · Все серверы',
                    badge: '',
                    shortDesc: 'Базовая привилегия для комфортного старта после вайпа.',
                    perks: [
                        'Доступ к /kit vip раз в 24 часа на любом сервере',
                        '3 точки дома (/sethome) вместо 1',
                        'Приоритетный вход при заполненном сервере',
                        'Префикс [VIP] в игровом чате',
                        'Ускоренная переплавка руды на 15%'
                    ]
                },
                uk: {
                    title: 'VIP',
                    duration: '30 днів · Усі сервери',
                    badge: '',
                    shortDesc: 'Базовий привілей для комфортного старту після вайпу.',
                    perks: [
                        'Доступ до /kit vip раз на 24 години на будь-якому сервері',
                        '3 точки дому (/sethome) замість 1',
                        'Пріоритетний вхід на заповнений сервер',
                        'Префікс [VIP] в ігровому чаті',
                        'Прискорена переплавка руди на 15%'
                    ]
                },
                en: {
                    title: 'VIP',
                    duration: '30 days · All servers',
                    badge: '',
                    shortDesc: 'Essential rank for a smooth start right after wipe.',
                    perks: [
                        'Access to /kit vip every 24h on any server',
                        '3 /sethome points instead of 1',
                        'Queue priority on full servers',
                        '[VIP] chat prefix across all servers',
                        '15% faster ore smelting'
                    ]
                }
            }
        },
        {
            id: 'priv-elite',
            category: 'privileges',
            price: 449,
            tone: 'tone-blue',
            icon: 'crown',
            bgImage: 'assets/shop/priv-elite.svg',
            badgeTone: 'amber',
            i18n: {
                ru: {
                    title: 'ELITE',
                    duration: '30 дней · Все серверы',
                    badge: 'ПОПУЛЯРНОЕ',
                    shortDesc: 'Расширенные возможности, скины оружия и быстрая телепортация.',
                    perks: [
                        'Все бонусы VIP + ежедневный /kit elite',
                        '5 точек /sethome и телепорт за 3 секунды',
                        'Бесплатный доступ к /skinbox для оружия и брони',
                        'Сохранение 1 выбранного слота при смерти (вне рейда)',
                        'Префикс [ELITE] и цветное имя в списке игроков'
                    ]
                },
                uk: {
                    title: 'ELITE',
                    duration: '30 днів · Усі сервери',
                    badge: 'ПОПУЛЯРНЕ',
                    shortDesc: 'Розширені можливості, скіни зброї та швидка телепортація.',
                    perks: [
                        'Усі бонуси VIP + щоденний /kit elite',
                        '5 точок /sethome та телепорт за 3 секунди',
                        'Безкоштовний доступ до /skinbox для зброї та броні',
                        'Збереження 1 обраного слота при смерті (поза рейдом)',
                        'Префікс [ELITE] і кольорове імʼя у списку гравців'
                    ]
                },
                en: {
                    title: 'ELITE',
                    duration: '30 days · All servers',
                    badge: 'POPULAR',
                    shortDesc: 'Extended perks, weapon skins, and fast teleportation.',
                    perks: [
                        'All VIP perks + daily /kit elite',
                        '5 /sethome points & 3-second teleport',
                        'Free access to /skinbox for weapons & armor',
                        'Keep 1 selected slot on death (outside raids)',
                        '[ELITE] prefix & colored player name'
                    ]
                }
            }
        },
        {
            id: 'priv-titan',
            category: 'privileges',
            price: 890,
            tone: 'tone-coral',
            icon: 'crown',
            bgImage: 'assets/shop/priv-titan.svg',
            badgeTone: '',
            featured: true,
            i18n: {
                ru: {
                    title: 'TITAN',
                    duration: 'Навсегда · Все серверы',
                    badge: 'ХИТ ПРОДАЖ',
                    shortDesc: 'Постоянный статус с удалённым верстаком, эндер-сундуком и ×2 плавкой.',
                    perks: [
                        'Все бонусы ELITE + мощный /kit titan',
                        '8 точек /sethome и мгновенная плавка ×2 в печках',
                        'Доступ к командам /workbench и /ec из любой точки',
                        'Приоритет очереди на старте вайпа',
                        'Уникальный коралловый префикс [TITAN]'
                    ]
                },
                uk: {
                    title: 'TITAN',
                    duration: 'Назавжди · Усі сервери',
                    badge: 'ХІТ ПРОДАЖІВ',
                    shortDesc: 'Постійний статус із віддаленим верстаком, ендер-скринею та ×2 плавкою.',
                    perks: [
                        'Усі бонуси ELITE + потужний /kit titan',
                        '8 точок /sethome та миттєва плавка ×2 у печах',
                        'Доступ до команд /workbench та /ec із будь-якої точки',
                        'Пріоритет черги на старті вайпу',
                        'Унікальний кораловий префікс [TITAN]'
                    ]
                },
                en: {
                    title: 'TITAN',
                    duration: 'Lifetime · All servers',
                    badge: 'BESTSELLER',
                    shortDesc: 'Permanent rank with remote workbench, ender chest, and 2x smelting.',
                    perks: [
                        'All ELITE perks + powerful /kit titan',
                        '8 /sethome points & instant 2x furnace smelting',
                        'Use /workbench and /ec anywhere',
                        'High queue priority on wipe day',
                        'Unique coral [TITAN] prefix'
                    ]
                }
            }
        },
        {
            id: 'priv-suprem',
            category: 'privileges',
            price: 1490,
            tone: 'tone-purple',
            icon: 'crown',
            bgImage: 'assets/shop/priv-suprem.svg',
            badgeTone: 'amber',
            featured: true,
            i18n: {
                ru: {
                    title: 'SUPREM',
                    duration: 'Навсегда · Все серверы',
                    badge: 'MAX СТАТУС',
                    shortDesc: 'Максимальная привилегия SUPREM со всеми командами, китом и космическим скин-паком.',
                    perks: [
                        'Все бонусы TITAN + эксклюзивный /kit suprem',
                        '12 точек /sethome и расширенный лимит клана',
                        'Ускорение крафта всех предметов на 35%',
                        'Бесплатный космический скин-пак для оружия и брони',
                        'Фирменный префикс [SUPREM] и выделение в TAB'
                    ]
                },
                uk: {
                    title: 'SUPREM',
                    duration: 'Назавжди · Усі сервери',
                    badge: 'MAX СТАТУС',
                    shortDesc: 'Максимальний привілей SUPREM з усіма командами, кітом і космічним скін-паком.',
                    perks: [
                        'Усі бонуси TITAN + ексклюзивний /kit suprem',
                        '12 точок /sethome та розширений ліміт клану',
                        'Прискорення крафту всіх предметів на 35%',
                        'Безкоштовний космічний скін-пак для зброї та броні',
                        'Фірмовий префікс [SUPREM] і виділення в TAB'
                    ]
                },
                en: {
                    title: 'SUPREM',
                    duration: 'Lifetime · All servers',
                    badge: 'MAX RANK',
                    shortDesc: 'Ultimate SUPREM rank with full command access, kit, and cosmic skin pack.',
                    perks: [
                        'All TITAN perks + exclusive /kit suprem',
                        '12 /sethome points & expanded clan limit',
                        '35% faster crafting for all items',
                        'Free Cosmic skin pack for weapons & armor',
                        'Signature [SUPREM] prefix & TAB highlight'
                    ]
                }
            }
        },

        // KITS
        {
            id: 'kit-start',
            category: 'kits',
            price: 79,
            tone: 'tone-slate',
            icon: 'kit',
            bgImage: 'assets/shop/kit-start.svg',
            badgeTone: '',
            i18n: {
                ru: {
                    title: 'КИТ «СТАРТ ВЫЖИВШЕГО»',
                    duration: 'Разовая выдача · Все серверы',
                    badge: '',
                    shortDesc: 'Быстрый набор инструментов и ресурсов для первой базы.',
                    perks: [
                        'Каменный топор и каменная кирка',
                        '2 000 дерева и 1 500 камня',
                        'Кожаная броня, охотничий лук и 32 стрелы',
                        'Спальный мешок, кодовый замок и 150 Scrap'
                    ]
                },
                uk: {
                    title: 'КІТ «СТАРТ ВИЖИВАННЯ»',
                    duration: 'Разова видача · Усі сервери',
                    badge: '',
                    shortDesc: 'Швидкий набір інструментів і ресурсів для першої бази.',
                    perks: [
                        'Камʼяна сокира та камʼяне кайло',
                        '2 000 дерева та 1 500 каменю',
                        'Шкіряна броня, мисливський лук і 32 стріли',
                        'Спальний мішок, кодовий замок і 150 Scrap'
                    ]
                },
                en: {
                    title: 'SURVIVOR STARTER KIT',
                    duration: 'One-time claim · All servers',
                    badge: '',
                    shortDesc: 'Quick tool and resource bundle to build your first base.',
                    perks: [
                        'Stone hatchet & stone pickaxe',
                        '2,000 wood & 1,500 stone',
                        'Leather armor, hunting bow & 32 arrows',
                        'Sleeping bag, code lock & 150 Scrap'
                    ]
                }
            }
        },
        {
            id: 'kit-farmer',
            category: 'kits',
            price: 149,
            tone: 'tone-green',
            icon: 'kit',
            bgImage: 'assets/shop/kit-farmer.svg',
            badgeTone: '',
            i18n: {
                ru: {
                    title: 'КИТ «ФЕРМЕР И РЕСУРСЫ»',
                    duration: 'Разовая выдача · Все серверы',
                    badge: '',
                    shortDesc: 'Крупный запас руды, дерева, МВК и инструментов добычи.',
                    perks: [
                        '10 000 дерева, 8 000 камня, 3 500 железной руды',
                        '500 МВК (высококачественного металла) и 400 Scrap',
                        'Отбойный молоток и бензопила для мгновенного фарма',
                        '3 большие печи для быстрой переплавки'
                    ]
                },
                uk: {
                    title: 'КІТ «ФЕРМЕР І РЕСУРСИ»',
                    duration: 'Разова видача · Усі сервери',
                    badge: '',
                    shortDesc: 'Великий запас руди, дерева, МВК та інструментів для видобутку.',
                    perks: [
                        '10 000 дерева, 8 000 каменю, 3 500 залізної руди',
                        '500 МВК (високоякісного металу) та 400 Scrap',
                        'Відбійний молоток і бензопила для миттєвого фарму',
                        '3 великі печі для швидкої переплавки'
                    ]
                },
                en: {
                    title: 'FARMER & RESOURCES KIT',
                    duration: 'One-time claim · All servers',
                    badge: '',
                    shortDesc: 'Massive stockpile of wood, stone, HQM, and harvesting tools.',
                    perks: [
                        '10,000 wood, 8,000 stone & 3,500 metal ore',
                        '500 High Quality Metal & 400 Scrap',
                        'Jackhammer & chainsaw for instant farming',
                        '3 large furnaces for fast smelting'
                    ]
                }
            }
        },
        {
            id: 'kit-gunsmith',
            category: 'kits',
            price: 229,
            tone: 'tone-amber',
            icon: 'gun',
            bgImage: 'assets/shop/kit-gunsmith.svg',
            badgeTone: 'amber',
            i18n: {
                ru: {
                    title: 'КИТ «ОРУЖЕЙНИК»',
                    duration: 'Разовая выдача · Все серверы',
                    badge: 'ВЫГОДНО',
                    shortDesc: 'Готовый боевой комплект: АК-47, Томпсон, броня и медикаменты.',
                    perks: [
                        'Штурмовая винтовка АК-47 + 120 патронов 5.56',
                        'ПП Томпсон + 120 патронов 9мм',
                        'Металлическая нагрудная броня и шлем',
                        '4 большие аптечки и 6 шприцов восстановления'
                    ]
                },
                uk: {
                    title: 'КІТ «ЗБРОЯР»',
                    duration: 'Разова видача · Усі сервери',
                    badge: 'ВИГІДНО',
                    shortDesc: 'Готовий бойовий комплект: АК-47, Томпсон, броня та медикаменти.',
                    perks: [
                        'Штурмова гвинтівка АК-47 + 120 набоїв 5.56',
                        'ПП Томпсон + 120 набоїв 9мм',
                        'Металевий нагрудник і шолом',
                        '4 великі аптечки та 6 шприців відновлення'
                    ]
                },
                en: {
                    title: 'GUNSMITH KIT',
                    duration: 'One-time claim · All servers',
                    badge: 'GREAT VALUE',
                    shortDesc: 'Combat-ready loadout: AK-47, Thompson, metal armor, and meds.',
                    perks: [
                        'AK-47 assault rifle + 120 5.56 ammo',
                        'Thompson SMG + 120 9mm ammo',
                        'Metal chest plate & facemask',
                        '4 large medkits & 6 medical syringes'
                    ]
                }
            }
        },
        {
            id: 'kit-raider',
            category: 'kits',
            price: 349,
            tone: 'tone-coral',
            icon: 'gun',
            bgImage: 'assets/shop/kit-raider.svg',
            badgeTone: '',
            featured: true,
            i18n: {
                ru: {
                    title: 'КИТ «РЕЙДЕР»',
                    duration: 'Разовая выдача · Все серверы',
                    badge: 'РЕЙД',
                    shortDesc: 'Взрывчатка C4, ракеты и тяжёлая броня для штурма вражеских баз.',
                    perks: [
                        '4 заряда C4 + ракетница и 6 боевых ракет',
                        '12 сачелей и 300 разрывных патронов 5.56',
                        'Сет тяжёлой брони + револьвер для прикрытия',
                        '2 спальника и 2 строительные киянки'
                    ]
                },
                uk: {
                    title: 'КІТ «РЕЙДЕР»',
                    duration: 'Разова видача · Усі сервери',
                    badge: 'РЕЙД',
                    shortDesc: 'Вибухівка C4, ракети та важка броня для штурму ворожих баз.',
                    perks: [
                        '4 заряди C4 + ракетниця і 6 бойових ракет',
                        '12 сачелів і 300 розривних набоїв 5.56',
                        'Сет важкої броні + револьвер для прикриття',
                        '2 спальники та 2 будівельні киянки'
                    ]
                },
                en: {
                    title: 'RAIDER KIT',
                    duration: 'One-time claim · All servers',
                    badge: 'RAID',
                    shortDesc: 'C4 explosives, rockets, and heavy armor to breach enemy bases.',
                    perks: [
                        '4 C4 charges + rocket launcher & 6 rockets',
                        '12 satchel charges & 300 explosive 5.56 ammo',
                        'Heavy plate armor set + backup revolver',
                        '2 sleeping bags & 2 building hammers'
                    ]
                }
            }
        },
        {
            id: 'kit-builder',
            category: 'kits',
            price: 189,
            tone: 'tone-blue',
            icon: 'kit',
            bgImage: 'assets/shop/kit-builder.svg',
            badgeTone: '',
            i18n: {
                ru: {
                    title: 'КИТ «СТРОИТЕЛЬ БАЗЫ»',
                    duration: 'Разовая выдача · Все серверы',
                    badge: '',
                    shortDesc: 'Всё для возведения укреплённого бункера и турельной обороны.',
                    perks: [
                        '3 шкафа с инструментами, план постройки и киянка',
                        '40 000 камня и 15 000 фрагментов металла',
                        '6 бронированных дверей и 8 кодовых замков',
                        '3 автотурели и 300 патронов 5.56 для них'
                    ]
                },
                uk: {
                    title: 'КІТ «БУДІВЕЛЬНИК БАЗИ»',
                    duration: 'Разова видача · Усі сервери',
                    badge: '',
                    shortDesc: 'Усе для зведення укріпленого бункера та турельної оборони.',
                    perks: [
                        '3 шафи з інструментами, план будівлі та киянка',
                        '40 000 каменю та 15 000 фрагментів металу',
                        '6 броньованих дверей і 8 кодових замків',
                        '3 автотурелі та 300 набоїв 5.56 для них'
                    ]
                },
                en: {
                    title: 'BASE BUILDER KIT',
                    duration: 'One-time claim · All servers',
                    badge: '',
                    shortDesc: 'Everything needed to fortify a bunker and deploy auto-turrets.',
                    perks: [
                        '3 tool cupboards, building plan & hammer',
                        '40,000 stone & 15,000 metal fragments',
                        '6 armored doors & 8 code locks',
                        '3 auto-turrets & 300 5.56 ammo'
                    ]
                }
            }
        },

        // CASES & SKINS
        {
            id: 'case-weapon',
            category: 'cases',
            price: 99,
            tone: 'tone-amber',
            icon: 'case',
            badgeTone: 'amber',
            i18n: {
                ru: {
                    title: 'ОРУЖЕЙНЫЙ КЕЙС «RUSTME»',
                    duration: '1 открытие · Все серверы',
                    badge: 'КЕЙС',
                    shortDesc: 'Гарантированное оружие или редкий скин из оружейной линейки.',
                    perks: [
                        'Револьвер, ПП Томпсон, Полуавтомат, АК-47 или Болт',
                        'Шанс выпадения редкого скина «Space Edition»',
                        'Моментальная выдача через /claim на любом сервере'
                    ]
                },
                uk: {
                    title: 'ЗБРОЙОВИЙ КЕЙС «RUSTME»',
                    duration: '1 відкриття · Усі сервери',
                    badge: 'КЕЙС',
                    shortDesc: 'Гарантована зброя або рідкісний скін зі збройової лінійки.',
                    perks: [
                        'Револьвер, ПП Томпсон, Напівавтомат, АК-47 або Болт',
                        'Шанс випадіння рідкісного скіну «Space Edition»',
                        'Миттєва видача через /claim на будь-якому сервері'
                    ]
                },
                en: {
                    title: 'RUSTME WEAPON CASE',
                    duration: '1 roll · All servers',
                    badge: 'CASE',
                    shortDesc: 'Guaranteed firearm or rare weapon skin from the RustMe collection.',
                    perks: [
                        'Revolver, Thompson, SAR, AK-47, or Bolt Action',
                        'Chance to drop rare «Space Edition» skin',
                        'Instant claim via /claim on any server'
                    ]
                }
            }
        },
        {
            id: 'case-scrap',
            category: 'cases',
            price: 129,
            tone: 'tone-green',
            icon: 'case',
            badgeTone: '',
            i18n: {
                ru: {
                    title: 'КЕЙС РЕСУРСОВ И SCRAP',
                    duration: '1 открытие · Все серверы',
                    badge: '',
                    shortDesc: 'От 500 до 3 000 Scrap, сера, порох и компоненты 3-го тира.',
                    perks: [
                        'Гарантированно от 500 до 3 000 Scrap',
                        'Набор шестерёнок, пружин, труб и микросхем',
                        'Бонусная сера и порох для крафта боеприпасов'
                    ]
                },
                uk: {
                    title: 'КЕЙС РЕСУРСІВ І SCRAP',
                    duration: '1 відкриття · Усі сервери',
                    badge: '',
                    shortDesc: 'Від 500 до 3 000 Scrap, сірка, порох і компоненти 3-го тиру.',
                    perks: [
                        'Гарантовано від 500 до 3 000 Scrap',
                        'Набір шестерень, пружин, труб і мікросхем',
                        'Бонусна сірка та порох для крафту боєприпасів'
                    ]
                },
                en: {
                    title: 'SCRAP & RESOURCES CASE',
                    duration: '1 roll · All servers',
                    badge: '',
                    shortDesc: '500 to 3,000 Scrap, sulfur, gunpowder, and Tier 3 components.',
                    perks: [
                        'Guaranteed 500 to 3,000 Scrap',
                        'Gears, springs, pipes & tech trash bundle',
                        'Bonus sulfur & gunpowder for ammo crafting'
                    ]
                }
            }
        },
        {
            id: 'skin-space',
            category: 'cases',
            price: 299,
            tone: 'tone-purple',
            icon: 'case',
            badgeTone: '',
            i18n: {
                ru: {
                    title: 'СКИН-ПАК «КОСМОС»',
                    duration: 'Навсегда · Все серверы',
                    badge: 'ЭКСКЛЮЗИВ',
                    shortDesc: 'Фирменный набор скинов SpaceRust для оружия и металлической брони.',
                    perks: [
                        'Скины для АК-47, Томпсона, Болта и Револьвера',
                        'Космическая раскраска металлического сета брони',
                        'Доступ через /skinbox на всех серверах SpaceRust'
                    ]
                },
                uk: {
                    title: 'СКІН-ПАК «КОСМОС»',
                    duration: 'Назавжди · Усі сервери',
                    badge: 'ЕКСКЛЮЗИВ',
                    shortDesc: 'Фірмовий набір скінів SpaceRust для зброї та металевої броні.',
                    perks: [
                        'Скіни для АК-47, Томпсона, Болта та Револьвера',
                        'Космічне забарвлення металевого сету броні',
                        'Доступ через /skinbox на всіх серверах SpaceRust'
                    ]
                },
                en: {
                    title: 'COSMOS SKIN PACK',
                    duration: 'Lifetime · All servers',
                    badge: 'EXCLUSIVE',
                    shortDesc: 'Signature SpaceRust skin set for weapons and metal armor.',
                    perks: [
                        'Skins for AK-47, Thompson, Bolt Action & Revolver',
                        'Cosmic paintjob for full metal armor set',
                        'Available via /skinbox on all SpaceRust servers'
                    ]
                }
            }
        },

        // SERVICES
        {
            id: 'srv-unmute',
            category: 'services',
            price: 99,
            tone: 'tone-slate',
            icon: 'shield',
            badgeTone: '',
            i18n: {
                ru: {
                    title: 'СНЯТИЕ МУТА',
                    duration: 'Разовая услуга · Все серверы',
                    badge: '',
                    shortDesc: 'Досрочное снятие блокировки текстового и голосового чата.',
                    perks: [
                        'Мгновенно снимает активный мут на серверах SpaceRust',
                        'Не распространяется на перманентные блокировки администрации'
                    ]
                },
                uk: {
                    title: 'ЗНЯТТЯ МУТУ',
                    duration: 'Разова послуга · Усі сервери',
                    badge: '',
                    shortDesc: 'Дострокове зняття блокування текстового та голосового чату.',
                    perks: [
                        'Миттєво знімає активний мут на серверах SpaceRust',
                        'Не поширюється на перманентні блокування адміністрації'
                    ]
                },
                en: {
                    title: 'UNMUTE',
                    duration: 'One-time service · All servers',
                    badge: '',
                    shortDesc: 'Early removal of text and voice chat restrictions.',
                    perks: [
                        'Instantly lifts active chat mute across SpaceRust servers',
                        'Does not apply to permanent admin mutes'
                    ]
                }
            }
        },
        {
            id: 'srv-unban',
            category: 'services',
            price: 499,
            tone: 'tone-coral',
            icon: 'shield',
            badgeTone: '',
            i18n: {
                ru: {
                    title: 'РАЗБАН НА СЕРВЕРАХ',
                    duration: 'Разовая услуга · Все серверы',
                    badge: '',
                    shortDesc: 'Разблокировка аккаунта на игровых серверах SpaceRust.',
                    perks: [
                        'Снимает игровую блокировку на серверах SpaceRust',
                        'Применяется автоматически в течение 1–2 минут',
                        'Не действует при блокировках за читы и дюпы'
                    ]
                },
                uk: {
                    title: 'РОЗБАН НА СЕРВЕРАХ',
                    duration: 'Разова послуга · Усі сервери',
                    badge: '',
                    shortDesc: 'Розблокування акаунту на ігрових серверах SpaceRust.',
                    perks: [
                        'Знімає ігрове блокування на серверах SpaceRust',
                        'Застосовується автоматично протягом 1–2 хвилин',
                        'Не діє при блокуваннях за чіти та дюпи'
                    ]
                },
                en: {
                    title: 'SERVER UNBAN',
                    duration: 'One-time service · All servers',
                    badge: '',
                    shortDesc: 'Removes an active account ban across SpaceRust servers.',
                    perks: [
                        'Lifts active gameplay ban on SpaceRust servers',
                        'Applied automatically within 1–2 minutes',
                        'Not valid for cheating or duping bans'
                    ]
                }
            }
        },
        {
            id: 'srv-tag',
            category: 'services',
            price: 149,
            tone: 'tone-blue',
            icon: 'shield',
            badgeTone: '',
            i18n: {
                ru: {
                    title: 'КАСТОМНЫЙ ПРЕФИКС',
                    duration: '30 дней · Все серверы',
                    badge: '',
                    shortDesc: 'Уникальный цветной тег перед вашим никнеймом в чате и TAB.',
                    perks: [
                        'Настройка текста и цвета тега через команду /prefix',
                        'Отображается в игровом чате и списке игроков на всех серверах'
                    ]
                },
                uk: {
                    title: 'КАСТОМНИЙ ПРЕФІКС',
                    duration: '30 днів · Усі сервери',
                    badge: '',
                    shortDesc: 'Унікальний кольоровий тег перед вашим нікнеймом у чаті та TAB.',
                    perks: [
                        'Налаштування тексту та кольору тегу через команду /prefix',
                        'Відображається в ігровому чаті та списку гравців на всіх серверах'
                    ]
                },
                en: {
                    title: 'CUSTOM PREFIX',
                    duration: '30 days · All servers',
                    badge: '',
                    shortDesc: 'Custom colored tag before your nickname in chat and TAB.',
                    perks: [
                        'Customize tag text and colors with /prefix',
                        'Displayed in chat & player list on all servers'
                    ]
                }
            }
        }
    ];

    const shopGrid = $('shop-grid');
    const shopEmpty = $('shop-empty');
    const searchInput = $('shop-search');
    const catButtons = document.querySelectorAll('.shop-cat');

    const shopUserAvatar = $('shop-user-avatar');
    const shopUserName = $('shop-user-name');
    const cartCountEl = $('cart-count');
    const ordersCountEl = $('orders-count');

    const itemModal = $('item-modal');
    const cartModal = $('cart-modal');
    const ordersModal = $('orders-modal');
    const docModal = $('shop-doc-modal');

    const cartItemsEl = $('cart-items');
    const cartEmptyEl = $('cart-empty');
    const checkoutForm = $('checkout-form');
    const checkoutNick = $('checkout-nick');
    const checkoutError = $('checkout-error');
    const promoInput = $('promo-input');
    const promoStatus = $('promo-status');
    const cartSubtotalEl = $('cart-subtotal');
    const cartDiscountRow = $('cart-discount-row');
    const cartDiscountLabel = $('cart-discount-label');
    const cartDiscountVal = $('cart-discount-val');
    const cartTotalEl = $('cart-total');
    const toastContainer = $('toast-container');

    let activeCategory = 'all';
    let searchQuery = '';
    let activeProduct = null;
    let activePromo = null;
    let activeShopDoc = null;

    function getProductText(product) {
        const lang = i18n.getLang();
        return product.i18n[lang] || product.i18n.ru;
    }

    function readJson(key, fallback) {
        try {
            const raw = localStorage.getItem(key);
            return raw ? JSON.parse(raw) : fallback;
        } catch {
            return fallback;
        }
    }

    function getLocale() {
        const lang = i18n.getLang();
        if (lang === 'uk') return 'uk-UA';
        if (lang === 'en') return 'en-US';
        return 'ru-RU';
    }

    function formatRub(amount) {
        return `${Number(amount).toLocaleString(getLocale())} ₽`;
    }

    function showToast(message, type = 'ok') {
        const el = document.createElement('div');
        el.className = `toast ${type}`;
        el.textContent = message;
        toastContainer.appendChild(el);
        setTimeout(() => el.remove(), 3000);
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

    function getCart() {
        return readJson(STORAGE_CART, []);
    }

    function saveCart(cart) {
        localStorage.setItem(STORAGE_CART, JSON.stringify(cart));
        updateCounters();
    }

    function getOrders() {
        return readJson(STORAGE_ORDERS, []);
    }

    function saveOrders(orders) {
        localStorage.setItem(STORAGE_ORDERS, JSON.stringify(orders));
        updateCounters();
    }

    function syncUser() {
        const current = readJson(STORAGE_CURRENT, null);
        if (current && current.nickname) {
            shopUserName.textContent = current.nickname;
            shopUserAvatar.src = `https://mc-heads.net/avatar/${encodeURIComponent(current.nickname)}/64`;
            shopUserAvatar.onerror = () => { shopUserAvatar.src = 'assets/logo.png'; };
            if (!checkoutNick.value) {
                checkoutNick.value = current.nickname;
            }
        } else {
            shopUserName.textContent = i18n.t('guestName');
            shopUserAvatar.src = 'assets/logo.png';
        }
    }

    function updateCounters() {
        const cart = getCart();
        const totalQty = cart.reduce((sum, item) => sum + item.qty, 0);
        cartCountEl.textContent = String(totalQty);
        ordersCountEl.textContent = String(getOrders().length);
    }

    function addToCart(productId) {
        const product = PRODUCTS.find((p) => p.id === productId);
        if (!product) return;

        const cart = getCart();
        const existing = cart.find((item) => item.id === productId);
        if (existing) {
            existing.qty += 1;
        } else {
            cart.push({ id: productId, qty: 1 });
        }
        saveCart(cart);
        const text = getProductText(product);
        showToast(i18n.t('toastAddedCart', { title: text.title }));
    }

    function renderCatalog() {
        const query = searchQuery.trim().toLowerCase();
        const filtered = PRODUCTS.filter((p) => {
            const catMatch = activeCategory === 'all' || p.category === activeCategory;
            if (!catMatch) return false;
            if (!query) return true;
            const text = getProductText(p);
            const hay = `${text.title} ${text.shortDesc} ${text.perks.join(' ')}`.toLowerCase();
            return hay.includes(query);
        });

        shopEmpty.hidden = filtered.length > 0;

        shopGrid.innerHTML = filtered.map((p) => {
            const text = getProductText(p);
            const badgeHtml = text.badge
                ? `<span class="product-badge ${p.badgeTone}">${text.badge}</span>`
                : '';
            const artHtml = p.bgImage
                ? `<div class="product-art" style="background-image: url('${p.bgImage}')"></div><div class="product-art-scrim"></div>`
                : '';
            const perksHtml = text.perks.slice(0, 3).map((perk) => `<li>${perk}</li>`).join('');
            return `
                <article class="product-card ${p.featured ? 'featured' : ''} ${p.bgImage ? 'has-art' : ''}">
                    ${artHtml}
                    ${badgeHtml}
                    <div class="product-head">
                        <div class="product-icon ${p.tone}">${ICONS[p.icon] || ICONS.kit}</div>
                        <div class="product-meta">
                            <div class="product-cat">${i18n.t(CATEGORY_KEYS[p.category])}</div>
                            <h3 class="product-title">${text.title}</h3>
                            <div class="product-duration">${text.duration}</div>
                        </div>
                    </div>
                    <p class="product-desc">${text.shortDesc}</p>
                    <ul class="product-preview-perks">${perksHtml}</ul>
                    <div class="product-foot">
                        <strong class="product-price">${formatRub(p.price)}</strong>
                        <div class="product-actions">
                            <button type="button" class="btn-card-info" data-detail="${p.id}">${i18n.t('detailsBtn')}</button>
                            <button type="button" class="btn-card-buy" data-buy="${p.id}">${i18n.t('addToCartBtn')}</button>
                        </div>
                    </div>
                </article>
            `;
        }).join('');
    }

    function renderProductDetails() {
        if (!activeProduct) return;
        const p = activeProduct;
        const text = getProductText(p);

        const bannerEl = $('item-detail-banner');
        if (bannerEl) {
            if (p.bgImage) {
                bannerEl.hidden = false;
                bannerEl.style.backgroundImage = `url('${p.bgImage}')`;
            } else {
                bannerEl.hidden = true;
                bannerEl.style.backgroundImage = '';
            }
        }

        $('item-modal-title').textContent = i18n.t(CATEGORY_KEYS[p.category]).toUpperCase();
        $('item-detail-icon').className = `item-detail-icon ${p.tone}`;
        $('item-detail-icon').innerHTML = ICONS[p.icon] || ICONS.kit;
        $('item-detail-cat').textContent = i18n.t(CATEGORY_KEYS[p.category]);
        $('item-detail-dur').textContent = text.duration;
        $('item-detail-name').textContent = text.title;
        $('item-detail-desc').textContent = text.shortDesc;
        $('item-detail-list').innerHTML = text.perks.map((perk) => `<li>${perk}</li>`).join('');
        $('item-detail-price').textContent = formatRub(p.price);
    }

    function openProductDetails(productId) {
        const p = PRODUCTS.find((item) => item.id === productId);
        if (!p) return;
        activeProduct = p;
        renderProductDetails();
        openModal(itemModal);
    }

    function renderCart() {
        const cart = getCart();
        const isEmpty = cart.length === 0;
        cartEmptyEl.hidden = !isEmpty;
        checkoutForm.hidden = isEmpty;
        checkoutError.hidden = true;

        if (isEmpty) {
            cartItemsEl.innerHTML = '';
            return;
        }

        let subtotal = 0;
        cartItemsEl.innerHTML = cart.map((entry) => {
            const p = PRODUCTS.find((item) => item.id === entry.id);
            if (!p) return '';
            const text = getProductText(p);
            const lineTotal = p.price * entry.qty;
            subtotal += lineTotal;
            return `
                <div class="cart-row" data-cart-id="${p.id}">
                    <div class="cart-row-info">
                        <h4>${text.title}</h4>
                        <p>${i18n.t(CATEGORY_KEYS[p.category])} · ${text.duration} · ${formatRub(p.price)}</p>
                    </div>
                    <div class="cart-row-controls">
                        <button type="button" class="qty-btn" data-qty-dec="${p.id}" aria-label="Decrease">-</button>
                        <span class="cart-qty">${entry.qty}</span>
                        <button type="button" class="qty-btn" data-qty-inc="${p.id}" aria-label="Increase">+</button>
                        <span class="cart-row-price">${formatRub(lineTotal)}</span>
                        <button type="button" class="remove-btn" data-remove="${p.id}" aria-label="Remove">&times;</button>
                    </div>
                </div>
            `;
        }).join('');

        const discountPct = activePromo ? PROMO_CODES[activePromo] : 0;
        const discountVal = Math.round((subtotal * discountPct) / 100);
        const total = Math.max(0, subtotal - discountVal);

        cartSubtotalEl.textContent = formatRub(subtotal);
        if (discountPct > 0) {
            cartDiscountRow.hidden = false;
            cartDiscountLabel.textContent = i18n.t('discountLabel', { code: activePromo, pct: discountPct });
            cartDiscountVal.textContent = `−${formatRub(discountVal)}`;
        } else {
            cartDiscountRow.hidden = true;
        }
        cartTotalEl.textContent = formatRub(total);
    }

    function renderOrders() {
        const orders = getOrders();
        const listEl = $('orders-list');
        if (orders.length === 0) {
            listEl.innerHTML = `<div class="cart-empty"><p>${i18n.t('ordersEmptyText')}</p></div>`;
            return;
        }

        listEl.innerHTML = orders.map((order) => {
            const itemsText = order.items.map((item) => {
                const product = PRODUCTS.find((p) => p.id === item.id);
                const title = product ? getProductText(product).title : item.title;
                return `${title} ×${item.qty}`;
            }).join(', ');

            return `
                <div class="order-card">
                    <div class="order-top">
                        <span class="order-id">${i18n.t('orderPrefix')}${order.id}</span>
                        <span class="order-date">${order.date}</span>
                    </div>
                    <div class="order-meta"><b>${i18n.t('orderPlayer')}</b> ${order.nickname} · <b>${i18n.t('orderScope')}</b> ${i18n.t('orderScopeAll')}</div>
                    <div class="order-meta"><b>${i18n.t('orderItems')}</b> ${itemsText}</div>
                    <div class="order-bottom">
                        <span class="order-status">${i18n.t('orderStatusDemo')}</span>
                        <strong class="order-total">${formatRub(order.total)}</strong>
                    </div>
                </div>
            `;
        }).join('');
    }

    function renderShopDocModal() {
        if (!activeShopDoc) return;
        const map = {
            terms: ['docShopTermsTitle', 'docShopTermsHtml'],
            rules: ['docRulesTitle', 'docRulesHtml']
        };
        const keys = map[activeShopDoc];
        if (!keys) return;
        $('shop-doc-title').textContent = i18n.t(keys[0]);
        $('shop-doc-content').innerHTML = i18n.t(keys[1]);
    }

    // Category & search bindings
    catButtons.forEach((btn) => {
        btn.addEventListener('click', () => {
            activeCategory = btn.dataset.cat;
            catButtons.forEach((b) => {
                const isAct = b === btn;
                b.classList.toggle('active', isAct);
                b.setAttribute('aria-selected', String(isAct));
            });
            renderCatalog();
        });
    });

    searchInput.addEventListener('input', (e) => {
        searchQuery = e.target.value;
        renderCatalog();
    });

    $('shop-reset-filter').addEventListener('click', () => {
        activeCategory = 'all';
        searchQuery = '';
        searchInput.value = '';
        catButtons.forEach((b) => {
            const isAct = b.dataset.cat === 'all';
            b.classList.toggle('active', isAct);
            b.setAttribute('aria-selected', String(isAct));
        });
        renderCatalog();
    });

    // Grid clicks (delegation)
    shopGrid.addEventListener('click', (e) => {
        const detailBtn = e.target.closest('[data-detail]');
        if (detailBtn) {
            openProductDetails(detailBtn.dataset.detail);
            return;
        }
        const buyBtn = e.target.closest('[data-buy]');
        if (buyBtn) {
            addToCart(buyBtn.dataset.buy);
        }
    });

    $('item-detail-add').addEventListener('click', () => {
        if (!activeProduct) return;
        addToCart(activeProduct.id);
        closeModal(itemModal);
    });

    // Cart open & item adjustments
    $('btn-open-cart').addEventListener('click', () => {
        syncUser();
        renderCart();
        openModal(cartModal);
    });

    cartItemsEl.addEventListener('click', (e) => {
        const inc = e.target.closest('[data-qty-inc]');
        const dec = e.target.closest('[data-qty-dec]');
        const rem = e.target.closest('[data-remove]');
        const cart = getCart();

        if (inc) {
            const item = cart.find((x) => x.id === inc.dataset.qtyInc);
            if (item) item.qty += 1;
            saveCart(cart);
            renderCart();
        } else if (dec) {
            const idx = cart.findIndex((x) => x.id === dec.dataset.qtyDec);
            if (idx !== -1) {
                if (cart[idx].qty > 1) cart[idx].qty -= 1;
                else cart.splice(idx, 1);
            }
            saveCart(cart);
            renderCart();
        } else if (rem) {
            const next = cart.filter((x) => x.id !== rem.dataset.remove);
            saveCart(next);
            renderCart();
        }
    });

    // Promo code apply
    $('btn-apply-promo').addEventListener('click', () => {
        const code = promoInput.value.trim().toUpperCase();
        promoStatus.hidden = false;

        if (!code || !PROMO_CODES[code]) {
            activePromo = null;
            promoStatus.textContent = i18n.t('promoInvalid');
            promoStatus.classList.add('err');
            renderCart();
            return;
        }

        activePromo = code;
        promoStatus.textContent = i18n.t('promoValid', { code, pct: PROMO_CODES[code] });
        promoStatus.classList.remove('err');
        renderCart();
    });

    // Checkout submit
    checkoutForm.addEventListener('submit', (e) => {
        e.preventDefault();
        checkoutError.hidden = true;

        const nick = checkoutNick.value.trim();
        const cart = getCart();

        if (!/^[a-zA-Z0-9_]{3,16}$/.test(nick)) {
            checkoutError.textContent = i18n.t('errCheckoutNick');
            checkoutError.hidden = false;
            return;
        }
        if (cart.length === 0) return;

        let subtotal = 0;
        const orderItems = cart.map((entry) => {
            const p = PRODUCTS.find((item) => item.id === entry.id);
            if (!p) return null;
            subtotal += p.price * entry.qty;
            return { id: p.id, title: getProductText(p).title, qty: entry.qty, price: p.price };
        }).filter(Boolean);

        const discountPct = activePromo ? PROMO_CODES[activePromo] : 0;
        const total = Math.max(0, subtotal - Math.round((subtotal * discountPct) / 100));

        const order = {
            id: String(Math.floor(100000 + Math.random() * 900000)),
            date: new Date().toLocaleString(getLocale(), {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            }),
            nickname: nick,
            items: orderItems,
            promo: activePromo,
            total
        };

        const orders = getOrders();
        orders.unshift(order);
        saveOrders(orders);
        saveCart([]);
        activePromo = null;
        promoInput.value = '';
        promoStatus.hidden = true;

        closeModal(cartModal);
        showToast(i18n.t('toastOrderDone', { id: order.id, nick }));
    });

    // Orders history
    $('btn-orders-history').addEventListener('click', () => {
        renderOrders();
        openModal(ordersModal);
    });

    // Docs
    document.querySelectorAll('.footer-link[data-doc]').forEach((btn) => {
        btn.addEventListener('click', () => {
            activeShopDoc = btn.dataset.doc;
            renderShopDocModal();
            openModal(docModal);
        });
    });

    // Close modals
    $('item-modal-close').addEventListener('click', () => closeModal(itemModal));
    $('cart-close').addEventListener('click', () => closeModal(cartModal));
    $('orders-close').addEventListener('click', () => closeModal(ordersModal));
    $('orders-ok').addEventListener('click', () => closeModal(ordersModal));
    $('shop-doc-close').addEventListener('click', () => closeModal(docModal));
    $('shop-doc-ok').addEventListener('click', () => closeModal(docModal));

    document.querySelectorAll('.modal-backdrop').forEach((backdrop) => {
        backdrop.addEventListener('click', (e) => {
            if (e.target === backdrop) closeModal(backdrop);
        });
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') closeAllModals();
    });

    function refreshAll() {
        document.title = i18n.t('titleShop');
        syncUser();
        updateCounters();
        renderCatalog();
        renderProductDetails();
        renderCart();
        renderOrders();
        renderShopDocModal();
    }

    i18n.applyStaticTranslations();
    i18n.bindSwitcher($('lang-switcher'), refreshAll);
    refreshAll();
});
