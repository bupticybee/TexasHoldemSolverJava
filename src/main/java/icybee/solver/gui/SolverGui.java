package icybee.solver.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    private JTextField a50100200TextField;
    private JTextField a2TextField;
    private JCheckBox allinCheckBox;
    private JTextField a05TextField;
    private JTextField a1TextField;
    private JTextField a10TextField;
    private JComboBox mode;
    private JTextArea log;
    private JButton startSolvingButton;
    private JTextField iteration;
    private JTextField exploitability;
    private JTextField log_interval;
    private JTextField threads;
    private JCheckBox mc;
    private JComboBox comboBox1;

    public static void main(String[] args) {
        JFrame frame = new JFrame("SolverGui");
        frame.setContentPane(new SolverGui().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }


    public SolverGui() {
    }
}
