package pers.yozora7.lanfirewallmgr.utils;

import org.apache.commons.net.util.SubnetUtils;

import java.util.Set;

/**
 * 网络地址工具集
 */
public class Utils {
    // set -> string
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

    // 判断IP是否在CIDR(x.x.x.x/x)内
    public static boolean isIpInCidr(String ip, String cidr) {
        if (cidr.contains(":")) {
            return false;
        }
        if (cidr.split("/")[1].equals("32")) {
            return (ip.equals(cidr.split("/")[0]));
        }
        SubnetUtils.SubnetInfo subnetInfo = new SubnetUtils(cidr.trim()).getInfo();
        return subnetInfo.isInRange(ip.trim());
    }

    // 判断CIDR1是否在CIDR2内
    public static boolean isCidrInCidr(String cidr1, String cidr2) {
        if (cidr2.contains(":")) {
            return false;
        }
        // 分离地址和掩码
        int mask1 = Integer.parseInt(cidr1.split("/")[1]);
        int mask2 = Integer.parseInt(cidr2.split("/")[1]);
        Long bin1 = Long.parseLong(ipToBinary(cidr1.split("/")[0]), 2);
        Long bin2 = Long.parseLong(ipToBinary(cidr2.split("/")[0]), 2);
        // 取较小的掩码值
        if (mask1 < mask2) {
            return false;
        }
        // 掩码转换为bit
        Long maskBit = Long.parseLong(maskToBinary(mask2),2);
        return (bin1 & maskBit) == (bin2 & maskBit);
    }

    // 判断IP是否在IP1~IP2内
    public static boolean isIpInRange (String ip, String ip1, String ip2) {
        if (ip1.contains(":") || ip2.contains(":")) {
            return false;
        }
        // IP转为二进制数
        long bin = Long.valueOf(ipToBinary(ip), 2);
        long bin1 = Long.valueOf(ipToBinary(ip1), 2);
        long bin2 = Long.valueOf(ipToBinary(ip2), 2);
        // 比较大小
        if (bin >= bin1 && bin <= bin2) {
            return true;
        }
        return false;
    }

    // IP地址 (xxx.xxx.xxx.xxx) 转32位二进制串
    public static String ipToBinary (String ip) {
        String[] temp = ip.split("\\.");
        String bin = "";
        for (String i : temp) {
            bin += decimalToBinary(Integer.parseInt(i)).substring(24);
        }
        return bin;
    }

    // 32位二进制串转IP地址 (xxx.xxx.xxx.xxx)
    public static String binaryToIp (String bin) {
        String ip = "";
        for (int i = 8; i <= 32; i++) {
            if (i % 8 == 0) {
                ip += Integer.valueOf(bin.substring(i - 8, i), 2).toString();
                if (i < 32) {
                    ip += ".";
                }
            }
        }
        return ip;
    }

    // 十进制数转二进制数
    public static String decimalToBinary (int n) {
        String bin = "";
        for (int i = 31; i >= 0; i--) {
            bin += (n >>> i & 1);
        }
        return bin;
    }

    // 2位数掩码转二进制串
    public static String maskToBinary(int mask) {
        String bit = "";
        for (int i = 0; i < mask; i++) {
            bit += "1";
        }
        for (int i = 0; i < 32 - mask; i++) {
            bit += "0";
        }
        return bit;
    }

    // 掩码 (xxx.xxx.xxx.xxx) 转2位数
    public static String longMaskToShort (String mask) {
        int result = 0;
        String[] temp = mask.split("\\.");
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 8; j++) {
                if (ipToBinary(temp[i]).charAt(j) == '0') {
                    break;
                }
                result += 1;
            }
        }
        return Integer.toString(result);
    }

    // 由反掩码得到掩码
    public static String wildcardToMask (String wildcard) {
        // 反掩码 (xxx.xxx.xxx.xxx) 转为32位二进制串
        String binary = ipToBinary(wildcard);
        // 取反得到掩码
        String mask = "";
        for (int i = 0; i < 32; i++) {
            mask += (binary.charAt(i) == '0')?"1":"0";
        }
        mask = binaryToIp(mask);
        // 转换为十进制2位数
        return longMaskToShort(mask);
    }

}
