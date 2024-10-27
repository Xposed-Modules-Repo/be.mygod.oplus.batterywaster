package be.mygod.oplus.batterywaster;

import android.content.Intent;
import android.os.BatteryManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class BatteryWaster implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        switch (lpparam.packageName) {
            case "android":
                handleAndroid(lpparam);
                break;
            case "com.oplus.battery":
                handleOplusBattery(lpparam);
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

    private void handleOplusBattery(XC_LoadPackage.LoadPackageParam lpparam) {
        var thread = XposedHelpers.findClass("android.app.ActivityThread$ApplicationThread", lpparam.classLoader);
        for (var method : thread.getDeclaredMethods()) if (method.getName().equals("scheduleRegisteredReceiver")) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length >= 2 && parameterTypes[1].equals(Intent.class)) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        var intent = (Intent) param.args[1];
                        if (!Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) return;
                        intent.removeExtra(BatteryManager.EXTRA_TEMPERATURE);
                    }
                });
                return;
            }
        }
        throw new NoSuchMethodError("android.app.ActivityThread$ApplicationThread.scheduleRegisteredReceiver");
    }
}
