package pers.yozora7.lanfirewallmgr.utils;

import java.util.Set;

import static jdk.nashorn.internal.objects.NativeString.substring;

public class Utils {
    public static <T> String setToString (Set<T> set, Class<T> tClass) {
        String result = null;
        for (T t : set) {
            if (result == null) {
                result = "";
            }
            result += t.toString() + ",";
        }
        if (result != null && result.length() > 0) {
            result = substring(result, 0, result.length() - 1);
        }
        return result;
    }
}
