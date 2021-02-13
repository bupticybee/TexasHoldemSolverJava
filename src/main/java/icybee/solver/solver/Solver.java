package icybee.solver.solver;

import icybee.solver.GameTree;

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
    public GameTree getTree() {
        return tree;
    }

    public abstract void train(Map training_config) throws  Exception;
}
