package be.mygod.oplus.batterywaster;

import java.lang.invoke.MethodHandles;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class BatteryWaster implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws ReflectiveOperationException {
        switch (lpparam.packageName) {
            case "android":
                handleAndroid(lpparam);
                break;
            case "com.android.settings":
                handleAndroidSettings(lpparam);
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

    private void handleAndroidSettings(XC_LoadPackage.LoadPackageParam lpparam) throws ReflectiveOperationException {
        bypassSettingsActivityPlugin(lpparam, "AppNotificationSettings");
        bypassSettingsActivityPlugin(lpparam, "ConfigureNotificationSettings");
        bypassSettingsActivityPlugin(lpparam, "NotificationAppList");

        var baseController = XposedHelpers.findClass("com.android.settings.applications.appinfo.AppInfoPreferenceControllerBase",
                lpparam.classLoader);
        var preference = XposedHelpers.findClass("androidx.preference.Preference", lpparam.classLoader);
        var handleClick = MethodHandles.privateLookupIn(baseController, null).unreflectSpecial(
                baseController.getDeclaredMethod("handlePreferenceTreeClick", preference), baseController);
        XposedHelpers.findAndHookMethod(
                "com.oplus.settings.feature.appmanager.details.controller.OplusAppNotificationPreferenceController",
                lpparam.classLoader, "handlePreferenceTreeClick", preference, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(handleClick.bindTo(param.thisObject).invokeWithArguments(param.args[0]));
            }
        });
    }
    private void bypassSettingsActivityPlugin(XC_LoadPackage.LoadPackageParam lpparam, String subsetting) {
        var thread = XposedHelpers.findClass("com.oplus.settings.SettingsActivityPlugin$" + subsetting,
                lpparam.classLoader);
        for (var method : thread.getDeclaredMethods()) if (method.getName().equals("onCreate") &&
                method.getReturnType() == void.class) {
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(null);
                }
            });
            return;
        }
        throw new NoSuchMethodError("com.oplus.settings.SettingsActivityPlugin$" + subsetting + ".onCreate");
    }

    private void handleOplusBattery(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod("com.oplus.thermalcontrol.ThermalControlConfig", lpparam.classLoader,
                "isThermalControlEnable", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(false);
            }
        });
    }
}
