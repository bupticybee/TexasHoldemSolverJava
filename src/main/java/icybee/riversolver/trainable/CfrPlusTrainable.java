package icybee.riversolver.trainable;

import com.alibaba.fastjson.JSONObject;
import icybee.riversolver.nodes.ActionNode;
import icybee.riversolver.nodes.GameActions;
import icybee.riversolver.ranges.PrivateCards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        this.cum_r_plus = new float[this.action_number * this.card_number];
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
                    if(this.cum_r_plus_sum[private_id] != 0) {
                        retval[index] = this.cum_r_plus[index] / this.cum_r_plus_sum[private_id];
                    }else{
                        retval[index] = Float.valueOf(1) / (this.action_number);
                    }
                }
            }
        }
        return retval;
    }

    @Override
    public float[] getcurrentStrategy() {
        float[] retval = new float[this.action_number * this.card_number];
        if(this.r_plus_sum == null ){
            Arrays.fill(retval,Float.valueOf(1) / (this.action_number));
        }else {
            for (int action_id = 0; action_id < action_number; action_id++) {
                for (int private_id = 0; private_id < this.card_number; private_id++) {
                    int index = action_id * this.card_number + private_id;
                    if(this.r_plus_sum[private_id] != 0) {
                        retval[index] = this.r_plus[index] / this.r_plus_sum[private_id];
                    }else{
                        retval[index] = Float.valueOf(1) / (this.action_number);
                    }
                    if(this.r_plus[index] != this.r_plus[index]) throw new RuntimeException();
                    /*
                    if(this.r_plus_sum[private_id] == 0)
                    {
                        System.out.println("Exception regret status, r_plus_sum == 0:");
                        System.out.println(String.format("r plus length %s , card num %s",r_plus.length,this.card_number));
                        for(int i = index % this.card_number;i < this.r_plus.length;i += this.card_number){
                            System.out.print(String.format("%s:%s ",i,this.r_plus[i]));
                            if(i == index){
                                System.out.print("[current]");
                            }
                        }
                        System.out.println();
                        System.out.println();
                        throw new RuntimeException();
                    }
                     */
                }
            }
        }
        return retval;
    }

    public float[] getcurrentStrategy(int private_id) {
        float[] retval = new float[this.action_number];
        if(this.r_plus_sum == null || this.r_plus_sum[private_id] == 0 ){
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

        //Arrays.fill(this.r_plus_sum,0);
        Arrays.fill(this.r_plus_sum,0);
        Arrays.fill(this.cum_r_plus_sum,0);
        for (int action_id = 0;action_id < action_number;action_id ++) {
            for(int private_id = 0;private_id < this.card_number;private_id ++){
                int index = action_id * this.card_number + private_id;
                float one_reg = regrets[index];

                // 更新 R+
                this.r_plus[index] = Math.max(0,one_reg + this.r_plus[index]);
                this.r_plus_sum[private_id] += this.r_plus[index];

                // 更新累计策略
                this.cum_r_plus[index] += this.r_plus[index] * iteration_number;
                this.cum_r_plus_sum[private_id] += this.cum_r_plus[index];
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

    @Override
    public JSONObject dumps(boolean with_state) {
        if(with_state) throw new RuntimeException("state storage not implemented");

        JSONObject strategy = new JSONObject();
        float[] average_strategy = this.getAverageStrategy();
        List<GameActions> game_actions = action_node.getActions();
        List<String> actions_str = new ArrayList<>();
        for(GameActions one_action:game_actions) actions_str.add(one_action.toString());

        for(int i = 0;i < this.privateCards.length;i ++){
            PrivateCards one_private_card = this.privateCards[i];
            float[] one_strategy = new float[this.action_number];

            for(int j = 0;j < this.action_number;j ++){
                int strategy_index = j * this.privateCards.length + i;
                one_strategy[j] = average_strategy[strategy_index];
            }
            strategy.put(one_private_card.toString(),
                    one_strategy
                    );
        }

        JSONObject retjson = new JSONObject();
        retjson.put("actions",actions_str);
        retjson.put("strategy",strategy);
        return retjson;
    }
}
