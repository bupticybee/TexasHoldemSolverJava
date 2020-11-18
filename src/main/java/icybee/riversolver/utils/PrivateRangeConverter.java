package icybee.riversolver.utils;

import icybee.riversolver.Card;
import icybee.riversolver.ranges.PrivateCards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PrivateRangeConverter {
    public static PrivateCards[] rangeStr2Cards(String range_str,int[] initial_boards){
        List<String> range_list = Arrays.asList(range_str.split(","));
        List<PrivateCards> private_cards = new ArrayList<PrivateCards>();

        for(String one_range:range_list){
            PrivateCards this_card = null;
            List<String> cardstr_arr = Arrays.asList(one_range.split(":"));
            if (cardstr_arr.size() > 2 || cardstr_arr.size() < 1){
                throw new RuntimeException("':' number exceeded 2");
            }
            float weight = 1;

            one_range = cardstr_arr.get(0);
            if(cardstr_arr.size() == 2){
                weight = Float.valueOf(cardstr_arr.get(1));
            }

            int range_len = one_range.length();
            // TODO finish here , convert str to a PrivateRanges[]
            if(range_len == 3){
                if(one_range.charAt(2) == 's'){
                    char rank1 = one_range.charAt(0);
                    char rank2 = one_range.charAt(1);
                    if(rank1 == rank2) throw new RuntimeException(String.format("%s%ss is not a valid card desc",rank1,rank2));
                    for(String one_suit :Card.getSuits()){
                        int card1 = Card.strCard2int(rank1 + one_suit);
                        int card2 = Card.strCard2int(rank2 + one_suit);
                        this_card = new PrivateCards(card1,card2,weight);
                        private_cards.add(this_card);
                    }

                }else if(one_range.charAt(2) == 'o'){
                    char rank1 = one_range.charAt(0);
                    char rank2 = one_range.charAt(1);

                    String[] suits = Card.getSuits();
                    for(int i = 0;i < suits.length;i++){
                        String one_suit = suits[i];
                        int begin_index = rank1 == rank2 ? i:0;
                        for(int j = begin_index;j < suits.length;j++){
                            String another_suit = suits[j];
                            if(one_suit == another_suit && rank1 == rank2){
                                continue;
                            }
                            int card1 = Card.strCard2int(rank1 + one_suit);
                            int card2 = Card.strCard2int(rank2 + another_suit);
                            if(Card.boardsHasIntercept(
                                    Card.boardInts2long(new int[]{card1,card2}),
                                    Card.boardInts2long(initial_boards)
                            )){
                                continue;
                            }
                            this_card = new PrivateCards(card1, card2, weight);
                            private_cards.add(this_card);
                        }
                    }
                }else{
                    throw new RuntimeException("format not recognize");
                }
            }else if(range_len == 2){
                char rank1 = one_range.charAt(0);
                char rank2 = one_range.charAt(1);
                String[] suits = Card.getSuits();
                for(int i = 0;i < suits.length;i++){
                    String one_suit = suits[i];
                    int begin_index = rank1 == rank2 ? i:0;
                    for(int j = begin_index;j < suits.length;j++){
                        String another_suit = suits[j];
                        if(one_suit == another_suit && rank1 == rank2){
                            continue;
                        }
                        int card1 = Card.strCard2int(rank1 + one_suit);
                        int card2 = Card.strCard2int(rank2 + another_suit);
                        if(Card.boardsHasIntercept(
                                Card.boardInts2long(new int[]{card1,card2}),
                                Card.boardInts2long(initial_boards)
                        )){
                            continue;
                        }
                        this_card = new PrivateCards(card1, card2, weight);
                        private_cards.add(this_card);
                    }
                }

            }else throw new RuntimeException(String.format(" range str %s len not valid ",one_range));
        }

        // 排除初试range中重复的情况
        for(int i = 0;i < private_cards.size();i ++){
            for(int j = i + 1;j < private_cards.size();j ++) {
                PrivateCards one_cards = private_cards.get(i);
                PrivateCards another_cards = private_cards.get(j);
                if (one_cards.card1 == another_cards.card1 && one_cards.card2 == another_cards.card2){
                    throw new RuntimeException(String.format("card %s %s duplicate"
                            , Card.intCard2Str(one_cards.card1)
                            , Card.intCard2Str(one_cards.card2)
                    ));
                }
                if(one_cards.card1 == another_cards.card2 && one_cards.card2 == another_cards.card1) {
                    throw new RuntimeException(String.format("card %s %s duplicate"
                            , Card.intCard2Str(one_cards.card1)
                            , Card.intCard2Str(one_cards.card2)
                    ));
                }
            }
        }

        PrivateCards[] private_cards_list = new PrivateCards[private_cards.size()];
        for(int i = 0;i < private_cards.size();i ++){
            private_cards_list[i] = private_cards.get(i);
            //System.out.print(String.format("[%s-%s]",Card.intCard2Str(private_cards_list[i].card1),Card.intCard2Str(private_cards_list[i].card2)));
        }
        /*
            output all private combos

        System.out.println("private range number:");
        System.out.println(private_cards.size());
        System.out.println();
         */
        return private_cards_list;
    }
}
