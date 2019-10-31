package icybee.riversolver.ranges;

import icybee.riversolver.Card;

/**
 * Created by huangxuefeng on 2019/10/11.
 * contains code to describe private ranges
 */
public class PrivateCards {
    public int card1;
    public int card2;
    public float weight;
    public float relative_prob;
    PrivateCards(int card1, int card2, float weight){
        this.card1 = card1;
        this.card2 = card2;
        this.weight = weight;
        this.relative_prob = 0;
    }

    @Override
    public int hashCode() {
        if (card1 > card2){
            return card1 * 52 + card2;
        }else{
            return card2 * 52 + card1;
        }
    }

    public static int hash_hand(int card1,int card2){
        if (card1 > card2){
            return card1 * 52 + card2;
        }else{
            return card2 * 52 + card1;
        }
    }

    @Override
    public String toString() {
        if (card1 > card2) {
            return Card.intCard2Str(card1) + Card.intCard2Str(card2);
        }else{
            return Card.intCard2Str(card2) + Card.intCard2Str(card1);
        }
    }

    public int[] get_hands(){
        if (card1 > card2) {
            return new int[]{card1,card2};
        }else{
            return new int[]{card2,card1};
        }
    }



}
