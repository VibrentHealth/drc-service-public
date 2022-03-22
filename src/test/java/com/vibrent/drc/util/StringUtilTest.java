package com.vibrent.drc.util;

import com.vibrent.vxp.push.LanguageEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringUtilTest {

    @Test
    void prefixPIIState() {
        String state = StringUtil.prefixPIIState("OR");
        assertEquals("PIIState_OR", state);

        String state1 = StringUtil.prefixPIIState("PIIState_OR");
        assertEquals("PIIState_OR", state1);

        String state0 = StringUtil.prefixPIIState("");
        assertEquals("", state0);
    }

    @Test
    void getLanguageKey() {
        String langKey = StringUtil.getLanguageKey(LanguageEnum.ENGLISH);
        assertEquals("en",langKey);

        String langKeySp = StringUtil.getLanguageKey(LanguageEnum.SPANISH);
        assertEquals("es",langKeySp);
    }
}