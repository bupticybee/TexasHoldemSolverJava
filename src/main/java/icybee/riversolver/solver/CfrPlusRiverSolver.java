package icybee.riversolver.solver;

import icybee.riversolver.Card;
import icybee.riversolver.Deck;
import icybee.riversolver.GameTree;
import icybee.riversolver.RiverRangeManager;
import icybee.riversolver.compairer.Compairer;
import icybee.riversolver.exceptions.BoardNotFoundException;
import icybee.riversolver.nodes.ActionNode;
import icybee.riversolver.nodes.GameTreeNode;
import icybee.riversolver.nodes.ShowdownNode;
import icybee.riversolver.nodes.TerminalNode;
import icybee.riversolver.ranges.PrivateCards;
import icybee.riversolver.ranges.PrivateCardsManager;
import icybee.riversolver.ranges.RiverCombs;
import icybee.riversolver.trainable.CfrPlusTrainable;

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
    Compairer compairer;

    Deck deck;
    ForkJoinPool forkJoinPool;
    RiverRangeManager rrm;
    int player_number;
    int iteration_number;
    PrivateCardsManager pcm;

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
        this.compairer = compairer;
        long board_long = Card.boardInts2long(board);

        this.deck = deck;

        int nThreads = Runtime.getRuntime().availableProcessors();
        this.forkJoinPool = new ForkJoinPool(nThreads);

        this.rrm = RiverRangeManager.getInstance(compairer);
        this.player_number = 2;
        this.iteration_number = 1000;

        PrivateCards[][] private_cards = new PrivateCards[this.player_number][];
        private_cards[0] = range1;
        private_cards[1] = range2;
        pcm = new PrivateCardsManager(private_cards,this.player_number,this.board);

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

        for(int i = 0;i < this.iteration_number;i++){
            for(int player_id = 0;player_id < this.player_number;player_id ++) {
                cfr(player_id,this.tree.getRoot(),i);
            }
        }
    }

    float[] cfr(int player,GameTreeNode node,int iter){
        // TODO finish here
        return new float[1];
    }
}
