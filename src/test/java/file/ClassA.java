package file;

import org.apache.commons.lang3.StringUtils;

public class ClassA extends SuperClass implements Comparable{
    private String str;

    public ClassA(String str) {
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

    @Override
    public boolean equals(Object obj) {
        return StringUtils.equals(((ClassA) obj).getStr(), str);
    }

    @Override
    public void inheritMethod() {
        super.inheritMethod();
        new ClassB().method2();
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }

    private void A1(Object method) {
    }

    private boolean isSingle() {
        return false;
    }

    class InnerClass {
        public void innerMethod() {
            new ClassB().method2();
        }
    }
}
