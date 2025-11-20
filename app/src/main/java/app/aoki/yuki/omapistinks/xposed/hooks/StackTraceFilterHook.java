package app.aoki.yuki.omapistinks.xposed.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Hooks stack trace retrieval methods to filter out Xposed and module frames.
 * This prevents detection through stack trace analysis by removing suspicious frames.
 * 
 * Addresses root detection that examines stack traces for Xposed/module presence.
 */
public class StackTraceFilterHook {
    
    // Patterns to filter out from stack traces
    private static final String[] FILTER_PATTERNS = {
        "de.robv.android.xposed",
        "app.aoki.yuki.omapistinks",
        "XposedBridge",
        "XC_MethodHook",
        "EdXposed",
        "LSPosed"
    };
    
    /**
     * Install hooks to filter stack traces for the given package
     */
    public static void installHooks(LoadPackageParam lpparam) {
        try {
            hookThreadGetStackTrace(lpparam);
            hookThrowableGetStackTrace(lpparam);
        } catch (Throwable t) {
            // Hook installation failed - silently continue
            // We don't want to break the app if stack trace filtering fails
        }
    }
    
    /**
     * Hook Thread.getStackTrace() to filter out our frames
     */
    private static void hookThreadGetStackTrace(LoadPackageParam lpparam) {
        // Note: lpparam is required by the API but not used here since we hook Thread class globally
        try {
            XposedHelpers.findAndHookMethod(Thread.class, "getStackTrace", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        StackTraceElement[] original = (StackTraceElement[]) param.getResult();
                        if (original != null && original.length > 0) {
                            StackTraceElement[] filtered = filterStackTrace(original);
                            param.setResult(filtered);
                        }
                    } catch (Throwable t) {
                        // If filtering fails, return original to avoid breaking the app
                    }
                }
            });
        } catch (Throwable t) {
            // Method might not be available or hookable
        }
    }
    
    /**
     * Hook Throwable.getStackTrace() to filter out our frames
     */
    private static void hookThrowableGetStackTrace(LoadPackageParam lpparam) {
        // Note: lpparam is required by the API but not used here since we hook Throwable class globally
        try {
            XposedHelpers.findAndHookMethod(Throwable.class, "getStackTrace", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        StackTraceElement[] original = (StackTraceElement[]) param.getResult();
                        if (original != null && original.length > 0) {
                            StackTraceElement[] filtered = filterStackTrace(original);
                            param.setResult(filtered);
                        }
                    } catch (Throwable t) {
                        // If filtering fails, return original to avoid breaking the app
                    }
                }
            });
        } catch (Throwable t) {
            // Method might not be available or hookable
        }
    }
    
    /**
     * Filter stack trace elements to remove Xposed and module frames
     * 
     * @param original Original stack trace
     * @return Filtered stack trace with suspicious frames removed
     */
    private static StackTraceElement[] filterStackTrace(StackTraceElement[] original) {
        List<StackTraceElement> filtered = new ArrayList<>();
        
        for (StackTraceElement element : original) {
            if (!shouldFilter(element)) {
                filtered.add(element);
            }
        }
        
        // If we filtered everything, return at least one frame to avoid empty stack traces
        // which could itself be suspicious
        if (filtered.isEmpty() && original.length > 0) {
            // Return the deepest non-filtered frame using shouldFilter() for consistency
            for (int i = original.length - 1; i >= 0; i--) {
                if (!shouldFilter(original[i])) {
                    filtered.add(original[i]);
                    break;
                }
            }
            // If still empty (all frames were filtered), add the last frame to prevent completely empty stack
            if (filtered.isEmpty()) {
                filtered.add(original[original.length - 1]);
            }
        }
        
        return filtered.toArray(new StackTraceElement[0]);
    }
    
    /**
     * Check if a stack trace element should be filtered out
     * 
     * @param element Stack trace element to check
     * @return true if element should be removed, false otherwise
     */
    private static boolean shouldFilter(StackTraceElement element) {
        String className = element.getClassName();
        String methodName = element.getMethodName();
        
        // Check class name against filter patterns
        for (String pattern : FILTER_PATTERNS) {
            if (className.contains(pattern)) {
                return true;
            }
        }
        
        // Also check method names for common Xposed patterns
        if (methodName.contains("hooked") || methodName.contains("Hooked")) {
            return true;
        }
        
        return false;
    }
}
