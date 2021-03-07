package icybee.solver.gui;

import icybee.solver.Card;
import icybee.solver.GameTree;
import icybee.solver.nodes.ActionNode;
import icybee.solver.nodes.ChanceNode;
import icybee.solver.nodes.GameActions;
import icybee.solver.nodes.GameTreeNode;
import icybee.solver.ranges.PrivateCards;
import icybee.solver.trainable.DiscountedCfrTrainable;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SolverResult {
    public JPanel resultPanel;
    private JTable strategy_table;
    private JScrollPane tree_pane;
    private JTree game_tree_field;
    private JTable detail_table;
    private JPanel tree_and_strategy;
    private JScrollPane tree_panel;
    private JTabbedPane tabbedPane1;
    private JTable global_info;
    private JTable table_url;
    private JScrollPane detail_strategy_panel;
    private JTextPane text_info_panel;
    private NodeDesc global_node_desc = null;

    GameTree game_tree;
    GameTreeNode root;
    GameTreeNode.GameRound round;

    String[][] grid_names;
    String[] columnName;
    String boardstr;

    float sum(float[] ins){
        float sumnum = 0;
        for(int i = 0;i < ins.length;i ++)sumnum += ins[i];
        return sumnum;
    }

    class NodeDesc{
        GameTreeNode node;
        String last_action;
        int action_ind;
        NodeDesc(GameTreeNode node,String last_action,int action_ind){
            this.node = node;
            this.last_action = last_action;
            this.action_ind = action_ind;
        }

        public Color getColor(){
            if(this.last_action == null) {
                return Color.ORANGE;
            }else if(this.node.getParent() != null && this.node.getParent() instanceof  ActionNode){
                ActionNode actionNode = ((ActionNode) this.node.getParent());
                GameActions action = actionNode.getActions().get(this.action_ind);
                if(action.getAction() == GameTreeNode.PokerActions.CALL || action.getAction() == GameTreeNode.PokerActions.CHECK){
                    return Color.GREEN;
                }else if(action.getAction() == GameTreeNode.PokerActions.BET || action.getAction() == GameTreeNode.PokerActions.RAISE){
                    return Color.RED;
                }else{
                    return Color.GRAY;
                }
            }else{
                return Color.YELLOW;
            }
        }

        @Override
        public String toString() {
            if(this.last_action == null) {
                return String.format("%s begin", GameTreeNode.gameRound2String(this.node.getRound()));
            }else if(this.node.getParent() != null && this.node.getParent() instanceof  ActionNode){
                int player = ((ActionNode) this.node.getParent()).getPlayer();
                String pname = player == 0? "ip":"oop";
                return String.format("%s %s",pname,last_action);
            }else{
                return last_action;
            }
        }
    }

    SolverResult(GameTree game_tree,GameTreeNode root,String boardstr){
        this.game_tree = game_tree;
        this.root = root;
        this.round = root.getRound();
        this.boardstr = boardstr;
        DefaultMutableTreeNode treenode = new DefaultMutableTreeNode();
        treenode.setUserObject(new NodeDesc(root,null,0));
        reGenerateTree(this.root,treenode,root.getRound());
        JTree jtree_field = new JTree(treenode);
        game_tree_field.setModel(jtree_field.getModel());

        game_tree_field.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        game_tree_field.getLastSelectedPathComponent();

                /* if nothing is selected */
                if (node == null) return;
                update_url(node);
                Object nodeInfoObject = node.getUserObject();
                NodeDesc nodeinfo = (NodeDesc) nodeInfoObject;
                global_node_desc = nodeinfo;
                TableCellRenderer tcr = new ColorTableCellRenderer(nodeinfo);
                strategy_table.setDefaultRenderer(Object.class,tcr);

                update_global_strategy(nodeinfo);

                if(nodeinfo.node instanceof ChanceNode && node.getChildCount() == 0) {
                    reGenerateTree(nodeinfo.node,node,nodeinfo.node.getRound());
                }
                strategy_table.updateUI();
            }
        });
        construct_inital_table();
        strategy_table.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                strategy_table.setRowHeight(26);
                Dimension p = strategy_table.getPreferredSize();
                Dimension v = tree_panel.getViewportBorderBounds().getSize();
                if (v.height > p.height)
                {
                    int available = v.height -
                            strategy_table.getRowCount() * strategy_table.getRowMargin();
                    int perRow = available / strategy_table.getRowCount();
                    strategy_table.setRowHeight(perRow);
                }
            }
        });


        detail_table.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                if(detail_table.getRowCount() == 0)return;
                detail_table.setRowHeight(26);
                Dimension p = detail_table.getPreferredSize();
                Dimension v = detail_strategy_panel.getViewportBorderBounds().getSize();
                if (v.height > p.height)
                {
                    int available = v.height -
                            detail_table.getRowCount() * detail_table.getRowMargin();
                    int perRow = available / detail_table.getRowCount();
                    detail_table.setRowHeight(perRow);
                }
            }
        });
        strategy_table.addMouseMotionListener(new MouseMotionAdapter() {
            int last_id = -1;
            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
                Point p = e.getPoint();
                int row = strategy_table.rowAtPoint(p);
                int col = strategy_table.columnAtPoint(p);
                int this_id = row * 100 + col;
                if(global_node_desc  != null && last_id != this_id) {
                    setDetailStrategyInfo(row, col, global_node_desc);
                }
                last_id = this_id;
            }
        });
        global_info.setRowHeight(100);
    }

    void update_url(DefaultMutableTreeNode node) {
        List<String> strs = new ArrayList<String>();
        ArrayList<Color> colors = new ArrayList<Color>();
        while(node != null) {
            Object nodeInfoObject = node.getUserObject();
            NodeDesc nodeinfo = (NodeDesc) nodeInfoObject;
            strs.add(0,nodeinfo.toString());
            colors.add(0,nodeinfo.getColor());
            node = (DefaultMutableTreeNode) node.getParent();
        }
        String[] urls = strs.toArray(new String[0]);
        String[][] content = new String[][]{urls};
        DefaultTableModel defaultTableModel = new DefaultTableModel(content,urls);
        table_url.setModel(defaultTableModel);
        table_url.setDefaultRenderer(Object.class,new UrlColorTableCellRenderer(colors));
        table_url.updateUI();
    }

    void update_global_strategy(NodeDesc desc){
        String[][] content = new String[1][];

        GameTreeNode node = desc.node;
        if(!(node instanceof ActionNode))return;
        ActionNode actionNode = (ActionNode) node;
        String[] actions = new String[actionNode.getActions().size()];
        for(int i = 0;i < actions.length;i ++){
            actions[i] = actionNode.getActions().get(i).toString();
        }
        content[0] = actions;

        if(global_node_desc.node.getType() != GameTreeNode.GameTreeNodeType.ACTION){
            return;
        }
        actionNode = (ActionNode)global_node_desc.node;
        DiscountedCfrTrainable dct = (DiscountedCfrTrainable) actionNode.getTrainable();
        int player = actionNode.getPlayer();
        float[] reach_probs = dct.getReach_probs()[player];
        float[] evs = dct.getEvs();
        float[] strategy = dct.getAverageStrategy();
        assert(evs.length == reach_probs.length);

        float total_sum = 0;
        int action_number = actionNode.getActions().size();
        float[] global_strategy = new float[action_number];
        float[] combos = new float[action_number];
        for (int action_id = 0;action_id < action_number;action_id ++) {
            for(int private_id = 0;private_id < dct.getPrivateCards().length;private_id ++) {
                int index = action_id * dct.getPrivateCards().length + private_id;
                float current_sum = strategy[index] * reach_probs[private_id];
                total_sum += current_sum;
                global_strategy[action_id] += current_sum;
                combos[action_id] += strategy[index] * reach_probs[private_id];
            }
        }
        for(int i = 0;i < global_strategy.length;i ++){
            global_strategy[i] /= total_sum;
            actions[i] = String.format("<html><h4>%s</h4><p style=\"font-size:7px\"> %.1f combos<br>%.1f %s</p></html>"
                    ,actionNode.getActions().get(i).toString()
                    ,combos[i]
                    ,global_strategy[i] * 100
                    ,"%"
            );
        }
        DefaultTableModel defaultTableModel = new DefaultTableModel(content,actions);
        global_info.setModel(defaultTableModel);
        global_info.setTableHeader(null);
        GlobalStrategyColorTableCellRenderer gcr = new GlobalStrategyColorTableCellRenderer(actionNode.getActions(),global_strategy,combos);
        global_info.setDefaultRenderer(Object.class,gcr);
        ComponentListener[] listeners = global_info.getComponentListeners();
        for(ComponentListener cl:listeners)
            global_info.removeComponentListener(cl);
        global_info.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                setInfoTableWidths(global_strategy);

            }
        });
        setInfoTableWidths(global_strategy);
        global_info.updateUI();

        String board_str_toshow = this.boardstr.replace('c','♣')
                .replace('d','♦')
                .replace('h','♥')
                .replace('s','♠');
        String player_toshow = player == 0? "IP":"OOP";
        String info_toshow = String.format("<html>board: %s<br><h3>%s strategy</h3></html>",board_str_toshow,player_toshow);
        text_info_panel.setContentType("text/html");
        text_info_panel.setText(info_toshow);
        text_info_panel.setEditable(false);
    }

    private void setInfoTableWidths(float[] global_strategy) {
        int tW = global_info.getWidth();
        TableColumn column;
        TableColumnModel jTableColumnModel = global_info.getColumnModel();
        int cantCols = jTableColumnModel.getColumnCount();
        for (int i = 0; i < cantCols; i++) {
            column = jTableColumnModel.getColumn(i);
            int pWidth = Math.round(global_strategy[i] * tW);
            column.setPreferredWidth(pWidth);
        }
    }

    void construct_inital_table(){
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
        strategy_table.setRowHeight(26);
        strategy_table.setRowSelectionAllowed(true);

        detail_table.setTableHeader(null);
        detail_table.setShowHorizontalLines(true);
        detail_table.setShowVerticalLines(true);
        detail_table.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        detail_table.setGridColor(Color.BLACK);
        detail_table.setRowHeight(26);
        detail_table.setRowSelectionAllowed(true);
    }

    void reGenerateTree(GameTreeNode node,DefaultMutableTreeNode parent,GameTreeNode.GameRound stop_round){
        if(node.getRound() != stop_round) return;
        if(node instanceof ActionNode){
            ActionNode actionNode = (ActionNode) node;
            List<GameTreeNode> childs = actionNode.getChildrens();
            List<GameActions> actions = actionNode.getActions();

            assert(childs.size() == actions.size());
            for(int i = 0;i < childs.size();i ++){
                GameTreeNode one_child = childs.get(i);
                GameActions one_action = actions.get(i);
                DefaultMutableTreeNode one_tree_child = new DefaultMutableTreeNode();

                one_tree_child.setUserObject(new NodeDesc(one_child,one_action.toString(),i));
                parent.add(one_tree_child);
                reGenerateTree(one_child,one_tree_child,stop_round);
            }
        }else if(node instanceof ChanceNode){
            ChanceNode chanceNode = (ChanceNode)node;
            List<GameTreeNode> childs = chanceNode.getChildrens();
            List<Card> cards = chanceNode.getCards();

            assert(childs.size() == cards.size());
            for(int i = 0;i < childs.size();i ++){
                GameTreeNode one_child = childs.get(i);
                Card one_card = cards.get(i);
                DefaultMutableTreeNode one_tree_child = new DefaultMutableTreeNode();
                String act_str = String.format("%s - %s",one_card.toFormatString(),node.getRound());
                one_tree_child.setUserObject(new NodeDesc(one_child,act_str,i));
                parent.add(one_tree_child);
                reGenerateTree(one_child,one_tree_child,stop_round);
            }

        }
    }


    class EachCellRenderer extends DefaultTableCellRenderer {

        int row,colunm;
        NodeDesc desc;
        float[] node_strategy = null;
        String name;
        List<GameActions> actions;
        boolean selected;
        public EachCellRenderer(int row,int colunm,NodeDesc desc,boolean selected) {
            this.row = row;
            this.colunm = colunm;
            this.desc = desc;
            this.name = grid_names[row][colunm];
            this.selected = selected;

            GameTreeNode node = desc.node;
            if(!(node instanceof ActionNode)) {
                return;
            }
            ActionNode actionNode = (ActionNode) node;
            DiscountedCfrTrainable trainable = (DiscountedCfrTrainable) actionNode.getTrainable();
            float[] strategy = trainable.getAverageStrategy();
            List<GameActions> actions = actionNode.getActions();
            this.actions = actions;
            PrivateCards[] cards = trainable.getPrivateCards();

            int num_cases = 0;
            node_strategy = new float[actions.size()];
            Arrays.fill(node_strategy,0);

            for(int i = 0;i < cards.length;i ++){
                PrivateCards one_private_card = cards[i];
                if(!this.name.equals(one_private_card.summary()))continue;
                float[] one_strategy = new float[actions.size()];
                num_cases += 1;
                for(int j = 0;j < actions.size();j ++){
                    int strategy_index = j * cards.length + i;
                    one_strategy[j] = strategy[strategy_index];
                    node_strategy[j] = node_strategy[j] * (num_cases - 1) / num_cases + strategy[strategy_index] / num_cases;
                }
            }
            /*
            System.out.print(String.format("%s : ",this.name));
            for (float prob:node_strategy)
                System.out.print(String.format(" %s",prob));
            System.out.println();
            */
            assert(actions.size() == node_strategy.length);
        }

        private void paintBlackSide(Graphics g){
            Graphics2D g2=(Graphics2D)g;
            final BasicStroke stroke=new BasicStroke(3.0f);
            g2.setStroke(stroke);
            g2.setColor(Color.BLACK);
            g2.drawRect(0,0,getWidth(),getHeight());
        }

        public void paintComponent(Graphics g){
            if(node_strategy == null || sum(node_strategy) == 0){
                super.paintComponent(g);
                if(this.selected)paintBlackSide(g);
                return;
            }
            Graphics2D g2=(Graphics2D)g;
            float check_fold_prob = 0;
            for(int i = 0;i < actions.size();i ++){
                GameActions one_action = actions.get(i);
                if(one_action.getAction() == GameTreeNode.PokerActions.FOLD){
                    check_fold_prob = node_strategy[i];
                }
            }

            int disable_height = (int)(check_fold_prob * getHeight());
            int remain_height = getHeight() - disable_height;
            g2.setColor(Color.GRAY);
            g2.fillRect(0,0,getWidth(),disable_height);

            int begin_w = 0;
            for(int i = 0;i < actions.size();i ++){
                GameActions one_action = actions.get(i);
                if(one_action.getAction() == GameTreeNode.PokerActions.CHECK
                        ||one_action.getAction() == GameTreeNode.PokerActions.CALL
                ){
                    int prob_width = Math.round(node_strategy[i] / (1 - check_fold_prob) * getWidth());
                    if(node_strategy[i] != 0) prob_width = Math.max(1,prob_width);
                    g2.setColor(Color.GREEN);
                    g2.fillRect(begin_w,disable_height,prob_width,remain_height);
                    begin_w += prob_width;
                }else if(one_action.getAction() == GameTreeNode.PokerActions.BET
                        ||one_action.getAction() == GameTreeNode.PokerActions.RAISE
                ){
                    int prob_width = Math.round(node_strategy[i] / (1 - check_fold_prob) * getWidth());
                    if(node_strategy[i] != 0) prob_width = Math.max(1,prob_width);
                    if(i == actions.size() - 1)  prob_width = Math.max(prob_width,getWidth() - begin_w);
                    int color_base = Math.max(128 - 32 * i - 1,0);
                    g2.setColor(new Color(255,color_base,color_base));
                    g2.fillRect(begin_w,disable_height,prob_width,remain_height);
                    begin_w += prob_width;
                }
            }
            super.paintComponent(g);
            if(this.selected)paintBlackSide(g);
        }
    }

    class ColorTableCellRenderer extends DefaultTableCellRenderer
    {

        NodeDesc desc;
        public ColorTableCellRenderer(NodeDesc desc) {
            this.desc = desc;
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            EachCellRenderer cell_renderer = new EachCellRenderer(row,column,desc,isSelected);
            //if(isSelected) setDetailStrategyInfo(row,column,desc);
            return cell_renderer.getTableCellRendererComponent(table, value, false,false, row, column);
        }
    }

    class DetailStrategyInfo{
        public String cards_name;
        public List<GameActions> actions;
        public float[] strategy;

        public DetailStrategyInfo(String cards_name, List<GameActions> actions, float[] strategy) {
            this.cards_name = cards_name;
            this.actions = actions;
            this.strategy = strategy;
            assert(this.strategy.length == actions.size());
        }
    }

    void setDetailStrategyInfo(int row,int colunm,NodeDesc desc){
        GameTreeNode node = desc.node;
        if(!(node instanceof ActionNode)) {
            return;
        }

        String name = grid_names[row][colunm];
        String[] columnName;

        if(name.length() == 3 && name.charAt(2) == 'o') columnName= new String[]{"","","",""};
        else columnName = new String[]{"",""};
        int col_len = columnName.length;

        ActionNode actionNode = (ActionNode) node;
        int player = actionNode.getPlayer();
        DiscountedCfrTrainable trainable = (DiscountedCfrTrainable) actionNode.getTrainable();
        float[] strategy = trainable.getAverageStrategy();
        float[] evs = trainable.getEvs();
        List<GameActions> actions = actionNode.getActions();
        PrivateCards[] cards = trainable.getPrivateCards();

        List<DetailStrategyInfo> infos = new ArrayList<DetailStrategyInfo>();
        float norm_ev = sum(trainable.getReach_probs()[player]);

        for(int i = 0;i < cards.length;i ++){
            PrivateCards one_private_card = cards[i];
            if(!name.equals(one_private_card.summary()))continue;
            float[] one_strategy = new float[actions.size()];
            String strategy_str = "";
            for(int j = 0;j < actions.size();j ++){
                int strategy_index = j * cards.length + i;
                one_strategy[j] = strategy[strategy_index];
                strategy_str += String.format("<p style=\"font-size:7px\">%s : %.1f %s </p>",actions.get(j).toString(),one_strategy[j] * 100,"%");
            }
            String card_infos = String.format("<html><h2 style=\"background-color:rgb(255, 255, 255);\"> %s </h2><br><h3>EV: %.2f</h3>%s</html>",one_private_card.toFormatString(),evs[i] / norm_ev,strategy_str);;
            infos.add(new DetailStrategyInfo(card_infos,actions,one_strategy));
        }
        int line_num = (int)Math.ceil(((float)infos.size() / columnName.length));
        String[][] detail_grid_names = new String[line_num][columnName.length];
        DetailStrategyInfo[][] strategy2d = new DetailStrategyInfo[line_num][columnName.length];
        for(int i = 0;i < infos.size();i ++) {
            detail_grid_names[i / columnName.length][i % columnName.length] = infos.get(i).cards_name;
            strategy2d[i / columnName.length][i % columnName.length] = infos.get(i);
        }

        DefaultTableModel defaultTableModel = new DefaultTableModel(detail_grid_names, columnName);
        TableCellRenderer tcr = new StrategyDetailColorTableCellRenderer(strategy2d);
        detail_table.setModel(defaultTableModel);
        detail_table.setDefaultRenderer(Object.class,tcr);
        detail_table.updateUI();
    }


    class StragetyDetailCellRenderer extends DefaultTableCellRenderer {

        int row,colunm;
        float[] node_strategy = null;
        List<GameActions> actions;
        boolean selected;
        public StragetyDetailCellRenderer(int row,int colunm,DetailStrategyInfo[][] strategy2d,boolean selected) {
            this.row = row;
            this.colunm = colunm;
            this.selected = selected;

            if(!(row < strategy2d.length || colunm < strategy2d[0].length) || strategy2d[row][colunm] == null)return;
            actions = strategy2d[row][colunm].actions;
            node_strategy = strategy2d[row][colunm].strategy;
            assert(actions.size() == node_strategy.length);
        }

        private void paintBlackSide(Graphics g){
            Graphics2D g2=(Graphics2D)g;
            final BasicStroke stroke=new BasicStroke(2.0f);
            g2.setStroke(stroke);
            g2.setColor(Color.BLACK);
            g2.drawRect(0,0,getWidth(),getHeight());
        }

        public void paintComponent(Graphics g){
            if(node_strategy == null || sum(node_strategy) == 0){
                super.paintComponent(g);
                if(this.selected)paintBlackSide(g);
                return;
            }
            Graphics2D g2=(Graphics2D)g;
            float check_fold_prob = 0;
            for(int i = 0;i < actions.size();i ++){
                GameActions one_action = actions.get(i);
                if(one_action.getAction() == GameTreeNode.PokerActions.FOLD){
                    check_fold_prob = node_strategy[i];
                }
            }

            int disable_height = (int)(check_fold_prob * getHeight());
            int remain_height = getHeight() - disable_height;
            g2.setColor(Color.GRAY);
            g2.fillRect(0,0,getWidth(),disable_height);

            int begin_w = 0;
            for(int i = 0;i < actions.size();i ++){
                GameActions one_action = actions.get(i);
                if(one_action.getAction() == GameTreeNode.PokerActions.CHECK
                        ||one_action.getAction() == GameTreeNode.PokerActions.CALL
                ){
                    int prob_width = Math.round(node_strategy[i] / (1 - check_fold_prob) * getWidth());
                    if(node_strategy[i] != 0) prob_width = Math.max(1,prob_width);
                    g2.setColor(Color.GREEN);
                    g2.fillRect(begin_w,disable_height,prob_width,remain_height);
                    begin_w += prob_width;
                }else if(one_action.getAction() == GameTreeNode.PokerActions.BET
                        ||one_action.getAction() == GameTreeNode.PokerActions.RAISE
                ){
                    int prob_width = Math.round(node_strategy[i] / (1 - check_fold_prob) * getWidth());
                    if(node_strategy[i] != 0) prob_width = Math.max(1,prob_width);
                    if(i == actions.size() - 1)  prob_width = Math.max(prob_width,getWidth() - begin_w);
                    int color_base = Math.max(128 - 32 * i - 1,0);
                    g2.setColor(new Color(255,color_base,color_base));
                    g2.fillRect(begin_w,disable_height,prob_width,remain_height);
                    begin_w += prob_width;
                }
            }
            super.paintComponent(g);
            paintBlackSide(g);
        }
    }

    class StrategyDetailColorTableCellRenderer extends DefaultTableCellRenderer
    {
        DetailStrategyInfo[][] strategy2d;
        public StrategyDetailColorTableCellRenderer(DetailStrategyInfo[][] strategy2d) {
            this.strategy2d = strategy2d;
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            StragetyDetailCellRenderer cell_renderer = new StragetyDetailCellRenderer(row,column,this.strategy2d,isSelected);
            return cell_renderer.getTableCellRendererComponent(table, value, false,false, row, column);
        }
    }


    class GlobalStrategyCellRenderer extends DefaultTableCellRenderer {

        int row,colunm;
        float node_strategy;
        float node_combos;
        GameActions action = null;
        Color color;
        boolean selected;
        public GlobalStrategyCellRenderer(int row,int colunm,boolean selected,GameActions action,float strategy,float combos) {
            this.row = row;
            this.colunm = colunm;
            this.selected = selected;
            this.action = action;
            this.node_strategy = strategy;
            this.node_combos = combos;


            if(this.action.getAction() == GameTreeNode.PokerActions.CHECK
                    ||this.action.getAction() == GameTreeNode.PokerActions.CALL
            ){
                //int prob_width = Math.round(node_strategy[i] / (1 - check_fold_prob) * getWidth());
                //if(node_strategy[i] != 0) prob_width = Math.max(1,prob_width);
                this.color = Color.GREEN;
            }else if(this.action.getAction() == GameTreeNode.PokerActions.BET
                    || this.action.getAction() == GameTreeNode.PokerActions.RAISE
            ){
                int color_base = Math.max(128 - 32 * colunm - 1,0);
                this.color = new Color(255,color_base,color_base);
            } else if(this.action.getAction() == GameTreeNode.PokerActions.FOLD){
                this.color = Color.GRAY;
            }

        }

        public void paintComponent(Graphics g){
            if(this.action == null){
                super.paintComponent(g);
                return;
            }
            setBackground(this.color);
            super.paintComponent(g);
        }
    }

    class GlobalStrategyColorTableCellRenderer extends DefaultTableCellRenderer
    {
        List<GameActions> actions;
        float[] global_strategy;
        float[] combos;
        public GlobalStrategyColorTableCellRenderer(List<GameActions> actions, float[] global_strategy, float[] combos) {
            this.actions = actions;
            this.global_strategy = global_strategy;
            this.combos = combos;
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            GlobalStrategyCellRenderer cell_renderer = new GlobalStrategyCellRenderer(row,column,isSelected,this.actions.get(column),this.global_strategy[column],this.combos[column]);
            return cell_renderer.getTableCellRendererComponent(table, value, false,false, row, column);
        }
    }
    class UrlColorTableCellRenderer extends DefaultTableCellRenderer
    {
        ArrayList<Color> colors;
        public UrlColorTableCellRenderer(ArrayList<Color> colors) {
            this.colors = colors;
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(this.colors.get(column));
            return super.getTableCellRendererComponent(table, value, false,false, row, column);
        }
    }
}
