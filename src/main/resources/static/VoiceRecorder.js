// VoiceRecorder — SpeechRecognition wrapper with webkit fallback + auto-stop after 2s silence
// window.VoiceRecorder is the only export — no build step, no ESM.
window.VoiceRecorder = (function () {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;

    return {
        isSupported: !!SpeechRecognition,

        create: function (onResult, onStateChange) {
            if (!SpeechRecognition) return null;

            const recognition = new SpeechRecognition();
            recognition.continuous = false;
            recognition.interimResults = false;
            recognition.lang = 'en-US';

            let silenceTimer = null;

            recognition.onstart = function () {
                onStateChange && onStateChange('recording');
            };

            recognition.onspeechend = function () {
                // Auto-stop 2s after speech stops — gives user a natural pause
                clearTimeout(silenceTimer);
                silenceTimer = setTimeout(function () {
                    recognition.stop();
                }, 2000);
            };

            recognition.onresult = function (event) {
                clearTimeout(silenceTimer);
                const transcript = event.results[0][0].transcript;
                onResult(transcript);
            };

            recognition.onend = function () {
                clearTimeout(silenceTimer);
                onStateChange && onStateChange('idle');
            };

            recognition.onerror = function (event) {
                clearTimeout(silenceTimer);
                onStateChange && onStateChange('idle');
                if (event.error !== 'no-speech') {
                    console.error('SpeechRecognition error:', event.error);
                }
            };

            return {
                start: function () {
                    try { recognition.start(); }
                    catch (e) { console.warn('SpeechRecognition start failed:', e.message); }
                },
                stop: function () {
                    clearTimeout(silenceTimer);
                    try { recognition.stop(); }
                    catch (e) { /* already stopped */ }
                }
            };
        }
    };
})();
