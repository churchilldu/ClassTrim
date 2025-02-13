package pack;

import java.io.IOException;

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
 * CBO = super class 1
 * interface 1
 * Exception 1
 * Declaring method arguments type 1
 * Declaring method return type 1
 * field type 1
 * invoked methods class 1
 * = 7
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


class A1 extends A {
    static class A_InnerClass {
        public void A_innerMethod1() {
            new B().method_B1();
        }
    }
}

class A2 extends A1 {
}

class B {
    public B() {
        A a = new A();
    }

    public void method_B1() {
    }

    public A method_B3() {
        return new A();
    }

    public void method_B4(A a) {
    }
}


enum Enum {
    ENUM1,
    ENUM2;
    private Enum() {}
}

