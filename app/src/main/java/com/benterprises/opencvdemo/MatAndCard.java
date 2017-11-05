package com.benterprises.opencvdemo;

import org.opencv.core.Mat;

/**
 * Created by Ben on 11/4/17.
 */

public class MatAndCard {

    public Mat preprocessed;
    public Card card;

    public MatAndCard(Mat preprocessed, Card card) {
        this.card = card;
        this.preprocessed = preprocessed;
    }
}
