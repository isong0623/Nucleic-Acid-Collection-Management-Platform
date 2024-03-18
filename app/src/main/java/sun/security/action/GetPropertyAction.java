//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package sun.security.action;

import java.security.PrivilegedAction;

public class GetPropertyAction implements PrivilegedAction<String> {
    private String theProp;
    private String defaultVal;

    public GetPropertyAction(String var1) {
        this.theProp = var1;
    }

    public GetPropertyAction(String var1, String var2) {
        this.theProp = var1;
        this.defaultVal = var2;
    }

    public String run() {
        String var1 = System.getProperty(this.theProp);
        return var1 == null ? this.defaultVal : var1;
    }
}
