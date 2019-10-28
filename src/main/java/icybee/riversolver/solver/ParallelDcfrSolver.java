package icybee.riversolver.solver;

import icybee.riversolver.GameTree;

import java.util.Map;

/**
 * Created by huangxuefeng on 2019/10/9.
 * Contains DCFR implemtation
 */
public class ParallelDcfrSolver extends Solver{
    public ParallelDcfrSolver(GameTree tree){
        super(tree);
    }
    @Override
    public void train(Map training_config) {
        Map<String,Object> config_map = training_config;
        Integer iterations = (Integer)config_map.get("iterations");
        if(iterations == null){
            throw new RuntimeException("iteration is null");
        }
        //TODO read the paper and carry on writing
    }
}
