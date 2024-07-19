package com.jazzkuh.commandlib.velocity.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

@UtilityClass
public class StringUtils {
    @NotNull
    public static <T extends Collection<? super String>> T copyPartialMatches(@NotNull String token, @NotNull Iterable<String> originals, @NotNull T collection) {
        for (String string : originals) {
            if (startsWithIgnoreCase(string, token)) {
                collection.add(string);
            }
        }

        return collection;
    }

    public static boolean startsWithIgnoreCase(@NotNull String string, @NotNull String prefix) {
        return string.length() >= prefix.length() && string.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
