package me.darkakyloff.core.api.command.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TabComplete
{

    int argumentIndex();

    String[] suggestions() default {};

    boolean playersList() default false;

    String customListMethod() default "";

    boolean filterByInput() default true;
}