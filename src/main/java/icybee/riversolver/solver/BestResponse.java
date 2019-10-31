package icybee.riversolver.solver;

import icybee.riversolver.Card;
import icybee.riversolver.RiverRangeManager;
import icybee.riversolver.compairer.Compairer;
import icybee.riversolver.exceptions.BoardNotFoundException;
import icybee.riversolver.exceptions.NodeNotFoundException;
import icybee.riversolver.nodes.ActionNode;
import icybee.riversolver.nodes.GameTreeNode;
import icybee.riversolver.nodes.ShowdownNode;
import icybee.riversolver.nodes.TerminalNode;
import icybee.riversolver.ranges.PrivateCards;
import icybee.riversolver.ranges.RiverCombs;
import icybee.riversolver.utils.Range;

/**
 * Created by huangxuefeng on 2019/10/12.
 * best response calculator
 */
public class BestResponse {

    // player -> preflop combos
    RiverCombs[][] river_combos;
    int[] player_hands;
    int player_number;
    RiverRangeManager rrm;

    public BestResponse(RiverCombs[][] river_combos, int player_number, Compairer compairer) {
        this.river_combos = river_combos;
        this.player_number = player_number;
        this.rrm = RiverRangeManager.getInstance(compairer);

        if(river_combos.length != player_number)
            throw new RuntimeException(
                String.format("river combo length NE player nunber: %d -- %d",river_combos.length,player_number)
            );
        player_hands = new int[player_number];
        for(int i = 0;i < player_number;i ++) {
            player_hands[i] = river_combos[i].length;
            int oppo = (i + 1) % player_number;
            if(river_combos[i].length != river_combos[oppo].length){
                throw new RuntimeException("river combo length not match");
            }
        }

    }

    public void printExploitability(GameTreeNode root, int iterationCount, float initial_pot, int[] initialBoard) throws BoardNotFoundException{
        float[][] reach_probs = new float[this.player_number][];

        System.out.println(String.format("Iter: %d",iterationCount));
        float exploitible = 0;
        // 构造双方初始reach probs(按照手牌weights)
        for (int player_id = 0; player_id < this.player_number; player_id++) {
            float[] reach_prob_player = new float[river_combos[player_id].length];
            for (int hc = 0; hc < river_combos[player_id].length; hc++)
                reach_prob_player[hc] = river_combos[player_id][hc].private_cards.weight;
            reach_probs[player_id] = reach_prob_player;
        }

        for (int player_id = 0; player_id < this.player_number; player_id++) {
            float player_exploitability = getBestReponseEv(root, player_id, reach_probs, initialBoard);
            exploitible += player_exploitability;
            System.out.println(String.format("player %d exploitability %f", player_id, player_exploitability));
        }
        float total_exploitability = exploitible / this.player_number / initial_pot * 100;
        System.out.println(String.format("Total exploitability %f ", total_exploitability));
    }

    public float getBestReponseEv(GameTreeNode node, int player, float[][] reach_probs, int[] initialBoard) throws BoardNotFoundException{
        float ev = 0;
        //考虑（1）相对的手牌 proability,(2)被场面和对手ban掉的手牌
        float[] private_cards_evs = bestResponse(node, player, reach_probs, initialBoard);
        // TODO finish here
        RiverCombs[] player_combo = this.river_combos[player];
        RiverCombs[] oppo_combo = this.river_combos[1 - player];

        for(int player_hand = 0;player_hand < player_combo.length;player_hand ++){
            float one_payoff = private_cards_evs[player_hand];
            RiverCombs one_player_hand = player_combo[player_hand];
            long private_long = Card.boardInts2long(new int[]{one_player_hand.private_cards.card1,one_player_hand.private_cards.card2});
            if(Card.boardsHasIntercept(private_long,Card.boardInts2long(initialBoard))){
                continue;
            }
            float oppo_sum = 0;

            for(int oppo_hand = 0;oppo_hand < oppo_combo.length;oppo_hand ++){
                RiverCombs one_oppo_hand = oppo_combo[oppo_hand];
                long private_long_oppo = Card.boardInts2long(new int[]{one_oppo_hand.private_cards.card1,one_oppo_hand.private_cards.card2});
                if(Card.boardsHasIntercept(private_long,private_long_oppo)
                        || Card.boardsHasIntercept(private_long_oppo,Card.boardInts2long(initialBoard))){
                    continue;
                }
                oppo_sum += one_oppo_hand.private_cards.weight;
            }
            ev +=  one_payoff * one_player_hand.private_cards.relative_prob / oppo_sum;

        }

        return ev;
    }

    /*
    public float[] getUnblockedComboCounts(PreflopCombo[] heroCombos, PreflopCombo[] villainCombos, int[] initialBoard)
    {
    }
    */

    public float[] bestResponse(GameTreeNode node, int player, float[][] reach_probs, int[] board) throws BoardNotFoundException{
        if (node instanceof ActionNode)
            return actionBestResponse((ActionNode) node, player, reach_probs, board);
        else if (node instanceof ShowdownNode)
            return showdownBestResponse((ShowdownNode) node, player, reach_probs, board);
        else if (node instanceof TerminalNode)
            return terminalBestReponse((TerminalNode) node, player, reach_probs, board);
        else
            throw new RuntimeException(String.format("Node type not understood %s", node.getClass().getName()));
    }

    public float[] actionBestResponse(ActionNode node, int player, float[][] reach_probs, int[] board) throws BoardNotFoundException{
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
            return my_exploitability;
        }else{
            // 如果是别人做决定，那么就按照别人的策略加权算出一个 ev
            float[] total_payoffs = new float[player_hands[player]];
            for(int i = 0 ;i < total_payoffs.length;i ++){
                total_payoffs[i] = 0;
            }

            float[] node_strategy = node.getTrainable().getAverageStrategy();
            if(node_strategy.length != node.getChildrens().size())
                throw new RuntimeException(String.format("strategy size not match %d - %d",
                        node_strategy.length,node.getChildrens().size()));

            // 构造reach probs矩阵
            for(int action_ind = 0;action_ind < node.getChildrens().size();action_ind ++){
                float[][] next_reach_probs = new float[this.player_number][];
                for(int i = 0;i < this.player_number;i ++){
                    float[] next_reach_probs_current_player = new float[reach_probs.length];
                    for(int j = 0;j < reach_probs.length;j ++){
                        next_reach_probs_current_player[j] = reach_probs[node.getPlayer()][j] * node_strategy[action_ind];
                    }
                    next_reach_probs[i] = next_reach_probs_current_player;
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
            return total_payoffs;
        }
    }

    /*
    public float[] chanceBestReponse(ChanceNode node, float[] villainReachProbs, int[] board)
    {
    }
    */

    public float[] terminalBestReponse(TerminalNode node, int player, float[][] reach_probs, int[] board) throws BoardNotFoundException{
        // TODO finish this
        long board_long = Card.boardInts2long(board);

        Double player_payoff = node.get_payoffs()[player];
        if(player_payoff == null) throw new RuntimeException(String.format("player %d 's payoff is not found",player));
        float[] payoffs = new float[player_hands[player]];

        //TODO support  more player
        if(this.player_number != 2) throw new RuntimeException("player NE 2 not supported");
        // 对手的手牌可能需要和其reach prob一样长
        if(this.river_combos[1 - player].length != reach_probs[1 - player].length) throw new RuntimeException("length not match");

        // TODO 写的通用一些，这里用了hard code，因为一副牌，不管是长牌还是短牌，最多扑克牌的数量都是52张
        float[] oppo_card_sum = new float[52];
        for(int i = 0;i < oppo_card_sum.length;i ++) oppo_card_sum[i] = 0;

        //用于记录对手总共的手牌绝对prob之和
        float oppo_prob_sum = 0;
        RiverCombs[] oppo_combs = this.river_combos[1 - player];

        float[] oppo_reach_prob = reach_probs[1 - player];
        for(int oppo_hand = 0;oppo_hand < oppo_reach_prob.length; oppo_hand ++){
            PrivateCards one_hc = oppo_combs[oppo_hand].private_cards;
            long one_hc_long  = Card.boardInts2long(new int[]{one_hc.card1,one_hc.card2});

            // 如果对手手牌和public card有重叠，那么这组牌不可能存在
            if(Card.boardsHasIntercept(one_hc_long,board_long)){
                continue;
            }

            oppo_prob_sum += oppo_reach_prob[oppo_hand];
            oppo_card_sum[one_hc.card1] += oppo_reach_prob[oppo_hand];
            oppo_card_sum[one_hc.card2] += oppo_reach_prob[oppo_hand];
        }


        for(int player_hand = 0;player_hand < this.river_combos[player].length;player_hand ++) {
            RiverCombs player_hc = this.river_combos[player][player_hand];
            long player_hc_long = Card.boardInts2long(new int[]{player_hc.private_cards.card1,player_hc.private_cards.card2});
            if(Card.boardsHasIntercept(player_hc_long,board_long)){
                payoffs[player_hand] = 0;
            }else{
                payoffs[player_hand] = (oppo_prob_sum
                        - oppo_card_sum[player_hc.private_cards.card1]
                        - oppo_card_sum[player_hc.private_cards.card2]
                        + oppo_reach_prob[player_hand]
                        ) * player_payoff.floatValue();
            }
        }


        return payoffs;
    }

    /*
    //assumes that both players got allin on the turn
    float[] allinBestResponse(TerminalNode node, float[] villainReachProbs, int[] board)
    {
    }
    */

    float[] showdownBestResponse(ShowdownNode node, int player, float[][] reach_probs, int[] board) throws BoardNotFoundException {
        // TODO finish this
        if(this.player_number != 2) throw new RuntimeException("player number is not 2");

        int oppo = 1 - player;
        RiverCombs[] player_combs = this.rrm.getRiverCombos(player,this.river_combos[player],board);  //this.river_combos[player];
        RiverCombs[] oppo_combs = this.rrm.getRiverCombos(1 - player,this.river_combos[1 - player],board);  //this.river_combos[player];

        float win_payoff = node.get_payoffs(ShowdownNode.ShowDownResult.NOTTIE,player)[player].floatValue();
        // TODO hard code, 假设了player只有两个
        float lose_payoff = node.get_payoffs(ShowdownNode.ShowDownResult.NOTTIE,1 - player)[player].floatValue();

        float[] payoffs = new float[player_hands[player]];


        // 计算胜利时的payoff
        // TODO 修改掉这里的hard code
        float winsum = 0;
        float[] card_winsum = new float[52];
        for(int i = 0;i < card_winsum.length;i ++) card_winsum[i] = 0;

        int j = 0;
        if(player_combs.length != oppo_combs.length) throw new RuntimeException("");

        for(int i = 0;i < player_combs.length;i ++){
            RiverCombs one_player_comb = player_combs[i];
            while (one_player_comb.rank < oppo_combs[j].rank){
                RiverCombs one_oppo_comb = player_combs[j];
                winsum += reach_probs[oppo][one_oppo_comb.reach_prob_index];

                // TODO 这里有问题，要加上reach prob，但是reach prob的index怎么解决？
                card_winsum[one_oppo_comb.private_cards.card1] += reach_probs[oppo][one_oppo_comb.reach_prob_index];
                card_winsum[one_oppo_comb.private_cards.card2] += reach_probs[oppo][one_oppo_comb.reach_prob_index];
                j ++;
            }
            payoffs[i] = (winsum
                    - card_winsum[one_player_comb.private_cards.card1]
                    - card_winsum[one_player_comb.private_cards.card2]
                    ) * win_payoff;
        }

        // 计算失败时的payoff
        float losssum = 0;
        float[] card_losssum = new float[52];
        for(int i = 0;i < card_losssum.length;i ++) card_losssum[i] = 0;

        j = oppo_combs.length - 1;
        for(int i = player_combs.length - 1;i >= 0;i --){
            RiverCombs one_player_comb = player_combs[i];
            while (one_player_comb.rank > oppo_combs[j].rank){
                RiverCombs one_oppo_comb = player_combs[j];
                losssum += reach_probs[oppo][one_oppo_comb.reach_prob_index];

                // TODO 这里有问题，要加上reach prob，但是reach prob的index怎么解决？
                card_losssum[one_oppo_comb.private_cards.card1] += reach_probs[oppo][one_oppo_comb.reach_prob_index];
                card_losssum[one_oppo_comb.private_cards.card2] += reach_probs[oppo][one_oppo_comb.reach_prob_index];
                j --;
            }
            payoffs[i] -= (losssum
                    - card_losssum[one_player_comb.private_cards.card1]
                    - card_losssum[one_player_comb.private_cards.card2]
            ) * lose_payoff;
        }
        return payoffs;
    }
}

