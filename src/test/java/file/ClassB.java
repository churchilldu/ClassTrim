package file;


public class ClassB {
    public ClassB() {
        ClassA classA = new ClassA("");
    }

    public void method_B1() {
        new ClassA("").inheritMethod();
    }

    public void method_B2() {
        new ClassA("").new A_InnerClass().A_innerMethod1();
    }

    public ClassA method_B3() {
        return new ClassA("");
    }

    public void method_B4(ClassA a) {
    }
}
