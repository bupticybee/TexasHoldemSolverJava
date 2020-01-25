package icybee.riversolver.runtime;

import icybee.riversolver.*;
import icybee.riversolver.compairer.Compairer;
import icybee.riversolver.ranges.PrivateCards;
import icybee.riversolver.solver.CfrPlusRiverSolver;
import icybee.riversolver.solver.MonteCarolAlg;
import icybee.riversolver.solver.ParallelCfrPlusSolver;
import icybee.riversolver.solver.Solver;
import icybee.riversolver.trainable.CfrPlusTrainable;
import icybee.riversolver.trainable.CfrTrainable;
import icybee.riversolver.trainable.DiscountedCfrTrainable;
import icybee.riversolver.utils.PrivateRangeConverter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PokerSolver {
    Deck deck;
    GameTree tree;
    Compairer compairer;

    Config loadConfig(String conf_name){
        File file = new File(conf_name);

        Config config;
        try {
            config = new Config(file.getAbsolutePath());
        }catch(Exception e){
            throw new RuntimeException();
        }
        return config;
    }

    public PokerSolver(String compairer_type, String compairer_dic_dir, int compairer_lines, String[] ranks,String[] suits) throws IOException {
        this.deck = new Deck(Arrays.asList(ranks), Arrays.asList(suits));
        this.compairer = SolverEnvironment.compairerFromFile(compairer_type,compairer_dic_dir,compairer_lines);
        this.tree = null;
    }

    public void build_game_tree(String tree_json){
        tree = SolverEnvironment.gameTreeFromJson(tree_json,this.deck);
    }

    public void train(
            String player1_range,
            String player2_range,
            String initial_board,
            int iteration_number,
            int print_interval,
            boolean debug,
            boolean parallel,
            String output_strategy_file,
            String logfile,
            String algorithm,
            String monte_carol,
            int threads
    ) throws Exception {
        if(this.tree == null)
            throw new RuntimeException("tree not initized");
        String[] initial_board_split = initial_board.split(",");
        int[] initial_board_arr = Arrays.stream(initial_board_split).map(e -> Card.strCard2int(e)).mapToInt(i->i).toArray();
        Class<?> algorithm_class;
        switch(algorithm){
            case "cfr":
                algorithm_class = CfrTrainable.class;
                break;
            case "cfr_plus":
                algorithm_class = CfrPlusTrainable.class;
                break;
            case "discounted_cfr":
                algorithm_class = DiscountedCfrTrainable.class;
                break;
            default:
                throw new RuntimeException(String.format("algorithm not found :%s",algorithm));
        }

        MonteCarolAlg monte_coral_alg;
        switch(monte_carol){
            case "none":
                monte_coral_alg = MonteCarolAlg.NONE;
                break;
            case "public":
                monte_coral_alg = MonteCarolAlg.PUBLIC;
                break;
            default:
                throw new RuntimeException(String.format("monte coral type not found :%s",monte_carol));
        }

        PrivateCards[] player1Range = PrivateRangeConverter.rangeStr2Cards(player1_range,initial_board_arr);
        PrivateCards[] player2Range = PrivateRangeConverter.rangeStr2Cards(player2_range,initial_board_arr);

        Solver solver;
        if(parallel) {
            solver = new ParallelCfrPlusSolver(this.tree
                    , player1Range
                    , player2Range
                    , initial_board_arr
                    , compairer
                    , deck
                    , iteration_number
                    , debug
                    , print_interval
                    , logfile
                    , algorithm_class
                    , monte_coral_alg
                    , threads
            );
        }else{
            solver = new CfrPlusRiverSolver(this.tree
                    , player1Range
                    , player2Range
                    , initial_board_arr
                    , compairer
                    , deck
                    , iteration_number
                    , debug
                    , print_interval
                    , logfile
                    , algorithm_class
                    , monte_coral_alg
            );
        }
        Map train_config = new HashMap();
        solver.train(train_config);

        String strategy_json = solver.getTree().dumps(false).toJSONString();
        File output_file = new File(output_strategy_file);
        FileWriter writer = new FileWriter(output_file);
        writer.write(strategy_json);
        writer.flush();
        writer.close();
    }

}
