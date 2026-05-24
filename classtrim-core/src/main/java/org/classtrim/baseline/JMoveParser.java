package org.classtrim.baseline;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.classtrim.core.model.JavaClass;
import org.classtrim.core.model.JavaMethod;
import org.classtrim.core.model.JavaProject;

import java.nio.file.Path;
import java.util.List;

@Slf4j
public class JMoveParser extends JDeodorantParser {

    @Override
    public List<Pair<JavaMethod, JavaClass>> parse(Path file, JavaProject project) {
        return super.parse(file, project);
    }
}


