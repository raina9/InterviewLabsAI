const React = window.React;

const LANGUAGES = [
    { id: 'java',       label: 'Java' },
    { id: 'python',     label: 'Python' },
    { id: 'javascript', label: 'JavaScript' },
];

const DEFAULT_STARTER = {
    java:       '// Write your Java solution here\npublic class Solution {\n    public static void main(String[] args) {\n        \n    }\n}\n',
    python:     '# Write your Python solution here\ndef solution():\n    pass\n',
    javascript: '// Write your JavaScript solution here\nfunction solution() {\n    \n}\n',
};

function CodeEditor() {
    const [phase, setPhase]           = React.useState('setup'); // setup | challenge | result
    const [topic, setTopic]           = React.useState('');
    const [difficulty, setDifficulty] = React.useState('medium');
    const [challenge, setChallenge]   = React.useState(null);
    const [language, setLanguage]     = React.useState('java');
    const [code, setCode]             = React.useState(DEFAULT_STARTER['java']);
    const [hint, setHint]             = React.useState(null);
    const [result, setResult]         = React.useState(null);
    const [loading, setLoading]       = React.useState(false);
    const [hintLoading, setHintLoading] = React.useState(false);
    const [error, setError]           = React.useState(null);

    const editorContainerRef = React.useRef(null);
    const monacoEditorRef    = React.useRef(null);
    const monacoLoadedRef    = React.useRef(false);

    // Mount Monaco after challenge is shown
    React.useEffect(() => {
        if (phase !== 'challenge' || !editorContainerRef.current) return;
        if (monacoLoadedRef.current && monacoEditorRef.current) return;

        function mountEditor() {
            if (!window.monaco) return;
            monacoEditorRef.current = window.monaco.editor.create(editorContainerRef.current, {
                value:     code,
                language:  language,
                theme:     'vs-dark',
                fontSize:  13,
                minimap:   { enabled: false },
                lineNumbers: 'on',
                scrollBeyondLastLine: false,
                automaticLayout: true,
                fontFamily: '"JetBrains Mono", monospace',
            });
            monacoLoadedRef.current = true;
        }

        if (window.monaco) {
            mountEditor();
        } else if (window.require) {
            window.require.config({ paths: { vs: 'https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/vs' } });
            window.require(['vs/editor/editor.main'], mountEditor);
        }

        return () => {
            if (monacoEditorRef.current) {
                monacoEditorRef.current.dispose();
                monacoEditorRef.current = null;
                monacoLoadedRef.current = false;
            }
        };
    }, [phase]);

    function handleLanguageChange(lang) {
        setLanguage(lang);
        setCode(DEFAULT_STARTER[lang]);
        if (monacoEditorRef.current && window.monaco) {
            const model = monacoEditorRef.current.getModel();
            if (model) {
                window.monaco.editor.setModelLanguage(model, lang);
                monacoEditorRef.current.setValue(
                    (challenge?.starterCode && challenge.starterCode[lang]) || DEFAULT_STARTER[lang]
                );
            }
        }
    }

    function getCurrentCode() {
        if (monacoEditorRef.current) return monacoEditorRef.current.getValue();
        return code;
    }

    async function generateChallenge() {
        if (!topic.trim() || loading) return;
        setLoading(true);
        setError(null);
        try {
            const res = await window.apiFetch('/api/v1/code/challenge', {
                method: 'POST',
                body: JSON.stringify({ topic: topic.trim(), difficulty })
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Failed to generate challenge');
            }
            const data = await res.json();
            setChallenge(data);
            const starter = (data.starterCode && data.starterCode[language]) || DEFAULT_STARTER[language];
            setCode(starter);
            setPhase('challenge');
            setResult(null);
            setHint(null);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    async function submitCode() {
        if (loading || !challenge) return;
        setLoading(true);
        setError(null);
        const submittedCode = getCurrentCode();
        try {
            const res = await window.apiFetch('/api/v1/code/submit', {
                method: 'POST',
                body: JSON.stringify({ challengeId: challenge.id, code: submittedCode, language })
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Failed to evaluate submission');
            }
            setResult(await res.json());
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    async function getHint() {
        if (hintLoading || !challenge) return;
        setHintLoading(true);
        try {
            const res = await window.apiFetch(`/api/v1/code/challenge/${challenge.id}/hint`);
            if (res.ok) setHint(await res.text());
        } catch (_) {}
        finally { setHintLoading(false); }
    }

    // SETUP
    if (phase === 'setup') {
        return (
            <div className="max-w-lg mx-auto px-4 py-12 space-y-6">
                <div>
                    <h1 className="text-2xl font-bold text-white">Code Challenge</h1>
                    <p className="text-muted text-sm mt-1">AI-generated coding problem with in-browser editor</p>
                </div>

                <div className="bg-surface border border-border rounded-2xl p-5 space-y-4">
                    <div>
                        <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Topic</label>
                        <input value={topic} onChange={e => setTopic(e.target.value)}
                            onKeyDown={e => e.key === 'Enter' && generateChallenge()}
                            placeholder="e.g. Arrays, Dynamic Programming, Trees…"
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
                            <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Language</label>
                            <select value={language} onChange={e => handleLanguageChange(e.target.value)}
                                className="w-full bg-base border border-border rounded-xl px-3 py-2 text-sm text-gray-200 focus:outline-none focus:border-accent cursor-pointer">
                                {LANGUAGES.map(l => (
                                    <option key={l.id} value={l.id} style={{ background: '#0B0D10' }}>{l.label}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    {error && <div className="text-red-400 text-sm bg-red-950/50 border border-red-800 rounded-xl p-3">{error}</div>}

                    <button onClick={generateChallenge} disabled={loading || !topic.trim()}
                        className="w-full bg-accent hover:bg-amber-400 disabled:opacity-40 text-black font-bold py-3 rounded-xl text-sm transition-colors">
                        {loading ? 'Generating challenge…' : 'Generate Challenge →'}
                    </button>
                </div>
            </div>
        );
    }

    // CHALLENGE
    return (
        <div className="flex flex-col h-[calc(100vh-57px)]">

            {/* Toolbar */}
            <div className="flex items-center justify-between px-5 py-2.5 border-b border-border bg-surface flex-shrink-0">
                <div className="flex items-center gap-3">
                    <button onClick={() => setPhase('setup')}
                        className="text-xs text-gray-500 hover:text-gray-300 transition-colors">
                        ← Back
                    </button>
                    <span className="text-xs text-gray-500">|</span>
                    <span className="text-xs font-semibold text-gray-300 truncate max-w-[200px]">{challenge?.title}</span>
                </div>
                <div className="flex items-center gap-2">
                    {/* Language selector */}
                    <select value={language} onChange={e => handleLanguageChange(e.target.value)}
                        className="bg-base border border-border rounded-lg px-2 py-1 text-xs text-gray-300 focus:outline-none focus:border-accent cursor-pointer">
                        {LANGUAGES.map(l => (
                            <option key={l.id} value={l.id} style={{ background: '#0B0D10' }}>{l.label}</option>
                        ))}
                    </select>
                    <button onClick={getHint} disabled={hintLoading}
                        className="text-xs text-gray-500 hover:text-amber-400 border border-gray-700 hover:border-amber-700 px-3 py-1 rounded-lg transition-colors disabled:opacity-40">
                        {hintLoading ? 'Getting hint…' : 'Hint'}
                    </button>
                    <button onClick={submitCode} disabled={loading}
                        className="text-xs bg-accent hover:bg-amber-400 text-black font-bold px-4 py-1.5 rounded-lg transition-colors disabled:opacity-40">
                        {loading ? 'Running…' : 'Run →'}
                    </button>
                </div>
            </div>

            {/* Body — split: left=problem, right=editor */}
            <div className="flex flex-1 overflow-hidden">

                {/* Left panel — problem statement */}
                <div className="w-[380px] flex-shrink-0 border-r border-border overflow-y-auto px-5 py-5 space-y-5 bg-surface/50">
                    <div>
                        <h2 className="text-base font-bold text-white mb-1">{challenge?.title}</h2>
                        <p className="text-xs text-muted font-mono">{difficulty} · {topic}</p>
                    </div>

                    <div>
                        <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Description</p>
                        <p className="text-sm text-gray-300 leading-relaxed whitespace-pre-wrap">{challenge?.description}</p>
                    </div>

                    {challenge?.testCases && challenge.testCases.length > 0 && (
                        <div>
                            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Test Cases</p>
                            <div className="space-y-1.5">
                                {challenge.testCases.map((tc, i) => (
                                    <div key={i} className="bg-base border border-border rounded-lg px-3 py-2 text-xs text-gray-400 font-mono">
                                        {tc}
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {challenge?.constraints && challenge.constraints.length > 0 && (
                        <div>
                            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Constraints</p>
                            <ul className="space-y-1">
                                {challenge.constraints.map((c, i) => (
                                    <li key={i} className="text-xs text-gray-500 font-mono">• {c}</li>
                                ))}
                            </ul>
                        </div>
                    )}

                    {hint && (
                        <div className="bg-amber-950/30 border border-amber-800/50 rounded-xl p-3">
                            <p className="text-xs font-semibold text-amber-400 mb-1">Hint</p>
                            <p className="text-sm text-amber-200/80 leading-relaxed">{hint}</p>
                        </div>
                    )}
                </div>

                {/* Right panel — Monaco editor + output */}
                <div className="flex-1 flex flex-col min-w-0">
                    {/* Monaco editor container */}
                    <div ref={editorContainerRef} className="flex-1" style={{ minHeight: 0 }} />

                    {/* Output panel */}
                    {(result || error) && (
                        <div className="flex-shrink-0 border-t border-border bg-surface max-h-48 overflow-y-auto">
                            {error && (
                                <div className="px-4 py-3 text-xs text-red-400 font-mono">{error}</div>
                            )}
                            {result && (
                                <div className="px-4 py-3 space-y-2">
                                    <div className="flex items-center gap-2">
                                        <span className={`text-xs font-bold ${result.passed ? 'text-green-400' : 'text-red-400'}`}>
                                            {result.passed ? '✓ Passed' : '✗ Failed'}
                                        </span>
                                        {result.executionResult && (
                                            <span className="text-xs text-gray-600 font-mono">{result.executionResult}</span>
                                        )}
                                    </div>
                                    {result.feedback && (
                                        <p className="text-xs text-gray-300 leading-relaxed">{result.feedback}</p>
                                    )}
                                    {result.explanation && (
                                        <p className="text-xs text-gray-500 leading-relaxed">{result.explanation}</p>
                                    )}
                                    {result.refinedCode && (
                                        <details className="mt-1">
                                            <summary className="text-xs text-purple-400 cursor-pointer hover:text-purple-300">
                                                View refined code
                                            </summary>
                                            <pre className="mt-2 text-xs text-gray-300 font-mono bg-base rounded-lg p-3 overflow-x-auto whitespace-pre-wrap">
                                                {result.refinedCode}
                                            </pre>
                                        </details>
                                    )}
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
window.CodeEditor = CodeEditor;
