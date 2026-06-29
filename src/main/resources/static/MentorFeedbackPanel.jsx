const React = window.React;

function MentorFeedbackPanel({ feedback }) {
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
        <div className="flex flex-col gap-3 p-4 h-full overflow-y-auto">
            <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Mentor</span>
                <span className={`px-2.5 py-1 rounded-lg border font-mono font-bold text-sm ${scoreColor}`}>
                    {score}/10
                </span>
            </div>

            {feedback.feedbackGood && (
                <div className="bg-green-900/20 border border-green-800/50 rounded-xl p-3">
                    <div className="flex gap-2 items-start">
                        <span className="text-green-400 mt-0.5 flex-shrink-0">✓</span>
                        <p className="text-green-300 text-sm leading-relaxed">{feedback.feedbackGood}</p>
                    </div>
                </div>
            )}

            {feedback.feedbackImprove && (
                <div className="bg-amber-900/20 border border-amber-800/50 rounded-xl p-3">
                    <div className="flex gap-2 items-start">
                        <span className="text-accent mt-0.5 flex-shrink-0">💡</span>
                        <p className="text-amber-200 text-sm leading-relaxed">{feedback.feedbackImprove}</p>
                    </div>
                </div>
            )}

            {feedback.psychologyNote && (
                <p className="italic text-xs text-muted px-1 leading-relaxed">{feedback.psychologyNote}</p>
            )}
        </div>
    );
}
window.MentorFeedbackPanel = MentorFeedbackPanel;
