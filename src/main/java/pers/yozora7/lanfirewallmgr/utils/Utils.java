package pers.yozora7.lanfirewallmgr.utils;

import java.util.Set;

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
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
