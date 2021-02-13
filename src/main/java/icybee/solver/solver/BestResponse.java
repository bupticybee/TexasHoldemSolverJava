package icybee.solver.solver;

import icybee.solver.Card;
import icybee.solver.Deck;
import icybee.solver.RiverRangeManager;
import icybee.solver.compairer.Compairer;
import icybee.solver.exceptions.BoardNotFoundException;
import icybee.solver.exceptions.NodeNotFoundException;
import icybee.solver.nodes.*;
import icybee.solver.ranges.PrivateCards;
import icybee.solver.ranges.PrivateCardsManager;
import icybee.solver.ranges.RiverCombs;
import icybee.solver.utils.Range;

import java.util.Arrays;
import java.util.List;

/**
 * Created by huangxuefeng on 2019/10/12.
 * best response calculator
 */
public class BestResponse {

    private Deck deck;
    // player -> preflop combos
    PrivateCards[][] private_combos;
    int[] player_hands;
    int player_number;
    RiverRangeManager rrm;
    PrivateCardsManager pcm;
    boolean debug;

    public BestResponse(PrivateCards[][] private_combos, int player_number, Compairer compairer, PrivateCardsManager pcm, RiverRangeManager rrm, Deck deck, boolean debug) {
        this.private_combos = private_combos;
        this.player_number = player_number;
        this.rrm = rrm;
        this.pcm = pcm;
        this.debug = debug;
        this.deck = deck;

        if(private_combos.length != player_number)
            throw new RuntimeException(
                String.format("river combo length NE player nunber: %d -- %d",private_combos.length,player_number)
            );
        player_hands = new int[player_number];
        for(int i = 0;i < player_number;i ++) {
            player_hands[i] = private_combos[i].length;
            /*
            int oppo = (i + 1) % player_number;
            if(river_combos[i].length != river_combos[oppo].length){
                throw new RuntimeException("river combo length not match");
            }
             */
        }

    }

    public float printExploitability(GameTreeNode root, int iterationCount, float initial_pot, long initialBoard) throws BoardNotFoundException{
        float[][] reach_probs = new float[this.player_number][];

        System.out.println(String.format("Iter: %d",iterationCount));
        float exploitible = 0;
        // 构造双方初始reach probs(按照手牌weights)
        for (int player_id = 0; player_id < this.player_number; player_id++) {
            float[] reach_prob_player = new float[private_combos[player_id].length];
            for (int hc = 0; hc < private_combos[player_id].length; hc++)
                reach_prob_player[hc] = private_combos[player_id][hc].weight;
            reach_probs[player_id] = reach_prob_player;
        }

        for (int player_id = 0; player_id < this.player_number; player_id++) {
            float player_exploitability = getBestReponseEv(root, player_id, reach_probs, initialBoard);
            exploitible += player_exploitability;
            System.out.println(String.format("player %d exploitability %f", player_id, player_exploitability));
        }
        float total_exploitability = exploitible / this.player_number / initial_pot * 100;
        System.out.println(String.format("Total exploitability %f precent", total_exploitability));
        return total_exploitability;
    }

    public float getBestReponseEv(GameTreeNode node, int player, float[][] reach_probs, long initialBoard) throws BoardNotFoundException{
        float ev = 0;
        //考虑（1）相对的手牌 proability,(2)被场面和对手ban掉的手牌
        float[] private_cards_evs = bestResponse(node, player, reach_probs, initialBoard);
        PrivateCards[] player_combo = this.private_combos[player];
        PrivateCards[] oppo_combo = this.private_combos[1 - player];

        for(int player_hand = 0;player_hand < player_combo.length;player_hand ++){
            float one_payoff = private_cards_evs[player_hand];
            PrivateCards one_player_hand = player_combo[player_hand];
            long private_long = one_player_hand.toBoardLong();
            if(Card.boardsHasIntercept(private_long,initialBoard)){
                continue;
            }
            float oppo_sum = 0;

            for(int oppo_hand = 0;oppo_hand < oppo_combo.length;oppo_hand ++){
                PrivateCards one_oppo_hand = oppo_combo[oppo_hand];
                long private_long_oppo = one_oppo_hand.toBoardLong();
                if(Card.boardsHasIntercept(private_long,private_long_oppo)
                        || Card.boardsHasIntercept(private_long_oppo,initialBoard)){
                    continue;
                }
                oppo_sum += one_oppo_hand.weight;
            }
            ev +=  one_payoff * one_player_hand.relative_prob / oppo_sum;

        }

        return ev;
    }

    /*
    public float[] getUnblockedComboCounts(PreflopCombo[] heroCombos, PreflopCombo[] villainCombos, int[] initialBoard)
    {
    }
    */

    public float[] bestResponse(GameTreeNode node, int player, float[][] reach_probs, long board){
        if (node instanceof ActionNode)
            return actionBestResponse((ActionNode) node, player, reach_probs, board);
        else if (node instanceof ShowdownNode)
            return showdownBestResponse((ShowdownNode) node, player, reach_probs, board);
        else if (node instanceof TerminalNode)
            return terminalBestReponse((TerminalNode) node, player, reach_probs, board);
        else if (node instanceof ChanceNode)
            return chanceBestReponse((ChanceNode) node, player, reach_probs, board);
        else
            throw new RuntimeException(String.format("Node type not understood %s", node.getClass().getName()));
    }

    private float[] chanceBestReponse(ChanceNode node, int player, float[][] reach_probs, long current_board) {
        List<Card> cards = this.deck.getCards();
        if(cards.size() != node.getChildrens().size()) throw new RuntimeException();
        //float[] cardWeights = getCardsWeights(player,reach_probs[1 - player],current_board);

        int card_num = node.getCards().size();
        // 可能的发牌情况,2代表每个人的holecard是两张
        int possible_deals = node.getChildrens().size() - Card.long2board(current_board).length - 2;
        float[] chance_utility = new float[reach_probs[player].length];
        // 遍历每一种发牌的可能性
        for(int card = 0;card < node.getCards().size();card ++){
            GameTreeNode one_child = node.getChildrens().get(card);
            Card one_card = node.getCards().get(card);
            long card_long = Card.boardCards2long(new Card[]{one_card});

            // 不可能发出和board重复的牌，对吧
            if(Card.boardsHasIntercept(card_long,current_board)) continue;

            if(one_child == null || one_card == null) throw new RuntimeException("child is null");

            PrivateCards[] playerPrivateCard = this.pcm.getPreflopCards(player);//this.getPlayerPrivateCard(player);
            PrivateCards[] oppoPrivateCards = this.pcm.getPreflopCards(1 - player);

            float[][] new_reach_probs = new float[2][];

            if (!( reach_probs[player].length == playerPrivateCard.length))
                throw new RuntimeException("length mismatch");

            new_reach_probs[player] = new float[playerPrivateCard.length];
            new_reach_probs[1 - player] = new float[oppoPrivateCards.length];

            // 检查是否双方 hand和reach prob长度符合要求
            if(playerPrivateCard.length != reach_probs[player].length) throw new RuntimeException("length not match");
            if(oppoPrivateCards.length != reach_probs[1 - player].length) throw new RuntimeException("length not match");

            for(int one_player = 0;one_player < 2;one_player ++) {
                int player_hand_len = this.pcm.getPreflopCards(one_player).length;
                for (int player_hand = 0; player_hand < player_hand_len; player_hand++) {
                    PrivateCards one_private = this.pcm.getPreflopCards(one_player)[player_hand];
                    long privateBoardLong = one_private.toBoardLong();
                    if (Card.boardsHasIntercept(card_long, privateBoardLong)) continue;
                    new_reach_probs[one_player][player_hand] = reach_probs[one_player][player_hand] / possible_deals;
                }
            }

            if(Card.boardsHasIntercept(current_board,card_long))
                throw new RuntimeException("board has intercept with dealt card");
            long new_board_long = current_board | card_long;

            float[] child_utility = this.bestResponse(one_child,player,new_reach_probs,new_board_long);
            if(child_utility.length != chance_utility.length) throw new RuntimeException("length not match");
            for(int i = 0;i < child_utility.length;i ++)
                chance_utility[i] += child_utility[i];
        }

        return chance_utility;
    }

    public float[] actionBestResponse(ActionNode node, int player, float[][] reach_probs, long board){
        if(player == node.getPlayer()){
            // 如果是自己在做决定，那么肯定选对自己的最有利的，反之对于对方来说，这个就是我方expliot了对方,
            // 这里可以当成"player"做决定的时候，action prob是0-1分布，因为需要使用最好的策略去expliot对方，最好的策略一定是ont-hot的
            float[] my_exploitability = null;
            for(GameTreeNode one_node:node.getChildrens()){
                float[] node_ev = this.bestResponse(one_node,player,reach_probs,board);
                if(my_exploitability == null){
                    my_exploitability = node_ev;
                }else {
                    for (int i : Range.range(node_ev.length)) {
                        my_exploitability[i] = Float.max(my_exploitability[i],node_ev[i]);
                    }
                }
            }
            if(this.debug) {
                System.out.println("[action]");
                node.printHistory();
                System.out.println(Arrays.toString(my_exploitability));
            }
            return my_exploitability;
        }else{
            // 如果是别人做决定，那么就按照别人的策略加权算出一个 ev
            float[] total_payoffs = new float[player_hands[player]];

            float[] node_strategy = null;
            node_strategy = node.getTrainable().getAverageStrategy();
            if(node_strategy.length != node.getChildrens().size() * reach_probs[node.getPlayer()].length) {
                throw new RuntimeException(String.format("strategy size not match %d - %d",
                        node_strategy.length, node.getChildrens().size() * reach_probs[node.getPlayer()].length));
            }

            // 构造reach probs矩阵
            for(int action_ind = 0;action_ind < node.getChildrens().size();action_ind ++){
                float[][] next_reach_probs = new float[this.player_number][];
                for(int i = 0;i < this.player_number;i ++){
                    if(i == node.getPlayer()) {
                        int private_combo_numbers = reach_probs[i].length;
                        float[] next_reach_probs_current_player = new float[private_combo_numbers];
                        for (int j = 0; j < private_combo_numbers; j++) {
                            next_reach_probs_current_player[j] =
                                    reach_probs[node.getPlayer()][j] * node_strategy[action_ind * private_combo_numbers + j];
                        }
                        next_reach_probs[i] = next_reach_probs_current_player;
                    }else{
                        next_reach_probs[i] = reach_probs[i];
                    }
                }


                GameTreeNode one_child = node.getChildrens().get(action_ind);
                if (one_child == null)
                    throw new NodeNotFoundException("child node not found");
                float[] action_payoffs = this.bestResponse(one_child,player,next_reach_probs,board);
                if (action_payoffs.length != total_payoffs.length)
                    throw new RuntimeException(
                            String.format(
                                    "length not match between action payoffs and total payoffs %d -- %d",
                                    action_payoffs.length,total_payoffs.length
                            )
                    );

                for(int i = 0 ;i < total_payoffs.length;i ++){
                    total_payoffs[i] += action_payoffs[i];//  * node_strategy[i] 的动作实际上已经在递归的时候做过了，所以这里不需要乘
                }
            }
            if(this.debug) {
                System.out.println("[action]");
                node.printHistory();
                System.out.println(Arrays.toString(total_payoffs));
            }
            return total_payoffs;
        }
    }

    /*
    public float[] chanceBestReponse(ChanceNode node, float[] villainReachProbs, int[] board)
    {
    }
    */

    public float[] terminalBestReponse(TerminalNode node, int player, float[][] reach_probs, long board){
        long board_long = board;
        int oppo = 1 - player;
        RiverCombs[] player_combs = this.rrm.getRiverCombos(player,this.pcm.getPreflopCards(player),board);  //this.river_combos[player];
        RiverCombs[] oppo_combs = this.rrm.getRiverCombos(1 - player,this.pcm.getPreflopCards(1 - player),board);  //this.river_combos[player];

        Double player_payoff = node.get_payoffs()[player];
        if(player_payoff == null) throw new RuntimeException(String.format("player %d 's payoff is not found",player));
        float[] payoffs = new float[player_hands[player]];

        if(this.player_number != 2) throw new RuntimeException("player NE 2 not supported");
        // 对手的手牌可能需要和其reach prob一样长
        // 这里用了hard code，因为一副牌，不管是长牌还是短牌，最多扑克牌的数量都是52张
        float[] oppo_card_sum = new float[52];

        //用于记录对手总共的手牌绝对prob之和
        float oppo_prob_sum = 0;

        float[] oppo_reach_prob = reach_probs[1 - player];
        for(int oppo_hand = 0;oppo_hand < oppo_combs.length; oppo_hand ++){
            RiverCombs one_hc = oppo_combs[oppo_hand];
            long one_hc_long  = Card.boardInts2long(new int[]{one_hc.private_cards.card1,one_hc.private_cards.card2});

            // 如果对手手牌和public card有重叠，那么这组牌不可能存在
            if(Card.boardsHasIntercept(one_hc_long,board_long)){
                continue;
            }

            oppo_prob_sum += oppo_reach_prob[one_hc.reach_prob_index];
            oppo_card_sum[one_hc.private_cards.card1] += oppo_reach_prob[one_hc.reach_prob_index];
            oppo_card_sum[one_hc.private_cards.card2] += oppo_reach_prob[one_hc.reach_prob_index];
        }


        for(int player_hand = 0;player_hand < player_combs.length;player_hand ++) {
            RiverCombs player_hc = player_combs[player_hand];
            long player_hc_long = Card.boardInts2long(new int[]{player_hc.private_cards.card1,player_hc.private_cards.card2});
            if(Card.boardsHasIntercept(player_hc_long,board_long)){
                payoffs[player_hand] = 0;
            }else{
                Integer oppo_hand = this.pcm.indPlayer2Player(player,oppo,player_hc.reach_prob_index);
                float add_reach_prob;
                if(oppo_hand == null){
                    add_reach_prob = 0;
                }else{
                    add_reach_prob = oppo_reach_prob[oppo_hand];
                }
                payoffs[player_hc.reach_prob_index] = (oppo_prob_sum
                        - oppo_card_sum[player_hc.private_cards.card1]
                        - oppo_card_sum[player_hc.private_cards.card2]
                        + add_reach_prob
                        ) * player_payoff.floatValue();
            }
        }

        if(this.debug) {
            System.out.println("[terminal]");
            node.printHistory();
            System.out.println(Arrays.toString(payoffs));
        }
        return payoffs;
    }

    /*
    //assumes that both players got allin on the turn
    float[] allinBestResponse(TerminalNode node, float[] villainReachProbs, int[] board)
    {
    }
    */

    float[] showdownBestResponse(ShowdownNode node, int player, float[][] reach_probs, long board) {
        if(this.player_number != 2) throw new RuntimeException("player number is not 2");

        int oppo = 1 - player;
        RiverCombs[] player_combs = this.rrm.getRiverCombos(player,this.pcm.getPreflopCards(player),board);  //this.river_combos[player];
        RiverCombs[] oppo_combs = this.rrm.getRiverCombos(1 - player,this.pcm.getPreflopCards(1 - player),board);  //this.river_combos[player];

        float win_payoff = node.get_payoffs(ShowdownNode.ShowDownResult.NOTTIE,player)[player].floatValue();
        // hard code, 假设了player只有两个
        float lose_payoff = node.get_payoffs(ShowdownNode.ShowDownResult.NOTTIE,1 - player)[player].floatValue();

        float[] payoffs = new float[player_hands[player]];


        // 计算胜利时的payoff
        float winsum = 0;
        float[] card_winsum = new float[52];
        for(int i = 0;i < card_winsum.length;i ++) card_winsum[i] = 0;

        int j = 0;
        //if(player_combs.length != oppo_combs.length) throw new RuntimeException("");

        for(int i = 0;i < player_combs.length;i ++){
            RiverCombs one_player_comb = player_combs[i];
            while (j < oppo_combs.length && one_player_comb.rank < oppo_combs[j].rank){
                RiverCombs one_oppo_comb = oppo_combs[j];
                winsum += reach_probs[oppo][one_oppo_comb.reach_prob_index];

                card_winsum[one_oppo_comb.private_cards.card1] += reach_probs[oppo][one_oppo_comb.reach_prob_index];
                card_winsum[one_oppo_comb.private_cards.card2] += reach_probs[oppo][one_oppo_comb.reach_prob_index];
                j ++;
            }
            payoffs[one_player_comb.reach_prob_index] = (winsum
                    - card_winsum[one_player_comb.private_cards.card1]
                    - card_winsum[one_player_comb.private_cards.card2]
                    ) * win_payoff;
        }

        // 计算失败时的payoff
        float losssum = 0;
        float[] card_losssum = new float[52];

        j = oppo_combs.length - 1;
        for(int i = player_combs.length - 1;i >= 0;i --){
            RiverCombs one_player_comb = player_combs[i];
            while (j >= 0 && one_player_comb.rank > oppo_combs[j].rank){
                RiverCombs one_oppo_comb = oppo_combs[j];
                losssum += reach_probs[oppo][one_oppo_comb.reach_prob_index];

                card_losssum[one_oppo_comb.private_cards.card1] += reach_probs[oppo][one_oppo_comb.reach_prob_index];
                card_losssum[one_oppo_comb.private_cards.card2] += reach_probs[oppo][one_oppo_comb.reach_prob_index];
                j --;
            }
            payoffs[one_player_comb.reach_prob_index] += (losssum
                    - card_losssum[one_player_comb.private_cards.card1]
                    - card_losssum[one_player_comb.private_cards.card2]
            ) * lose_payoff;
        }
        if(this.debug) {
            System.out.println("[showdown]");
            node.printHistory();
            System.out.println(Arrays.toString(payoffs));
        }
        return payoffs;
    }
}

