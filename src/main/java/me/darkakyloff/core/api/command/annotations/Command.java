package me.darkakyloff.core.api.command.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Command
{
    String name();

    String[] aliases() default {};

    String permission() default "";

    String usage() default "";

    boolean allowConsole() default true;

    int minArgs() default 0;

    int maxArgs() default -1;
}