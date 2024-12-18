package file;


public class ClassB {
    public ClassB() {
        ClassA classA = new ClassA("");
    }

    public void method2() {
        new ClassA("").inheritMethod();
    }

    public void methodB2() {
        new ClassA("").new InnerClass().innerMethod();
    }
}
