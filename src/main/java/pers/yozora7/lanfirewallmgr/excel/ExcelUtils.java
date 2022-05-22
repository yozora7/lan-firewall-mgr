package pers.yozora7.lanfirewallmgr.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class ExcelUtils {

    // 获取Excel表的实际行数
    public static int getRealLastRowNum(Sheet sheet) {
        boolean flag = false;
        for (int i = 1; i <= sheet.getLastRowNum(); ) {
            Row r = sheet.getRow(i);
            if (r == null) {
                // 如果是空行（即没有任何数据、格式），直接把它以下的数据往上移动
                sheet.shiftRows(i + 1, sheet.getLastRowNum(), -1);
                continue;
            }
            flag = false;
            for (Cell c : r) {
                if (c.getCellType() != CellType.BLANK) {
                    flag = true;
                    break;
                }
            }
            if (flag) {
                i++;
                continue;
            }
            else {
                // 如果是空白行（即可能没有数据, 但是有一定格式）
                // 如果到了最后一行, 直接remove该行
                if (i == sheet.getLastRowNum()) {
                    sheet.removeRow(r);
                }
                // 如果还没到最后一行, 则数据往上移一行
                else {
                    sheet.shiftRows(i + 1, sheet.getLastRowNum(), -1);
                }
            }
        }
        return sheet.getLastRowNum();
    }
}
