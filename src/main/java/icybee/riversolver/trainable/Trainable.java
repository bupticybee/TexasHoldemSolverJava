package icybee.riversolver.trainable;

import com.alibaba.fastjson.JSONObject;

/**
 * Created by huangxuefeng on 2019/10/12.
 * include code for a abstract class trainable,which describes a class that can be trained by cfr
 */
public abstract class Trainable {
    public abstract float[] getAverageStrategy();
    public abstract float[] getcurrentStrategy();
    public abstract void updateRegrets(float[] regrets,int iteration_number);
    public abstract JSONObject dumps(boolean with_state);
}
