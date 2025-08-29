package org.refactor.common;

import lombok.Getter;
import org.refactor.baseline.JDeodorantParser;
import org.refactor.baseline.JMoveParser;
import org.refactor.baseline.REsolutionParser;
import org.refactor.baseline.RefactorSuggestionParser;

@Getter
public enum BaselineEnum {
    JDEODORANT ("JDeodorant", JDeodorantParser.class),
    JMOVE ("JMove", JMoveParser.class),
    RESOLUTION ("REsolution", REsolutionParser.class);

    private final String name;
    private final Class<? extends RefactorSuggestionParser> parserClass;

    BaselineEnum(String name, Class<? extends RefactorSuggestionParser> parserClass) {
        this.name = name;
        this.parserClass = parserClass;
    }
}


