package com.vibrent.drc.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PrettyPrintUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrettyPrintUtil.class);

    private PrettyPrintUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Helper method to convert VXP request/response data to pretty json for output to external server log
     * @param data  - data to pretty print
     * @return      - String representation of data
     */
    public static String prettyPrint(Object data) {
        if (data == null) {
            return "";
        }

        String jsonString;
        ObjectMapper mapper = JacksonUtil.getMapper();
        try {
            Object json = mapper.readValue(mapper.writeValueAsString(data), Object.class);
            jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (IOException e) {
            jsonString = data.toString();
            LOGGER.debug("Unable to pretty print VXPMessageDTO", e);
        }
        return jsonString;
    }

}
