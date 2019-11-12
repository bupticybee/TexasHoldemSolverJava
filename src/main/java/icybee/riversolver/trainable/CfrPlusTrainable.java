package icybee.riversolver.trainable;

import icybee.riversolver.nodes.ActionNode;
import icybee.riversolver.ranges.PrivateCards;

import java.util.Arrays;

/**
 * Created by huangxuefeng on 2019/10/12.
 * trainable by cfr
 */
public class CfrPlusTrainable extends Trainable{
    ActionNode action_node;
    PrivateCards[] privateCards;
    int action_number;
    int card_number;
    float[] r_plus = null;

    public float[] getR_plus() {
        return r_plus;
    }

    public float[] getR_plus_sum() {
        return r_plus_sum;
    }

    public float[] getCum_r_plus() {
        return cum_r_plus;
    }

    public float[] getCum_r_plus_sum() {
        return cum_r_plus_sum;
    }

    float[] r_plus_sum = null;

    float[] cum_r_plus = null;
    float[] cum_r_plus_sum = null;

    float[] regrets = null;

    public CfrPlusTrainable(ActionNode action_node, PrivateCards[] privateCards){
        this.action_node = action_node;
        this.privateCards = privateCards;
        this.action_number = action_node.getChildrens().size();
        this.card_number = privateCards.length;

        this.r_plus = new float[this.action_number * this.card_number];
        this.r_plus_sum = new float[this.card_number];

        this.cum_r_plus = new float[this.card_number * this.card_number];
        this.cum_r_plus_sum = new float[this.card_number];
    }

    private boolean isAllZeros(float[] input_array){
        for(float i:input_array){
            if (i != 0)return false;
        }
        return true;
    }

    @Override
    public float[] getAverageStrategy() {
        float[] retval = new float[this.action_number * this.card_number];
        if(this.cum_r_plus_sum == null || this.isAllZeros(this.cum_r_plus_sum)){
            Arrays.fill(retval,Float.valueOf(1) / (this.action_number));
        }else {
            for (int action_id = 0; action_id < action_number; action_id++) {
                for (int private_id = 0; private_id < this.card_number; private_id++) {
                    int index = action_id * this.card_number + private_id;
                    retval[index] = this.cum_r_plus[index] / this.cum_r_plus_sum[private_id];
                }
            }
        }
        return retval;
    }

    @Override
    public float[] getcurrentStrategy() {
        float[] retval = new float[this.action_number * this.card_number];
        if(this.r_plus_sum == null || this.isAllZeros(this.r_plus_sum)){
            Arrays.fill(retval,Float.valueOf(1) / (this.action_number));
        }else {
            for (int action_id = 0; action_id < action_number; action_id++) {
                for (int private_id = 0; private_id < this.card_number; private_id++) {
                    int index = action_id * this.card_number + private_id;
                    retval[index] = this.r_plus[index] / this.r_plus_sum[private_id];
                }
            }
        }
        return retval;
    }

    public float[] getcurrentStrategy(int private_id) {
        float[] retval = new float[this.action_number];
        if(this.r_plus_sum == null || this.isAllZeros(this.r_plus_sum)){
            Arrays.fill(retval,Float.valueOf(1) / (this.action_number));
        }else {
            for (int action_id = 0; action_id < action_number; action_id++) {
                int index = action_id * this.card_number + private_id;
                retval[action_id] = this.r_plus[index] / this.r_plus_sum[private_id];
            }
        }
        return retval;
    }

    public float[] getcurrentRegrets(int private_id) {
        float[] retval = new float[this.action_number];
        for (int action_id = 0; action_id < action_number; action_id++) {
            int index = action_id * this.card_number + private_id;
            retval[action_id] = this.regrets[index];
        }
        return retval;
    }

    public float[] getRPlus(int private_id) {
        float[] retval = new float[this.action_number];
        for (int action_id = 0; action_id < action_number; action_id++) {
            int index = action_id * this.card_number + private_id;
            retval[action_id] = this.r_plus[index];
        }
        return retval;
    }

    @Override
    public void updateRegrets(float[] regrets,int iteration_number) {
        this.regrets = regrets;
        if(regrets.length != this.action_number * this.card_number) throw new RuntimeException("length not match");

        Arrays.fill(this.r_plus_sum,0);
        for (int action_id = 0;action_id < action_number;action_id ++) {
            float cum_r_plus_sum_float = 0;
            for(int private_id = 0;private_id < this.card_number;private_id ++){
                int index = action_id * this.card_number + private_id;
                float one_reg = regrets[index];

                // 更新 R+
                this.r_plus[index] = Math.max(0,one_reg + this.r_plus[index]);
                this.r_plus_sum[private_id] += this.r_plus[index];

                // 更新累计策略
                this.cum_r_plus[index] += this.r_plus[index] * iteration_number;
                cum_r_plus_sum_float += this.cum_r_plus[index];
                this.cum_r_plus_sum[private_id] = cum_r_plus_sum_float;
            }
        }


        /*
        for (int action_id = 0;action_id < action_number;action_id ++) {
            for(int private_id = 0;private_id < this.card_number;private_id ++){
                regrets[action_id * this.card_number + private_id] /= this.current_regret_sum[action_id];
            }
        }
        */
    }
}
