/**
 * SpaceRust / RustMe Server Directory & Mock Database
 */

const SERVERS_DATA = [
    {
        id: "server-1",
        name: "SPACERUST #1 [VANILLA x1]",
        category: "vanilla",
        online: 194,
        maxOnline: 200,
        queue: 4,
        ping: 18,
        region: "RU (Москва)",
        map: "Procedural Map 3800",
        lastWipe: "1 день назад",
        nextWipe: "Пятница, 17:00 МСК",
        isFreshWipe: true,
        tags: ["VANILLA x1", "NO BP WIPE", "PROCEDURAL", "AIRDROP", "CLANS MAX 4"],
        description: "Классический режим RustMe без модификаторов добычи. Честное выживание, хардкорный лут, стабильный TPS и активная администрация.",
        ip: "vanilla1.spacerust.net:25565"
    },
    {
        id: "server-2",
        name: "SPACERUST #2 [MAX 3 | x2 LOOT]",
        category: "modded",
        online: 188,
        maxOnline: 200,
        queue: 0,
        ping: 22,
        region: "RU (Москва)",
        map: "Hapis Island Custom",
        lastWipe: "2 дня назад",
        nextWipe: "Вторник, 18:00 МСК",
        isFreshWipe: false,
        tags: ["x2 RES", "TRIO ONLY", "FAST SMELT", "CUSTOM MAP", "TELEPORT"],
        description: "Сбалансированный сервер для игры втроем. Ускоренная плавка, x2 ресурсы и компоненты, мини-коптеры на дорогах.",
        ip: "trio.spacerust.net:25565"
    },
    {
        id: "server-3",
        name: "SPACERUST #3 [SOLO ONLY x1.5]",
        category: "solo",
        online: 142,
        maxOnline: 150,
        queue: 1,
        ping: 25,
        region: "RU (СПб)",
        map: "Barren Island 3200",
        lastWipe: "Сегодня в 14:00",
        nextWipe: "Суббота, 16:00 МСК",
        isFreshWipe: true,
        tags: ["SOLO ONLY", "NO ALLIES", "x1.5 WOOD/STONE", "ANTI-TEAM"],
        description: "Хардкорный сервер строго для одиночек. Автоматическая блокировка тиминга, оптимизированная Barren карта для высокого FPS.",
        ip: "solo.spacerust.net:25565"
    },
    {
        id: "server-4",
        name: "SPACERUST #4 [DUO | x3 FAST RAID]",
        category: "modded",
        online: 165,
        maxOnline: 200,
        queue: 0,
        ping: 20,
        region: "RU (Москва)",
        map: "Procedural 3500",
        lastWipe: "3 дня назад",
        nextWipe: "Четверг, 17:00 МСК",
        isFreshWipe: false,
        tags: ["x3 ALL", "DUO MAX 2", "FAST RECYCLER", "KITS", "SKINBOX"],
        description: "Динамичный геймплей для дуэтов: увеличенный лут в ящиках, бесплатный SkinBox для оружия, быстрый ресайклер и готовые базы.",
        ip: "duo3.spacerust.net:25565"
    },
    {
        id: "server-5",
        name: "SPACERUST #5 [BATTLEGROUNDS / PVP]",
        category: "pvp",
        online: 82,
        maxOnline: 100,
        queue: 0,
        ping: 15,
        region: "RU (Москва)",
        map: "Rust Arenas 1v1 / FFA",
        lastWipe: "Не требуется",
        nextWipe: "Всегда онлайн",
        isFreshWipe: false,
        tags: ["PVP ARENA", "AK-47 / BOLT", "INSTANT RESPAWN", "AIM TRAIN", "1v1 DUEL"],
        description: "Тренировочная PVP арена для оттачивания спрея с калаша, стрельбы из блочного лука, пвп на револьверах и дуэлей 1 на 1.",
        ip: "pvp.spacerust.net:25565"
    },
    {
        id: "server-6",
        name: "SPACERUST #6 [BUILD LAB & RAID SIM]",
        category: "pvp",
        online: 44,
        maxOnline: 80,
        queue: 0,
        ping: 28,
        region: "RU (Москва)",
        map: "Flat Build Sandbox",
        lastWipe: "Не требуется",
        nextWipe: "Каждое воскресенье",
        isFreshWipe: false,
        tags: ["SANDBOX", "FREE BUILD", "RAID SIM", "NO CLIP", "INFINITE RES"],
        description: "Песочница для проектирования баз, антирейд построек, проверки прочности стен и тренировки взрыва ракетницами и C4.",
        ip: "build.spacerust.net:25565"
    }
];

// Helper to simulate subtle live online fluctuations
function simulateOnlineJitter(servers) {
    servers.forEach(s => {
        if (s.category !== 'pvp') {
            const delta = Math.floor(Math.random() * 5) - 2; // -2 to +2
            s.online = Math.max(10, Math.min(s.maxOnline, s.online + delta));
            // Slight ping jitter
            s.ping = Math.max(12, Math.min(45, s.ping + Math.floor(Math.random() * 3) - 1));
        }
    });
}
