package com.interviewlab.ai;

/**
 * Decorator (Decorator pattern) — wraps every AiProviderStrategy returned by
 * AIProviderFactory so every provider call passes through AIRequestQueue (concurrency
 * cap) and AiBudgetGuard (daily global cap), without any provider implementation
 * (Ollama/Gemini/Claude/OpenAI) needing to know about either concern. Single choke
 * point: no per-provider duplication of queue/budget logic. See ADR-011.
 */
final class QueuedAiProviderStrategy implements AiProviderStrategy {

    private final AiProviderStrategy delegate;
    private final AIRequestQueue     aiRequestQueue;
    private final AiBudgetGuard      aiBudgetGuard;

    QueuedAiProviderStrategy(AiProviderStrategy delegate, AIRequestQueue aiRequestQueue, AiBudgetGuard aiBudgetGuard) {
        this.delegate       = delegate;
        this.aiRequestQueue = aiRequestQueue;
        this.aiBudgetGuard  = aiBudgetGuard;
    }

    @Override
    public String generate(String prompt, AIOptions options) {
        return aiRequestQueue.execute(() -> {
            aiBudgetGuard.checkAndIncrement();
            return delegate.generate(prompt, options);
        });
    }

    @Override
    public String generateJson(String prompt, AIOptions options) {
        return aiRequestQueue.execute(() -> {
            aiBudgetGuard.checkAndIncrement();
            return delegate.generateJson(prompt, options);
        });
    }

    @Override
    public AiProvider providerName() {
        return delegate.providerName();
    }

    // Package-private — lets AIProviderFactoryTest assert routing without stubbing
    // providerName() on every provider mock just to prove identity.
    AiProviderStrategy delegate() {
        return delegate;
    }
}
