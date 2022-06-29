package com.vibrent.drc.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DrcResponseVo {
    boolean isSuccess;
    long httpCode;
    String errorResponse;
}
