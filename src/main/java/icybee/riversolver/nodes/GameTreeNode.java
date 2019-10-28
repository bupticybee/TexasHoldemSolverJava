package icybee.riversolver.nodes;

import icybee.riversolver.GameTree;

import java.util.List;
import java.util.Map;

/**
 * Created by huangxuefeng on 2019/10/7.
 * This class contains code of the game tree's node
 */
public abstract class GameTreeNode {
    public enum PokerActions{
        BEGIN,
        ROUNDBEGIN,
        BET,
        RAISE,
        CHECK,
        FOLD,
        CALL
    }

    public enum GameRound{
        PREFLOP,
        FLOP,
        TURN,
        RIVER
    }

    GameRound round;
    Double pot;
    public GameTreeNode(GameRound round,Double pot){
        if(round == null){
            throw new RuntimeException("round is null in GameTreeNode");
        }
        this.round = round;
        if(pot == null){
            throw new RuntimeException("pot is null in GameTreeNode");
        }
        this.pot = pot;

    }

    public GameRound getRound() {
        return round;
    }

    public Double getPot() {
        return pot;
    }
}
