package com.example.yspringcore.aop.exception;

public class AopException extends RuntimeException{
    public AopException() {
    }

    public AopException(String message) {
        super(message);
    }

    public AopException(String message, Throwable cause) {
        super(message, cause);
    }

    public AopException(Throwable cause) {
        super(cause);
    }

    public AopException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
