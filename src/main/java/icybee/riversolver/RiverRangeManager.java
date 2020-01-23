package icybee.riversolver;

import icybee.riversolver.compairer.Compairer;
import icybee.riversolver.exceptions.BoardNotFoundException;
import icybee.riversolver.ranges.PrivateCards;
import icybee.riversolver.ranges.RiverCombs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RiverRangeManager
{
    Map<Long, RiverCombs[]> p1RiverRanges = new HashMap<>();
    Map<Long, RiverCombs[]> p2RiverRanges = new HashMap<>();

    Compairer handEvaluator;

    public RiverRangeManager(Compairer compairer){
        this.handEvaluator = compairer;
    }

    public RiverCombs[] getRiverCombos(int player, RiverCombs[] riverCombos, int[] board) throws BoardNotFoundException
    {
        PrivateCards[] preflopCombos = new PrivateCards[riverCombos.length];
        for(int i = 0;i < riverCombos.length;i ++){
            preflopCombos[i] = riverCombos[i].private_cards;
        }
        return getRiverCombos(player,preflopCombos,board);
    }

    public RiverCombs[] getRiverCombos(int player, PrivateCards[] preflopCombos, int[] board) throws BoardNotFoundException {
        long board_long = Card.boardInts2long(board);
        return this.getRiverCombos(player,preflopCombos,board_long);
    }

    public RiverCombs[] getRiverCombos(int player, PrivateCards[] preflopCombos, long board_long)
    {
        Map<Long, RiverCombs[]> riverRanges;

        if (player == 1)
            riverRanges = p1RiverRanges;
        else
            riverRanges = p2RiverRanges;

        long key = board_long;

        if (riverRanges.get(key) != null)
            return riverRanges.get(key);

        int count = 0;

        for (int hand = 0; hand < preflopCombos.length; hand++) {
            PrivateCards one_hand = preflopCombos[hand];
            if (!Card.boardsHasIntercept(
                    one_hand.toBoardLong(), board_long
            ))
                count++;
        }

        int index = 0;
        RiverCombs[] riverCombos = new RiverCombs[count];

        for (int hand = 0; hand < preflopCombos.length; hand++)
        {
            PrivateCards preflopCombo = preflopCombos[hand];


            if (Card.boardsHasIntercept(
                    preflopCombo.toBoardLong(), board_long
            )){
                continue;
            }

            int rank = this.handEvaluator.get_rank(preflopCombo.toBoardLong(),board_long);
            RiverCombs riverCombo = new RiverCombs(Card.long2board(board_long),preflopCombo,rank, hand);
            riverCombos[index++] = riverCombo;
        }

        Arrays.sort(riverCombos);

        riverRanges.put(key, riverCombos);

        return riverCombos;
    }
}

