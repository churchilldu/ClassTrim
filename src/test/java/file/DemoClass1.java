package file;

import org.apache.commons.lang3.StringUtils;

public class DemoClass1 extends SuperClass implements Comparable{
    private String str;

    public DemoClass1(String str) {
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
        return StringUtils.equals(((DemoClass1) obj).getStr(), str);
    }

    @Override
    public void inheritMethod() {
        super.inheritMethod();
        new DemoClass2().method2();
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }

    private void method1(Object method) {
        return;
    }

    private boolean isSingle() {
        return false;
    }

    class InnerClass {
        public void innerMethod() {
            new DemoClass2().method2();
        }
    }
}
