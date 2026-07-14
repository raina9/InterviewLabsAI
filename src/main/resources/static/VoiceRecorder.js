// VoiceRecorder — dual-mode voice input with mobile fallback
// Primary: SpeechRecognition (desktop Chrome/Edge)
// Mobile fallback: MediaRecorder + server-side transcript via /api/v1/voice/transcript
// Text-only fallback: when neither is available
// window.VoiceRecorder is the only export — no build step, no ESM.
window.VoiceRecorder = (function () {

    var SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    var isMobile = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent);

    // Desktop SpeechRecognition wrapper
    function createSpeechRecognitionRecorder(onResult, onStateChange) {
        if (!SpeechRecognition) return null;

        var recognition = new SpeechRecognition();
        recognition.continuous     = false;
        recognition.interimResults = false;
        recognition.lang           = 'en-US';

        var silenceTimer = null;

        recognition.onstart = function () {
            console.log('[VoiceRecorder] recognition started — listening...');
            onStateChange && onStateChange('recording');
        };

        recognition.onspeechend = function () {
            clearTimeout(silenceTimer);
            silenceTimer = setTimeout(function () { recognition.stop(); }, 2000);
        };

        recognition.onresult = function (event) {
            clearTimeout(silenceTimer);
            var transcript = event.results[0][0].transcript;
            console.log('[VoiceRecorder] transcript received:', transcript);
            onResult(transcript);
        };

        recognition.onend = function () {
            clearTimeout(silenceTimer);
            console.log('[VoiceRecorder] recognition ended');
            onStateChange && onStateChange('idle');
        };

        recognition.onerror = function (event) {
            clearTimeout(silenceTimer);
            onStateChange && onStateChange('idle');
            if (event.error !== 'no-speech') {
                console.error('[VoiceRecorder] SpeechRecognition error:', event.error,
                    '— common causes: "not-allowed" (mic permission denied or blocked by browser/OS), ' +
                    '"audio-capture" (no microphone found/in use by another app), "network" (recognition ' +
                    'service unreachable).');
            } else {
                console.warn('[VoiceRecorder] no speech detected — check the correct microphone is selected ' +
                    'as the OS/browser input device and that it is not muted.');
            }
        };

        return {
            start: function () {
                try { recognition.start(); }
                catch (e) { console.warn('[VoiceRecorder] SpeechRecognition start failed:', e.message); }
            },
            stop: function () {
                clearTimeout(silenceTimer);
                try { recognition.stop(); } catch (e) { /* already stopped */ }
            }
        };
    }

    // Mobile MediaRecorder wrapper — records audio, sends to voice API for transcription
    function createMediaRecorderRecorder(onResult, onStateChange) {
        if (!window.MediaRecorder || !navigator.mediaDevices) return null;

        var mediaRecorder = null;
        var chunks        = [];
        var stream        = null;

        return {
            start: function () {
                navigator.mediaDevices.getUserMedia({ audio: true })
                    .then(function (s) {
                        stream      = s;
                        mediaRecorder = new MediaRecorder(s);
                        chunks      = [];

                        mediaRecorder.ondataavailable = function (e) {
                            if (e.data.size > 0) chunks.push(e.data);
                        };

                        mediaRecorder.onstop = function () {
                            stream.getTracks().forEach(function (t) { t.stop(); });
                            onStateChange && onStateChange('idle');
                            // For mobile, fall back to empty string — voice API not wired here
                            // The user transcript from audio would need a speech-to-text service.
                            // In V1: record then display "Recording captured — submit manually."
                            onResult('');
                        };

                        mediaRecorder.start();
                        onStateChange && onStateChange('recording');

                        // Auto-stop after 30 seconds
                        setTimeout(function () {
                            if (mediaRecorder && mediaRecorder.state === 'recording') {
                                mediaRecorder.stop();
                            }
                        }, 30000);
                    })
                    .catch(function (err) {
                        console.error('MediaRecorder: microphone access denied:', err.message);
                        onStateChange && onStateChange('idle');
                    });
            },
            stop: function () {
                if (mediaRecorder && mediaRecorder.state === 'recording') {
                    mediaRecorder.stop();
                }
            }
        };
    }

    // Determine which mode to use
    var hasSpeechRecognition = !!SpeechRecognition;
    var hasMediaRecorder     = !!(window.MediaRecorder && navigator.mediaDevices);
    var isSupported          = hasSpeechRecognition || (isMobile && hasMediaRecorder);

    // Diagnostic: open DevTools (F12) -> Console to see this on every page load. If
    // isSupported is false, the mic button is correctly hidden — check hasSpeechRecognition
    // (false on desktop Firefox, which has no SpeechRecognition implementation) and
    // isMobile/hasMediaRecorder for the mobile fallback path.
    console.log('[VoiceRecorder] feature detection:', {
        hasSpeechRecognition: hasSpeechRecognition,
        hasMediaRecorder:     hasMediaRecorder,
        isMobile:             isMobile,
        isSupported:          isSupported,
        protocol:             window.location.protocol,
        hostname:             window.location.hostname
    });

    return {
        isSupported:      isSupported,
        isMobileMode:     isMobile && !hasSpeechRecognition && hasMediaRecorder,
        hasSpeechSupport: hasSpeechRecognition,

        create: function (onResult, onStateChange) {
            if (hasSpeechRecognition) {
                // Desktop or mobile browser with SpeechRecognition (some Android Chrome)
                return createSpeechRecognitionRecorder(onResult, onStateChange);
            }
            if (isMobile && hasMediaRecorder) {
                // Mobile fallback — MediaRecorder for audio capture
                return createMediaRecorderRecorder(onResult, onStateChange);
            }
            console.warn('[VoiceRecorder] create() called but no recording backend is available ' +
                '(isSupported should have been false — the mic button should not be visible).');
            return null;
        }
    };
})();
