package be.mygod.oplus.batterywaster;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Process;

import org.xmlpull.v1.XmlPullParser;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

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
            case "com.android.systemui":
                handleSystemui(lpparam);
                break;
            case "com.oplus.battery":
                handleOplusBattery(lpparam);
                break;
        }
    }

    @SuppressLint("BlockedPrivateApi")
    private void handleAndroid(XC_LoadPackage.LoadPackageParam lpparam) throws NoSuchMethodException {
        XposedHelpers.findAndHookMethod("com.android.server.am.OplusAppStartupManager", lpparam.classLoader,
                "handleStrictModeChange", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.args[0] = false;  // disable strict mode always
            }
        });

        XposedHelpers.findAndHookMethod(
                "com.android.server.notification.OplusNotificationManagerServiceExtImpl$NotificationUidObserver",
                lpparam.classLoader, "onUidGone", int.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(null);
            }
        });

        var logOffender = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                var pid = Binder.getCallingPid();
                if (Process.myPid() == pid) return;
                XposedBridge.log(String.format(Locale.ENGLISH, "Dangerous method %s(%s) denied from uid=%d, pid=%d",
                        param.method.getName(), Arrays.deepToString(param.args), Binder.getCallingUid(), pid));
                param.setResult(null);
            }
        };
        var foundMethods = new HashSet<String>();
        for (var method : XposedHelpers.findClass(
                "com.android.server.notification.OplusNotificationManagerServiceExtImpl",
                lpparam.classLoader).getDeclaredMethods()) switch (method.getName()) {
            case "cancelAllLocked":
            case "cancelAllNotificationsInt":
            case "onClearAllNotifications":
                XposedBridge.hookMethod(method, logOffender);
                foundMethods.add(method.getName());
                break;
        }
        if (foundMethods.size() < 3) throw new NoSuchMethodError(foundMethods.toString());

        XposedBridge.hookMethod(NotificationChannel.class.getDeclaredMethod(
                "isImportanceLockedByCriticalDeviceFunction"), new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(false);
            }
        });
    }

    private void handleSystemui(XC_LoadPackage.LoadPackageParam lpparam) {
        if (Build.VERSION.SDK_INT < 35) return;
        XposedHelpers.findAndHookMethod("com.android.systemui.util.HeadsUpToZoomUtils", lpparam.classLoader,
                "calculateFlexibleWindowBounds", Intent.class, Context.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(null);
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

        var notificationController = XposedHelpers.findClass(
                "com.android.settings.notification.app.NotificationPreferenceController", lpparam.classLoader);
        var setOverrideCanConfigure = notificationController.getDeclaredMethod(
                "setOverrideCanConfigure", boolean.class);
        XposedBridge.hookAllConstructors(notificationController, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                setOverrideCanConfigure.invoke(param.thisObject, true);
            }
        });
    }
    private void bypassSettingsActivityPlugin(XC_LoadPackage.LoadPackageParam lpparam, String subsetting) {
        for (var method : XposedHelpers.findClass("com.oplus.settings.SettingsActivityPlugin$" + subsetting,
                lpparam.classLoader).getDeclaredMethods()) {
            if (!method.getName().equals("onCreate") || method.getReturnType() != void.class) continue;
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
        XposedHelpers.findAndHookMethod("com.android.org.kxml2.io.KXmlParser", lpparam.classLoader, "nextText",
                new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                var parser = (XmlPullParser) param.thisObject;
                switch (parser.getName()) {
                    case "HighTemperatureFirstStepSwitch":
                    case "HighTemperatureProtectSwitch":
                    case "HighTemperatureShutdownSwitch":
                        param.setResult("false");
                        break;
                }
            }
        });
        XposedHelpers.findAndHookMethod("com.oplus.thermalcontrol.ThermalControlConfig", lpparam.classLoader,
                "isThermalControlEnable", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.oplus.thermalcontrol.ThermalControllerCenter", lpparam.classLoader,
                "isSafetyStateExcludedScene", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(true);
            }
        });
    }
}
