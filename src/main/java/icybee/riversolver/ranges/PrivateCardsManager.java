package icybee.riversolver.ranges;

import icybee.riversolver.Card;
import icybee.riversolver.exceptions.BoardNotFoundException;

/**
 * Created by huangxuefeng on 2019/10/17.
 * getting and setting private infos
 */
public class PrivateCardsManager {
    PrivateCards[][] private_cards;
    int player_number;
    long board;

    //TODO finish this
    public PrivateCardsManager(PrivateCards[][] private_cards,int player_number,long board){
        this.private_cards = private_cards;
        this.player_number = player_number;
        this.board = board;
        setRelativeProbs();
    }

    public PrivateCards[] getPreflopCards(int player){
        return this.private_cards[player];
    }

    public float[] getInitialReachProb(int player,long board) throws BoardNotFoundException{
        int cards_len =  this.private_cards[player].length;
        float[] probs = new float[cards_len];
        for(int i = 0;i < cards_len;i ++){
            PrivateCards pc = this.private_cards[player][i];
            if(Card.boardsHasIntercept(board,Card.boardInts2long(new int[]{pc.card1,pc.card2}))) {
                probs[i] = 0;
            }else{
                probs[i] = this.private_cards[player][i].weight;
            }
        }
        return probs;
    }

    public void setRelativeProbs(){
        int players = this.private_cards.length;
        for(int player_id = 0; player_id < players;player_id ++){
            // TODO 这里只考虑了两个玩家的情况
            int oppo = 1 - player_id;
            float oppo_prob_sum = 0;

            for(int i = 0;i < this.private_cards[oppo].length;i ++){

            }
        }
    }
}
