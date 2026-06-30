const React = window.React;

function TopicBar({ topic, selfRating, level }) {
    const pct = (selfRating / 10) * 100;
    const barColor = selfRating >= 7
        ? 'bg-green-500'
        : selfRating >= 4
        ? 'bg-amber-500'
        : 'bg-red-500';
    const levelColor = selfRating >= 7
        ? 'text-green-400'
        : selfRating >= 4
        ? 'text-amber-400'
        : 'text-red-400';

    return (
        <div className="space-y-1.5">
            <div className="flex items-center justify-between text-sm">
                <span className="text-gray-300 font-medium">{topic}</span>
                <div className="flex items-center gap-3">
                    <span className={`text-xs font-semibold ${levelColor}`}>{level}</span>
                    <span className="font-mono text-xs text-gray-400">{selfRating}/10</span>
                </div>
            </div>
            <div className="h-2 bg-gray-800 rounded-full overflow-hidden">
                <div className={`h-full rounded-full transition-all ${barColor}`} style={{ width: `${pct}%` }} />
            </div>
        </div>
    );
}

function ProfileScreen() {
    const [report, setReport]           = React.useState(null);
    const [curriculum, setCurriculum]   = React.useState(null);
    const [loading, setLoading]         = React.useState(true);
    const [planLoading, setPlanLoading] = React.useState(false);
    const [error, setError]             = React.useState(null);
    const [planError, setPlanError]     = React.useState(null);

    const userId = window.__user && window.__user.id;

    React.useEffect(() => {
        if (!userId) { setLoading(false); return; }
        window.apiFetch(`/api/v1/assessment/report/${userId}`)
            .then(res => {
                if (res.status === 404) return null; // no assessment yet
                if (!res.ok) throw new Error('Failed to load proficiency data');
                return res.json();
            })
            .then(data => setReport(data))
            .catch(err => setError(err.message))
            .finally(() => setLoading(false));
    }, [userId]);

    async function generatePlan() {
        if (planLoading || !userId) return;
        setPlanLoading(true);
        setPlanError(null);
        try {
            const res = await window.apiFetch(`/api/v1/curriculum/${userId}`);
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Failed to generate learning plan');
            }
            setCurriculum(await res.json());
        } catch (err) {
            setPlanError(err.message);
        } finally {
            setPlanLoading(false);
        }
    }

    function priorityColor(priority) {
        if (priority === 'HIGH')   return 'bg-red-900/40 text-red-400 border-red-800';
        if (priority === 'MEDIUM') return 'bg-amber-900/30 text-amber-400 border-amber-800';
        return 'bg-gray-800 text-gray-400 border-gray-700';
    }

    if (loading) {
        return (
            <div className="flex items-center justify-center py-24">
                <div className="w-6 h-6 border-2 border-border border-t-accent rounded-full animate-spin" />
            </div>
        );
    }

    return (
        <div className="max-w-2xl mx-auto px-4 py-12 space-y-8">
            <div>
                <h1 className="text-2xl font-bold text-white">My Profile</h1>
                <p className="text-muted text-sm mt-1">Topic proficiency based on your self-assessment</p>
            </div>

            {error && (
                <div className="bg-red-950/50 border border-red-800 rounded-2xl p-4 text-red-400 text-sm">
                    {error}
                </div>
            )}

            {!report && !error && (
                <div className="bg-surface border border-border rounded-2xl p-8 text-center space-y-3">
                    <p className="text-gray-400 text-sm">No assessment data yet.</p>
                    <p className="text-muted text-xs">Complete a self-assessment to see your proficiency chart.</p>
                    <a href="#/"
                        className="inline-block mt-2 text-accent hover:text-amber-400 text-sm font-medium transition-colors">
                        Start an interview session →
                    </a>
                </div>
            )}

            {report && (
                <>
                    {/* Overall level */}
                    <div className="bg-surface border border-border rounded-2xl px-5 py-4 flex items-center justify-between">
                        <span className="text-sm font-semibold text-gray-400 uppercase tracking-wider">Overall Level</span>
                        <span className={`font-bold text-lg ${
                            report.overallLevel === 'Senior' ? 'text-green-400' :
                            report.overallLevel === 'Intermediate' ? 'text-amber-400' : 'text-red-400'
                        }`}>{report.overallLevel}</span>
                    </div>

                    {/* Topic bars */}
                    <div className="bg-surface border border-border rounded-2xl p-5 space-y-4">
                        <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Topics</p>
                        {report.topics.map((t, i) => (
                            <TopicBar key={i} topic={t.topic} selfRating={t.selfRating} level={t.level} />
                        ))}
                    </div>

                    {/* Critical gaps + Quick wins */}
                    {(report.criticalGaps.length > 0 || report.quickWins.length > 0) && (
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            {report.criticalGaps.length > 0 && (
                                <div className="bg-surface border border-red-800/50 rounded-2xl p-4">
                                    <p className="text-xs font-semibold text-red-400 uppercase tracking-wider mb-2">Critical Gaps</p>
                                    <ul className="space-y-1">
                                        {report.criticalGaps.map((g, i) => (
                                            <li key={i} className="text-sm text-gray-300">• {g}</li>
                                        ))}
                                    </ul>
                                </div>
                            )}
                            {report.quickWins.length > 0 && (
                                <div className="bg-surface border border-amber-800/50 rounded-2xl p-4">
                                    <p className="text-xs font-semibold text-amber-400 uppercase tracking-wider mb-2">Quick Wins</p>
                                    <ul className="space-y-1">
                                        {report.quickWins.map((g, i) => (
                                            <li key={i} className="text-sm text-gray-300">• {g}</li>
                                        ))}
                                    </ul>
                                </div>
                            )}
                        </div>
                    )}

                    {/* Generate learning plan */}
                    {!curriculum && (
                        <button onClick={generatePlan} disabled={planLoading}
                            className="w-full bg-accent hover:bg-amber-400 disabled:opacity-40 text-black font-bold py-3 rounded-xl text-sm transition-colors">
                            {planLoading ? 'Generating your learning plan…' : 'Generate my learning plan →'}
                        </button>
                    )}
                    {planError && (
                        <div className="bg-red-950/50 border border-red-800 rounded-2xl p-3 text-red-400 text-sm">
                            {planError}
                        </div>
                    )}
                </>
            )}

            {/* Curriculum plan */}
            {curriculum && (
                <div className="space-y-4">
                    <div className="flex items-center justify-between">
                        <h2 className="text-lg font-bold text-white">Learning Plan</h2>
                        <div className="flex items-center gap-3">
                            <span className="text-xs text-gray-500 font-mono">{curriculum.estimatedWeeks} weeks</span>
                            <span className="text-xs text-accent font-medium">{curriculum.focus}</span>
                        </div>
                    </div>

                    {curriculum.items.map((item, i) => (
                        <div key={i} className="bg-surface border border-border rounded-2xl p-5 space-y-2">
                            <div className="flex items-start justify-between gap-3">
                                <h3 className="font-semibold text-gray-200">{item.topic}</h3>
                                <div className="flex items-center gap-2 flex-shrink-0">
                                    <span className={`text-xs border px-2 py-0.5 rounded-full font-mono ${priorityColor(item.priority)}`}>
                                        {item.priority}
                                    </span>
                                    <span className="text-xs text-muted font-mono">{item.estimatedDays}d</span>
                                </div>
                            </div>
                            <p className="text-sm text-gray-400 leading-relaxed">{item.whyThisMatters}</p>
                            {item.keyConceptsToCover && item.keyConceptsToCover.length > 0 && (
                                <div className="flex flex-wrap gap-1.5 mt-2">
                                    {item.keyConceptsToCover.map((c, j) => (
                                        <span key={j} className="text-xs bg-gray-800 text-gray-400 px-2 py-0.5 rounded-full font-mono">
                                            {c}
                                        </span>
                                    ))}
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
window.ProfileScreen = ProfileScreen;
