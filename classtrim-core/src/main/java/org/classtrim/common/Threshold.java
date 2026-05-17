package org.classtrim.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Threshold {
    private final int WMC;
    private final int CBO;
    private final int RFC;
}
