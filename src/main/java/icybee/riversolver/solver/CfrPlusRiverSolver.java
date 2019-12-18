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

import java.util.*;
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
    boolean debug;
    int print_interval;

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

    public PrivateCards[] noDuplicateRange(PrivateCards[] private_range,long board_long){
        List<PrivateCards> range_array = new ArrayList<>();
        Map<Integer,Boolean> rangekv = new HashMap<>();
        for(PrivateCards one_range:private_range){
            if(one_range == null) throw new RuntimeException();
            if(rangekv.get(one_range.hashCode()) != null)
                throw new RuntimeException(String.format("duplicated key %d",one_range.toString()));
            rangekv.put(one_range.hashCode(),Boolean.TRUE);
            long hand_long = Card.boardInts2long(new int[]{
                    one_range.card1,
                    one_range.card2
            });
            if(!Card.boardsHasIntercept(hand_long,board_long)){
                range_array.add(one_range);
            }
        }
        PrivateCards[] ret = new PrivateCards[range_array.size()];
        range_array.toArray(ret);
        return ret;
    }

    public CfrPlusRiverSolver(GameTree tree, PrivateCards[] range1 , PrivateCards[] range2, int[] board, Compairer compairer,Deck deck,int iteration_number,boolean debug,int print_interval) throws BoardNotFoundException{
        super(tree);
        // TODO currently only support river
        if(board.length != 5) throw new RuntimeException(String.format("board length %d",board.length));
        this.board = board;
        this.board_long = Card.boardInts2long(board);

        range1 = this.noDuplicateRange(range1,board_long);
        range2 = this.noDuplicateRange(range2,board_long);
        this.range1 = range1;
        this.range2 = range2;
        this.compairer = compairer;

        this.deck = deck;

        int nThreads = Runtime.getRuntime().availableProcessors();
        this.forkJoinPool = new ForkJoinPool(nThreads);

        this.rrm = RiverRangeManager.getInstance(compairer);
        this.player_number = 2;
        this.iteration_number = iteration_number;

        PrivateCards[][] private_cards = new PrivateCards[this.player_number][];
        private_cards[0] = range1;
        private_cards[1] = range2;
        pcm = new PrivateCardsManager(private_cards,this.player_number,Card.boardInts2long(this.board));
        this.debug = debug;
        this.print_interval = print_interval;
    }


    void setTrainable(GameTreeNode root){
        if(root instanceof ActionNode){
            ActionNode action_node = (ActionNode)root;

            int player = action_node.getPlayer();
            PrivateCards[] player_privates = this.getPlayerPrivateCard(player);

            action_node.setTrainable(new CfrPlusTrainable(action_node,player_privates));

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

        BestResponse br = new BestResponse(player_rivers,this.player_number,this.compairer,this.pcm,this.debug);

        br.printExploitability(tree.getRoot(), 0, tree.getRoot().getPot().floatValue(), board);

        float[][] reach_probs = this.getReachProbs();

        for(int i = 0;i < this.iteration_number;i++){
            for(int player_id = 0;player_id < this.player_number;player_id ++) {
                if(this.debug){
                    System.out.println(String.format(
                            "---------------------------------     player %s --------------------------------",
                            player_id
                    ));
                }
                cfr(player_id,this.tree.getRoot(),reach_probs,i);

            }
            if(i % this.print_interval == 0 || i < 30) {
                System.out.println("-------------------");
                br.printExploitability(tree.getRoot(), i + 1, tree.getRoot().getPot().floatValue(), board);
            }
        }
        // System.out.println(this.tree.dumps(false).toJSONString());
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
        // TODO 检查regret 是否符合期望
        int oppo = 1 - player;
        PrivateCards[] node_player_private_cards = this.getPlayerPrivateCard(node.getPlayer());
        Trainable trainable = node.getTrainable();

        float[] payoffs = new float[this.getPlayerPrivateCard(player).length];
        List<GameTreeNode> children =  node.getChildrens();
        List<GameActions> actions =  node.getActions();

        float[] current_strategy = trainable.getcurrentStrategy();
        if(this.debug){
            for(float one_strategy:current_strategy){
                if(one_strategy != one_strategy) {
                    System.out.println(Arrays.toString(current_strategy));
                    throw new RuntimeException();
                }

            }
            for(int one_player = 0;one_player < this.player_number;one_player ++){
                float[] one_reach_prob = reach_probs[one_player];
                for(float one_prob:one_reach_prob){
                    if(one_prob != one_prob)
                        throw new RuntimeException();
                }
            }
        }
        float[][] new_reach_prob = new float[this.player_number][];
        if (current_strategy.length != actions.size() * node_player_private_cards.length) {
            node.printHistory();
            throw new RuntimeException(String.format(
                    "length not match %s - %s \n action size %s private_card size %s"
                    ,current_strategy.length
                    ,actions.size() * node_player_private_cards.length
                    ,actions.size()
                    ,node_player_private_cards.length
            ));
        }

        //为了节省计算成本将action regret 存在一位数组而不是二维数组中，两个纬度分别是（该infoset有多少动作,该palyer有多少holecard）
        float[] regrets = new float[actions.size() * node_player_private_cards.length];

        float[][] all_action_utility = new float[actions.size()][];
        int node_player = node.getPlayer();
        new_reach_prob[1 - node_player] = reach_probs[1 - node_player];
        for(int action_id = 0;action_id < actions.size(); action_id++) {
            float[] player_new_reach = new float[reach_probs[node_player].length];
            for(int hand_id = 0;hand_id < player_new_reach.length;hand_id ++){
                float strategy_prob = current_strategy[hand_id + action_id * node_player_private_cards.length];
                player_new_reach[hand_id] = reach_probs[node_player][hand_id] * strategy_prob;
            }
            new_reach_prob[node_player] = player_new_reach;
            float[] action_utilities = this.cfr(player,children.get(action_id),new_reach_prob,iter);
            all_action_utility[action_id] = action_utilities;

            // cfr结果是每手牌的收益，payoffs代表的也是每手牌的收益，他们的长度理应相等
            if(action_utilities.length != payoffs.length){
                System.out.println("errmsg");
                System.out.println(String.format("node player %s ",node.getPlayer()));
                node.printHistory();
                throw new RuntimeException(
                        String.format(
                                "action and payoff length not match %s - %s"
                                ,action_utilities.length
                                ,payoffs.length
                        )
                );
            }

            for(int hand_id = 0;hand_id < action_utilities.length;hand_id ++){
                if(player == node.getPlayer()) {
                    float strategy_prob = current_strategy[hand_id + action_id * node_player_private_cards.length];
                    payoffs[hand_id] += strategy_prob * action_utilities[hand_id];
                }else{
                    payoffs[hand_id] += action_utilities[hand_id];
                }
            }
        }


        if(player == node.getPlayer()) {
            for(int i = 0;i < node_player_private_cards.length;i ++){
                //boolean regrets_all_negative = true;
                for(int action_id = 0;action_id < actions.size(); action_id++) {
                    // 下面是regret计算的伪代码
                    // regret[action_id * player_hc: (action_id + 1) * player_hc]
                    //     = all_action_utilitiy[action_id] - payoff[action_id]
                    regrets[action_id * node_player_private_cards.length + i] = all_action_utility[action_id][i] - payoffs[i];
                }
            }
            trainable.updateRegrets(regrets, iter);
        }
        //if(this.debug && player == node.getPlayer()) {
        if(this.debug) {
            int card_id = 0;

            System.out.println("[ACTIONS]============");
            System.out.println("=======================================");
            System.out.println(String.format("player %s card %s",player,getPlayerPrivateCard(player)[card_id]));
            System.out.print("actions: ");
            for (GameActions one_action : node.getActions()) {
                System.out.print(one_action.toString());
                System.out.print(" ");
            }
            System.out.println();

            System.out.println("Strategys:");
            for(int i = 0;i < node.getActions().size(); i++) {
                float one_strategy = current_strategy[0 + i * node_player_private_cards.length];
                System.out.print(String.format(" %s:%s ",node.getActions().get(i),one_strategy));
            }
            System.out.println();

            System.out.print("history: ");
            node.printHistory();

            System.out.print("payoffs : ");
            for(float[] one_action_utility: all_action_utility) {
                System.out.print(one_action_utility[card_id]);
                System.out.print(" ");
            }
            System.out.println();

            if(player  == node.getPlayer()) {
                System.out.print("regrets: ");
                System.out.println(
                        Arrays.toString(
                                ((CfrPlusTrainable) node.getTrainable()).getcurrentRegrets(card_id)
                        )
                );

                System.out.print("r plus:");
                System.out.println(
                        Arrays.toString(
                                ((CfrPlusTrainable) node.getTrainable()).getRPlus(card_id)
                        )
                );

                System.out.print("r plus sum:");
                System.out.println(
                        ((CfrPlusTrainable) node.getTrainable()).getR_plus_sum()[card_id]
                );
            }

            System.out.print("strategy: ");
            System.out.println(
                    Arrays.toString(
                            ((CfrPlusTrainable) node.getTrainable()).getcurrentStrategy(card_id)
                    )
            );
            System.out.println(String.format("final payoff: %s",payoffs[0]));
        }

        return payoffs;
    }

    float[] showdownUtility(int player,ShowdownNode node,float[][]reach_probs,int iter) throws BoardNotFoundException{
        // player win时候player的收益，player lose的时候收益明显为-player_payoff
        int oppo = 1 - player;
        float win_payoff = node.get_payoffs(ShowdownNode.ShowDownResult.NOTTIE,player)[player].floatValue();
        float lose_payoff = node.get_payoffs(ShowdownNode.ShowDownResult.NOTTIE,oppo)[player].floatValue();
        PrivateCards[] player_private_cards = this.getPlayerPrivateCard(player);
        PrivateCards[] oppo_private_cards = this.getPlayerPrivateCard(oppo);

        RiverCombs[] player_combs = this.rrm.getRiverCombos(player,player_private_cards,this.board);
        RiverCombs[] oppo_combs = this.rrm.getRiverCombos(oppo,oppo_private_cards,this.board);

        float[] payoffs = new float[player_private_cards.length];


        float winsum = 0;
        float[] card_winsum = new float[52];

        int j = 0;
        //if(player_combs.length != oppo_combs.length) throw new RuntimeException("");

        if(this.debug){
            System.out.println("[PRESHOWDOWN]=======================");
            System.out.println(String.format("player0 reach_prob %s",Arrays.toString(reach_probs[0])));
            System.out.println(String.format("player1 reach_prob %s",Arrays.toString(reach_probs[1])));
            System.out.print("preflop combos: ");
            for(RiverCombs one_river_comb:player_combs){
                System.out.print(String.format("%s(%s) "
                        ,one_river_comb.private_cards.toString()
                        ,one_river_comb.rank
                ));
            }
            System.out.println();
        }

        for(int i = 0;i < player_combs.length;i ++){
            RiverCombs one_player_comb = player_combs[i];
            while (j < oppo_combs.length && one_player_comb.rank < oppo_combs[j].rank){
                RiverCombs one_oppo_comb = oppo_combs[j];
                winsum += reach_probs[oppo][one_oppo_comb.reach_prob_index];
                if(this.debug) {
                    if (one_player_comb.reach_prob_index == 0) {
                        System.out.print(String.format("[%s]%s:%s-%s(%s) "
                                ,j
                                ,one_oppo_comb.private_cards.toString()
                                ,this.getPlayerPrivateCard(oppo)[one_oppo_comb.reach_prob_index].weight
                                ,winsum
                                ,one_oppo_comb.rank
                        ));
                    }
                }

                // TODO 这里有问题，要加上reach prob，但是reach prob的index怎么解决？
                card_winsum[one_oppo_comb.private_cards.card1] += reach_probs[oppo][one_oppo_comb.reach_prob_index];
                card_winsum[one_oppo_comb.private_cards.card2] += reach_probs[oppo][one_oppo_comb.reach_prob_index];
                j ++;
            }
            if(this.debug){
                //调查这里为什么加完了是负数
                System.out.println(String.format("Before Adding %s, win_payoff %s winsum %s, subcard1 %s subcard2 %s"
                        ,payoffs[one_player_comb.reach_prob_index]
                        ,win_payoff
                        ,winsum
                        ,- card_winsum[one_player_comb.private_cards.card1]
                        ,- card_winsum[one_player_comb.private_cards.card2]
                ));
            }
            payoffs[one_player_comb.reach_prob_index] = (winsum
                    - card_winsum[one_player_comb.private_cards.card1]
                    - card_winsum[one_player_comb.private_cards.card2]
            ) * win_payoff;
            if(this.debug) {
                if (one_player_comb.reach_prob_index == 0) {
                    System.out.println(String.format("winsum %s",winsum));
                }
            }
        }

        // 计算失败时的payoff
        float losssum = 0;
        float[] card_losssum = new float[52];
        for(int i = 0;i < card_losssum.length;i ++) card_losssum[i] = 0;

        j = oppo_combs.length - 1;
        for(int i = player_combs.length - 1;i >= 0;i --){
            RiverCombs one_player_comb = player_combs[i];
            while (j >= 0 && one_player_comb.rank > oppo_combs[j].rank){
                RiverCombs one_oppo_comb = oppo_combs[j];
                losssum += reach_probs[oppo][one_oppo_comb.reach_prob_index];
                if(this.debug) {
                    if (one_player_comb.reach_prob_index == 0) {
                        System.out.print(String.format("lose %s:%s "
                                ,one_oppo_comb.private_cards.toString()
                                ,this.getPlayerPrivateCard(oppo)[one_oppo_comb.reach_prob_index].weight
                        ));
                    }
                }

                // TODO 这里有问题，要加上reach prob，但是reach prob的index怎么解决？
                card_losssum[one_oppo_comb.private_cards.card1] += reach_probs[oppo][one_oppo_comb.reach_prob_index];
                card_losssum[one_oppo_comb.private_cards.card2] += reach_probs[oppo][one_oppo_comb.reach_prob_index];
                j --;
            }
            if(this.debug) {
                System.out.println(String.format("Before Substract %s", payoffs[one_player_comb.reach_prob_index]));
            }
            payoffs[one_player_comb.reach_prob_index] += (losssum
                    - card_losssum[one_player_comb.private_cards.card1]
                    - card_losssum[one_player_comb.private_cards.card2]
            ) * lose_payoff;
            if(this.debug) {
                if (one_player_comb.reach_prob_index == 0) {
                    System.out.println(String.format("losssum %s",losssum));
                }
            }
        }
        if(this.debug) {
            System.out.println();
            System.out.println("[SHOWDOWN]============");
            node.printHistory();
            System.out.println(String.format("loss payoffs: %s",lose_payoff));
            /*
                player 0 card AdAc
                actions: CALL FOLD
                history: <- (player 1 BET 2.0)
                payoffs : -778.0 -394.0
                regrets: [-192.0, 191.0]
             */
            System.out.println(String.format("oppo sum %s, substracted payoff %s",losssum,payoffs[0]));
        }

        /*
        float[] oppo_cardsum = new float[52];
        float oppo_sum = 0;
        for(int i = 0;i < this.pcm.getPreflopCards(oppo).length;i ++){
            PrivateCards one_oppo_cards = this.pcm.getPreflopCards(oppo)[i];
            oppo_cardsum[one_oppo_cards.card1] += reach_probs[oppo][i];
            oppo_cardsum[one_oppo_cards.card2] += reach_probs[oppo][i];
            oppo_sum += reach_probs[oppo][i];
        }

        for(int i = 0;i < this.pcm.getPreflopCards(player).length;i ++){
            PrivateCards one_player_cards = this.pcm.getPreflopCards(oppo)[i];
            float oppo_same_card_sum = 0;
            oppo_same_card_sum += oppo_cardsum[one_player_cards.card1];
            oppo_same_card_sum += oppo_cardsum[one_player_cards.card2];
            oppo_same_card_sum -= reach_probs[oppo][this.pcm.indPlayer2Player(player,oppo,i)];
            if(oppo_sum - oppo_same_card_sum == 0) throw new RuntimeException("oppo sum is zero");
            payoffs[i] /= (oppo_sum - oppo_same_card_sum);
        }
        */

        /*
        if(true){
            node.printHistory();
            int ind = -1;
            for(int i = 0;i < this.getPlayerPrivateCard(player).length;i ++){
                if(this.getPlayerPrivateCard(player)[i].hashCode() ==
                        (new PrivateCards(
                                Card.strCard2int("Qd"),
                                Card.strCard2int("7h"),
                                1
                        )).hashCode()
                ){
                    ind = i;
                }
            }
            if(ind == -1){
                throw new RuntimeException();
            }
            PrivateCards pc = this.getPlayerPrivateCard(player)[ind];
            System.out.println(pc.toString());
        }
        if(true){
            node.printHistory();
            int ind = -1;
            for(int i = 0;i < this.getPlayerPrivateCard(player).length;i ++){
                if(this.getPlayerPrivateCard(player)[i].hashCode() ==
                        (new PrivateCards(
                                Card.strCard2int("Qc"),
                                Card.strCard2int("7h"),
                                1
                        )).hashCode()
                ){
                    ind = i;
                }
            }
            if(ind == -1){
                throw new RuntimeException();
            }
            PrivateCards pc = this.getPlayerPrivateCard(player)[ind];
            System.out.println(pc.toString());
        }

         */
        //node.printHistory();
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
            oppo_card_sum[oppo_hand[i].card1] += reach_prob[oppo][i];
            oppo_card_sum[oppo_hand[i].card2] += reach_prob[oppo][i];
            oppo_sum += reach_prob[oppo][i];
        }

        if(this.debug) {
            System.out.println("[PRETERMINAL]============");
        }
        for(int i = 0;i < player_hand.length;i ++){
            PrivateCards one_player_hand = player_hand[i];
            if(Card.boardsHasIntercept(board_long,Card.boardInts2long(new int[]{one_player_hand.card1,one_player_hand.card2}))){
                continue;
            }
            //TODO bug here
            Integer oppo_same_card_ind = this.pcm.indPlayer2Player(player,oppo,i);
            float plus_reach_prob;
            if(oppo_same_card_ind == null){
                plus_reach_prob = 0;
            }else{
                plus_reach_prob = reach_prob[oppo][oppo_same_card_ind];
            }
            payoffs[i] = player_payoff.floatValue() * (
                    oppo_sum - oppo_card_sum[one_player_hand.card1]
                    - oppo_card_sum[one_player_hand.card2]
                    + plus_reach_prob
                    );
            if(this.debug) {
                System.out.println(String.format("oppo_card_sum1 %s ", oppo_card_sum[one_player_hand.card1]));
                System.out.println(String.format("oppo_card_sum2 %s ", oppo_card_sum[one_player_hand.card2]));
                System.out.println(String.format("reach_prob i %s ", plus_reach_prob));
            }
        }

        //TODO 校对图上每个节点payoff
        if(this.debug) {
            System.out.println("[TERMINAL]============");
            node.printHistory();
            System.out.println(String.format("PPPayoffs: %s",player_payoff));
            System.out.println(String.format("reach prob %s",reach_prob[oppo][0]));
            System.out.println(String.format("oppo sum %s, substracted sum %s",oppo_sum,payoffs[0] / player_payoff));
            System.out.println(String.format("substracted sum %s",payoffs[0]));
        }
        return payoffs;
    }
}
