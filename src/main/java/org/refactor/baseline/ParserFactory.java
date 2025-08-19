package org.refactor.baseline;

import org.refactor.common.BaselineEnum;

public final class ParserFactory {
    private ParserFactory() {}

    public static RefactorSuggestionParser getParser(BaselineEnum baseline) {
        try {
            return baseline.getParserClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot create parser for " + baseline.name(), e);
        }
    }
}


