const React            = window.React;
const MentorFeedbackPanel = window.MentorFeedbackPanel;

const FONT_SIZES = { small: 'text-sm', medium: 'text-base', large: 'text-lg' };

// Speaks each text in the array in sequence; calls onAllDone after the last utterance ends.
function speakSequence(texts, onAllDone) {
    if (!window.speechSynthesis || !texts || texts.length === 0) {
        onAllDone && onAllDone();
        return;
    }
    window.speechSynthesis.cancel();
    function speakNext(index) {
        if (index >= texts.length) { onAllDone && onAllDone(); return; }
        const text = (texts[index] || '').trim();
        if (!text) { speakNext(index + 1); return; }
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.rate = 1.0;
        utterance.onend = () => speakNext(index + 1);
        window.speechSynthesis.speak(utterance);
    }
    speakNext(0);
}

function nudgeStyle(pattern) {
    if (pattern === 'IMPROVING' || pattern === 'SOLID')
        return 'bg-green-950/60 border-green-800 text-green-300';
    if (pattern === 'NERVOUS')
        return 'bg-amber-950/60 border-amber-800 text-amber-300';
    return 'bg-red-950/60 border-red-800 text-red-300';
}

function nudgeIcon(pattern) {
    if (pattern === 'IMPROVING') return '↑';
    if (pattern === 'SOLID')     return '✓';
    if (pattern === 'NERVOUS')   return '⚠';
    return '↗';
}

function MentorBubble({ feedback }) {
    const [showRefined, setShowRefined] = React.useState(false);
    const [showModel,   setShowModel]   = React.useState(false);

    if (!feedback) return null;

    const scoreColor = feedback.score >= 7
        ? 'text-green-400'
        : feedback.score >= 4
        ? 'text-amber-400'
        : 'text-red-400';

    return (
        <div className="flex justify-start">
            <div className="max-w-[85%] rounded-2xl rounded-tl-sm border border-purple-700/50 bg-purple-950/30 px-4 py-3 space-y-2.5">
                <div className="flex items-center gap-2">
                    <span className="text-xs font-semibold text-purple-400 uppercase tracking-wide">Mentor</span>
                    <span className={`font-mono text-xs font-bold px-2 py-0.5 rounded-full bg-gray-900 ${scoreColor}`}>
                        {feedback.score}/10
                    </span>
                </div>

                {feedback.feedbackGood && (
                    <p className="text-sm text-green-300/90 leading-relaxed">
                        ✓ {feedback.feedbackGood}
                    </p>
                )}

                {feedback.feedbackImprove && (
                    <p className="text-sm text-amber-300/90 leading-relaxed">
                        ↑ {feedback.feedbackImprove}
                    </p>
                )}

                {feedback.refinedAnswer && (
                    <div>
                        <button
                            onClick={() => setShowRefined(s => !s)}
                            className="text-xs text-purple-400 hover:text-purple-300 flex items-center gap-1 transition-colors">
                            {showRefined ? '▾' : '▸'} Refined answer
                        </button>
                        {showRefined && (
                            <p className="text-sm text-gray-300 mt-2 pl-3 border-l border-purple-700/50 leading-relaxed whitespace-pre-wrap font-mono">
                                {feedback.refinedAnswer}
                            </p>
                        )}
                    </div>
                )}

                {feedback.modelAnswer && (
                    <div>
                        <button
                            onClick={() => setShowModel(s => !s)}
                            className="text-xs text-teal-400 hover:text-teal-300 flex items-center gap-1 transition-colors">
                            {showModel ? '▾' : '▸'} Model answer
                        </button>
                        {showModel && (
                            <p className="text-sm text-gray-300 mt-2 pl-3 border-l border-teal-700/50 leading-relaxed whitespace-pre-wrap font-mono">
                                {feedback.modelAnswer}
                            </p>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}

function InterviewScreen({ sessionId, voiceEnabled, firstQuestion, totalQuestions }) {
    const [messages, setMessages]             = React.useState(
        firstQuestion ? [{ role: 'INTERVIEWER', content: firstQuestion }] : []
    );
    const [latestFeedback, setLatestFeedback] = React.useState(null);
    const [psychologyNudge, setPsychologyNudge] = React.useState(null);
    const [inputText, setInputText]           = React.useState('');
    const [isLoading, setIsLoading]           = React.useState(false);
    const [voiceState, setVoiceState]         = React.useState('idle'); // idle | recording
    const [sessionComplete, setSessionComplete] = React.useState(false);
    const [error, setError]                   = React.useState(null);
    const [fontSize, setFontSize]             = React.useState('medium');
    const [finishing, setFinishing]           = React.useState(false);

    const recorderRef    = React.useRef(null);
    const messagesEndRef = React.useRef(null);
    // Kept current every render so voice recorder callbacks always call the latest sendAnswer.
    const sendAnswerRef  = React.useRef(null);

    React.useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages, isLoading]);

    // Opens a new SpeechRecognition session; transcript auto-submits when voice is enabled.
    function createAndStartRecorder() {
        if (!window.VoiceRecorder.isSupported) return;
        const recorder = window.VoiceRecorder.create(
            function onResult(transcript) {
                setInputText(transcript);
                setVoiceState('idle');
                if (voiceEnabled) sendAnswerRef.current(transcript);
            },
            function onStateChange(state) { setVoiceState(state); }
        );
        if (!recorder) return;
        recorderRef.current = recorder;
        recorder.start();
    }

    function toggleVoice() {
        if (!window.VoiceRecorder.isSupported) return;
        if (voiceState === 'recording') { recorderRef.current?.stop(); return; }
        createAndStartRecorder();
    }

    async function sendAnswer(textArg) {
        const answer = (typeof textArg === 'string' ? textArg : inputText).trim();
        if (!answer || isLoading || sessionComplete) return;

        // textArg is a string only when called from the voice onResult callback.
        const fromVoice = typeof textArg === 'string';
        setIsLoading(true);
        setError(null);
        setMessages(prev => [...prev, { role: 'CANDIDATE', content: answer }]);
        setInputText('');

        try {
            let res;
            if (fromVoice) {
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

            if (data.mentorFeedback) {
                setLatestFeedback(data.mentorFeedback);
                setMessages(prev => [...prev, { role: 'MENTOR', feedback: data.mentorFeedback }]);
            }

            if (data.psychologyNudge) {
                setPsychologyNudge(data.psychologyNudge);
            }

            if (data.sessionComplete) {
                setSessionComplete(true);
            } else if (data.agentResponse) {
                setMessages(prev => [...prev, { role: 'INTERVIEWER', content: data.agentResponse }]);
                if (voiceEnabled) {
                    const feedbackSpeech = data.mentorFeedback
                        ? `Score ${data.mentorFeedback.score} out of 10. ${data.mentorFeedback.feedbackGood || ''}`
                        : '';
                    speakSequence(
                        [feedbackSpeech, data.agentResponse].filter(t => t.trim()),
                        () => createAndStartRecorder()
                    );
                }
            }

        } catch (err) {
            setError(err.message);
            setMessages(prev => prev.slice(0, -1));
            setInputText(answer);
        } finally {
            setIsLoading(false);
        }
    }

    // Keep the ref current on every render so stale voice closures always reach the latest sendAnswer.
    sendAnswerRef.current = sendAnswer;

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

            {/* Left panel — compact feedback summary (300px) */}
            <aside className="w-[300px] flex-shrink-0 border-r border-border bg-surface flex flex-col">
                <div className="px-4 py-3 border-b border-border flex items-center justify-between">
                    <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Last Score</span>
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

                {/* Messages — aria-live announces new interviewer/mentor turns to screen readers */}
                <div className="flex-1 overflow-y-auto px-5 py-5 space-y-4"
                    aria-live="polite" aria-relevant="additions">
                    {messages.map((msg, idx) => {
                        if (msg.role === 'MENTOR') {
                            return <MentorBubble key={idx} feedback={msg.feedback} />;
                        }
                        return (
                            <div key={idx} className={`flex ${msg.role === 'CANDIDATE' ? 'justify-end' : 'justify-start'}`}>
                                <div className={`max-w-[75%] px-4 py-3 rounded-2xl ${fontClass} leading-relaxed whitespace-pre-wrap
                                    ${msg.role === 'INTERVIEWER'
                                        ? 'bg-surface border border-border text-gray-200 rounded-tl-sm'
                                        : 'bg-accent text-black font-medium rounded-tr-sm'}`}>
                                    {msg.content}
                                </div>
                            </div>
                        );
                    })}

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

                {/* Psychology nudge banner — shown after every 3rd scored answer */}
                {psychologyNudge && !sessionComplete && (
                    <div className={`px-5 py-2.5 border-t flex items-start gap-2.5 text-xs ${nudgeStyle(psychologyNudge.pattern)}`}>
                        <span className="flex-shrink-0 font-bold text-sm">{nudgeIcon(psychologyNudge.pattern)}</span>
                        <div className="flex-1 min-w-0">
                            <p className="font-semibold leading-snug">{psychologyNudge.nudge}</p>
                            <p className="mt-0.5 opacity-80 leading-snug">{psychologyNudge.actionableAdvice}</p>
                        </div>
                        <button
                            onClick={() => setPsychologyNudge(null)}
                            className="flex-shrink-0 opacity-50 hover:opacity-100 transition-opacity ml-2">
                            ✕
                        </button>
                    </div>
                )}

                {/* Input bar */}
                {!sessionComplete && (
                    <div className="flex items-end gap-2.5 px-5 py-4 border-t border-border bg-surface flex-shrink-0">
                        {voiceEnabled && window.VoiceRecorder.isSupported && (
                            <div className="relative flex-shrink-0">
                                {voiceState === 'recording' && (
                                    <span className="absolute -top-1 -right-1 w-2.5 h-2.5 bg-red-500 rounded-full animate-ping z-10" />
                                )}
                                <button onClick={toggleVoice} disabled={isLoading}
                                    className={`w-10 h-10 rounded-xl flex items-center justify-center transition-colors disabled:opacity-40
                                        ${voiceState === 'recording'
                                            ? 'bg-red-600'
                                            : 'bg-border hover:bg-gray-600'}`}
                                    title={voiceState === 'recording' ? 'Stop recording' : 'Start voice input'}>
                                    🎤
                                </button>
                            </div>
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
