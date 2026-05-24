package org.classtrim.core.engine;

import lombok.Getter;
import org.classtrim.core.model.JavaClass;
import org.classtrim.core.model.JavaMethod;

@Getter
public class RefactoringSuggestion {
    private final JavaMethod method;
    private final JavaClass sourceClass;
    private final JavaClass targetClass;

    public RefactoringSuggestion(JavaMethod method, JavaClass sourceClass, JavaClass targetClass) {
        this.method = method;
        this.sourceClass = sourceClass;
        this.targetClass = targetClass;
    }
}
