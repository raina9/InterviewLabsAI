const React            = window.React;
const MentorFeedbackPanel = window.MentorFeedbackPanel;

const FONT_SIZES = { small: 'text-sm', medium: 'text-base', large: 'text-lg' };

function InterviewScreen({ sessionId, voiceEnabled, firstQuestion, totalQuestions }) {
    const [messages, setMessages] = React.useState(
        firstQuestion
            ? [{ role: 'INTERVIEWER', content: firstQuestion }]
            : []
    );
    const [latestFeedback, setLatestFeedback] = React.useState(null);
    const [inputText, setInputText] = React.useState('');
    const [isLoading, setIsLoading] = React.useState(false);
    const [voiceState, setVoiceState] = React.useState('idle'); // idle | recording
    const [sessionComplete, setSessionComplete] = React.useState(false);
    const [error, setError] = React.useState(null);
    const [fontSize, setFontSize] = React.useState('medium');
    const [finishing, setFinishing] = React.useState(false);

    const recorderRef   = React.useRef(null);
    const messagesEndRef = React.useRef(null);

    React.useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages, isLoading]);

    function toggleVoice() {
        if (!window.VoiceRecorder.isSupported) return;

        if (voiceState === 'recording') {
            recorderRef.current?.stop();
            return;
        }

        const recorder = window.VoiceRecorder.create(
            function onResult(transcript) {
                setInputText(transcript);
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

    async function sendAnswer() {
        const answer = inputText.trim();
        if (!answer || isLoading || sessionComplete) return;

        const useVoice = voiceState === 'recording';
        setIsLoading(true);
        setError(null);
        setMessages(prev => [...prev, { role: 'CANDIDATE', content: answer }]);
        setInputText('');

        try {
            let res;
            if (useVoice) {
                res = await window.apiFetch('/api/v1/voice/transcript', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ sessionId, transcript: answer })
                });
            } else {
                res = await window.apiFetch(`/api/v1/interview/${sessionId}/respond`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ answer, voiceUsed: false })
                });
            }

            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Failed to submit answer');
            }

            const data = await res.json();
            setLatestFeedback(data.mentorFeedback);

            if (data.sessionComplete) {
                setSessionComplete(true);
            } else if (data.agentResponse) {
                setMessages(prev => [...prev, { role: 'INTERVIEWER', content: data.agentResponse }]);
            }

        } catch (err) {
            setError(err.message);
            setMessages(prev => prev.slice(0, -1));
            setInputText(answer);
        } finally {
            setIsLoading(false);
        }
    }

    async function handleFinish() {
        if (finishing) return;
        setFinishing(true);
        try {
            await window.apiFetch(`/api/v1/sessions/${sessionId}/complete`, { method: 'POST' });
        } catch (_) {
            // best effort — navigate regardless
        }
        window.location.hash = '#/history';
    }

    function handleKeyDown(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendAnswer();
        }
    }

    const fontClass = FONT_SIZES[fontSize];

    return (
        <div className="flex h-[calc(100vh-57px)]">

            {/* Left panel — Mentor Feedback (300px) */}
            <aside className="w-[300px] flex-shrink-0 border-r border-border bg-surface flex flex-col">
                <div className="px-4 py-3 border-b border-border flex items-center justify-between">
                    <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Feedback</span>
                    {latestFeedback && (
                        <span className={`font-mono text-xs font-bold ${latestFeedback.score >= 7 ? 'text-green-400' : latestFeedback.score >= 4 ? 'text-accent' : 'text-red-400'}`}>
                            {latestFeedback.score}/10
                        </span>
                    )}
                </div>
                <div className="flex-1 overflow-hidden">
                    <MentorFeedbackPanel feedback={latestFeedback} />
                </div>
            </aside>

            {/* Right panel — Chat */}
            <div className="flex-1 flex flex-col min-w-0">

                {/* Chat toolbar */}
                <div className="flex items-center justify-between px-5 py-2.5 border-b border-border bg-surface flex-shrink-0">
                    <div className="flex items-center gap-3">
                        <span className="text-xs text-muted font-mono">
                            Session <span className="text-gray-400">{sessionId.slice(0, 8)}…</span>
                        </span>
                        {voiceEnabled && window.VoiceRecorder.isSupported && (
                            <span className="text-xs text-green-500 bg-green-900/30 px-2 py-0.5 rounded-full">
                                Voice on
                            </span>
                        )}
                    </div>
                    <div className="flex items-center gap-2">
                        {/* Font size controls */}
                        <button onClick={() => setFontSize('small')}
                            className={`w-7 h-7 rounded-lg text-xs font-bold transition-colors ${fontSize === 'small' ? 'bg-border text-gray-200' : 'text-gray-600 hover:text-gray-300'}`}>
                            A
                        </button>
                        <button onClick={() => setFontSize('medium')}
                            className={`w-7 h-7 rounded-lg text-sm font-bold transition-colors ${fontSize === 'medium' ? 'bg-border text-gray-200' : 'text-gray-600 hover:text-gray-300'}`}>
                            A
                        </button>
                        <button onClick={() => setFontSize('large')}
                            className={`w-7 h-7 rounded-lg font-bold transition-colors ${fontSize === 'large' ? 'bg-border text-gray-200' : 'text-gray-600 hover:text-gray-300'}`}>
                            A
                        </button>
                        <div className="w-px h-5 bg-border mx-1" />
                        {!sessionComplete && (
                            <button onClick={handleFinish} disabled={finishing}
                                className="text-xs text-gray-500 hover:text-red-400 border border-gray-700 hover:border-red-700 px-3 py-1 rounded-lg transition-colors disabled:opacity-40">
                                {finishing ? 'Finishing…' : 'Finish'}
                            </button>
                        )}
                    </div>
                </div>

                {/* Messages */}
                <div className="flex-1 overflow-y-auto px-5 py-5 space-y-4">
                    {messages.map((msg, idx) => (
                        <div key={idx} className={`flex ${msg.role === 'CANDIDATE' ? 'justify-end' : 'justify-start'}`}>
                            <div className={`max-w-[75%] px-4 py-3 rounded-2xl ${fontClass} leading-relaxed whitespace-pre-wrap
                                ${msg.role === 'INTERVIEWER'
                                    ? 'bg-surface border border-border text-gray-200 rounded-tl-sm'
                                    : 'bg-accent text-black font-medium rounded-tr-sm'}`}>
                                {msg.content}
                            </div>
                        </div>
                    ))}

                    {/* Typing indicator */}
                    {isLoading && (
                        <div className="flex justify-start">
                            <div className="bg-surface border border-border rounded-2xl rounded-tl-sm px-4 py-3 flex gap-1 items-center">
                                <span className="w-1.5 h-1.5 bg-gray-500 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                                <span className="w-1.5 h-1.5 bg-gray-500 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                                <span className="w-1.5 h-1.5 bg-gray-500 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                            </div>
                        </div>
                    )}

                    {/* Session complete banner */}
                    {sessionComplete && (
                        <div className="flex flex-col items-center py-8 gap-4">
                            <div className="text-4xl">🎉</div>
                            <p className="text-white font-semibold">Session complete!</p>
                            <button onClick={handleFinish} disabled={finishing}
                                className="bg-accent hover:bg-amber-400 text-black font-bold px-6 py-2.5 rounded-xl text-sm transition-colors disabled:opacity-40">
                                {finishing ? 'Saving…' : 'View History →'}
                            </button>
                        </div>
                    )}

                    <div ref={messagesEndRef} />
                </div>

                {/* Error bar */}
                {error && (
                    <div className="px-5 py-2 bg-red-950/60 border-t border-red-800 text-red-400 text-xs">
                        {error}
                    </div>
                )}

                {/* Input bar */}
                {!sessionComplete && (
                    <div className="flex items-end gap-2.5 px-5 py-4 border-t border-border bg-surface flex-shrink-0">
                        {voiceEnabled && window.VoiceRecorder.isSupported && (
                            <button onClick={toggleVoice} disabled={isLoading}
                                className={`flex-shrink-0 w-10 h-10 rounded-xl flex items-center justify-center transition-colors disabled:opacity-40
                                    ${voiceState === 'recording'
                                        ? 'bg-red-600 recording-pulse'
                                        : 'bg-border hover:bg-gray-600'}`}
                                title={voiceState === 'recording' ? 'Stop recording' : 'Start voice input'}>
                                🎤
                            </button>
                        )}
                        <textarea
                            value={inputText}
                            onChange={e => setInputText(e.target.value)}
                            onKeyDown={handleKeyDown}
                            disabled={isLoading}
                            rows={2}
                            placeholder="Type your answer… (Enter to send, Shift+Enter for newline)"
                            className="flex-1 resize-none bg-base border border-border rounded-xl px-4 py-2.5 text-sm text-gray-200 font-mono focus:outline-none focus:border-accent placeholder-gray-600 disabled:opacity-50 transition-colors" />
                        <button onClick={sendAnswer} disabled={isLoading || !inputText.trim()}
                            className="flex-shrink-0 bg-accent hover:bg-amber-400 disabled:opacity-40 text-black font-bold px-4 py-2.5 rounded-xl text-sm transition-colors h-10">
                            Send
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}
window.InterviewScreen = InterviewScreen;
