package com.example.yspringcore.ioc.exception;

public class IocException extends  RuntimeException{
    public IocException() {
    }

    public IocException(String message) {
        super(message);
    }

    public IocException(String message, Throwable cause) {
        super(message, cause);
    }

    public IocException(Throwable cause) {
        super(cause);
    }

    public IocException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
