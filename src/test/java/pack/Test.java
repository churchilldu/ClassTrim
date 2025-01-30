package pack;

import org.apache.commons.lang3.StringUtils;

public interface Test {
    class Clazz {
        public static final String SUPER = "pack/SuperClass";
    }

    class Method {
        public static final String OVERRIDE = "pack/SuperClass";
        public static final String GETTER = "pack/SuperClass";
        public static final String SETTER = "pack/SuperClass";
    }
}


interface AInterface {
    void interfaceMethod();
}

class SuperClass {
    public void superMethod_1() {
    }
}

class A extends SuperClass implements Comparable<A>, AInterface {
    private String str;

    public A() {
        this.str = str;
    }

    public String getStr() {
        return str;
    }

    /* Overload */
    public void setStr(Integer str) {
        this.str = String.valueOf(str);
    }

    public void setStr(String str) {
        this.str = str;
    }

    public boolean isSingle_A() {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public void interfaceMethod() {
    }


    private void method_A1_invoke_super_method1() {
        superMethod_1();
    }

    private void method_A2_external_lib() {
        StringUtils.equals(this.str, "");
    }

    private void method_A3_getMethod(int a, long[] bArr, String c) {
    }

    @Override
    public int compareTo(A o) {
        return 0;
    }

    static class A_InnerClass {
        public void A_innerMethod1() {
            new ClassB().method_B1();
        }
    }
}


class A1 extends A {}
class A2 extends A1 {
    public void method_A2() {
        superMethod_1();
    }
}

class ClassB {
    public ClassB() {
        A a = new A();
    }

    public void method_B1() {
    }

    public void method_B2() {
        new A.A_InnerClass().A_innerMethod1();
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

