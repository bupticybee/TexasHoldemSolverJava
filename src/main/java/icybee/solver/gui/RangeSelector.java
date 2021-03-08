package icybee.solver.gui;
import icybee.solver.nodes.GameActions;
import icybee.solver.nodes.GameTreeNode;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class RangeSelector {

    public JPanel range_selector_main_panel;
    private JTextArea range_text;
    private JTable range_table;
    private JTree file_tree;
    private JPanel range_table_holder;
    private JSlider num_slider;
    private JScrollPane range_panel;
    private JLabel number;
    private JButton confirmButton;

    RangeSelectorCallback callback;
    RangeType rangeType;
    String[] columnName;
    String[][] grid_names;
    float[][] range_matrix;
    float global_range_num = 1;
    JFrame frame;

    public enum RangeType{
        HOLDEM,
        SHORTDECK
    }

    public RangeSelector(RangeSelectorCallback callback,String init_range,RangeType rangeType,JFrame frame) {
        this.callback = callback;
        this.rangeType = rangeType;
        this.frame = frame;

        if (rangeType == RangeType.HOLDEM){
            this.columnName = new String[]{"A","K","Q","J","T","9","8","7","6","5","4","3","2"};
        }else if(rangeType == RangeType.SHORTDECK){
            this.columnName = new String[]{"A","K","Q","J","T","9","8","7","6"};
        }else{
            throw new RuntimeException("range type unknown");
        }

        grid_names = new String[columnName.length][columnName.length];
        range_matrix = new float[columnName.length][columnName.length];
        for(int i = 0;i < columnName.length;i ++) {
            boolean s_start = false;
            for(int j = 0;j < columnName.length;j ++) {
                if(j == i){s_start = true;
                    grid_names[i][j] = String.format("%s%s",columnName[i],columnName[j]);}
                else if(s_start) grid_names[i][j] = String.format("%s%ss",columnName[i],columnName[j]);
                else grid_names[i][j] = String.format("%s%so",columnName[j],columnName[i]);
            }
        }

        DefaultTableModel defaultTableModel = new DefaultTableModel(grid_names, columnName);

        range_table.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                range_table.setRowHeight(26);
                Dimension p = range_table.getPreferredSize();
                Dimension v = range_panel.getViewportBorderBounds().getSize();
                if (v.height > p.height) {
                    int available = v.height -
                            range_table.getRowCount() * range_table.getRowMargin();
                    int perRow = available / range_table.getRowCount();
                    range_table.setRowHeight(perRow);
                }
            }
        });
        num_slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                float num = (float)num_slider.getValue() / 1000;
                number.setText(String.valueOf(num));
                global_range_num = num;
            }
        });

        range_text.setLineWrap(true);
        processInputString(init_range);
        range_table.setModel(defaultTableModel);

        range_table.setTableHeader(null);
        range_table.setShowHorizontalLines(true);
        range_table.setShowVerticalLines(true);
        range_table.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        range_table.setGridColor(Color.BLACK);
        range_table.setRowHeight(26);
        range_table.setCellSelectionEnabled(true);
        range_table.setDefaultRenderer(Object.class,new RangeGridColorTableCellRenderer());
        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                callback.onFinish(convertRangeToText());
                frame.setVisible(false);
                frame.dispose();
            }
        });
        range_panel.addComponentListener(new ComponentAdapter() {
        });
        range_table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                range_text.setText(convertRangeToText());
                range_text.updateUI();
                super.mouseClicked(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                range_text.setText(convertRangeToText());
                range_text.updateUI();
                super.mouseReleased(e);
            }

        });

        range_text.setText(convertRangeToText());
        range_text.updateUI();
    }

    private String convertRangeToText(){
        String rangestr = "";
        for(int i = 0;i < columnName.length;i ++){
            for(int j = 0;j < columnName.length;j ++){
                if(range_matrix[i][j] != 0){
                    rangestr += String.format("%s:%.3f,",grid_names[i][j],range_matrix[i][j]);
                }
            }
        }
        return rangestr;
    }

    private void process(String input_str,float value){
        for(int i = 0;i < columnName.length;i ++){
            for(int j = 0;j < columnName.length;j ++){
                if(input_str.equals(grid_names[i][j])){
                    range_matrix[i][j] = value;
                    return;
                }
            }
        }
        throw new RuntimeException(String.format("range %s unknown",input_str));
    }

    private void processInputString(String input_range){
        String[] range_split = input_range.split(",");
        for(String one_range_str:range_split){
            if(one_range_str.length() == 0)continue;
            String[] range_weight = one_range_str.split(":");
            String pure_range_str;
            float weight = 1;
            pure_range_str = range_weight[0];
            if(range_weight.length == 2) weight = Float.valueOf(range_weight[1]);

            if(pure_range_str.length() == 2 && pure_range_str.charAt(0) != pure_range_str.charAt(1)){
                process(pure_range_str + "o", weight);
                process(pure_range_str + "s", weight);
            }else if(pure_range_str.length() <= 3){
                process(pure_range_str,weight);
            }else throw new RuntimeException(String.format("range %s not valid",pure_range_str));
        }
    }

    class RangeGridCellRenderer extends DefaultTableCellRenderer {

        int row,colunm;
        float node_range;
        boolean selected;
        public RangeGridCellRenderer(int row,int colunm,boolean selected) {
            this.row = row;
            this.colunm = colunm;
            this.selected = selected;
            if(this.selected) range_matrix[row][colunm] = global_range_num;
            this.node_range = range_matrix[row][colunm];
        }

        private void paintBlackSide(Graphics g){
            Graphics2D g2=(Graphics2D)g;
            final BasicStroke stroke=new BasicStroke(3.0f);
            g2.setStroke(stroke);
            g2.setColor(Color.BLACK);
            g2.drawRect(0,0,getWidth(),getHeight());
        }

        public void paintComponent(Graphics g){
            int disable_height = (int)((1 - this.node_range) * getHeight());
            Graphics2D g2=(Graphics2D)g;
            g2.setColor(Color.YELLOW);
            g2.fillRect(0,0,getWidth(),getHeight());
            g2.setColor(Color.GRAY);
            g2.fillRect(0,0,getWidth(),disable_height);
            String origin_str = getText();
            setText(String.format("<html>%s<br>%.3f</html>",getText(),this.node_range));
            if(this.selected)paintBlackSide(g);
            super.paintComponent(g);
            setText(origin_str);
        }
    }

    class RangeGridColorTableCellRenderer extends DefaultTableCellRenderer
    {
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            RangeGridCellRenderer cell_renderer = new RangeGridCellRenderer(row,column,isSelected);
            return cell_renderer.getTableCellRendererComponent(table, value, false,false, row, column);
        }
    }

}
