package pers.yozora7.lanfirewallmgr.excel;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import pers.yozora7.lanfirewallmgr.service.ConfDao;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 读取固定格式的Excel文件, 获得网络拓扑, 存入数据库
 */
@Slf4j
public class ExcelDao {
    static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int BATCH_COUNT = 100;

    /**
     * @param fileName
     * @param tClass
     * @param <T>
     * @throws IOException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    public static <T, D> void importExcel(String fileName, Class<T> tClass, String sheetName, ConfDao service) throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        log.info("DAO Service: {}", service.getClass().getCanonicalName());
        log.info("Data type: {}", tClass.getCanonicalName());
        // 返回数据
        List<T> list = new ArrayList<>(BATCH_COUNT);
        // 读取excel
        InputStream inputStream = new FileInputStream(fileName);
        Workbook workbook = WorkbookFactory.create(inputStream);
        // 读取指定的sheet
        Sheet sheet = workbook.getSheet(sheetName);
        // 获取最大行数
        int countRows = ExcelUtils.getRealLastRowNum(sheet);
        // 反射获取字段
        Field[] fields = tClass.getDeclaredFields();
        // 校验表头
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (field.isAnnotationPresent(ExcelHeader.class)) {
                ExcelHeader annotation = field.getAnnotation(ExcelHeader.class);
                Cell cell = sheet.getRow(0).getCell(i);
                if (cell == null || !getCellValue(cell).equals(annotation.value())) {
                    throw new RuntimeException("Error: wrong excel format");
                }
            }
        }
        // 处理行数据
        for (int i = 1; i <= countRows; i++) {
            Row row = sheet.getRow(i);
            // 遇到空行则结束
            if (row == null) {
                break;
            }
            T rowData = tClass.getDeclaredConstructor().newInstance();
            // 处理列数据
            for (int j = 0; j < fields.length; j++) {
                Field field = fields[j];
                // 设置属性可访问
                field.setAccessible(true);
                if (field.isAnnotationPresent(ExcelHeader.class)) {
                    ExcelHeader annotation = field.getAnnotation(ExcelHeader.class);
                    int columnIndex = annotation.columnIndex();
                    Cell cell = row.getCell(j);
                    if (cell == null) {
                        continue;
                    }
                    // 获取列值
                    Object value = getCellValue(cell);
                    // 设置属性
                    setFieldValue(rowData, field, value);
                }
            }
            // 批量持久化
            list.add(rowData);
            if (i % BATCH_COUNT == 0) {
                // TODO: DAO
//                    if (service.getClass() == OrientExcelService.class) {
//                        OrientExcelService orientService = (OrientExcelService) service;
//                        orientService.excel2Graph(tClass.getCanonicalName(), list);
//                    }
                // 释放内存
                list.clear();
            }
        }
        // 确保最后遗留的数据也存储到数据库
        if (!list.isEmpty()) {
            log.info("Upload data: {}", list.toString());
//                // TODO: DAO
//                if (service.getClass() == OrientExcelService.class) {
//                    OrientExcelService orientService = (OrientExcelService) service;
//                    orientService.excel2Graph(tClass.getCanonicalName(), list);
//                }
        }
    }

    private static <T> void setFieldValue(T rowData, Field field, Object value) throws IllegalAccessException {
        // 整形
        if (field.getType() == int.class || field.getType() == Integer.class) {
            field.set(rowData, value);
        }
        // 长整型
        else if (field.getType() == long.class || field.getType() == Long.class) {
            field.set(rowData, value);
        }
        // 浮点数
        else if (field.getType() == double.class || field.getType() == Double.class) {
            field.set(rowData, value);
        }
        // 字符串
        else if (field.getType() == String.class) {
            field.set(rowData, String.valueOf(value));
        }
        // 日期
        else if (field.getType() == LocalDateTime.class) {
            field.set(rowData, LocalDateTime.parse(String.valueOf(value), dateTimeFormatter));
        }
    }

    private static Object getCellValue(Cell cell) {
        CellType cellType = cell.getCellType();
        Object cellValue = null;
        // 数值
        if (cellType == CellType.NUMERIC) {
            // 日期
            if (DateUtil.isCellDateFormatted(cell)) {
                Date date = cell.getDateCellValue();
                cellValue = dateTimeFormatter.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
            } else {
                double numericCellValue = cell.getNumericCellValue();
                BigDecimal bigDecimal = new BigDecimal(numericCellValue);
                // 整型
                if ((bigDecimal + ".0").equals(Double.toString(numericCellValue))) {
                    cellValue = bigDecimal;
                }
                // 科学记数法
                else if (String.valueOf(numericCellValue).contains("E10")) {
                    cellValue = new BigDecimal(numericCellValue).toPlainString();
                }
                // 浮点数
                else {
                    cellValue = numericCellValue;
                }
            }
        }
        // 字符串, 默认全部大写
        else if (cellType == CellType.STRING) {
            cellValue = cell.getStringCellValue().toLowerCase();
        }
        // 公式
        else if (cellType == CellType.FORMULA) {
        }
        // 布尔值
        else if (cellType == CellType.BOOLEAN) {
            cellValue = cell.getBooleanCellValue();
        }
        // 空值
        else if (cellType == CellType.BLANK) {
        }
        // 错误
        else if (cellType == CellType.ERROR) {
            cellValue = cell.getErrorCellValue();
        }
        log.info("cellType={}, cellValue={}", cellType.name(), cellValue);
        return cellValue;
    }
}
