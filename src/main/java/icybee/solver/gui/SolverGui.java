package icybee.solver.gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import icybee.solver.*;
import icybee.solver.compairer.Compairer;
import icybee.solver.ranges.PrivateCards;
import icybee.solver.solver.CfrPlusRiverSolver;
import icybee.solver.solver.MonteCarolAlg;
import icybee.solver.solver.ParallelCfrPlusSolver;
import icybee.solver.solver.Solver;
import icybee.solver.trainable.CfrPlusTrainable;
import icybee.solver.trainable.DiscountedCfrTrainable;
import icybee.solver.utils.PrivateRangeConverter;

public class SolverGui {

    private JPanel mainPanel;
    private JPanel left;
    private JTabbedPane tabbedPane1;
    private JPanel board;
    private JTextArea ooprange;
    private JButton selectOOPRangeButton;
    private JTextArea iprange;
    private JButton selectIPRangeButton;
    private JTextArea boardstr;
    private JButton selectBoardCardButton;
    private JButton buildTreeButton;
    private JTextField oop_commit;
    private JTextField ip_commit;
    private JTextField bet_size;
    private JTextField raise_limit;
    private JCheckBox allin;
    private JTextField sm_blind;
    private JTextField big_blind;
    private JTextField stacks;
    private JComboBox mode;
    private JTextArea log;
    private JButton startSolvingButton;
    private JTextField iteration;
    private JTextField exploitability;
    private JTextField log_interval;
    private JTextField threads;
    private JCheckBox mc;
    private JComboBox algorithm;

    private Compairer compairer_holdem = null;
    private Compairer compairer_shortdeck = null;
    private Deck holdem_deck = null;
    private Deck shortdeck_deck = null;
    GameTree game_tree;

    public static void main(String[] args) {
        JFrame frame = new JFrame("SolverGui");
        frame.setContentPane(new SolverGui().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

    }


    Config loadConfig(String conf_name){

        ClassLoader classLoader = getClass().getClassLoader();
        URL fpath = classLoader.getResource(conf_name);
        File file;
        if(fpath == null) {
            // this happens when debug mode is on
            file = new File("src/test/resources/" + conf_name);
        }else{
            // this happens when debug mode is off
            file = new File(fpath.getFile());
        }

        Config config;
        try {
            config = new Config(file.getAbsolutePath());
        }catch(Exception e){
            throw new RuntimeException();
        }
        return config;
    }

    private void load_compairer() throws IOException {
        System.out.println("loading holdem compairer dictionary...");
        String config_name = "yamls/rule_holdem_simple.yaml";
        Config config = this.loadConfig(config_name);
        this.compairer_holdem = SolverEnvironment.compairerFromConfig(config,false);
        this.holdem_deck = SolverEnvironment.deckFromConfig(config);
        System.out.println("loading holdem compairer dictionary complete");

        System.out.println("loading shortdeck compairer dictionary...");
        config_name = "yamls/rule_shortdeck_simple.yaml";
        config = this.loadConfig(config_name);
        this.compairer_shortdeck = SolverEnvironment.compairerFromConfig(config,false);
        this.shortdeck_deck = SolverEnvironment.deckFromConfig(config);
        System.out.println("loading shortdeck compairer dictionary complete");
    }

    private void onBuildTree(){
        System.out.println("building tree...");
        int mode = this.mode.getSelectedIndex();

        String board = boardstr.getText();
        int board_num = board.split(",").length;
        int round;
        if (board_num == 3){
            round = 2;
        }
        else if (board_num == 4){
            round = 3;
        }
        else if (board_num == 5){
            round = 4;
        }else throw new RuntimeException("board number not valid");

        String[] bet_sizes = bet_size.getText().split(" ");
        if(allin.isSelected()){
            String[] new_bet_sizes = new String[bet_sizes.length + 1];
            for(int i = 0;i < bet_sizes.length;i ++) new_bet_sizes[i] = bet_sizes[i];
            new_bet_sizes[bet_sizes.length] = "all_in";
            bet_sizes = new_bet_sizes;
        }

        if(mode == 0) {
            this.game_tree = SolverEnvironment.gameTreeFromParams(
                    this.holdem_deck,
                    Float.valueOf(this.oop_commit.getText()),
                    Float.valueOf(this.ip_commit.getText()),
                    round,
                    Integer.valueOf(raise_limit.getText()),
                    Float.valueOf(this.sm_blind.getText()),
                    Float.valueOf(this.big_blind.getText()),
                    Float.valueOf(this.stacks.getText()),
                    bet_sizes
            );
        }else if(mode == 1){
            this.game_tree = SolverEnvironment.gameTreeFromParams(
                    this.shortdeck_deck,
                    Float.valueOf(this.oop_commit.getText()),
                    Float.valueOf(this.ip_commit.getText()),
                    round,
                    Integer.valueOf(raise_limit.getText()),
                    Float.valueOf(this.sm_blind.getText()),
                    Float.valueOf(this.big_blind.getText()),
                    Float.valueOf(this.stacks.getText()),
                    bet_sizes
            );
        }else{
            throw new RuntimeException("game mode unknown");
        }
        System.out.println("build tree complete");
    }

    private void initize(){
        System.out.println("initizing...");
        try {
            load_compairer();
        }catch (java.io.IOException err){
            err.printStackTrace();
        }
        System.out.println("initization complete");
    }

    private void solve() throws Exception{
        if (this.game_tree == null){
            System.out.println("please build tree first.");
            return;
        }
        System.out.println("solving...");
        String player1RangeStr = iprange.getText();
        String player2RangeStr = ooprange.getText();
        // TODO check these ranges

        String board = boardstr.getText();
        String[] board_cards = board.split(",");

        int[] initialBoard = new int[board_cards.length];
        for(int i = 0;i < board_cards.length;i ++){
            initialBoard[i] = Card.strCard2int(board_cards[i]);
        }

        PrivateCards[] player1Range = PrivateRangeConverter.rangeStr2Cards(player1RangeStr,initialBoard);
        PrivateCards[] player2Range = PrivateRangeConverter.rangeStr2Cards(player2RangeStr,initialBoard);
        String logfile_name = "src/test/resources/outputs/outputs_log.txt";

        Compairer compairer =  mode.getSelectedIndex() == 0 ? this.compairer_holdem:this.compairer_shortdeck;
        Deck deck = mode.getSelectedIndex() == 0 ? this.holdem_deck:this.shortdeck_deck;

        Solver solver = new ParallelCfrPlusSolver(game_tree
                , player1Range
                , player2Range
                , initialBoard
                , compairer
                , deck
                , Integer.valueOf(iteration.getText())
                , false
                , Integer.valueOf(log_interval.getText())
                , logfile_name
                , algorithm.getSelectedIndex() == 0? DiscountedCfrTrainable.class : CfrPlusTrainable.class
                , mc.isSelected()? MonteCarolAlg.PUBLIC:MonteCarolAlg.NONE
                , Integer.valueOf(threads.getText())
                ,1
                ,0
                , 1
                , 0
        );
        Map train_config = new HashMap();
        train_config.put("stop_exploitibility",Double.valueOf(exploitability.getText()));
        solver.train(train_config);
        System.out.println("solve complete");
    }

    public SolverGui() {
        PrintStream printStream = new PrintStream(new CustomOutputStream(this.log));
        System.setOut(printStream);
        System.setErr(printStream);

        // run initize
        new Thread(){
            public void run(){
                initize();
            }
        }.start();

        // build trree
        buildTreeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(){
                    public void run(){
                        onBuildTree();
                    }
                }.start();
            }
        });
        startSolvingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(){
                    public void run(){
                        try {
                            solve();
                        }catch (Exception err){
                            err.printStackTrace();
                        }
                    }
                }.start();
            }
        });
    }
}
