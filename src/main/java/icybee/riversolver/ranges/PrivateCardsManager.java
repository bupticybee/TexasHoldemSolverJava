package icybee.riversolver.ranges;

import icybee.riversolver.Card;
import icybee.riversolver.exceptions.BoardNotFoundException;

import java.util.Arrays;

/**
 * Created by huangxuefeng on 2019/10/17.
 * getting and setting private infos
 */
public class PrivateCardsManager {
    PrivateCards[][] private_cards;
    int player_number;
    long board;
    int[][] card_player_index;

    //TODO finish this
    public PrivateCardsManager(PrivateCards[][] private_cards,int player_number,long board){
        this.private_cards = private_cards;
        this.player_number = player_number;
        this.card_player_index = new int[52 * 52][];
        for(int i = 0;i < 52 * 52;i ++){
            this.card_player_index[i] = new int[this.player_number];
            Arrays.fill(this.card_player_index[i],-1);
        }

        // 用一个二维数组记录每个Private Combo的对应index,方便从一方的手牌找对方的同名卡牌的index
        for(int player_id = 0;player_id < player_number;player_id ++){
            PrivateCards[] privateCombos = private_cards[player_id];
            for(int i = 0;i < privateCombos.length;i ++){
                PrivateCards one_private_combo = privateCombos[i];
                this.card_player_index[one_private_combo.hashCode()][player_id] = i;
            }
        }

        this.board = board;
        try {
            setRelativeProbs();
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public PrivateCards[] getPreflopCards(int player){
        return this.private_cards[player];
    }

    public Integer indPlayer2Player(int from_player,int to_player,int index){
        if(index < 0 || index >= this.getPreflopCards(from_player).length) throw new RuntimeException();
        PrivateCards player_combo = this.getPreflopCards(from_player)[index];
        int to_player_index = this.card_player_index[player_combo.hashCode()][to_player];
        if(to_player_index == -1){
            return null;
        }else{
            return to_player_index;
        }
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

    public void setRelativeProbs() throws BoardNotFoundException{
        int players = this.private_cards.length;
        for(int player_id = 0; player_id < players;player_id ++){
            // TODO 这里只考虑了两个玩家的情况
            int oppo = 1 - player_id;
            float player_prob_sum = 0;

            for(int i = 0;i < this.private_cards[player_id].length;i ++) {
                float oppo_prob_sum = 0;
                PrivateCards player_card = this.private_cards[player_id][i];
                long player_long = Card.boardInts2long(new int[]{player_card.card1, player_card.card2});

                //
                if (Card.boardsHasIntercept(player_long,board)){
                    continue;
                }

                for (int j = 0; j < this.private_cards[oppo].length; j++) {
                    PrivateCards oppo_card = this.private_cards[oppo][j];
                    long oppo_long = Card.boardInts2long(new int[]{oppo_card.card1, oppo_card.card2});
                    if (Card.boardsHasIntercept(oppo_long,this.board)
                            || Card.boardsHasIntercept(oppo_long,player_long)
                            ){
                        continue;
                    }
                    oppo_prob_sum += oppo_card.weight;

                }
                player_card.relative_prob = oppo_prob_sum * player_card.weight;
                player_prob_sum += player_card.relative_prob;
            }
            for(int i = 0;i < this.private_cards[player_id].length;i ++) {
                PrivateCards player_card = this.private_cards[player_id][i];
                player_card.relative_prob = player_card.relative_prob / player_prob_sum;
            }

        }
    }
}
