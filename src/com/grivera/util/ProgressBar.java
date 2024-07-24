package com.grivera.util;

import java.time.Duration;

public class ProgressBar {

    private final int steps;
    private final int width;
    private final String title;
    private int currStep;
    private int stepCount;

    private long prevTime;
    private Duration avgStepTime;

    private char[] spinner = {'|', '/', '-', '\\'};

    public ProgressBar(int steps) {
        this(steps, "");
    }

    public ProgressBar(int steps, int width) {
        this(steps, width, "");
    }

    public ProgressBar(int steps, String title) {
        this(steps, 50, title);
    }

    public ProgressBar(int steps, int width, String title) {
        this.steps = steps;
        this.width = width;
        this.title = title;
    }

    public void step() {
        stepBy(1);
    }

    public void stepBy(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be positive");
        }

        if (this.currStep + count > this.steps) {
            throw new IllegalArgumentException("count must not exceed steps");
        }

        this.stepCount = count;
        if (this.currStep == 0) {
            this.prevTime = System.currentTimeMillis();
            this.avgStepTime = Duration.ofSeconds(10);
        } else {
            long currTime = System.currentTimeMillis();
            long stepTime = currTime - this.prevTime;
            this.prevTime = currTime;
            if (this.avgStepTime == null) {
                this.avgStepTime = Duration.ofMillis(stepTime);
            } else {
                avgStepTime = avgStepTime.plusMillis(stepTime).dividedBy(2);
            }
        }
        this.currStep += count;
        printBar();
    }

    public boolean isDone() {
        return this.steps <= this.currStep;
    }

    private void printBar() {
        System.out.printf("\r %c [", this.spinner[this.currStep % this.spinner.length]);
        int progressCount = (int) ((double) currStep / steps * width);
        for (int i = 1; i < progressCount; i++) {
            System.out.print("=");
        }
        System.out.print(">");
        for (int i = progressCount; i < width; i++) {
            System.out.print(" ");
        }
        System.out.printf("] %d/%d (~%.2f ms/it) %s",
            currStep, steps, (double) this.avgStepTime.toMillis() / this.stepCount, title
        );

        if (this.isDone()) {
            System.out.println(" => Done. \t");
        } else {
            for (int i = 0; i < this.width / 4 + this.title.length(); i++) {
                System.out.print(" ");
            }
            System.out.print("\r");
        }
    }
    
}
