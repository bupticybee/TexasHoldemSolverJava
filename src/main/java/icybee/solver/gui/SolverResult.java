package icybee.solver.gui;

import icybee.solver.GameTree;
import icybee.solver.nodes.ActionNode;
import icybee.solver.nodes.GameActions;
import icybee.solver.nodes.GameTreeNode;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.List;

public class SolverResult {
    public JPanel resultPanel;
    private JTable strategy_table;
    private JScrollPane tree_pane;
    private JTree game_tree_field;

    GameTree game_tree;
    GameTreeNode root;
    GameTreeNode.GameRound round;

    String[][] grid_names;

    class NodeDesc{
        GameTreeNode node;
        GameActions last_action;
        int action_ind;
        NodeDesc(GameTreeNode node,GameActions last_action,int action_ind){
            this.node = node;
            this.last_action = last_action;
            this.action_ind = action_ind;
        }

        @Override
        public String toString() {
            if(this.last_action == null) {
                return String.format("%s begin", GameTreeNode.gameRound2String(this.node.getRound()));
            }else{
                return String.format("p%d %s",((ActionNode) this.node).getPlayer(),last_action.toString());
            }
        }
    }

    SolverResult(GameTree game_tree,GameTreeNode root){
        this.game_tree = game_tree;
        this.root = root;
        this.round = root.getRound();
        DefaultMutableTreeNode treenode = new DefaultMutableTreeNode();
        treenode.setUserObject(new NodeDesc(root,null,0));
        reGenerateTree(this.root,treenode);
        JTree jtree_field = new JTree(treenode);
        game_tree_field.setModel(jtree_field.getModel());

        game_tree_field.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        game_tree_field.getLastSelectedPathComponent();

                /* if nothing is selected */
                if (node == null) return;
                Object nodeInfoObject = node.getUserObject();
                NodeDesc nodeinfo = (NodeDesc) nodeInfoObject;
                if(nodeinfo.last_action == null) return;
                System.out.println(nodeinfo.last_action.toString());
                TableCellRenderer tcr = new ColorTableCellRenderer();
                strategy_table.setDefaultRenderer(Object.class,tcr);

            }
        });
        construct_inital_table();
    }

    void construct_inital_table(){
        String[] columnName;
        if (this.game_tree.getDeck().getCards().size() == 52){
            columnName = new String[]{"A","K","Q","J","T","9","8","7","6","5","4","3","2"};
        }else if(this.game_tree.getDeck().getCards().size() == 36){
            columnName = new String[]{"A","K","Q","J","T","9","8","7","6"};
        }else{
            throw new RuntimeException(String.format("deck size %d unknown",this.game_tree.getDeck().getCards().size()));
        }

        grid_names = new String[columnName.length][columnName.length];
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
        strategy_table.setModel(defaultTableModel);
        strategy_table.setTableHeader(null);
        strategy_table.setShowHorizontalLines(true);
        strategy_table.setShowVerticalLines(true);
        strategy_table.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        strategy_table.setGridColor(Color.BLACK);
        strategy_table.setRowHeight(27);
    }

    void reGenerateTree(GameTreeNode node,DefaultMutableTreeNode parent){
        if(node.getRound() != this.root.getRound()) return;
        if(node instanceof ActionNode){
            ActionNode actionNode = (ActionNode) node;
            List<GameTreeNode> childs = actionNode.getChildrens();
            List<GameActions> actions = actionNode.getActions();

            assert(childs.size() == actions.size());
            for(int i = 0;i < childs.size();i ++){
                GameTreeNode one_child = childs.get(i);
                GameActions one_action = actions.get(i);
                DefaultMutableTreeNode one_tree_child = new DefaultMutableTreeNode();

                one_tree_child.setUserObject(new NodeDesc(root,one_action,i));
                parent.add(one_tree_child);
                reGenerateTree(one_child,one_tree_child);
            }
        }
    }


    class EachCellRenderer extends DefaultTableCellRenderer {

        int row,colunm;
        public EachCellRenderer(int row,int colunm) {
            this.row = row;
            this.colunm = colunm;
        }

        //该类继承与JLabel，Graphics用于绘制单元格,绘制红线
        public void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g;
            final BasicStroke stroke=new BasicStroke(2.0f);
            g2.setColor(Color.RED);
            g2.setStroke(stroke);
            g2.fillRect(0,0,row,colunm );
            super.paintComponent(g);
        }
    }

    class ColorTableCellRenderer extends DefaultTableCellRenderer
    {

        DefaultTableCellRenderer renderer=new DefaultTableCellRenderer();
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if((row + column)%2 == 0){
                EachCellRenderer cell_renderer = new EachCellRenderer(row,column);
                return cell_renderer.getTableCellRendererComponent(table, value, isSelected,hasFocus, row, column);
            }
            else{
                return renderer.getTableCellRendererComponent(table, value, isSelected,hasFocus, row, column);
            }
        }

    }

    void set_tree(){
    }
}
