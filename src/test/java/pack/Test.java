package pack;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

@SuppressWarnings("unused")
public interface Test {}

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
 * Basic and thorough computation.
 * super class 1
 * interface 1
 * Exception 1
 * Declaring method arguments type 1
 * Declaring method return type 1
 * field type 1
 * invoked methods class 1
 */
class A extends SuperClass implements AInterface {
    public static final Integer CBO = 7;
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
 */
class B implements Comparable<B> {
    public static final Integer CBO = 1;
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


class C0 {
    public void method_C0() {}
}
/**
 * Invoke methods from super class.
 * super class 1
 * interface 0
 * Exception 0
 * Declaring method arguments type 0
 * Declaring method return type 0
 * field type 0
 * invoked methods class 1
 */
class C extends C0 {
    public static final Integer CBO = 1;
    public void method_C() {
        this.method_C0();
    }
}

/**
 * Invoke methods from JDK super class.
 * super class 1
 * interface 0
 * Exception 0
 * Declaring method arguments type 0
 * Declaring method return type 0
 * field type 0
 * invoked methods class 1
 * CBO = 0 because super class is from jdk
 */
class D extends Date {
    public static final Integer CBO = 0;
    public void invoke_inherited_external_method() {
        this.setTime(0L);
    }
}


class E0 {
    public void super_method(){}
}

/**
 * Duplicate dependencies.
 * super class 0
 * interface 0
 * Exception 1
 * Declaring method arguments type 1
 * Declaring method return type 1
 * field type 1
 * invoked methods class 1
 */
class E extends E0 {
    public static final Integer CBO = 5;

    private FieldType field1;
    private FieldType field2;
    public void method_E1(){
        this.super_method();
    }
    public void method_E2() throws AException{
        this.super_method();
    }
    public ReturnType method_E3() throws AException{
        return null;
    }
    public ReturnType method_E4(ArgumentType arg1,
                                ArgumentType arg2) {
        return this.method_E3();
    }
}