package icybee.solver.gui;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;

public class BoardSelector {
    private JTextArea board_text;
    private JPanel board_table_holder;
    private JScrollPane board_panel;
    private JTable board_table;
    private JButton confirmButton;
    public JPanel main_panel;
    private JButton clearButton;

    private String[] columnName;
    String[][] grid_names;
    String[][] grid_text;
    boolean[][] selecte2d;

    BoardSelectorCallback callback;
    RangeSelector.RangeType type;

    public BoardSelector(BoardSelectorCallback callback,String init_board_str, RangeSelector.RangeType type,Frame frame) {
        this.callback = callback;
        this.type = type;
        board_text.setText(init_board_str);

        if (type == RangeSelector.RangeType.HOLDEM){
            this.columnName = new String[]{"A","K","Q","J","T","9","8","7","6","5","4","3","2"};
        }else if(type == RangeSelector.RangeType.SHORTDECK){
            this.columnName = new String[]{"A","K","Q","J","T","9","8","7","6"};
        }else{
            throw new RuntimeException("range type unknown");
        }

        grid_names = new String[4][columnName.length];
        grid_text = new String[4][columnName.length];
        selecte2d = new boolean[4][columnName.length];

        String[] boardstr = init_board_str.split(",");
        String colors = "cdhs";
        for(int i = 0;i < 4;i ++) {
            for(int j = 0;j < columnName.length;j ++) {
                grid_names[i][j] = String.format("%s%c",columnName[j],colors.charAt(i));
                String one_color_str = String.valueOf(colors.charAt(i)).replace("c","<font color=\"black\">♣</font>")
                        .replace("d","<font color=\"red\">♦</font>")
                        .replace("h","<font color=\"red\">♥</font>")
                        .replace("s","<font color=\"black\">♠</font>");
                grid_text[i][j] = String.format("<html>%s%s</html>",columnName[j],one_color_str);
                selecte2d[i][j] = false;
                for(String one_board:boardstr){
                    if(grid_names[i][j].equals(one_board)) selecte2d[i][j] = true;
                }
            }
        }


        DefaultTableModel defaultTableModel = new DefaultTableModel(grid_text, columnName){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        board_table.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                board_table.setRowHeight(26);
                Dimension p = board_table.getPreferredSize();
                Dimension v = board_panel.getViewportBorderBounds().getSize();
                if (v.height > p.height) {
                    int available = v.height -
                            board_table.getRowCount() * board_table.getRowMargin();
                    int perRow = available / board_table.getRowCount();
                    board_table.setRowHeight(perRow);
                }
            }
        });
        board_table.setModel(defaultTableModel);
        board_table.setTableHeader(null);
        board_table.setShowHorizontalLines(true);
        board_table.setShowVerticalLines(true);
        board_table.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        board_table.setGridColor(Color.BLACK);
        board_table.setRowHeight(26);
        board_table.setCellSelectionEnabled(true);
        board_table.setDefaultRenderer(Object.class,new BoardGridColorTableCellRenderer());

        board_table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int row = board_table.getSelectedRow();
                int col = board_table.getSelectedColumn();
                selecte2d[row][col] = !selecte2d[row][col];
                setTextByBoard();
                super.mouseClicked(e);
                board_table.updateUI();
            }
        });
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for(int i = 0;i < 4;i ++){
                    for(int j = 0;j < grid_names[0].length;j ++){
                        selecte2d[i][j] = false;
                    }
                }
                setTextByBoard();
                board_table.updateUI();
            }
        });
        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callback.onFinish(board_text.getText());
                frame.setVisible(false);
                frame.dispose();
            }
        });
    }

    void setTextByBoard(){
        String boardstr_toset = "";
        for(int i = 0;i < 4;i ++){
            for(int j = 0;j < grid_names[0].length;j ++){
                if(selecte2d[i][j])boardstr_toset += String.format("%s,",grid_names[i][j]);
            }
        }
        board_text.setText(boardstr_toset);
    }


    class BoardGridCellRenderer extends DefaultTableCellRenderer {
        int row,colunm;
        float node_range;
        boolean selected;
        public BoardGridCellRenderer(int row,int colunm,boolean selected) {
            this.row = row;
            this.colunm = colunm;
            this.selected = selecte2d[row][colunm];
        }

        public void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g;
            g2.setColor(Color.GRAY);
            if(this.selected)g2.setColor(Color.YELLOW);
            g2.fillRect(0,0,getWidth(),getHeight());
            super.paintComponent(g);
        }
    }

    class BoardGridColorTableCellRenderer extends DefaultTableCellRenderer
    {
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            BoardGridCellRenderer cell_renderer = new BoardGridCellRenderer(row,column,isSelected);
            return cell_renderer.getTableCellRendererComponent(table, value, false,false, row, column);
        }
    }
}
