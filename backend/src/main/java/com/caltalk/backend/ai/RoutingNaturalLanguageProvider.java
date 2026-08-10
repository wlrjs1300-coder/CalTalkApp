package com.caltalk.backend.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class RoutingNaturalLanguageProvider implements NaturalLanguageProvider {
    private static final Logger log = LoggerFactory.getLogger(RoutingNaturalLanguageProvider.class);
    private final OllamaNaturalLanguageProvider ollama;
    private final OpenAiNaturalLanguageProvider openAi;
    private final OpenAiFallbackBudget budget;

    public RoutingNaturalLanguageProvider(OllamaNaturalLanguageProvider ollama,
            OpenAiNaturalLanguageProvider openAi, OpenAiFallbackBudget budget) {
        this.ollama = ollama;
        this.openAi = openAi;
        this.budget = budget;
    }

    @Override
    public ScheduleCommand analyze(String utterance, AnalysisContext context) {
        if (!ollama.available()) {
            if (!openAi.available() || !budget.tryAcquire()) {
                throw new NaturalLanguageProviderException("No natural-language provider is available.");
            }
            log.info("Using OpenAI because the local provider is disabled");
            return openAi.analyze(utterance, context);
        }
        try {
            ScheduleCommand local = ollama.analyze(utterance, context);
            if (!needsFallback(local)) return local;
            return fallback(utterance, context, local);
        } catch (NaturalLanguageProviderException localFailure) {
            if (!openAi.available() || !budget.tryAcquire()) throw localFailure;
            log.info("Using OpenAI fallback because the local provider failed");
            return openAi.analyze(utterance, context);
        }
    }

    private ScheduleCommand fallback(String utterance, AnalysisContext context, ScheduleCommand local) {
        if (!openAi.available() || !budget.tryAcquire()) return local;
        try {
            log.info("Using OpenAI fallback for an unsupported local interpretation");
            return openAi.analyze(utterance, context);
        } catch (NaturalLanguageProviderException ignored) {
            log.warn("OpenAI fallback failed; preserving the local interpretation");
            return local;
        }
    }

    private static boolean needsFallback(ScheduleCommand command) {
        return command.intent() == ScheduleIntent.UNKNOWN || command.status() == CommandStatus.UNSUPPORTED;
    }
}
