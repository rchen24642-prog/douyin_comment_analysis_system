package com.example.springboot.common;

public enum ResultCode {
    SUCCESS("0","成功"),
    ERROR("-1","系统异常"),
    PARAM_ERROR("1001","参数异常"),

    USER_EXIST_ERROR("2001","用户名已存在"),
    USER_ACCOUNT_ERROR("2002","账号或密码错误"),
    USER_NOT_LOGIN("2003","用户未登录"),

    PARAM_LOST_ERROR("2004","参数缺失"),
    PARAM_PASSWORD_ERROR("2005","原密码输入无效"),

    TOKEN_INVALID_ERROR("401","无效的token"),
    TOKEN_CHECK_ERROR("401","token验证失败，请重新登录"),
    DATA_LESS("402","管理员数据小于5"),
    DATA_NOT_FOUND("403","数据缺失"),
    NO_AUTH("404","无权限"),
    SEARCH_ERROR("5001","ES搜索异常，请检查连接");
    // 注意：结尾别少了分号

    public String code;
    public String msg;

    ResultCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
