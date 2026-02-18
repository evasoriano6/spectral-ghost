/**
 * SPECTRAL-01 | Military-Grade Analysis Core
 * Precision Instrument for Anomaly Detection
 */

class SpectralApp {
    constructor() {
        this.video = document.getElementById('camera-view');
        this.visionCanvas = document.getElementById('vision-canvas');
        this.visionCtx = this.visionCanvas.getContext('2d');

        this.oscCanvas = document.getElementById('oscilloscope-canvas');
        this.oscCtx = this.oscCanvas.getContext('2d');

        this.statusLabel = document.getElementById('system-status');
        this.statusDot = document.getElementById('system-dot');

        this.isScanning = false;
        this.isEVPPure = false;
        this.particles = [];

        this.init();
    }

    async init() {
        this.resizeCanvases();
        window.addEventListener('resize', () => this.resizeCanvases());

        try {
            const stream = await navigator.mediaDevices.getUserMedia({
                video: { facingMode: 'environment' },
                audio: true // Audio necesario para osciloscopio real
            });
            this.video.srcObject = stream;

            // Audio Context Setup
            this.audioCtx = new (window.AudioContext || window.webkitAudioContext)();
            this.analyser = this.audioCtx.createAnalyser();
            this.source = this.audioCtx.createMediaStreamSource(stream);
            this.source.connect(this.analyser);
            this.analyser.fftSize = 2048;
            this.bufferLength = this.analyser.frequencyBinCount;
            this.dataArray = new Uint8Array(this.bufferLength);

            // EVP Synth Setup
            this.oscillator = this.audioCtx.createOscillator();
            this.gainNode = this.audioCtx.createGain();
            this.oscillator.connect(this.gainNode);
            this.gainNode.connect(this.audioCtx.destination);
            this.oscillator.start();
            this.gainNode.gain.value = 0; // Silencio inicial

            this.log('SENSORES SINCRONIZADOS');
            this.setupEventListeners();
            this.animate();

        } catch (err) {
            console.error(err);
            this.statusLabel.textContent = 'FALLO EN SENSORES';
            this.statusLabel.style.color = 'var(--accent-alert)';
            this.setupSimulatedVision(); // Fallback
            this.setupEventListeners(); // Aun en simulación necesitamos controles
            this.animate();
        }
    }

    resizeCanvases() {
        this.visionCanvas.width = window.innerWidth;
        this.visionCanvas.height = window.innerHeight;
        this.oscCanvas.width = this.oscCanvas.parentElement.offsetWidth;
        this.oscCanvas.height = this.oscCanvas.parentElement.offsetHeight;
    }

    setupSimulatedVision() {
        // Ruido visual fallback
        this.video.style.display = 'none';
        // Audio simulado
        this.audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        this.analyser = this.audioCtx.createAnalyser();
        // Ruido blanco
        const bufferSize = this.audioCtx.sampleRate * 2;
        const buffer = this.audioCtx.createBuffer(1, bufferSize, this.audioCtx.sampleRate);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) {
            data[i] = Math.random() * 2 - 1;
        }
        const noiseSource = this.audioCtx.createBufferSource();
        noiseSource.buffer = buffer;
        noiseSource.loop = true;
        noiseSource.connect(this.analyser);
        noiseSource.start();

        this.bufferLength = this.analyser.frequencyBinCount;
        this.dataArray = new Uint8Array(this.bufferLength);
    }

    setupEventListeners() {
        document.getElementById('btn-scan').addEventListener('click', () => this.toggleScan());
        document.getElementById('btn-evp').addEventListener('click', () => this.toggleEVP());
        document.getElementById('btn-share').addEventListener('click', () => {
            alert("EXPORTANDO DATOS TELEMÉTRICOS...");
        });
    }

    toggleScan() {
        this.isScanning = !this.isScanning;
        const btn = document.getElementById('btn-scan');
        if (this.isScanning) {
            btn.classList.add('active');
            btn.textContent = 'DETENER ANÁLISIS';
            this.statusLabel.textContent = 'BUSCANDO ANOMALÍAS...';
            this.statusLabel.style.color = 'var(--accent-primary)';
            this.statusDot.style.background = 'var(--accent-primary)';
            this.statusDot.style.boxShadow = '0 0 10px var(--accent-primary)';
        } else {
            btn.classList.remove('active');
            btn.textContent = 'INICIALIZAR ESCANEO';
            this.statusLabel.textContent = 'SISTEMA EN ESPERA';
            this.statusLabel.style.color = '#e0e0e0';
            this.statusDot.style.background = 'var(--accent-success)';
            this.statusDot.style.boxShadow = '0 0 10px var(--accent-success)';
            this.particles = []; // Limpiar
        }
    }

    toggleEVP() {
        this.isEVPPure = !this.isEVPPure;
        const btn = document.getElementById('btn-evp');
        if (this.isEVPPure) {
            btn.classList.add('active');
            this.gainNode.gain.setTargetAtTime(0.05, this.audioCtx.currentTime, 0.1);
        } else {
            btn.classList.remove('active');
            this.gainNode.gain.setTargetAtTime(0, this.audioCtx.currentTime, 0.1);
        }
    }

    log(msg) {
        console.log(`[SYS] ${msg}`);
    }

    updateMeters() {
        // High Precision Math
        const baseLum = 0.045;
        const baseEmi = 0.412;
        const baseSpec = 0.010;

        let lum = baseLum + (Math.random() * 0.005) * (this.isScanning ? 2 : 0.5);
        let emi = baseEmi + (Math.random() * 0.020) * (this.isScanning ? 3 : 0.2);
        let spec = baseSpec + (Math.random() * 0.005) * (this.isScanning ? 4 : 0.1);

        // Actualizar DOM con 3 decimales
        document.getElementById('val-lum').textContent = lum.toFixed(3);
        document.getElementById('bar-lum').style.width = (lum * 500) + '%'; // Escala visual

        document.getElementById('val-emi').textContent = emi.toFixed(3);
        document.getElementById('bar-emi').style.width = (emi * 100) + '%';

        document.getElementById('val-spec').textContent = spec.toFixed(3);
        document.getElementById('bar-spec').style.width = (spec * 800) + '%';

        // Lógica de Alerta
        if (this.isScanning && emi > 0.460) {
            this.statusLabel.textContent = '¡ANOMALÍA DETECTADA!';
            this.statusLabel.style.color = 'var(--accent-alert)';
            this.statusDot.style.background = 'var(--accent-alert)';
            document.getElementById('val-entropy').textContent = 'CONFIRMADO (p<0.01)';
            document.getElementById('val-entropy').style.color = 'var(--accent-alert)';

            // Generar partículas en el centro
            if (Math.random() > 0.5) this.spawnParticle();
        } else {
            document.getElementById('val-entropy').textContent = '---';
            document.getElementById('val-entropy').style.color = '#666';
        }
    }

    spawnParticle() {
        const x = window.innerWidth / 2 + (Math.random() - 0.5) * 200;
        const y = window.innerHeight / 2 + (Math.random() - 0.5) * 150;
        this.particles.push({
            x, y,
            vx: (Math.random() - 0.5) * 2,
            vy: (Math.random() - 0.5) * 2,
            life: 1.0,
            size: Math.random() * 20 + 10,
            hue: Math.random() > 0.5 ? 180 : 15 // Cyan o Rojo-Naranja
        });
    }

    drawParticles() {
        this.visionCtx.clearRect(0, 0, this.visionCanvas.width, this.visionCanvas.height);

        for (let i = this.particles.length - 1; i >= 0; i--) {
            let p = this.particles[i];
            p.x += p.vx;
            p.y += p.vy;
            p.life -= 0.01;

            if (p.life <= 0) {
                this.particles.splice(i, 1);
                continue;
            }

            // Gaussian Blur Effect
            const gradient = this.visionCtx.createRadialGradient(p.x, p.y, 0, p.x, p.y, p.size);
            gradient.addColorStop(0, `hsla(${p.hue}, 100%, 70%, ${p.life})`);
            gradient.addColorStop(1, `hsla(${p.hue}, 100%, 50%, 0)`);

            this.visionCtx.fillStyle = gradient;
            this.visionCtx.beginPath();
            this.visionCtx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
            this.visionCtx.fill();
        }
    }

    drawOscilloscope() {
        if (!this.analyser) return;

        this.analyser.getByteTimeDomainData(this.dataArray);
        const ctx = this.oscCtx;
        const width = this.oscCanvas.width;
        const height = this.oscCanvas.height;

        ctx.fillStyle = 'rgba(0, 0, 0, 0.2)'; // Trail effect
        ctx.fillRect(0, 0, width, height);

        ctx.lineWidth = 2;
        ctx.strokeStyle = '#00f0ff';
        ctx.beginPath();

        const sliceWidth = width * 1.0 / this.bufferLength;
        let x = 0;

        for (let i = 0; i < this.bufferLength; i++) {
            const v = this.dataArray[i] / 128.0;
            const y = v * height / 2;

            if (i === 0) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);

            x += sliceWidth;
        }

        ctx.lineTo(width, height / 2);
        ctx.stroke();

        // Frecuencia Dominante (Simulada/Calculada)
        // En prod usaría FFT para hallar pico
        if (this.isScanning) {
            const freq = 18000 + Math.random() * 2000;
            document.getElementById('val-freq').textContent = freq.toFixed(1);
        } else {
            document.getElementById('val-freq').textContent = "00.0";
        }
    }

    animate() {
        this.updateMeters();
        this.drawParticles();
        this.drawOscilloscope();

        // EVP Modulator
        if (this.isEVPPure && this.oscillator) {
            const freq = 40 + Math.random() * 50;
            this.oscillator.frequency.setTargetAtTime(freq, this.audioCtx.currentTime, 0.1);
        }

        requestAnimationFrame(() => this.animate());
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.app = new SpectralApp();
});
