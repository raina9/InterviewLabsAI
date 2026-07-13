const React = window.React;

function IntakeForm({ user, onSessionStarted }) {
    const [form, setForm] = React.useState({
        targetRole: 'Software Engineer',
        targetCompany: 'Acme Corp',
        jdText: 'Looking for a Software Engineer with experience in backend development, system design, and distributed systems.',
        interviewType: 'TECHNICAL',
        experienceYears: '3',
        topicFocus: '',
        customPrompt: '',
        voiceEnabled: false
    });
    const [errors, setErrors] = React.useState({});
    const [loading, setLoading] = React.useState(false);
    const [apiError, setApiError] = React.useState(null);
    const [resume, setResume] = React.useState({ file: null, uploading: false, url: null, error: null });

    const interviewTypes = [
        { value: 'TECHNICAL',     label: 'Technical' },
        { value: 'HR',            label: 'HR' },
        { value: 'SYSTEM_DESIGN', label: 'System Design' },
        { value: 'BEHAVIOURAL',   label: 'Behavioural' }
    ];

    function validate() {
        const e = {};
        if (!form.targetRole.trim())  e.targetRole = 'Target role is required';
        if (!form.jdText.trim())      e.jdText     = 'Job description is required';
        return e;
    }

    function handleChange(e) {
        const { name, value, type, checked } = e.target;
        setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
        if (errors[name]) setErrors(prev => ({ ...prev, [name]: null }));
    }

    function toggleVoice() {
        setForm(prev => ({ ...prev, voiceEnabled: !prev.voiceEnabled }));
    }

    async function handleResumeChange(e) {
        const file = e.target.files[0];
        if (!file) return;
        setResume({ file, uploading: true, url: null, error: null });
        try {
            const formData = new FormData();
            formData.append('file', file);
            // Not window.apiFetch — it force-sets Content-Type: application/json, which
            // overrides the multipart boundary the browser would otherwise attach to FormData.
            const res = await fetch('/api/v1/me/resume', {
                method: 'POST',
                headers: { 'X-Dev-Token': localStorage.getItem('devToken') || 'dev-secret-local' },
                body: formData
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Resume upload failed');
            }
            const data = await res.json();
            setResume({ file, uploading: false, url: data.resumeUrl, error: null });
        } catch (err) {
            setResume({ file, uploading: false, url: null, error: err.message });
        }
    }

    async function handleSubmit(e) {
        e.preventDefault();
        const errs = validate();
        if (Object.keys(errs).length) { setErrors(errs); return; }

        setLoading(true);
        setApiError(null);

        try {
            // Profile updates are best-effort — failures don't block session creation
            if (form.experienceYears) {
                await window.apiFetch('/api/v1/profile', {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ experienceYears: Number(form.experienceYears) })
                }).catch(() => {});
            }
            if (form.customPrompt.trim()) {
                await window.apiFetch('/api/v1/profile/custom-prompt', {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ customPrompt: form.customPrompt.trim() })
                }).catch(() => {});
            }

            // Create session
            const sessionRes = await window.apiFetch('/api/v1/sessions', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    interviewType:  form.interviewType,
                    targetRole:     form.targetRole.trim(),
                    jdText:         form.jdText.trim(),
                    difficulty:     'MEDIUM',
                    targetCompany:  form.targetCompany.trim() || null,
                    topicFocus:     form.topicFocus.trim()    || null
                })
            });
            if (!sessionRes.ok) {
                const err = await sessionRes.json().catch(() => ({}));
                throw new Error(err.message || 'Failed to create session');
            }
            const session = await sessionRes.json();

            // Start interview
            const startRes = await window.apiFetch('/api/v1/interview/start', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sessionId: session.id })
            });
            if (!startRes.ok) {
                const err = await startRes.json().catch(() => ({}));
                throw new Error(err.message || 'Failed to start interview');
            }
            const startData = await startRes.json();

            onSessionStarted(session.id, form.voiceEnabled, startData.firstQuestion, startData.totalQuestions);

        } catch (err) {
            setApiError(err.message);
        } finally {
            setLoading(false);
        }
    }

    const inputBase = "w-full bg-surface border rounded-xl px-4 py-3 text-sm text-gray-200 focus:outline-none focus:border-accent placeholder-gray-600 transition-colors";
    const inputNormal = `${inputBase} border-border`;
    const inputError  = `${inputBase} border-red-600`;

    return (
        <div className="max-w-2xl mx-auto px-4 py-12">
            <div className="mb-8">
                <h1 className="text-2xl font-bold text-white">New Interview</h1>
                <p className="text-muted text-sm mt-1">Configure your mock session</p>
            </div>

            {apiError && (
                <div className="mb-6 p-4 bg-red-950/60 border border-red-800 rounded-xl text-red-400 text-sm">
                    {apiError}
                </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-5">

                {/* Resume upload */}
                <div>
                    <label htmlFor="resumeFile" className="block text-xs font-semibold text-gray-500 mb-2 uppercase tracking-wider">
                        Resume (PDF) <span className="text-gray-600 normal-case tracking-normal font-normal">(optional)</span>
                    </label>
                    <input id="resumeFile" type="file" accept=".pdf" onChange={handleResumeChange}
                        disabled={resume.uploading}
                        className="w-full text-sm text-gray-400 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:bg-surface file:text-gray-200 file:cursor-pointer cursor-pointer disabled:opacity-40" />
                    {resume.uploading && (
                        <p className="text-xs text-gray-500 mt-1.5">Uploading…</p>
                    )}
                    {resume.url && !resume.uploading && (
                        <p className="text-xs text-emerald-500 mt-1.5">Uploaded: {resume.file?.name}</p>
                    )}
                    {resume.error && (
                        <p className="text-red-400 text-xs mt-1.5">{resume.error}</p>
                    )}
                </div>

                {/* Interview type + Experience */}
                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label htmlFor="interviewType" className="block text-xs font-semibold text-gray-500 mb-2 uppercase tracking-wider">
                            Interview Type
                        </label>
                        <select id="interviewType" name="interviewType" value={form.interviewType} onChange={handleChange}
                            className={inputNormal + ' cursor-pointer'}>
                            {interviewTypes.map(t => (
                                <option key={t.value} value={t.value} style={{ background: '#13161B' }}>
                                    {t.label}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label htmlFor="experienceYears" className="block text-xs font-semibold text-gray-500 mb-2 uppercase tracking-wider">
                            Experience (years)
                        </label>
                        <input id="experienceYears" type="number" name="experienceYears" value={form.experienceYears}
                            onChange={handleChange} min="0" max="40" placeholder="e.g. 3"
                            className={inputNormal} />
                    </div>
                </div>

                {/* Target role */}
                <div>
                    <label htmlFor="targetRole" className="block text-xs font-semibold text-gray-500 mb-2 uppercase tracking-wider">
                        Target Role <span className="text-accent normal-case tracking-normal font-normal">*</span>
                    </label>
                    <input id="targetRole" type="text" name="targetRole" value={form.targetRole} onChange={handleChange}
                        placeholder="e.g. Senior Backend Engineer"
                        className={errors.targetRole ? inputError : inputNormal} />
                    {errors.targetRole && (
                        <p className="text-red-400 text-xs mt-1.5">{errors.targetRole}</p>
                    )}
                </div>

                {/* Company */}
                <div>
                    <label htmlFor="targetCompany" className="block text-xs font-semibold text-gray-500 mb-2 uppercase tracking-wider">
                        Company <span className="text-gray-600 normal-case tracking-normal font-normal">(optional)</span>
                    </label>
                    <input id="targetCompany" type="text" name="targetCompany" value={form.targetCompany} onChange={handleChange}
                        placeholder="e.g. Google"
                        className={inputNormal} />
                </div>

                {/* Job Description */}
                <div>
                    <label htmlFor="jdText" className="block text-xs font-semibold text-gray-500 mb-2 uppercase tracking-wider">
                        Job Description <span className="text-accent normal-case tracking-normal font-normal">*</span>
                    </label>
                    <textarea id="jdText" name="jdText" value={form.jdText} onChange={handleChange} rows={5}
                        placeholder="Paste the job description here…"
                        className={`${errors.jdText ? inputError : inputNormal} font-mono resize-none`} />
                    {errors.jdText && (
                        <p className="text-red-400 text-xs mt-1.5">{errors.jdText}</p>
                    )}
                </div>

                {/* Topic focus */}
                <div>
                    <label htmlFor="topicFocus" className="block text-xs font-semibold text-gray-500 mb-2 uppercase tracking-wider">
                        Topic Focus <span className="text-gray-600 normal-case tracking-normal font-normal">(optional)</span>
                    </label>
                    <input id="topicFocus" type="text" name="topicFocus" value={form.topicFocus} onChange={handleChange}
                        placeholder="e.g. Kafka, System Design, Spring Boot"
                        className={inputNormal} />
                </div>

                {/* Custom instruction */}
                <div>
                    <label htmlFor="customPrompt" className="block text-xs font-semibold text-gray-500 mb-2 uppercase tracking-wider">
                        Custom Instruction <span className="text-gray-600 normal-case tracking-normal font-normal">(optional — saved to profile)</span>
                    </label>
                    <textarea id="customPrompt" name="customPrompt" value={form.customPrompt} onChange={handleChange} rows={2}
                        placeholder="e.g. Focus on distributed systems. Push back on vague answers."
                        className={`${inputNormal} font-mono resize-none`} />
                </div>

                {/* Voice toggle */}
                <div className="flex items-center gap-3 py-1">
                    <button type="button" onClick={toggleVoice}
                        className={`relative w-11 h-6 rounded-full transition-colors focus:outline-none ${form.voiceEnabled ? 'bg-accent' : 'bg-gray-700'}`}>
                        <div className={`absolute top-1 w-4 h-4 bg-white rounded-full shadow transition-transform ${form.voiceEnabled ? 'translate-x-6' : 'translate-x-1'}`} />
                    </button>
                    <span className="text-sm text-gray-300">Voice input</span>
                    {!window.VoiceRecorder.isSupported && (
                        <span className="text-xs text-amber-600">(not supported in this browser)</span>
                    )}
                </div>

                <button type="submit" disabled={loading}
                    className="w-full bg-accent hover:bg-amber-400 disabled:opacity-40 text-black font-bold py-3.5 rounded-xl text-sm transition-colors mt-2">
                    {loading ? 'Starting…' : 'Start Interview →'}
                </button>
            </form>
        </div>
    );
}
window.IntakeForm = IntakeForm;
