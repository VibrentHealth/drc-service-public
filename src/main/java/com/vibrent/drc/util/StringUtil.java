package com.vibrent.drc.util;

import com.vibrent.vxp.push.LanguageEnum;
import org.springframework.util.StringUtils;

public final class StringUtil {

    private static final String PII_STATE_PREFIX = "PIIState_";

    private StringUtil() {

    }

    /**
     * When the user's state is stored in PMI it is stored as PIIState_<state> so we need to concat the PIIState_ to get the state.
     *
     * @param state - the original state name to update if needed
     * @return - value of state with prefixed PIIState_ if it does not start with PIIState_ otherwise the original string.
     */
    public static String prefixPIIState(String state) {
        String stateToUse = state;
        if (StringUtils.hasText(state) && !state.startsWith(PII_STATE_PREFIX)) {
            stateToUse = PII_STATE_PREFIX.concat(state);
        }
        return stateToUse;
    }

    public static String getLanguageKey(LanguageEnum languageEnum) {
        if (LanguageEnum.SPANISH == languageEnum) {
            return "es";
        } else {
            return "en";
        }
    }
}
