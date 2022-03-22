package com.vibrent.drc.util;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CollectionUtil {

    private CollectionUtil() {
        //private constructor
    }

    /**
     * Get inputIds from optional List<String>
     *
     * @param input
     * @return
     */
    public static List<String> getInputIds(Optional<List<String>> input) {
        List<String> inputIds = Collections.emptyList();
        if (input.isPresent()) {
            inputIds = input.get();
        }
        return inputIds;
    }
}
