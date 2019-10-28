package icybee.riversolver.solver;

import icybee.riversolver.GameTree;

import java.util.Map;

/**
 * Created by huangxuefeng on 2019/10/9.
 * contains an abstract class Solver for cfr or other things.
 */
public abstract class Solver {
    GameTree tree;
    public Solver(GameTree tree){
        this.tree = tree;
    }

    public abstract void train(Map training_config) throws  Exception;
}
