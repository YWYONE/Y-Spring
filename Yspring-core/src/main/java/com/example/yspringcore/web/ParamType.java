package com.example.yspringcore.web;

public enum ParamType {
    //from url placeholder eg. url/{parama}
    PATH_VARIABLE,
    //from url request param/form table eg. url?parama=x
    REQUEST_PARAM,
    //from post‘s  Request body's  eg. json
    REQUEST_BODY,

    SERVLET_VARIABLE;
}
