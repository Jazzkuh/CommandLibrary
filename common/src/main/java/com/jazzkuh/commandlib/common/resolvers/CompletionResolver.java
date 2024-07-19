package com.jazzkuh.commandlib.common.resolvers;

import com.jazzkuh.commandlib.common.AnnotationCommandSender;

import java.util.List;

public interface CompletionResolver<T> {
    List<String> resolve(AnnotationCommandSender<T> sender, String arg);
}
