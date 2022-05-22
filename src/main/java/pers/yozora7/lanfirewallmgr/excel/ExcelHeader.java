package pers.yozora7.lanfirewallmgr.excel;
import java.lang.annotation.*;
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelHeader {
    // 第一行属性值
    String value() default "";

    // 列索引
    int columnIndex() default 0;

}