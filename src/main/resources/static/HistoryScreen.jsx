const React = window.React;

function HistoryScreen() {
    const [sessions, setSessions]           = React.useState([]);
    const [loading, setLoading]             = React.useState(true);
    const [error, setError]                 = React.useState(null);
    const [selected, setSelected]           = React.useState(null); // { session, feedback, loading }

    React.useEffect(() => {
        window.apiFetch('/api/v1/sessions')
            .then(res => {
                if (!res.ok) throw new Error('Failed to load sessions');
                return res.json();
            })
            .then(data => {
                // Sort newest first
                const sorted = [...data].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
                setSessions(sorted);
            })
            .catch(err => setError(err.message))
            .finally(() => setLoading(false));
    }, []);

    async function openSession(session) {
        setSelected({ session, feedback: null, loading: true });
        try {
            const res = await window.apiFetch(`/api/v1/interview/${session.id}/feedback`);
            if (!res.ok) throw new Error('Failed to load feedback');
            const feedback = await res.json();
            setSelected({ session, feedback, loading: false });
        } catch (err) {
            setSelected({ session, feedback: [], loading: false, error: err.message });
        }
    }

    function avgScore(feedback) {
        if (!feedback || !feedback.length) return null;
        const total = feedback.reduce((sum, f) => sum + f.score, 0);
        return (total / feedback.length).toFixed(1);
    }

    function formatDate(iso) {
        if (!iso) return '—';
        return new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
    }

    function statusBadge(status) {
        const map = {
            ACTIVE:    'bg-blue-900/40 text-blue-400 border-blue-800',
            COMPLETED: 'bg-green-900/40 text-green-400 border-green-800',
            ABANDONED: 'bg-gray-800 text-gray-500 border-gray-700'
        };
        return map[status] || 'bg-gray-800 text-gray-500 border-gray-700';
    }

    function scoreColor(score) {
        if (score === null) return 'text-gray-600';
        if (score >= 7) return 'text-green-400';
        if (score >= 4) return 'text-accent';
        return 'text-red-400';
    }

    // Detail modal
    if (selected) {
        const avg = avgScore(selected.feedback);
        return (
            <div className="max-w-3xl mx-auto px-4 py-10">
                <button onClick={() => setSelected(null)}
                    className="flex items-center gap-2 text-sm text-gray-500 hover:text-gray-200 transition-colors mb-6">
                    ← Back to history
                </button>

                {/* Session header */}
                <div className="bg-surface border border-border rounded-2xl p-6 mb-6">
                    <div className="flex items-start justify-between">
                        <div>
                            <h2 className="text-lg font-semibold text-white">
                                {selected.session.targetRole || 'Interview Session'}
                            </h2>
                            {selected.session.targetCompany && (
                                <p className="text-sm text-muted mt-0.5">{selected.session.targetCompany}</p>
                            )}
                        </div>
                        {avg !== null && (
                            <span className={`font-mono font-bold text-2xl ${scoreColor(Number(avg))}`}>
                                {avg}<span className="text-sm text-gray-600">/10</span>
                            </span>
                        )}
                    </div>
                    <div className="flex flex-wrap gap-3 mt-4 text-xs text-gray-500">
                        <span className="font-mono">{selected.session.interviewType?.replace('_', ' ')}</span>
                        <span>·</span>
                        <span>{formatDate(selected.session.createdAt)}</span>
                        <span>·</span>
                        <span className={`px-2 py-0.5 rounded-full border ${statusBadge(selected.session.status)}`}>
                            {selected.session.status}
                        </span>
                    </div>
                </div>

                {/* Feedback list */}
                {selected.loading && (
                    <div className="text-center py-12 text-muted text-sm animate-pulse">Loading feedback…</div>
                )}
                {selected.error && (
                    <div className="text-red-400 text-sm bg-red-950/50 border border-red-800 rounded-xl p-4">
                        {selected.error}
                    </div>
                )}
                {!selected.loading && selected.feedback && selected.feedback.length === 0 && (
                    <p className="text-muted text-sm text-center py-8">No feedback recorded for this session.</p>
                )}
                {!selected.loading && selected.feedback && selected.feedback.map((f, idx) => (
                    <div key={idx} className="bg-surface border border-border rounded-2xl p-5 mb-4">
                        <div className="flex items-start justify-between gap-4 mb-3">
                            <p className="text-sm text-gray-300 font-medium leading-relaxed flex-1">{f.question}</p>
                            <span className={`font-mono font-bold text-lg flex-shrink-0 ${scoreColor(f.score)}`}>
                                {f.score}/10
                            </span>
                        </div>
                        {f.candidateAnswer && (
                            <div className="mb-3 pl-3 border-l-2 border-border">
                                <p className="text-xs text-gray-600 mb-1 uppercase tracking-wider font-semibold">Your Answer</p>
                                <p className="text-sm text-gray-400 leading-relaxed">{f.candidateAnswer}</p>
                            </div>
                        )}
                        {f.feedbackGood && (
                            <p className="text-sm text-green-400 mb-1">
                                <span className="mr-1.5">✓</span>{f.feedbackGood}
                            </p>
                        )}
                        {f.feedbackImprove && (
                            <p className="text-sm text-amber-300 mb-2">
                                <span className="mr-1.5">💡</span>{f.feedbackImprove}
                            </p>
                        )}
                        {f.psychologyNote && (
                            <p className="italic text-xs text-muted mt-2 leading-relaxed">{f.psychologyNote}</p>
                        )}
                    </div>
                ))}
            </div>
        );
    }

    // Session list
    return (
        <div className="max-w-3xl mx-auto px-4 py-10">
            <div className="flex items-center justify-between mb-8">
                <div>
                    <h1 className="text-2xl font-bold text-white">History</h1>
                    <p className="text-muted text-sm mt-1">Your past interview sessions</p>
                </div>
                <a href="#/"
                    className="bg-accent hover:bg-amber-400 text-black font-bold px-4 py-2 rounded-xl text-sm transition-colors">
                    New Interview
                </a>
            </div>

            {loading && (
                <div className="text-center py-16 text-muted text-sm animate-pulse">Loading…</div>
            )}

            {error && (
                <div className="text-red-400 text-sm bg-red-950/50 border border-red-800 rounded-xl p-4">
                    {error}
                </div>
            )}

            {!loading && !error && sessions.length === 0 && (
                <div className="text-center py-16">
                    <p className="text-gray-600 text-sm">No sessions yet.</p>
                    <a href="#/" className="text-accent hover:underline text-sm mt-2 inline-block">
                        Start your first interview →
                    </a>
                </div>
            )}

            <div className="space-y-3">
                {sessions.map(session => (
                    <button key={session.id} onClick={() => openSession(session)}
                        className="w-full text-left bg-surface border border-border hover:border-accent/50 rounded-2xl p-5 transition-colors group">
                        <div className="flex items-start justify-between gap-4">
                            <div className="flex-1 min-w-0">
                                <p className="font-semibold text-gray-200 group-hover:text-white transition-colors truncate">
                                    {session.targetRole || 'Interview Session'}
                                </p>
                                <p className="text-sm text-muted mt-0.5 truncate">
                                    {session.targetCompany || 'No company specified'}
                                </p>
                            </div>
                            <div className="flex flex-col items-end gap-1.5 flex-shrink-0">
                                <span className={`text-xs px-2 py-0.5 rounded-full border ${statusBadge(session.status)}`}>
                                    {session.status}
                                </span>
                                <span className="text-xs text-muted font-mono">{formatDate(session.createdAt)}</span>
                            </div>
                        </div>
                        <div className="flex items-center gap-3 mt-3 text-xs text-gray-600">
                            <span className="font-mono">{session.interviewType?.replace('_', ' ')}</span>
                            <span>·</span>
                            <span>{session.difficulty}</span>
                        </div>
                    </button>
                ))}
            </div>
        </div>
    );
}
window.HistoryScreen = HistoryScreen;
