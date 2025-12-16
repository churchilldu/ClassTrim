package org.classtrim.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static org.classtrim.util.FileUtils.TAB;

@Getter
@AllArgsConstructor
public class AlgorithmParameter {
    private String name;
    private int population;
    private int generation;

    @Override
    public String toString() {
        return String.valueOf(this.getPopulation()) + TAB +
                String.valueOf(this.getGeneration()) + TAB +
                this.getName() + TAB;
    }
}
