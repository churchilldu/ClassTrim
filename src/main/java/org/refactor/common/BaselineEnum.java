package org.refactor.common;

import lombok.Getter;
import org.refactor.baseline.JDeodorantParser;
import org.refactor.baseline.JMoveParser;
import org.refactor.baseline.REsolutionParser;
import org.refactor.baseline.RefactorSuggestionParser;

@Getter
public enum BaselineEnum {
    JDEODORANT (JDeodorantParser.class),
    JMOVE (JMoveParser.class),
    RESOLUTION (REsolutionParser.class);

    private final Class<? extends RefactorSuggestionParser> parserClass;

    BaselineEnum(Class<? extends RefactorSuggestionParser> parserClass) {
        this.parserClass = parserClass;
    }
}


