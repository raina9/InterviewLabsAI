const React = window.React;

function MentorFeedbackPanel({ feedback }) {
    const [showRefined, setShowRefined] = React.useState(false);
    const [showModel, setShowModel] = React.useState(false);

    if (!feedback) {
        return (
            <div className="flex flex-col items-center justify-center h-full text-center px-6 py-12 text-muted">
                <div className="text-3xl mb-3 opacity-40">💬</div>
                <p className="text-sm font-medium text-gray-500">Answer a question</p>
                <p className="text-xs text-gray-600 mt-1">Feedback will appear here</p>
            </div>
        );
    }

    const score = feedback.score;
    const scoreColor = score >= 7
        ? 'text-green-400 bg-green-900/40 border-green-800'
        : score >= 4
        ? 'text-accent bg-amber-900/30 border-amber-800'
        : 'text-red-400 bg-red-900/30 border-red-800';

    return (
        <div className="flex flex-col gap-4 p-4 h-full overflow-y-auto">
            {/* Score badge */}
            <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Mentor Feedback</span>
                <span className={`px-2.5 py-1 rounded-lg border font-mono font-bold text-sm ${scoreColor}`}>
                    {score}/10
                </span>
            </div>

            {/* feedbackGood */}
            {feedback.feedbackGood && (
                <div className="bg-green-900/20 border border-green-800/50 rounded-xl p-3">
                    <div className="flex gap-2 items-start">
                        <span className="text-green-400 mt-0.5 flex-shrink-0">✓</span>
                        <p className="text-green-300 text-sm leading-relaxed">{feedback.feedbackGood}</p>
                    </div>
                </div>
            )}

            {/* feedbackImprove */}
            {feedback.feedbackImprove && (
                <div className="bg-amber-900/20 border border-amber-800/50 rounded-xl p-3">
                    <div className="flex gap-2 items-start">
                        <span className="text-accent mt-0.5 flex-shrink-0">💡</span>
                        <p className="text-amber-200 text-sm leading-relaxed">{feedback.feedbackImprove}</p>
                    </div>
                </div>
            )}

            {/* Psychology note */}
            {feedback.psychologyNote && (
                <p className="italic text-xs text-muted px-1 leading-relaxed">{feedback.psychologyNote}</p>
            )}

            {/* Refined answer (collapsible) */}
            {feedback.refinedAnswer && (
                <div className="border border-border rounded-xl overflow-hidden">
                    <button
                        onClick={() => setShowRefined(!showRefined)}
                        className="w-full flex items-center justify-between px-3 py-2.5 text-xs font-medium text-gray-400 hover:text-gray-200 hover:bg-surface transition-colors">
                        <span>Refined Answer</span>
                        <span className="font-mono">{showRefined ? '−' : '+'}</span>
                    </button>
                    {showRefined && (
                        <div className="px-3 pb-3 pt-1 text-sm text-gray-300 font-mono leading-relaxed whitespace-pre-wrap border-t border-border">
                            {feedback.refinedAnswer}
                        </div>
                    )}
                </div>
            )}

            {/* Model answer (collapsible) */}
            {feedback.modelAnswer && (
                <div className="border border-border rounded-xl overflow-hidden">
                    <button
                        onClick={() => setShowModel(!showModel)}
                        className="w-full flex items-center justify-between px-3 py-2.5 text-xs font-medium text-gray-400 hover:text-gray-200 hover:bg-surface transition-colors">
                        <span>Model Answer</span>
                        <span className="font-mono">{showModel ? '−' : '+'}</span>
                    </button>
                    {showModel && (
                        <div className="px-3 pb-3 pt-1 text-sm text-gray-300 font-mono leading-relaxed whitespace-pre-wrap border-t border-border">
                            {feedback.modelAnswer}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
window.MentorFeedbackPanel = MentorFeedbackPanel;
