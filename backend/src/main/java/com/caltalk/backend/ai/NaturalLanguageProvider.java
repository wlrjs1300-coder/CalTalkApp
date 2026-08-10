package com.caltalk.backend.ai;

public interface NaturalLanguageProvider {
    ScheduleCommand analyze(String utterance, AnalysisContext context);
}
