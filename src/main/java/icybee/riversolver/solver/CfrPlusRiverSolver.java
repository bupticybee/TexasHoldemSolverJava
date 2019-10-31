package icybee.riversolver.solver;

import icybee.riversolver.Card;
import icybee.riversolver.Deck;
import icybee.riversolver.GameTree;
import icybee.riversolver.RiverRangeManager;
import icybee.riversolver.compairer.Compairer;
import icybee.riversolver.exceptions.BoardNotFoundException;
import icybee.riversolver.nodes.*;
import icybee.riversolver.ranges.PrivateCards;
import icybee.riversolver.ranges.PrivateCardsManager;
import icybee.riversolver.ranges.RiverCombs;
import icybee.riversolver.trainable.CfrPlusTrainable;
import icybee.riversolver.trainable.Trainable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

/**
 * Created by huangxuefeng on 2019/10/11.
 * contains code for cfr solver
 * Be ware that river solvers doesn't consider chance nodes.
 */
public class CfrPlusRiverSolver extends Solver{
    PrivateCards[] range1;
    PrivateCards[] range2;
    int[] board;
    long board_long;
    Compairer compairer;

    Deck deck;
    ForkJoinPool forkJoinPool;
    RiverRangeManager rrm;
    int player_number;
    int iteration_number;
    PrivateCardsManager pcm;

    PrivateCards[] playerHands(int player){
        if(player == 0){
            return range1;
        }else if (player == 1){
            return range2;
        }else{
            throw new RuntimeException("player not found");
        }
    }

    PrivateCards[] getPlayerPrivateCard(int player){
        if(player == 0){
            return range1;
        }else if(player == 1){
            return range2;
        }else{
            throw new RuntimeException("player not found");
        }
    }

    float[][] getReachProbs(){
        float[][] retval = new float[this.player_number][];
        for(int player = 0;player < this.player_number;player ++){
            PrivateCards[] player_cards = this.playerHands(player);
            float[] reach_prob = new float[player_cards.length];
            for(int i = 0;i < player_cards.length;i ++){
                reach_prob[i] = player_cards[i].weight;
            }
            retval[player] = reach_prob;
        }
        return retval;
    }

    public void noDuplicateRange(PrivateCards[] private_range){
        Map<Integer,Boolean> rangekv = new HashMap<>();
        for(PrivateCards one_range:private_range){
            if(rangekv.get(one_range.hashCode()))
                throw new RuntimeException(String.format("duplicated key %d",one_range.toString()));
            rangekv.put(one_range.hashCode(),Boolean.TRUE);
        }
    }

    public CfrPlusRiverSolver(GameTree tree, PrivateCards[] range1 , PrivateCards[] range2, int[] board, Compairer compairer,Deck deck) throws BoardNotFoundException{
        super(tree);
        this.noDuplicateRange(range1);
        this.noDuplicateRange(range2);
        this.range1 = range1;
        this.range2 = range2;
        // TODO currently only support river
        if(board.length != 5) throw new RuntimeException(String.format("board length %d",board.length));
        this.board = board;
        this.board_long = Card.boardInts2long(board);
        this.compairer = compairer;

        this.deck = deck;

        int nThreads = Runtime.getRuntime().availableProcessors();
        this.forkJoinPool = new ForkJoinPool(nThreads);

        this.rrm = RiverRangeManager.getInstance(compairer);
        this.player_number = 2;
        this.iteration_number = 1000;

        PrivateCards[][] private_cards = new PrivateCards[this.player_number][];
        private_cards[0] = range1;
        private_cards[1] = range2;
        pcm = new PrivateCardsManager(private_cards,this.player_number,Card.boardInts2long(this.board));

    }


    void setTrainable(GameTreeNode root){
        if(root instanceof ActionNode){
            ActionNode action_node = (ActionNode)root;
            action_node.setTrainable(new CfrPlusTrainable(action_node));

            List<GameTreeNode> childrens =  action_node.getChildrens();
            for(GameTreeNode one_child:childrens) setTrainable(one_child);
        }else if(root instanceof TerminalNode){

        }else if(root instanceof ShowdownNode){

        }

    }

    @Override
    public void train(Map training_config) throws BoardNotFoundException {
        setTrainable(tree.getRoot());

        RiverCombs[][] player_rivers = new RiverCombs[this.player_number][];
        player_rivers[0] = rrm.getRiverCombos(0, this.range1, board);
        player_rivers[1] = rrm.getRiverCombos(1, this.range2, board);

        BestResponse br = new BestResponse(player_rivers,this.player_number,this.compairer);

        br.printExploitability(tree.getRoot(), 0, tree.getRoot().getPot().floatValue(), board);

        float[][] reach_probs = this.getReachProbs();

        for(int i = 0;i < this.iteration_number;i++){
            for(int player_id = 0;player_id < this.player_number;player_id ++) {
                cfr(player_id,this.tree.getRoot(),reach_probs,i);
            }
        }
    }

    float[] cfr(int player,GameTreeNode node,float[][] reach_probs,int iter) throws BoardNotFoundException{
        float[] utility = null;
        if(this.player_number != 2) throw new RuntimeException("player number is not 2");
        if(node instanceof ActionNode) {
            utility = actionUtility(player, (ActionNode) node,reach_probs, iter);
        }else if(node instanceof ShowdownNode){
            utility = showdownUtility(player,(ShowdownNode)node,reach_probs,iter);
        }else if(node instanceof TerminalNode){
            utility = terminalUtility(player,(TerminalNode) node,reach_probs,iter);
        }
        return utility;
    }

    float[] actionUtility(int player,ActionNode node,float[][]reach_probs,int iter) throws BoardNotFoundException{
        int oppo = 1 - player;
        PrivateCards[] player_private_cards = this.getPlayerPrivateCard(player);
        PrivateCards[] oppo_private_cards = this.getPlayerPrivateCard(oppo);
        Trainable trainable = node.getTrainable();

        float[] payoffs = new float[player_private_cards.length];
        List<GameTreeNode> children =  node.getChildrens();
        List<GameActions> actions =  node.getActions();
        if(node.getPlayer() == player){
            // node的player是当前cfr循环的player
            float[] current_strategy = trainable.getcurrentStrategy();
            float[][] new_reach_prob = new float[this.player_number][];
            if (current_strategy.length != actions.size() * player_private_cards.length) throw new RuntimeException("length not match");
            new_reach_prob[oppo] = reach_probs[oppo];
            for(int action_id = 0;action_id < actions.size(); action_id++) {
                float[] player_new_reach = new float[reach_probs[player].length];
                for(int hand_id = 0;hand_id < player_new_reach.length;hand_id ++){
                    float strategy_prob = current_strategy[hand_id + action_id * player_private_cards.length];
                    player_new_reach[hand_id] = reach_probs[player][hand_id] * strategy_prob;
                }
                new_reach_prob[player] = player_new_reach;
                float[] action_utilities = this.cfr(player,children.get(action_id),new_reach_prob,iter);

                // cfr结果是每手牌的收益，payoffs代表的也是每手牌的收益，他们的长度理应相等
                if(action_utilities.length != payoffs.length) throw new RuntimeException("action and payoff length not match");

                for(int hand_id = 0;hand_id < player_new_reach.length;hand_id ++){
                    float strategy_prob = current_strategy[hand_id + action_id * player_private_cards.length];
                    payoffs[hand_id] += strategy_prob * action_utilities[hand_id];
                }
            }
            // TODO finish here 完成regret matching
            return payoffs;
        }else{

        }
        return payoffs;
    }

    float[] showdownUtility(int player,ShowdownNode node,float[][]reach_probs,int iter) throws BoardNotFoundException{
        // player win时候player的收益，player lose的时候收益明显为-player_payoff
        int oppo = 1 - player;
        float win_payoff = node.get_payoffs(ShowdownNode.ShowDownResult.NOTTIE,player)[player].floatValue();
        float lose_payoff = node.get_payoffs(ShowdownNode.ShowDownResult.NOTTIE,player)[player].floatValue();
        PrivateCards[] player_private_cards = this.getPlayerPrivateCard(player);
        PrivateCards[] oppo_private_cards = this.getPlayerPrivateCard(oppo);

        RiverCombs[] player_combs = this.rrm.getRiverCombos(player,player_private_cards,this.board);
        RiverCombs[] oppo_combs = this.rrm.getRiverCombos(oppo,oppo_private_cards,this.board);

        float[] payoffs = new float[player_private_cards.length];


        float winsum = 0;
        float[] card_winsum = new float[52];

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

    float[] terminalUtility(int player,TerminalNode node,float[][]reach_prob,int iter) throws BoardNotFoundException{

        Double player_payoff = node.get_payoffs()[player];
        if(player_payoff == null) throw new RuntimeException(String.format("player %d 's payoff is not found",player));

        // TODO hard code
        int oppo = 1 - player;
        PrivateCards[] player_hand = playerHands(player);
        PrivateCards[] oppo_hand = playerHands(oppo);

        float[] payoffs = new float[this.playerHands(player).length];

        // TODO hard code
        float oppo_sum = 0;
        float[] oppo_card_sum = new float[52];
        Arrays.fill(oppo_card_sum,0);

        for(int i = 0;i < oppo_hand.length;i ++){
            oppo_card_sum[player_hand[i].card1] += reach_prob[player][i];
            oppo_card_sum[player_hand[i].card2] += reach_prob[player][i];
            oppo_sum += reach_prob[player][i];
        }

        for(int i = 0;i < player_hand.length;i ++){
            PrivateCards one_player_hand = player_hand[i];
            if(Card.boardsHasIntercept(board_long,Card.boardInts2long(new int[]{one_player_hand.card1,one_player_hand.card2}))){
                continue;
            }
            // TODO bug here
            payoffs[i] = player_payoff.floatValue() * (
                    oppo_sum - oppo_card_sum[one_player_hand.card1]
                    - oppo_card_sum[one_player_hand.card2]
                    + reach_prob[oppo][i]
                    );
        }

        return payoffs;
    }
}
