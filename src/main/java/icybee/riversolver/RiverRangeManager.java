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

    private static RiverRangeManager instance = null;

    RiverRangeManager(Compairer compairer){
        this.handEvaluator = compairer;
    }

    public static RiverRangeManager getInstance(Compairer compairer)
    {
        if (instance == null)
            instance = new RiverRangeManager(compairer);

        return instance;
    }

    public RiverCombs[] getRiverCombos(int player, RiverCombs[] riverCombos, int[] board) throws BoardNotFoundException
    {
        PrivateCards[] preflopCombos = new PrivateCards[riverCombos.length];
        for(int i = 0;i < riverCombos.length;i ++){
            preflopCombos[i] = riverCombos[i].private_cards;
        }
        return getRiverCombos(player,preflopCombos,board);
    }
    public RiverCombs[] getRiverCombos(int player, PrivateCards[] preflopCombos, int[] board) throws BoardNotFoundException
    {
        Map<Long, RiverCombs[]> riverRanges;

        if (player == 1)
            riverRanges = p1RiverRanges;
        else
            riverRanges = p2RiverRanges;

        long key = Card.boardInts2long(board);

        if (riverRanges.get(key) != null)
            return riverRanges.get(key);

        int count = 0;

        for (int hand = 0; hand < preflopCombos.length; hand++) {
            PrivateCards one_hand = preflopCombos[hand];
            if (!Card.boardsHasIntercept(
                    Card.boardInts2long(new int[]{one_hand.card1,one_hand.card2}), Card.boardInts2long(board)
            ))
                count++;
        }

        int index = 0;
        RiverCombs[] riverCombos = new RiverCombs[count];

        for (int hand = 0; hand < preflopCombos.length; hand++)
        {
            PrivateCards preflopCombo = preflopCombos[hand];


            if (Card.boardsHasIntercept(
                    Card.boardInts2long(new int[]{preflopCombo.card1,preflopCombo.card2}), Card.boardInts2long(board)
            )){
                continue;
            }

            int rank = this.handEvaluator.get_rank(new int[]{preflopCombo.card1,preflopCombo.card2},board);
            RiverCombs riverCombo = new RiverCombs(board,preflopCombo,rank, hand);
            riverCombos[index++] = riverCombo;
        }

        Arrays.sort(riverCombos);

        riverRanges.put(key, riverCombos);

        return riverCombos;
    }
}

