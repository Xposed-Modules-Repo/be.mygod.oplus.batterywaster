package be.mygod.oplus.batterywaster;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class BatteryWaster implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        switch (lpparam.packageName) {
            case "android":
                handleAndroid(lpparam);
                break;
        }
    }

    private void handleAndroid(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod("com.android.server.am.OplusAppStartupManager", lpparam.classLoader,
                "handleStrictModeChange", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.args[0] = false;  // disable strict mode always
            }
        });
    }
}
