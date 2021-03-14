package icybee.solver.gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import icybee.solver.*;
import icybee.solver.compairer.Compairer;
import icybee.solver.ranges.PrivateCards;
import icybee.solver.solver.*;
import icybee.solver.trainable.CfrPlusTrainable;
import icybee.solver.trainable.DiscountedCfrTrainable;
import icybee.solver.utils.PrivateRangeConverter;
import icybee.solver.utils.Range;

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
    private JButton showTreeButton;
    private JButton showResult;
    private JTextField raise_limit;
    private JTextField stacks;
    private JComboBox mode;
    private JTextArea log;
    private JButton startSolvingButton;
    private JTextField iteration;
    private JTextField exploitability;
    private JTextField log_interval;
    private JTextField pot;
    private JTextField threads;
    private JCheckBox mc;
    private JComboBox algorithm;
    private JButton clearLogButton;
    private JTextField flop_ip_bet;
    private JTextField flop_ip_raise;
    private JCheckBox flop_ip_allin;
    private JTextField turn_ip_bet;
    private JTextField turn_ip_raise;
    private JCheckBox turn_ip_allin;
    private JTextField river_ip_bet;
    private JTextField river_ip_raise;
    private JCheckBox river_ip_allin;
    private JTextField flop_oop_bet;
    private JTextField flop_oop_raise;
    private JCheckBox flop_oop_allin;
    private JTextField turn_oop_bet;
    private JTextField turn_oop_raise;
    private JTextField turn_oop_donk;
    private JCheckBox turn_oop_allin;
    private JTextField river_oop_bet;
    private JTextField river_oop_raise;
    private JTextField river_oop_donk;
    private JCheckBox river_oop_allin;
    private JButton copy;

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
        File file;
        for(String one_url: new String[]{conf_name,"src/test/" + conf_name}) {
            file = new File(one_url);
            Config config;
            try {
                config = new Config(file.getAbsolutePath());
            } catch (Exception e) {
                continue;
            }
            return config;
        }
        throw new RuntimeException("load config failed: cannot find config file");
    }

    private void load_compairer() throws IOException {
        System.out.println("loading holdem compairer dictionary...");
        String config_name = "resources/yamls/rule_holdem_simple.yaml";
        Config config = this.loadConfig(config_name);
        this.compairer_holdem = SolverEnvironment.compairerFromConfig(config,false);
        this.holdem_deck = SolverEnvironment.deckFromConfig(config);
        System.out.println("loading holdem compairer dictionary complete");

        System.out.println("loading shortdeck compairer dictionary...");
        config_name = "resources/yamls/rule_shortdeck_simple.yaml";
        config = this.loadConfig(config_name);
        this.compairer_shortdeck = SolverEnvironment.compairerFromConfig(config,false);
        this.shortdeck_deck = SolverEnvironment.deckFromConfig(config);
        System.out.println("loading shortdeck compairer dictionary complete");
    }

    float[] parseBetSizes(String betstr){
        String[] bets_str = betstr.split(" ");
        float[] bet_sizes = new float[bets_str.length];
        for(int i = 0;i < bets_str.length;i ++){
            String one_bet_str = bets_str[i];
            if(one_bet_str.length() == 0)continue;
            boolean multiplier = false;
            if(one_bet_str.charAt(one_bet_str.length() - 1) == 'x'){
                one_bet_str = one_bet_str.substring(0,one_bet_str.length() - 1);
                multiplier = true;
            }
            if(multiplier) bet_sizes[i] = Float.parseFloat(one_bet_str) * 100;
            else bet_sizes[i] = Float.parseFloat(one_bet_str);
        }
        return bet_sizes;
    }

    GameTreeBuildingSettings parseSettings(){
        GameTreeBuildingSettings.StreetSetting flop_ip = new GameTreeBuildingSettings.StreetSetting(
                parseBetSizes(flop_ip_bet.getText()),
                parseBetSizes(flop_ip_raise.getText()),
                null,
                flop_ip_allin.isSelected()
        );
        GameTreeBuildingSettings.StreetSetting turn_ip = new GameTreeBuildingSettings.StreetSetting(
                parseBetSizes(turn_ip_bet.getText()),
                parseBetSizes(turn_ip_raise.getText()),
                null,
                turn_ip_allin.isSelected()
        );
        GameTreeBuildingSettings.StreetSetting river_ip = new GameTreeBuildingSettings.StreetSetting(
                parseBetSizes(river_ip_bet.getText()),
                parseBetSizes(river_ip_raise.getText()),
                null,
                river_ip_allin.isSelected()
        );

        GameTreeBuildingSettings.StreetSetting flop_oop = new GameTreeBuildingSettings.StreetSetting(
                parseBetSizes(flop_oop_bet.getText()),
                parseBetSizes(flop_oop_raise.getText()),
                null,
                flop_oop_allin.isSelected()
        );
        GameTreeBuildingSettings.StreetSetting turn_oop = new GameTreeBuildingSettings.StreetSetting(
                parseBetSizes(turn_oop_bet.getText()),
                parseBetSizes(turn_oop_raise.getText()),
                parseBetSizes(turn_oop_donk.getText()),
                turn_oop_allin.isSelected()
        );
        GameTreeBuildingSettings.StreetSetting river_oop = new GameTreeBuildingSettings.StreetSetting(
                parseBetSizes(river_oop_bet.getText()),
                parseBetSizes(river_oop_raise.getText()),
                parseBetSizes(river_oop_donk.getText()),
                river_oop_allin.isSelected()
        );
        GameTreeBuildingSettings gameTreeBuildingSettings = new GameTreeBuildingSettings(flop_ip,turn_ip,river_ip,flop_oop,turn_oop,river_oop);
        return gameTreeBuildingSettings;
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


        if(mode == 0) {
            this.game_tree = SolverEnvironment.gameTreeFromParams(
                    this.holdem_deck,
                    Float.valueOf(this.pot.getText()) / 2,
                    Float.valueOf(this.pot.getText()) / 2,
                    round,
                    Integer.valueOf(raise_limit.getText()),
                    (float) 0.5,
                    (float) 1.0,
                    Float.valueOf(this.stacks.getText()) + Float.valueOf(this.pot.getText()) / 2,
                    parseSettings()
            );
        }else if(mode == 1){
            this.game_tree = SolverEnvironment.gameTreeFromParams(
                    this.shortdeck_deck,
                    Float.valueOf(this.pot.getText()) / 2,
                    Float.valueOf(this.pot.getText()) / 2,
                    round,
                    Integer.valueOf(raise_limit.getText()),
                    (float) 0.5,
                    (float) 1.0,
                    Float.valueOf(this.stacks.getText()) + Float.valueOf(this.pot.getText()) / 2,
                    parseSettings()
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
        String logfile_name = null;

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
                ,1
                , 1
                , 16
        );
        Map train_config = new HashMap();
        train_config.put("stop_exploitibility",Double.valueOf(exploitability.getText()));
        solver.train(train_config);

        /*
        String output_strategy_file = "out/demo.json";
        String strategy_json = solver.getTree().dumps(false).toJSONString();
        File output_file = new File(output_strategy_file);
        FileWriter writer = new FileWriter(output_file);
        writer.write(strategy_json);
        writer.flush();
        writer.close();
         */
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
                if(game_tree == null) {
                    System.out.println("Please build tree first");
                    return;
                }
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
        showTreeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(game_tree == null){
                    System.out.println("Please build tree first");
                }else {
                    game_tree.printTree(100);
                }
            }
        });
        clearLogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.setText("");
            }
        });
        showResult.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(game_tree == null) {
                    System.out.println("Please build tree first");
                    return;
                }
                SolverResult sr = new SolverResult(game_tree,game_tree.getRoot(),boardstr.getText());
                JFrame frame = new JFrame("SolverResult");
                frame.setContentPane(sr.resultPanel);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            }
        });
        ooprange.setLineWrap(true);
        iprange.setLineWrap(true);
        selectOOPRangeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                RangeSelectorCallback rsc = new RangeSelectorCallback() {
                    @Override
                    void onFinish(String content) {
                        ooprange.setText(content);
                    }
                };
                JFrame frame = new JFrame("RangeSelector");
                RangeSelector rr = new RangeSelector(rsc,ooprange.getText(),mode.getSelectedIndex() == 0? RangeSelector.RangeType.HOLDEM: RangeSelector.RangeType.SHORTDECK,frame);
                frame.setContentPane(rr.range_selector_main_panel);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);

            }
        });

        selectIPRangeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                RangeSelectorCallback rsc = new RangeSelectorCallback() {
                    @Override
                    void onFinish(String content) {
                        iprange.setText(content);
                    }
                };
                JFrame frame = new JFrame("RangeSelector");
                RangeSelector rr = new RangeSelector(rsc,iprange.getText(),mode.getSelectedIndex() == 0? RangeSelector.RangeType.HOLDEM: RangeSelector.RangeType.SHORTDECK,frame);
                frame.setContentPane(rr.range_selector_main_panel);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            }
        });
        selectBoardCardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BoardSelectorCallback bsc = new BoardSelectorCallback() {
                    @Override
                    void onFinish(String content) {
                        boardstr.setText(content);
                    }
                };
                JFrame frame = new JFrame("BoardSelector");
                BoardSelector br = new BoardSelector(bsc,boardstr.getText(),mode.getSelectedIndex() == 0? RangeSelector.RangeType.HOLDEM: RangeSelector.RangeType.SHORTDECK,frame);
                frame.setContentPane(br.main_panel);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            }
        });
        copy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                flop_oop_allin.setSelected(flop_ip_allin.isSelected());
                flop_oop_raise.setText(flop_ip_raise.getText());
                flop_oop_bet.setText(flop_ip_bet.getText());

                turn_oop_allin.setSelected(turn_ip_allin.isSelected());
                turn_oop_raise.setText(turn_ip_raise.getText());
                turn_oop_bet.setText(turn_ip_bet.getText());

                river_oop_allin.setSelected(river_ip_allin.isSelected());
                river_oop_raise.setText(river_ip_raise.getText());
                river_oop_bet.setText(river_ip_bet.getText());
            }
        });
    }
}
