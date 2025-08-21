package com.jazzkuh.commandlib.brigadier.utils.source;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommandSource<S> {
    private final S source;
}
