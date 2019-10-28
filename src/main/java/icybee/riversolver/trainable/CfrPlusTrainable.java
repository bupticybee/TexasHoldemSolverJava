package icybee.riversolver.trainable;

import icybee.riversolver.nodes.ActionNode;

/**
 * Created by huangxuefeng on 2019/10/12.
 * trainable by cfr
 */
public class CfrPlusTrainable extends Trainable{
    ActionNode action_node;
    public CfrPlusTrainable(ActionNode action_node){
        this.action_node = action_node;
    }

    @Override
    public float[] getAverageStrategy() {
        return new float[0];
    }

    @Override
    public float[] getcurrentStrategy() {
        return new float[0];
    }
}
