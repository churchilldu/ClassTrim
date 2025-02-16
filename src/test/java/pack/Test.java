package pack;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.Builder;
import org.apache.commons.lang3.exception.ContextedException;
import org.apache.commons.lang3.time.DateParser;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.time.TimeZones;
import org.apache.commons.math3.util.Pair;

import java.io.IOException;
import java.time.LocalDate;
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
    public void method_B1() {
        StringUtils.equals("", "");
    }

    public void method_B2() {
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
    public void method_D1() {
        this.setTime(0L);
    }
}


class E0 {
    public void super_method(){}
}

/**
 * Duplicate dependencies.
 * super class 1
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

/**
 * Test type from jdk.
 * super class 0
 * interface 0
 * Exception 0
 * Declaring method arguments type 0
 * Declaring method return type 0
 * field type 0
 * invoked methods class 0
 */
class F {
    public static final Integer CBO = 0;

    private int anInt;
    private long aLong;
    private String string;
    private Integer integer;
    private Long aBoxedLong;
    private Date date;
    private LocalDate localDate;
    private boolean aBoolean;
    private byte aByte;
    private short aShort;

    public Integer method_F1() {
        return null;
    }
    public void method_F2() throws RuntimeException {}
    public Long method_F3(int anInt,
                          long aLong) {
        return null;
    }
    public Long method_F4(Integer anInt,
                          Long aLong,
                          String string) {
        return null;
    }
}

/**
 * Test type from external project.
 * super class 0
 * interface 0
 * Exception 1
 * Declaring method arguments type 1
 * Declaring method return type 1
 * field type 1
 * invoked methods class 0
 */
class G {
    public static final Integer CBO = 4;

    private StopWatch stopWatch;

    public TimeZones method_G1(DateParser parser) {
        return null;
    }
    public void method_G2() throws ContextedException {}
}

/**
 * Test extends third-libs.
 * super class 1
 * interface 1
 * Exception 0
 * Declaring method arguments type 0
 * Declaring method return type 0
 * field type 0
 * invoked methods class 0
 */
class H extends Pair<Object, Object> implements Builder<Object> {
    public static final Integer CBO = 2;

    public H(Object o, Object o2) {
        super(o, o2);
    }

    @Override
    public Object build() {
        return Pair.create(null, null);
    }
}


/**
 * Test Array argument and return type.
 * super class 0
 * interface 0
 * Exception 0
 * Declaring method arguments type 0
 * Declaring method return type 0
 * field type 0
 * invoked methods class 0
 */
@SuppressWarnings("unused")
class I {
    public static final Integer CBO = 0;

    private int[] ints;
    private Integer[] integers;

    public long[] method_I1(int[] ints, byte[] bytes){
        return null;
    }

    public Long[] method_I2(Integer[] ints,
                            String[] strings,
                            Object[] objects,
                            I[] is){
        return null;
    }
}

/**
 * Get methods from super class,
 * but super class haven't been set yet.
 */
class J extends K {
    public static final Integer CBO = 2;
    public J() {
        register(I.class);
        register(H.class);
    }
}

class K0 {
    public void register(Class<?> clazz) {}
}

class K extends K0 {
    public void method_K1() {
        register(K.class);
    }
}

/**
 * Basic RFC calculation
 */
class L0 {
    public void method_L0() {}
}
class L {
    public static final Integer RFC = 3;
    public void method_L1() {
        new L0().method_L0();
        method_L2();
    }
    public void method_L2() {
        new L0().method_L0();
        method_L1();
    }
}

/**
 * RFC include method from Jdk
 */
class M {
    public static final Integer RFC = 5;
    public void method_M1() {}
}

/**
 * RFC include method from Jdk
 */
