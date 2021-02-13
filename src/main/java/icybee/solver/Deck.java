package icybee.solver;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by huangxuefeng on 2019/10/6.
 */
public class Deck {
    List<String> ranks;
    List<String> suits;
    List<String> cards_str = new ArrayList<String>();

    public List<Card> getCards() {
        return cards;
    }

    List<Card> cards = new ArrayList<Card>();
    public Deck(List<String> ranks, List<String> suits){
        this.ranks = ranks;
        this.suits = suits;
        for(String one_rank : ranks){
            for(String one_suit: suits){
                String one_card = one_rank + one_suit;
                cards_str.add(one_card);
                cards.add(new Card(one_card));
            }
        }

    }
}

