package pack;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Objects;

@SuppressWarnings("unused")
public interface Test {}

/**
 * Test computation CBO
 */

interface AInterface {}

class SuperClass {}

class AException extends RuntimeException {}

class ArgumentType {}

class ReturnType {}

class FieldType {}

class Caller {
    public void method() {}
}

/**
 * Basic computation.
 * super class 1
 * interface 1
 * Exception 1
 * Declaring method arguments type 1
 * Declaring method return type 1
 * field type 1
 * invoked methods class 1
 * CBO = 7
 */
class A extends SuperClass implements AInterface {
    private FieldType field;

    public void throwException() throws IOException {
        throw new AException();
    }

    public ReturnType method_A1(ArgumentType argument) {
        return null;
    }

    public void method_A2() {
        new Caller().method();
    }
}

/**
 * Invoke methods of Jdk or external lib.
 * super class 0
 * interface 0
 * Exception 0
 * Declaring method arguments type 0
 * Declaring method return type 0
 * field type 0
 * invoked methods class 1
 * CBO = 1
 */
class B implements Comparable<B> {
    public void invoke_external_method() {
        StringUtils.equals("", "");
    }

    public void invoke_jdk_method() {
        Objects.hash("");
    }
    @Override
    public int compareTo(B o) {
        return 0;
    }
}


class A1 extends A {
    static class A_InnerClass {
        public void A_innerMethod1() {
        }
    }
}

class A2 extends A1 {
}


enum Enum {
    ENUM1,
    ENUM2;
    private Enum() {}
}

