package icybee.solver;

import icybee.solver.compairer.Compairer;
import icybee.solver.compairer.Dic5Compairer;

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
    static SolverEnvironment se;

    public Compairer getCompairer() {
        return compairer;
    }

    public static SolverEnvironment getInstance(){
        return SolverEnvironment.se;
    }

    SolverEnvironment(Config config) throws ClassNotFoundException,IOException{
        this.config = config;
        this.deck = new Deck(config.ranks,config.suits);
        if(config.compairer_type.equals("Dic5Compairer")) {
            this.compairer = new Dic5Compairer(config.compairer_dic_dir,config.compairer_lines);
        }else{
            throw new ClassNotFoundException();
        }

        if(this.config.tree_builder){
            this.game_tree = new GameTree(this.config.tree_builder_json,this.deck);
        }
        if(this.config.solver_type.equals("cfrplus")){
            //solver = new CfrPlusRiverSolver(game_tree);
        }
        SolverEnvironment.se = this;
    }

    public static GameTree gameTreeFromConfig(Config config,Deck deck){
        try {
            return new GameTree(config.tree_builder_json, deck);
        }catch(IOException e){
            throw new RuntimeException();
        }
    }

    public static GameTree gameTreeFromJson(String json_path,Deck deck){
        try {
            return new GameTree(json_path, deck);
        }catch(IOException e){
            throw new RuntimeException();
        }
    }

    public static Deck deckFromConfig(Config config){
        return new Deck(config.ranks,config.suits);
    }

    public static Compairer compairerFromFile(String compairer_type,String compairer_dic_dir,int compairer_lines)throws IOException{
        if(compairer_type.equals("Dic5Compairer")) {
            return new Dic5Compairer(compairer_dic_dir,compairer_lines);
        }else{
            throw new RuntimeException();
        }
    }
    public static Compairer compairerFromConfig(Config config)throws IOException{
        if(config.compairer_type.equals("Dic5Compairer")) {
            return new Dic5Compairer(config.compairer_dic_dir,config.compairer_lines);
        }else{
            throw new RuntimeException();
        }
    }
    public static Compairer compairerFromConfig(Config config,boolean verbose)throws IOException{
        if(config.compairer_type.equals("Dic5Compairer")) {
            return new Dic5Compairer(config.compairer_dic_dir,config.compairer_lines,verbose);
        }else{
            throw new RuntimeException();
        }
    }
}
