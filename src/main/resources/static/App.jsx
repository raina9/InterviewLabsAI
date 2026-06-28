const React         = window.React;
const ReactDOM       = window.ReactDOM;
const IntakeForm     = window.IntakeForm;
const InterviewScreen = window.InterviewScreen;
const HistoryScreen  = window.HistoryScreen;
const EnglishCoach   = window.EnglishCoach;

function App() {
    const [authState, setAuthState] = React.useState('loading'); // loading | authenticated | error
    const [user, setUser]           = React.useState(null);
    const [route, setRoute]         = React.useState(null);

    // Auth check on mount — reads result of the pre-fetch in index.html
    React.useEffect(() => {
        (window.__authReady || Promise.resolve())
            .then(() => {
                if (window.__user) {
                    setUser(window.__user);
                    setAuthState('authenticated');
                    parseRoute();
                } else {
                    setAuthState('error');
                }
            })
            .catch(() => setAuthState('error'));
    }, []);

    // Hash change listener
    React.useEffect(() => {
        const onHash = () => parseRoute();
        window.addEventListener('hashchange', onHash);
        return () => window.removeEventListener('hashchange', onHash);
    }, []);

    function parseRoute() {
        const hash = window.location.hash;
        const interviewMatch = hash.match(/^#\/interview\/([^/]+)/);
        if (interviewMatch) {
            setRoute({ name: 'interview', sessionId: interviewMatch[1] });
        } else if (hash === '#/history') {
            setRoute({ name: 'history' });
        } else if (hash === '#/english') {
            setRoute({ name: 'english' });
        } else {
            setRoute({ name: 'intake' });
        }
    }

    function handleSessionStarted(sessionId, voiceEnabled, firstQuestion, totalQuestions) {
        setRoute({ name: 'interview', sessionId, voiceEnabled, firstQuestion, totalQuestions });
        window.location.hash = `#/interview/${sessionId}`;
    }

    async function handleLogout() {
        try { await window.apiFetch('/api/v1/auth/logout', { method: 'POST' }); } catch (_) {}
        window.location.href = '/oauth2/authorization/google';
    }

    // Loading state
    if (authState === 'loading') {
        return (
            <div className="flex items-center justify-center min-h-screen bg-base">
                <div className="text-center">
                    <div className="w-6 h-6 border-2 border-border border-t-accent rounded-full animate-spin mx-auto mb-3" />
                    <p className="text-muted text-sm font-mono">Checking session…</p>
                </div>
            </div>
        );
    }

    // Error state
    if (authState === 'error') {
        return (
            <div className="flex items-center justify-center min-h-screen bg-base">
                <div className="text-center">
                    <p className="text-gray-400 text-sm mb-3">Unable to reach the server.</p>
                    <button onClick={() => window.location.reload()}
                        className="text-accent hover:underline text-sm font-mono">
                        Retry →
                    </button>
                </div>
            </div>
        );
    }

    const isInterview = route?.name === 'interview';

    return (
        <div className="min-h-screen bg-base text-gray-200 font-sans">

            {/* Header */}
            <header className="flex items-center justify-between px-6 h-14 border-b border-border bg-surface/60 backdrop-blur-sm sticky top-0 z-10">
                <a href="#/" className="font-mono font-bold text-accent tracking-tight text-base hover:text-amber-400 transition-colors">
                    Interview Lab
                </a>

                {user && (
                    <div className="flex items-center gap-4">
                        {!isInterview && (
                            <>
                                <a href="#/history"
                                    className={`text-sm transition-colors ${route?.name === 'history' ? 'text-gray-200' : 'text-gray-500 hover:text-gray-200'}`}>
                                    History
                                </a>
                                <a href="#/english"
                                    className={`text-sm transition-colors ${route?.name === 'english' ? 'text-gray-200' : 'text-gray-500 hover:text-gray-200'}`}>
                                    English Coach
                                </a>
                            </>
                        )}
                        <span className="text-xs text-muted hidden sm:block">
                            {user.name || user.email}
                        </span>
                        <button onClick={handleLogout}
                            className="text-xs text-gray-600 hover:text-red-400 border border-border hover:border-red-800 px-3 py-1.5 rounded-lg transition-colors font-mono">
                            Logout
                        </button>
                    </div>
                )}
            </header>

            {/* Route */}
            <main>
                {(!route || route.name === 'intake') && (
                    <IntakeForm user={user} onSessionStarted={handleSessionStarted} />
                )}
                {route?.name === 'interview' && (
                    <InterviewScreen
                        sessionId={route.sessionId}
                        voiceEnabled={route.voiceEnabled || false}
                        firstQuestion={route.firstQuestion || null}
                        totalQuestions={route.totalQuestions || 10} />
                )}
                {route?.name === 'history' && (
                    <HistoryScreen />
                )}
                {route?.name === 'english' && (
                    <EnglishCoach />
                )}
            </main>
        </div>
    );
}

window.App = App;

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);
