package file;


/*
 * javac -cp "C:\Users\jesse\.m2\repository\org\apache\commons\commons-lang3\3.17.0\commons-lang3-3.17.0.jar" DemoClass1.java
 *
 */

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
    public void setStr(Object str) {
        this.str = (String) str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    @Override
    public boolean equals(Object obj) {
        return StringUtils.equals(((DemoClass1) obj).getStr(), str);
    }

    @Override
    public void method() {
        super.method();
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }

    class InnerClass {
        public void innerMethod() {
            return;
        }
    }
}
