package me.darkakyloff.core.api.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface HttpEndpoint
{
    String path();

    String[] methods() default {"GET"};

    String basePath() default "";

    String description() default "";

    boolean requiresAuth() default false;

    String[] permissions() default {};

    String contentType() default "application/json";

    int rateLimit() default -1;
}