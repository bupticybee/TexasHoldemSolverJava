package icybee.solver.nodes;

import icybee.solver.trainable.Trainable;

import java.util.List;

/**
 * Created by huangxuefeng on 2019/10/7.
 * This file contians action node implementation
 */
public class ActionNode extends GameTreeNode{

    List<GameActions> actions;
    List<GameTreeNode> childrens;


    Trainable trainable;

    int player;

    public ActionNode(List<GameActions> actions, List<GameTreeNode> childrens, int player, GameRound round,Double pot,GameTreeNode parent){
        super(round,pot,parent);
        assert(actions.size() == childrens.size());
        this.actions = actions;
        this.childrens = childrens;
        this.player = player;
    }

    public List<GameActions> getActions() {
        return actions;
    }

    public List<GameTreeNode> getChildrens() {
        return childrens;
    }

    public void setActions(List<GameActions> actions) {
        this.actions = actions;
    }

    public void setChildrens(List<GameTreeNode> childrens) {
        this.childrens = childrens;
    }


    public int getPlayer() {
        return player;
    }

    public Trainable getTrainable() {
        return trainable;
    }

    public void setTrainable(Trainable trainable) {
        this.trainable = trainable;
    }

    @Override
    public GameTreeNodeType getType() {
        return GameTreeNodeType.ACTION;
    }


}
