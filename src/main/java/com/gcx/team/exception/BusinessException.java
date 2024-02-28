package com.gcx.team.exception;

import com.gcx.team.common.ErrorCode;

/**
 * 自定义异常类
 *
 */
public class BusinessException extends RuntimeException {

    private final int code; // 定义一个私有的、最终的整型变量code。

    private final String description; // 定义一个私有的、最终的字符串变量description。

    public BusinessException(String message, int code, String description) { // 定义一个公开的构造方法，接收一个字符串message、一个整型code和一个字符串description作为参数。
        super(message); // 调用父类的构造方法，传入message参数。
        this.code = code; // 将传入的code参数赋值给当前对象的code属性。
        this.description = description; // 将传入的description参数赋值给当前对象的description属性。
    }

    public BusinessException(ErrorCode errorCode) { // 定义另一个公开的构造方法，接收一个ErrorCode类型的参数errorCode。
        super(errorCode.getMessage()); // 调用父类的构造方法，传入errorCode对象的getMessage()方法返回的字符串。
        this.code = errorCode.getCode(); // 将errorCode对象的getCode()方法返回的值赋值给当前对象的code属性。
        this.description = errorCode.getDescription(); // 将errorCode对象的getDescription()方法返回的字符串赋值给当前对象的description属性。
    }

    public BusinessException(ErrorCode errorCode, String description) { // 定义第三个公开的构造方法，接收一个ErrorCode类型的参数errorCode和一个字符串description作为参数。
        super(errorCode.getMessage()); // 调用父类的构造方法，传入errorCode对象的getMessage()方法返回的字符串。
        this.code = errorCode.getCode(); // 将errorCode对象的getCode()方法返回的值赋值给当前对象的code属性。
        this.description = description; // 将传入的description参数赋值给当前对象的description属性。
    }

    public int getCode() { // 定义一个公开的方法，返回code属性的值。
        return code; // 返回当前对象的code属性值。
    }

    public String getDescription() { // 定义一个公开的方法，返回description属性的值。
        return description; // 返回当前对象的description属性值。
    }
}
