const React = window.React;

function QuizScreen() {
    const [phase, setPhase]             = React.useState('setup'); // setup | quiz | result
    const [topic, setTopic]             = React.useState('');
    const [difficulty, setDifficulty]   = React.useState('medium');
    const [questionCount, setCount]     = React.useState(5);
    const [session, setSession]         = React.useState(null);
    const [selected, setSelected]       = React.useState(null);
    const [lastAnswer, setLastAnswer]   = React.useState(null); // QuizAnswerResponse
    const [result, setResult]           = React.useState(null);
    const [loading, setLoading]         = React.useState(false);
    const [error, setError]             = React.useState(null);

    async function startQuiz() {
        if (!topic.trim() || loading) return;
        setLoading(true);
        setError(null);
        try {
            const res = await window.apiFetch('/api/v1/quiz/start', {
                method: 'POST',
                body: JSON.stringify({ topic: topic.trim(), difficulty, questionCount })
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Failed to start quiz');
            }
            const data = await res.json();
            setSession(data);
            setPhase('quiz');
            setSelected(null);
            setLastAnswer(null);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    async function submitAnswer() {
        if (!selected || loading || !session) return;
        setLoading(true);
        setError(null);
        try {
            const res = await window.apiFetch(`/api/v1/quiz/${session.sessionId}/answer`, {
                method: 'POST',
                body: JSON.stringify({ answer: selected })
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Failed to submit answer');
            }
            const data = await res.json();
            setLastAnswer(data);

            if (data.sessionComplete) {
                // Fetch final result
                const resResult = await window.apiFetch(`/api/v1/quiz/${session.sessionId}/result`);
                if (resResult.ok) {
                    setResult(await resResult.json());
                }
                setPhase('result');
            } else {
                // Advance to next question
                setSession(prev => ({
                    ...prev,
                    currentIndex:    data.totalAnswered,
                    score:           data.score,
                    currentQuestion: data.nextQuestion,
                    currentOptions:  data.nextOptions
                }));
                setSelected(null);
                setLastAnswer(data); // keep for feedback display
            }
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    function scoreColor(pct) {
        if (pct >= 70) return 'text-green-400';
        if (pct >= 40) return 'text-amber-400';
        return 'text-red-400';
    }

    // SETUP phase
    if (phase === 'setup') {
        return (
            <div className="max-w-lg mx-auto px-4 py-12 space-y-6">
                <div>
                    <h1 className="text-2xl font-bold text-white">Quiz Mode</h1>
                    <p className="text-muted text-sm mt-1">AI-generated multiple-choice quiz on any technical topic</p>
                </div>

                <div className="bg-surface border border-border rounded-2xl p-5 space-y-4">
                    <div>
                        <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Topic</label>
                        <input
                            value={topic}
                            onChange={e => setTopic(e.target.value)}
                            onKeyDown={e => e.key === 'Enter' && startQuiz()}
                            placeholder="e.g. Java Collections, Spring Security, System Design…"
                            className="w-full bg-base border border-border rounded-xl px-4 py-2.5 text-sm text-gray-200 focus:outline-none focus:border-accent placeholder-gray-600 transition-colors" />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Difficulty</label>
                            <select value={difficulty} onChange={e => setDifficulty(e.target.value)}
                                className="w-full bg-base border border-border rounded-xl px-3 py-2 text-sm text-gray-200 focus:outline-none focus:border-accent cursor-pointer">
                                <option value="easy"   style={{ background: '#0B0D10' }}>Easy</option>
                                <option value="medium" style={{ background: '#0B0D10' }}>Medium</option>
                                <option value="hard"   style={{ background: '#0B0D10' }}>Hard</option>
                            </select>
                        </div>
                        <div>
                            <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Questions</label>
                            <select value={questionCount} onChange={e => setCount(Number(e.target.value))}
                                className="w-full bg-base border border-border rounded-xl px-3 py-2 text-sm text-gray-200 focus:outline-none focus:border-accent cursor-pointer">
                                {[3, 5, 10, 15].map(n => (
                                    <option key={n} value={n} style={{ background: '#0B0D10' }}>{n}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    {error && <div className="text-red-400 text-sm bg-red-950/50 border border-red-800 rounded-xl p-3">{error}</div>}

                    <button onClick={startQuiz} disabled={loading || !topic.trim()}
                        className="w-full bg-accent hover:bg-amber-400 disabled:opacity-40 text-black font-bold py-3 rounded-xl text-sm transition-colors">
                        {loading ? 'Generating questions…' : 'Start Quiz →'}
                    </button>
                </div>
            </div>
        );
    }

    // RESULT phase
    if (phase === 'result' && result) {
        return (
            <div className="max-w-lg mx-auto px-4 py-12 space-y-6">
                <div>
                    <h1 className="text-2xl font-bold text-white">Quiz Complete</h1>
                    <p className="text-muted text-sm mt-1">{session?.topic}</p>
                </div>

                <div className="bg-surface border border-border rounded-2xl p-8 text-center space-y-3">
                    <p className="text-xs text-gray-500 uppercase tracking-wider font-semibold">Score</p>
                    <p className={`text-6xl font-bold font-mono ${scoreColor(result.scorePercent)}`}>
                        {result.scorePercent}%
                    </p>
                    <p className="text-sm text-gray-400 font-mono">
                        {result.correctAnswers} / {result.totalQuestions} correct
                    </p>
                </div>

                <div className="flex gap-3">
                    <button onClick={() => { setPhase('setup'); setSession(null); setResult(null); setLastAnswer(null); }}
                        className="flex-1 border border-border hover:border-accent text-gray-300 hover:text-accent py-2.5 rounded-xl text-sm transition-colors">
                        New Quiz
                    </button>
                    <a href="#/"
                        className="flex-1 text-center bg-accent hover:bg-amber-400 text-black font-bold py-2.5 rounded-xl text-sm transition-colors">
                        Back to Home
                    </a>
                </div>
            </div>
        );
    }

    // QUIZ phase
    const totalAnswered = session?.currentIndex || 0;
    const total         = session?.totalQuestions || 0;
    const pct           = total > 0 ? Math.round((totalAnswered / total) * 100) : 0;

    return (
        <div className="max-w-xl mx-auto px-4 py-8 space-y-6">
            {/* Progress */}
            <div className="space-y-1.5">
                <div className="flex items-center justify-between text-xs text-gray-500 font-mono">
                    <span>{session?.topic} · {difficulty}</span>
                    <span>{totalAnswered}/{total}</span>
                </div>
                <div className="h-1.5 bg-gray-800 rounded-full overflow-hidden">
                    <div className="h-full bg-accent rounded-full transition-all" style={{ width: `${pct}%` }} />
                </div>
            </div>

            {/* Last answer feedback */}
            {lastAnswer && (
                <div className={`rounded-2xl p-4 border text-sm ${lastAnswer.correct ? 'bg-green-950/30 border-green-800 text-green-300' : 'bg-red-950/30 border-red-800 text-red-300'}`}>
                    <p className="font-semibold mb-1">{lastAnswer.correct ? '✓ Correct' : '✗ Incorrect'}</p>
                    {!lastAnswer.correct && (
                        <p className="text-xs opacity-80 mb-1">Correct answer: <span className="font-mono font-medium">{lastAnswer.correctAnswer}</span></p>
                    )}
                    <p className="text-xs opacity-80 leading-relaxed">{lastAnswer.explanation}</p>
                </div>
            )}

            {/* Current question */}
            {session?.currentQuestion && (
                <div className="bg-surface border border-border rounded-2xl p-5 space-y-4">
                    <p className="text-sm font-semibold text-gray-200 leading-relaxed">{session.currentQuestion}</p>

                    <div className="space-y-2">
                        {(session.currentOptions || []).map((opt, i) => (
                            <button key={i} onClick={() => setSelected(opt)}
                                className={`w-full text-left px-4 py-2.5 rounded-xl text-sm transition-colors border
                                    ${selected === opt
                                        ? 'bg-accent/20 border-accent text-amber-300 font-medium'
                                        : 'bg-base border-border text-gray-300 hover:border-gray-500'}`}>
                                {opt}
                            </button>
                        ))}
                    </div>

                    {error && <div className="text-red-400 text-xs bg-red-950/50 border border-red-800 rounded-xl p-2">{error}</div>}

                    <button onClick={submitAnswer} disabled={!selected || loading}
                        className="w-full bg-accent hover:bg-amber-400 disabled:opacity-40 text-black font-bold py-2.5 rounded-xl text-sm transition-colors">
                        {loading ? 'Submitting…' : 'Submit Answer →'}
                    </button>
                </div>
            )}
        </div>
    );
}
window.QuizScreen = QuizScreen;
