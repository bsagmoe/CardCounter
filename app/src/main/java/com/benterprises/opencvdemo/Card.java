package com.benterprises.opencvdemo;

/**
 * Created by Ben on 11/4/17.
 */

public class Card {

    public enum Suit { HEARTS, DIAMONDS, SPADES, CLUBS}
    public enum Denomination { TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING, ACE, JOKER}

    public Suit mSuit;
    public Denomination mDenomination;

    public Card(Suit suit, Denomination denomination) {
        mSuit = suit;
        mDenomination = denomination;
    }

    public int getValue () {
        switch (mDenomination) {
            case THREE:
            case FOUR:
            case FIVE:
            case SIX:
            case SEVEN:
                return 5;
            case EIGHT:
            case NINE:
            case TEN:
            case JACK:
            case QUEEN:
            case KING:
                return 10;
            case ACE:
            case JOKER:
            case TWO:
                return 20;
            default:
                return 5;
        }
    }

    @Override
    public String toString() {
        return this.mDenomination.toString() + " of " + this.mSuit.toString();
    }
}
