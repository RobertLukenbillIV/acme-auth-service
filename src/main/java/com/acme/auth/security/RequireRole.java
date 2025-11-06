package com.acme.auth.security;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    String[] value();
    boolean requireAll() default false;
}
