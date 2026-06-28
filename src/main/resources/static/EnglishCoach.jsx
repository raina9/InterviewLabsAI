const React = window.React;

function ImprovedVersionSection({ text }) {
    const [open, setOpen] = React.useState(false);
    return (
        <div className="border border-green-800/50 rounded-2xl overflow-hidden">
            <button onClick={() => setOpen(!open)}
                className="w-full flex items-center justify-between px-5 py-3.5 bg-green-900/20 hover:bg-green-900/30 transition-colors">
                <span className="text-sm font-semibold text-green-400">Improved Version</span>
                <span className="font-mono text-green-600">{open ? '−' : '+'}</span>
            </button>
            {open && (
                <div className="px-5 py-4 text-sm text-gray-300 font-mono leading-relaxed whitespace-pre-wrap border-t border-green-800/30">
                    {text}
                </div>
            )}
        </div>
    );
}

function EnglishCoach() {
    const [transcript, setTranscript] = React.useState('');
    const [context, setContext]       = React.useState('interview');
    const [loading, setLoading]       = React.useState(false);
    const [result, setResult]         = React.useState(null);
    const [error, setError]           = React.useState(null);
    const [voiceState, setVoiceState] = React.useState('idle'); // idle | recording
    const recorderRef = React.useRef(null);

    function toggleRecording() {
        if (!window.VoiceRecorder.isSupported) return;

        if (voiceState === 'recording') {
            recorderRef.current?.stop();
            return;
        }

        const recorder = window.VoiceRecorder.create(
            function onResult(captured) {
                setTranscript(captured);
                setVoiceState('idle');
            },
            function onStateChange(state) {
                setVoiceState(state);
            }
        );
        if (!recorder) return;
        recorderRef.current = recorder;
        recorder.start();
    }

    async function handleAnalyze() {
        const text = transcript.trim();
        if (!text || loading) return;

        setLoading(true);
        setError(null);
        setResult(null);

        try {
            const res = await window.apiFetch('/api/v1/english/analyze', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    transcript: text,
                    context:    context || null,
                    sessionId:  null
                })
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Analysis failed');
            }
            const data = await res.json();
            setResult(data.feedback);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    function renderFillerWords(fillerStr) {
        if (!fillerStr || fillerStr.toLowerCase() === 'none') {
            return <span className="text-green-400 text-sm">None detected</span>;
        }
        const words = fillerStr.split(/,\s*/).filter(Boolean);
        return (
            <div className="flex flex-wrap gap-2 mt-1">
                {words.map((w, i) => (
                    <span key={i}
                        className="bg-amber-900/40 border border-amber-700 text-amber-300 text-xs px-2.5 py-1 rounded-full font-mono">
                        {w.trim()}
                    </span>
                ))}
            </div>
        );
    }

    const hasTenseErrors = result && result.tenseFeedback &&
        result.tenseFeedback.toLowerCase() !== 'no errors detected' &&
        result.tenseFeedback.toLowerCase() !== 'none';

    const scoreClass = result
        ? result.fluencyScore >= 7
            ? 'text-green-400 bg-green-900/40 border-green-800'
            : result.fluencyScore >= 4
            ? 'text-accent bg-amber-900/30 border-amber-800'
            : 'text-red-400 bg-red-900/30 border-red-800'
        : '';

    return (
        <div className="max-w-2xl mx-auto px-4 py-12">
            <div className="mb-8">
                <h1 className="text-2xl font-bold text-white">English Coach</h1>
                <p className="text-muted text-sm mt-1">AI analysis of your spoken or written English</p>
            </div>

            {/* Input card */}
            <div className="bg-surface border border-border rounded-2xl p-5 mb-6 space-y-4">

                {/* Context */}
                <div>
                    <label className="block text-xs font-semibold text-gray-500 mb-2 uppercase tracking-wider">Context</label>
                    <select value={context} onChange={e => setContext(e.target.value)}
                        className="bg-base border border-border text-gray-200 rounded-xl px-3 py-2 text-sm focus:outline-none focus:border-accent cursor-pointer">
                        <option value="interview" style={{ background: '#0B0D10' }}>Interview</option>
                        <option value="presentation" style={{ background: '#0B0D10' }}>Presentation</option>
                        <option value="casual" style={{ background: '#0B0D10' }}>Casual</option>
                    </select>
                </div>

                {/* Textarea */}
                <div>
                    <label className="block text-xs font-semibold text-gray-500 mb-2 uppercase tracking-wider">
                        Transcript
                    </label>
                    <textarea
                        value={transcript}
                        onChange={e => setTranscript(e.target.value)}
                        rows={6}
                        placeholder="Type or speak your answer here…"
                        className="w-full bg-base border border-border rounded-xl px-4 py-3 text-sm text-gray-200 font-mono focus:outline-none focus:border-accent placeholder-gray-600 resize-none" />
                </div>

                {/* Voice control */}
                {window.VoiceRecorder.isSupported && (
                    <div className="flex items-center gap-3">
                        <button onClick={toggleRecording} disabled={loading}
                            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-colors disabled:opacity-40
                                ${voiceState === 'recording'
                                    ? 'bg-red-600 text-white recording-pulse'
                                    : 'bg-border hover:bg-gray-600 text-gray-300'}`}>
                            <span>🎤</span>
                            <span>{voiceState === 'recording' ? 'Recording… (stops after 2s silence)' : 'Record'}</span>
                        </button>
                        {voiceState === 'idle' && transcript && (
                            <span className="text-xs text-green-500 font-mono">Ready to analyse</span>
                        )}
                    </div>
                )}

                {error && (
                    <div className="text-red-400 text-sm bg-red-950/50 border border-red-800 rounded-xl p-3">
                        {error}
                    </div>
                )}

                <button onClick={handleAnalyze} disabled={loading || !transcript.trim()}
                    className="w-full bg-accent hover:bg-amber-400 disabled:opacity-40 text-black font-bold py-3 rounded-xl text-sm transition-colors">
                    {loading ? 'Analysing…' : 'Analyse'}
                </button>
            </div>

            {/* Results */}
            {result && (
                <div className="space-y-4">

                    {/* Fluency score */}
                    <div className="flex items-center justify-between bg-surface border border-border rounded-2xl px-5 py-4">
                        <span className="font-semibold text-gray-300">Fluency Score</span>
                        <span className={`font-mono font-bold text-2xl px-3 py-1 rounded-xl border ${scoreClass}`}>
                            {result.fluencyScore}/10
                        </span>
                    </div>

                    {/* Fluency note */}
                    {result.fluencyNote && (
                        <div className="bg-surface border border-border rounded-2xl p-5">
                            <p className="text-xs text-gray-500 uppercase tracking-wider font-semibold mb-2">Overall Fluency</p>
                            <p className="text-sm text-gray-300 leading-relaxed">{result.fluencyNote}</p>
                        </div>
                    )}

                    {/* Grammar & Tense */}
                    {result.tenseFeedback && (
                        <div className={`bg-surface border rounded-2xl p-5 ${hasTenseErrors ? 'border-red-800/60' : 'border-border'}`}>
                            <p className="text-xs text-gray-500 uppercase tracking-wider font-semibold mb-2">Grammar &amp; Tense</p>
                            <p className={`text-sm leading-relaxed ${hasTenseErrors ? 'text-red-300' : 'text-green-400'}`}>
                                {result.tenseFeedback}
                            </p>
                        </div>
                    )}

                    {/* Filler words */}
                    {result.fillerWordsDetected && (
                        <div className="bg-surface border border-border rounded-2xl p-5">
                            <p className="text-xs text-gray-500 uppercase tracking-wider font-semibold mb-2">Filler Words</p>
                            {renderFillerWords(result.fillerWordsDetected)}
                        </div>
                    )}

                    {/* Vocabulary + Confidence (side by side) */}
                    {(result.vocabularyNote || result.confidenceNote) && (
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            {result.vocabularyNote && (
                                <div className="bg-surface border border-border rounded-2xl p-5">
                                    <p className="text-xs text-gray-500 uppercase tracking-wider font-semibold mb-2">Vocabulary</p>
                                    <p className="text-sm text-gray-300 leading-relaxed">{result.vocabularyNote}</p>
                                </div>
                            )}
                            {result.confidenceNote && (
                                <div className="bg-surface border border-border rounded-2xl p-5">
                                    <p className="text-xs text-gray-500 uppercase tracking-wider font-semibold mb-2">Confidence</p>
                                    <p className="text-sm text-gray-300 leading-relaxed">{result.confidenceNote}</p>
                                </div>
                            )}
                        </div>
                    )}

                    {/* Improved version (collapsible) */}
                    {result.improvedVersion && (
                        <ImprovedVersionSection text={result.improvedVersion} />
                    )}
                </div>
            )}
        </div>
    );
}
window.EnglishCoach = EnglishCoach;
