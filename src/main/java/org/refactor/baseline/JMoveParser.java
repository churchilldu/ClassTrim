package org.refactor.baseline;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;

import java.nio.file.Path;
import java.util.List;

@Slf4j
public class JMoveParser implements RefactorSuggestionParser {

    @Override
    public List<Pair<JavaMethod, JavaClass>> parse(Path file, JavaProject project) {
        return null;
    }
}


