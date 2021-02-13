package icybee.solver.nodes;

import icybee.solver.Card;
import icybee.solver.trainable.Trainable;

import java.util.List;

/**
 * Created by huangxuefeng on 2019/10/7.
 * This file contians action node implementation
 */
public class ChanceNode extends GameTreeNode{
    // 如果一个chance node的game round是river，那么实际上它是一个介于turn和river之间的发牌节点

    List<GameTreeNode> childrens;

    Trainable trainable;

    int player;

    List<Card> cards;

    public ChanceNode(List<GameTreeNode> childrens, GameRound round, Double pot, GameTreeNode parent, List<Card> cards){
        super(round,pot,parent);
        this.childrens = childrens;
        this.cards = cards;
        if(childrens.size() != cards.size()) throw new RuntimeException("Card and childern length not match");
    }

    public List<Card> getCards() {
        return cards;
    }

    public List<GameTreeNode> getChildrens() {
        return childrens;
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
        return GameTreeNodeType.CHANCE;
    }

}
