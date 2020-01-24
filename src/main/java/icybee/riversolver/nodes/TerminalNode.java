package icybee.riversolver.nodes;

import java.util.Map;

/**
 * Created by huangxuefeng on 2019/10/7.
 * This file contains implemtation for terminal node, Where all player(s) folds except one player take all.
 */
public class TerminalNode extends GameTreeNode{
    Double[] payoffs;
    Integer winner;
    public TerminalNode(Double[] payoffs,Integer winner,GameTreeNode.GameRound round,Double pot,GameTreeNode parent) {
        super(round,pot,parent);
        this.payoffs = payoffs;
        this.winner = winner;
    }
    public Double[] get_payoffs(){
        return payoffs;
    }

    @Override
    public GameTreeNodeType getType() {
        return GameTreeNodeType.TERMINAL;
    }

}
