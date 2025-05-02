package org.dreamabout.sw.frp.be.domain;

import ch.qos.logback.classic.pattern.Abbreviator;
import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator2;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FrpUtils {

    private static final Abbreviator ABBREVIATOR = new TargetLengthBasedClassNameAbbreviator2(Constant.ABBREVIATOR_LENGTH);

    public static String abbreviate(String className) {
        return ABBREVIATOR.abbreviate(className);
    }
}
