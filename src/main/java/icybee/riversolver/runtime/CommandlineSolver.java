package icybee.riversolver.runtime;

import icybee.riversolver.*;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class CommandlineSolver {

    static Config loadConfig(String conf_name){
        ClassLoader classLoader = CommandlineSolver.class.getClassLoader();
        File file = new File(conf_name);

        Config config = null;
        try {
            config = new Config(file.getAbsolutePath());
        }catch(Exception e){
            throw new RuntimeException();
        }
        return config;
    }

    public static void main(String[] args) throws Exception {
        ArgumentParser parser = ArgumentParsers.newFor("CommandlineSolver").build()
                .defaultHelp(true)
                .description("use command line to solve poker cfr");
        parser.addArgument("-c", "--config")
                .help("route to the config file");
        parser.addArgument("-p1", "--player1_range")
                .help("player1 range str,like 'KT,K9,K8,K7,K6,QJ,QT,Q9,Q8,Q7,Q6,JT,J9' ");
        parser.addArgument("-p2", "--player2_range")
                .help("player2 range str,like 'KT,K9,K8,K7,K6,QJ,QT,Q9,Q8,Q7,Q6,JT,J9' ");
        parser.addArgument("-b", "--initial_board")
                .help("the board card when the game start");
        parser.addArgument("-n", "--iteration_number")
                .help("iteration number the cfr algorithm would run");
        parser.addArgument("-i", "--print_interval")
                .help("calculate best respond ev every other print_interval iterations of cfr");
        parser.addArgument("-d", "--debug")
                .setDefault(false)
                .help("open debug mode");
        parser.addArgument("-p", "--parallel")
                .setDefault(true)
                .help("whether to use thread pool");
        parser.addArgument("-o", "--output_strategy_file")
                .setDefault((Object) null)
                .help("where to output strategy json");
        parser.addArgument("-l", "--logfile")
                .setDefault((Object) null)
                .help("calculate best respond ev every other print_interval iterations of cfr");
        parser.addArgument("-a", "--algorithm")
                .choices("discounted_cfr", "cfr", "cfr_plus").setDefault("discounted_cfr")
                .help("cfr algorithm type");
        parser.addArgument("-m", "--monte_carol")
                .choices("none", "public").setDefault("none")
                .help("(experimental)whether to use monte carol algorithm");
        parser.addArgument("-t", "--threads")
                .setDefault(-1)
                .help("multi thread thread number");
        parser.addArgument("-fa", "--fork_at_action")
                .setDefault(1)
                .help("using multi-thread in each action node with this prob");
        parser.addArgument("-fc", "--fork_at_chance")
                .setDefault(1)
                .help("using multi-thread in each chance node with this prob");
        parser.addArgument("-fe", "--fork_every_n_depth")
                .setDefault(1)
                .help("fork in between n layer of trees, default 1");
        parser.addArgument("-fs", "--no_fork_subtree_size")
                .setDefault(0)
                .help("fork minimal subtree size, default 0");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        String config_file = ns.getString("config");
        String player1_range = ns.getString("player1_range");
        String player2_range = ns.getString("player2_range");
        String initial_board_str = ns.getString("initial_board");
        String[] initial_board_arr = initial_board_str.split(",");
        int[] initial_board = Arrays.stream(initial_board_arr).map(e -> Card.strCard2int(e)).mapToInt(i->i).toArray();
        int iteration_number = Integer.parseInt(ns.getString("iteration_number"));
        int print_interval = Integer.parseInt(ns.getString("print_interval"));
        float fork_at_action = Float.parseFloat(ns.getString("fork_at_action"));
        float fork_at_chance = Float.parseFloat(ns.getString("fork_at_chance"));
        boolean debug = Boolean.valueOf(ns.getString("debug"));
        boolean parallel = Boolean.valueOf(ns.getString("parallel"));
        String output_strategy_file = ns.getString("output_strategy_file");
        String logfile = ns.getString("logfile");

        String algorithm_str = ns.getString("algorithm");
        Class<?> algorithm;
        switch(algorithm_str){
            case "cfr":
                algorithm = CfrTrainable.class;
                break;
            case "cfr_plus":
                algorithm = CfrPlusTrainable.class;
                break;
            case "discounted_cfr":
                algorithm = DiscountedCfrTrainable.class;
                break;
            default:
                throw new RuntimeException(String.format("algorithm not found :%s",algorithm_str));
        }
        String monte_coral_str = ns.getString("monte_carol");
        MonteCarolAlg monte_coral;
        switch(monte_coral_str){
            case "none":
                monte_coral = MonteCarolAlg.NONE;
                break;
            case "public":
                monte_coral = MonteCarolAlg.PUBLIC;
                break;
            default:
                throw new RuntimeException(String.format("monte coral type not found :%s",monte_coral_str));
        }
        int threads = Integer.parseInt(ns.getString("threads"));
        int fork_every_n_depth = Integer.parseInt(ns.getString("fork_every_n_depth"));
        int no_fork_subtree_size = Integer.parseInt(ns.getString("no_fork_subtree_size"));



        Config config = loadConfig(config_file);
        Deck deck = SolverEnvironment.deckFromConfig(config);
        Compairer compairer = SolverEnvironment.compairerFromConfig(config);
        GameTree game_tree = SolverEnvironment.gameTreeFromConfig(config,deck);

        PrivateCards[] player1Range = PrivateRangeConverter.rangeStr2Cards(player1_range,initial_board);
        PrivateCards[] player2Range = PrivateRangeConverter.rangeStr2Cards(player2_range,initial_board);

        Solver solver;
        if(parallel) {
            solver = new ParallelCfrPlusSolver(game_tree
                    , player1Range
                    , player2Range
                    , initial_board
                    , compairer
                    , deck
                    , iteration_number
                    , debug
                    , print_interval
                    , logfile
                    , algorithm
                    , monte_coral
                    , threads
                    , fork_at_action
                    , fork_at_chance
                    , fork_every_n_depth
                    , no_fork_subtree_size
            );
        }else{
            solver = new CfrPlusRiverSolver(game_tree
                    , player1Range
                    , player2Range
                    , initial_board
                    , compairer
                    , deck
                    , iteration_number
                    , debug
                    , print_interval
                    , logfile
                    , algorithm
                    , monte_coral
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
