/**
 * Pure-JS RFC 6238 TOTP helper (works over file://, http://, and https://).
 */
window.SpaceRustTOTP = (() => {
    const BASE32_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';

    function rotl(val, bits) {
        return ((val << bits) | (val >>> (32 - bits))) >>> 0;
    }

    function sha1(bytes) {
        const msgLen = bytes.length;
        const bitLenHi = Math.floor((msgLen * 8) / 0x100000000);
        const bitLenLo = (msgLen * 8) >>> 0;

        const paddedLen = Math.ceil((msgLen + 9) / 64) * 64;
        const padded = new Uint8Array(paddedLen);
        padded.set(bytes);
        padded[msgLen] = 0x80;

        const view = new DataView(padded.buffer);
        view.setUint32(paddedLen - 8, bitLenHi, false);
        view.setUint32(paddedLen - 4, bitLenLo, false);

        let h0 = 0x67452301;
        let h1 = 0xEFCDAB89;
        let h2 = 0x98BADCFE;
        let h3 = 0x10325476;
        let h4 = 0xC3D2E1F0;

        const w = new Uint32Array(80);
        for (let offset = 0; offset < paddedLen; offset += 64) {
            for (let i = 0; i < 16; i++) {
                w[i] = view.getUint32(offset + i * 4, false);
            }
            for (let i = 16; i < 80; i++) {
                w[i] = rotl(w[i - 3] ^ w[i - 8] ^ w[i - 14] ^ w[i - 16], 1);
            }

            let a = h0;
            let b = h1;
            let c = h2;
            let d = h3;
            let e = h4;

            for (let i = 0; i < 80; i++) {
                let f;
                let k;
                if (i < 20) {
                    f = (b & c) | ((~b) & d);
                    k = 0x5A827999;
                } else if (i < 40) {
                    f = b ^ c ^ d;
                    k = 0x6ED9EBA1;
                } else if (i < 60) {
                    f = (b & c) | (b & d) | (c & d);
                    k = 0x8F1BBCDC;
                } else {
                    f = b ^ c ^ d;
                    k = 0xCA62C1D6;
                }
                const temp = (rotl(a, 5) + f + e + k + w[i]) >>> 0;
                e = d;
                d = c;
                c = rotl(b, 30);
                b = a;
                a = temp;
            }

            h0 = (h0 + a) >>> 0;
            h1 = (h1 + b) >>> 0;
            h2 = (h2 + c) >>> 0;
            h3 = (h3 + d) >>> 0;
            h4 = (h4 + e) >>> 0;
        }

        const out = new Uint8Array(20);
        const outView = new DataView(out.buffer);
        outView.setUint32(0, h0, false);
        outView.setUint32(4, h1, false);
        outView.setUint32(8, h2, false);
        outView.setUint32(12, h3, false);
        outView.setUint32(16, h4, false);
        return out;
    }

    function hmacSha1(key, message) {
        const blockSize = 64;
        const normKey = key.length > blockSize ? sha1(key) : key;
        const oPad = new Uint8Array(blockSize + 20);
        const iPad = new Uint8Array(blockSize + message.length);

        for (let i = 0; i < blockSize; i++) {
            const k = i < normKey.length ? normKey[i] : 0;
            oPad[i] = k ^ 0x5c;
            iPad[i] = k ^ 0x36;
        }
        iPad.set(message, blockSize);
        const inner = sha1(iPad);
        oPad.set(inner, blockSize);
        return sha1(oPad);
    }

    function decodeBase32(str) {
        const clean = String(str).toUpperCase().replace(/[^A-Z2-7]/g, '');
        const bytes = [];
        let buffer = 0;
        let bitsLeft = 0;

        for (let i = 0; i < clean.length; i++) {
            const val = BASE32_ALPHABET.indexOf(clean[i]);
            if (val === -1) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bytes.push((buffer >>> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        return new Uint8Array(bytes);
    }

    function generateSecret(length = 16) {
        const buf = new Uint8Array(length);
        if (window.crypto && window.crypto.getRandomValues) {
            window.crypto.getRandomValues(buf);
        } else {
            for (let i = 0; i < length; i++) buf[i] = Math.floor(Math.random() * 256);
        }
        let secret = '';
        for (let i = 0; i < length; i++) {
            secret += BASE32_ALPHABET[buf[i] % 32];
        }
        return secret;
    }

    function generateCode(secret, stepOffset = 0, timestampMs = Date.now()) {
        const keyBytes = decodeBase32(secret);
        const counter = Math.floor(timestampMs / 1000 / 30) + stepOffset;
        const counterBytes = new Uint8Array(8);
        const view = new DataView(counterBytes.buffer);
        view.setUint32(0, Math.floor(counter / 0x100000000), false);
        view.setUint32(4, counter >>> 0, false);

        const hmac = hmacSha1(keyBytes, counterBytes);
        const offset = hmac[19] & 0x0f;
        const code =
            ((hmac[offset] & 0x7f) << 24) |
            ((hmac[offset + 1] & 0xff) << 16) |
            ((hmac[offset + 2] & 0xff) << 8) |
            (hmac[offset + 3] & 0xff);
        return String(code % 1000000).padStart(6, '0');
    }

    function verifyCode(secret, inputCode, windowSteps = 1) {
        const clean = String(inputCode || '').replace(/\s+/g, '');
        if (!/^\d{6}$/.test(clean)) return false;
        const now = Date.now();
        for (let offset = -windowSteps; offset <= windowSteps; offset++) {
            if (generateCode(secret, offset, now) === clean) {
                return true;
            }
        }
        return false;
    }

    function generateBackupCodes(count = 4) {
        const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
        const codes = [];
        for (let i = 0; i < count; i++) {
            let part1 = '';
            let part2 = '';
            for (let j = 0; j < 4; j++) {
                part1 += chars[Math.floor(Math.random() * chars.length)];
                part2 += chars[Math.floor(Math.random() * chars.length)];
            }
            codes.push(`${part1}-${part2}`);
        }
        return codes;
    }

    function buildOtpAuthUri(accountName, secret, issuer = 'SpaceRust') {
        const label = encodeURIComponent(`${issuer}:${accountName}`);
        return `otpauth://totp/${label}?secret=${encodeURIComponent(secret)}&issuer=${encodeURIComponent(issuer)}`;
    }

    return {
        generateSecret,
        generateCode,
        verifyCode,
        generateBackupCodes,
        buildOtpAuthUri
    };
})();
