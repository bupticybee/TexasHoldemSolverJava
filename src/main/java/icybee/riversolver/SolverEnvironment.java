package icybee.riversolver;

import icybee.riversolver.compairer.Compairer;
import icybee.riversolver.compairer.Dic5Compairer;
import icybee.riversolver.exceptions.BoardNotFoundException;
import icybee.riversolver.solver.CfrPlusRiverSolver;
import icybee.riversolver.solver.Solver;

import java.io.IOException;

/**
 * Created by huangxuefeng on 2019/10/6.
 * This file contains the implemtation of the Texas Poker Solver Environment
 */
public class SolverEnvironment {
    Config config;
    Deck deck;
    Compairer compairer;
    GameTree game_tree = null;
    Solver solver;
    SolverEnvironment(Config config) throws ClassNotFoundException,IOException,BoardNotFoundException{
        this.config = config;
        this.deck = new Deck(config.ranks,config.suits);
        if(config.compairer_type.equals("Dic5Compairer")) {
            this.compairer = new Dic5Compairer(config.compairer_dic_dir,config.compairer_lines);
        }else{
            throw new ClassNotFoundException();
        }

        if(this.config.tree_builder){
            this.game_tree = new GameTree(this.config.tree_builder_json);
        }
        if(this.config.solver_type.equals("cfrplus")){
            solver = new CfrPlusRiverSolver(game_tree);
        }
    }
}
