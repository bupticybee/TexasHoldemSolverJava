package icybee.solver.nodes;

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

    public enum GameTreeNodeType{
        ACTION,
        SHOWDOWN,
        TERMINAL,
        CHANCE
    }

    public enum GameRound{
        PREFLOP,
        FLOP,
        TURN,
        RIVER
    }

    GameRound round;
    Double pot;
    GameTreeNode parent;
    public int depth;
    public int subtree_size;

    public static int gameRound2int(GameRound gameRound){
        if(gameRound == GameRound.PREFLOP) {
            return 0;
        }else if(gameRound == GameRound.FLOP){
            return 1;
        } else if(gameRound == GameRound.TURN) {
            return 2;
        } else if(gameRound == GameRound.RIVER) {
            return 3;
        }
        throw new RuntimeException("round not found");
    }

    public GameTreeNode getParent() {
        return parent;
    }

    public void setParent(GameTreeNode parent) {
        this.parent = parent;
    }

    public GameTreeNode(GameRound round, Double pot, GameTreeNode parent){
        if(round == null){
            throw new RuntimeException("round is null in GameTreeNode");
        }
        this.round = round;
        if(pot == null){
            throw new RuntimeException("pot is null in GameTreeNode");
        }
        this.pot = pot;
        this.parent = parent;

    }

    public GameRound getRound() {
        return round;
    }

    public Double getPot() {
        return pot;
    }

    public void printHistory(){
        GameTreeNode.printNodeHistory(this);
    }

    public static void printNodeHistory(GameTreeNode node){
        while(node != null){
            GameTreeNode parent_node = node.parent;
            if(parent_node == null) break;
            if(parent_node instanceof ActionNode){
                ActionNode action_node = (ActionNode)parent_node;
                for(int i = 0;i < action_node.getActions().size();i ++){
                    if(action_node.getChildrens().get(i) == node){
                        System.out.print(String.format("<- (player %s %s)",
                                action_node.getPlayer(),
                                action_node.getActions().get(i).toString()
                                ));
                    }
                }
            }else if(parent_node instanceof ChanceNode) {
                ChanceNode chance_node = (ChanceNode)parent_node;
                for(int i = 0;i < chance_node.getChildrens().size();i ++){
                    if(chance_node.getChildrens().get(i) == node){
                        System.out.print(String.format("<- (deal card %s)",
                                chance_node.getCards().get(i).toString()
                        ));
                    }
                }

            }else{
                System.out.print(String.format("<- (%s)",node.toString()));
            }
            node = parent_node;
        }
        System.out.println();
    }

    public abstract GameTreeNodeType getType();
}
