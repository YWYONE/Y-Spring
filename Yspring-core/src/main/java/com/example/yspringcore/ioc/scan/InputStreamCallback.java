package com.example.yspringcore.ioc.scan;

import java.io.IOException;
import java.io.InputStream;


@FunctionalInterface
public interface InputStreamCallback<T> {

    T doWithInputStream(InputStream stream) throws IOException;
}
