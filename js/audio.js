/**
 * SpaceRust / RustMe Launcher - Procedural Web Audio Engine
 * Generates crisp tactical UI sound effects with zero external audio file dependencies.
 */

class SoundEngine {
    constructor() {
        this.ctx = null;
        this.enabled = true;
        this.volume = 0.6;
        this.initialized = false;
    }

    init() {
        if (this.initialized) return;
        try {
            const AudioContext = window.AudioContext || window.webkitAudioContext;
            if (AudioContext) {
                this.ctx = new AudioContext();
                this.initialized = true;
            }
        } catch (e) {
            console.warn("Web Audio not supported or blocked", e);
        }
    }

    resume() {
        if (!this.initialized) this.init();
        if (this.ctx && this.ctx.state === 'suspended') {
            this.ctx.resume();
        }
    }

    setVolume(val) {
        this.volume = Math.max(0, Math.min(1, val));
    }

    setEnabled(val) {
        this.enabled = Boolean(val);
    }

    // Subtle crisp tick on UI button hover
    playHover() {
        if (!this.enabled) return;
        this.resume();
        if (!this.ctx) return;

        try {
            const now = this.ctx.currentTime;
            const osc = this.ctx.createOscillator();
            const gain = this.ctx.createGain();
            const filter = this.ctx.createBiquadFilter();

            osc.type = 'triangle';
            osc.frequency.setValueAtTime(2400, now);
            osc.frequency.exponentialRampToValueAtTime(1200, now + 0.025);

            filter.type = 'bandpass';
            filter.frequency.setValueAtTime(2000, now);
            filter.Q.setValueAtTime(3, now);

            gain.gain.setValueAtTime(0.04 * this.volume, now);
            gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.025);

            osc.connect(filter);
            filter.connect(gain);
            gain.connect(this.ctx.destination);

            osc.start(now);
            osc.stop(now + 0.03);
        } catch (_) {}
    }

    // Solid mechanical click on button press
    playClick() {
        if (!this.enabled) return;
        this.resume();
        if (!this.ctx) return;

        try {
            const now = this.ctx.currentTime;
            
            // Low transient thump
            const osc = this.ctx.createOscillator();
            const gain = this.ctx.createGain();
            osc.type = 'sine';
            osc.frequency.setValueAtTime(220, now);
            osc.frequency.exponentialRampToValueAtTime(40, now + 0.05);

            gain.gain.setValueAtTime(0.25 * this.volume, now);
            gain.gain.exponentialRampToValueAtTime(0.001, now + 0.06);

            osc.connect(gain);
            gain.connect(this.ctx.destination);
            osc.start(now);
            osc.stop(now + 0.06);

            // High metallic snappy snap
            const snap = this.ctx.createOscillator();
            const snapGain = this.ctx.createGain();
            snap.type = 'square';
            snap.frequency.setValueAtTime(1400, now);
            snap.frequency.exponentialRampToValueAtTime(300, now + 0.03);

            snapGain.gain.setValueAtTime(0.08 * this.volume, now);
            snapGain.gain.exponentialRampToValueAtTime(0.0001, now + 0.03);

            snap.connect(snapGain);
            snapGain.connect(this.ctx.destination);
            snap.start(now);
            snap.stop(now + 0.035);
        } catch (_) {}
    }

    // Heavy industrial power click for "ИГРАТЬ"
    playLaunch() {
        if (!this.enabled) return;
        this.resume();
        if (!this.ctx) return;

        try {
            const now = this.ctx.currentTime;

            // Sub bass boom
            const sub = this.ctx.createOscillator();
            const subGain = this.ctx.createGain();
            sub.type = 'sine';
            sub.frequency.setValueAtTime(160, now);
            sub.frequency.exponentialRampToValueAtTime(30, now + 0.35);

            subGain.gain.setValueAtTime(0.4 * this.volume, now);
            subGain.gain.exponentialRampToValueAtTime(0.001, now + 0.4);

            sub.connect(subGain);
            subGain.connect(this.ctx.destination);
            sub.start(now);
            sub.stop(now + 0.4);

            // Charging whoosh / laser build-up
            const charge = this.ctx.createOscillator();
            const chargeGain = this.ctx.createGain();
            charge.type = 'sawtooth';
            charge.frequency.setValueAtTime(120, now);
            charge.frequency.exponentialRampToValueAtTime(600, now + 0.25);

            chargeGain.gain.setValueAtTime(0.001, now);
            chargeGain.gain.linearRampToValueAtTime(0.12 * this.volume, now + 0.15);
            chargeGain.gain.exponentialRampToValueAtTime(0.0001, now + 0.3);

            charge.connect(chargeGain);
            chargeGain.connect(this.ctx.destination);
            charge.start(now);
            charge.stop(now + 0.32);
        } catch (_) {}
    }

    // Success sound for promo code or actions
    playSuccess() {
        if (!this.enabled) return;
        this.resume();
        if (!this.ctx) return;

        try {
            const notes = [440, 554.37, 659.25, 880]; // A major arpeggio
            notes.forEach((freq, idx) => {
                const now = this.ctx.currentTime + idx * 0.07;
                const osc = this.ctx.createOscillator();
                const gain = this.ctx.createGain();

                osc.type = 'sine';
                osc.frequency.setValueAtTime(freq, now);

                gain.gain.setValueAtTime(0.15 * this.volume, now);
                gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.22);

                osc.connect(gain);
                gain.connect(this.ctx.destination);
                osc.start(now);
                osc.stop(now + 0.25);
            });
        } catch (_) {}
    }
}

window.soundEngine = new SoundEngine();
