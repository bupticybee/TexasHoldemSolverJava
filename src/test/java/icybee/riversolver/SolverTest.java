package icybee.riversolver;

import static org.junit.Assert.assertTrue;


import icybee.riversolver.compairer.Compairer;
import icybee.riversolver.exceptions.BoardNotFoundException;
import icybee.riversolver.ranges.PrivateCards;
import icybee.riversolver.solver.CfrPlusRiverSolver;
import icybee.riversolver.solver.Solver;
import icybee.riversolver.trainable.CfrPlusTrainable;
import icybee.riversolver.trainable.CfrTrainable;
import icybee.riversolver.trainable.DiscountedCfrTrainable;
import icybee.riversolver.utils.PrivateRangeConverter;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Unit test
 */
public class SolverTest
{
    /**
     * Rigorous Test :-)
     */
    static SolverEnvironment se = null;
    @Before
    public void loadEnvironmentsTest()
    {
        String config_name = "yamls/rule_shortdeck_turnriversolver.yaml";
        //String config_name = "yamls/rule_shortdeck_simple.yaml";
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(config_name).getFile());

        Config config = null;
        try {
            config = new Config(file.getAbsolutePath());
        }catch(Exception e){
            e.printStackTrace();
            assertTrue(false);
        }


        if(SolverTest.se == null) {
            try {
                SolverEnvironment se = new SolverEnvironment(config);
                SolverTest.se = se;
            } catch (Exception e) {
                e.printStackTrace();
                assertTrue(false);
            }
        }

    }

    @Test
    public void cardCompairLGTest(){
        try {
            List<Card> board = Arrays.asList(
                    new Card("6c"),
                    new Card("6d"),
                    new Card("7c"),
                    new Card("7d"),
                    new Card("8s")
            );
            List<Card> private1 = Arrays.asList(
                    new Card("6h"),
                    new Card("6s")
            );
            List<Card> private2 = Arrays.asList(
                    new Card("9c"),
                    new Card("9s")
            );

            Compairer.CompairResult cr = SolverTest.se.compairer.compair(private1,private2,board);
            System.out.println(cr);
            assertTrue(cr == Compairer.CompairResult.LARGER);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void cardCompairEQTest(){
        try{
            List<Card> board = Arrays.asList(
                    new Card("6c"),
                    new Card("6d"),
                    new Card("7c"),
                    new Card("7d"),
                    new Card("8s")
            );
            List<Card> private1 = Arrays.asList(
                    new Card("8h"),
                    new Card("7s")
            );
            List<Card> private2 = Arrays.asList(
                    new Card("8d"),
                    new Card("7h")
            );

            Compairer.CompairResult cr = SolverTest.se.compairer.compair(private1,private2,board);
            System.out.println(cr);
            assertTrue(cr == Compairer.CompairResult.EQUAL);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void cardCompairSMTest(){
        try{
            List<Card> board = Arrays.asList(
                    new Card("6c"),
                    new Card("6d"),
                    new Card("7c"),
                    new Card("7d"),
                    new Card("8s")
            );
            List<Card> private1 = Arrays.asList(
                    new Card("6h"),
                    new Card("7s")
            );
            List<Card> private2 = Arrays.asList(
                    new Card("8h"),
                    new Card("7h")
            );

            Compairer.CompairResult cr = SolverTest.se.compairer.compair(private1,private2,board);
            System.out.println(cr);
            assertTrue(cr == Compairer.CompairResult.SMALLER);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void getRankTest(){
        List<Card> board = Arrays.asList(
                new Card("8d"),
                new Card("9d"),
                new Card("9s"),
                new Card("Jd"),
                new Card("Jh")
        );
        List<Card> private_cards = Arrays.asList(
                new Card("6h"),
                new Card("7s")
        );

        int rank = SolverTest.se.compairer.get_rank(private_cards,board);
        System.out.println(rank);
        assertTrue(rank == 687);
    }

    @Test
    public void printTreeTest(){
        System.out.println("The game tree :");
        try {
            se.game_tree.printTree(-1);
        }catch(Exception e){
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void printTreeLimitDepthTest(){
        System.out.println("The depth limit game tree :");
        try {
            se.game_tree.printTree(2);
        }catch(Exception e){
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void cardConvertTest(){
        System.out.println("cardConvertTest");
        try {
            Card card = new Card("6c");
            int card_int = Card.card2int(card);

            Card card_rev = new Card(Card.intCard2Str(card_int));
            int card_int_rev = Card.card2int(card_rev);
            System.out.println(card_int);
            System.out.println(card_int_rev);
            assert(card_int == card_int_rev);
        }catch (Exception e){
            e.printStackTrace();
            assertTrue(false);
        }
        System.out.println("end of cardConvertTest");
    }

    @Test
    public void cardsIntegerConvertTest(){
        Card[] board = {
                new Card("6c"),
                new Card("6d"),
                new Card("7c"),
                new Card("7d"),
                new Card("8s"),
                new Card("6h"),
                new Card("7s")
        };
        try {
            long board_int = Card.boardCards2long(board);
            Card[] board_cards = Card.long2boardCards(board_int);
            long board_int_rev = Card.boardCards2long(board_cards);

            for(Card i:board)
                System.out.println(i.getCard());
            System.out.println();
            for(Card i:board_cards)
                System.out.println(i.getCard());

            System.out.println(board_int);
            System.out.println(board_int_rev);
            assert(board_int == board_int_rev);
        }catch (Exception e){
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void cardsIntegerConvertNETest(){
        Card[] board1 = {
                new Card("6c"),
                new Card("6d"),
                new Card("7c"),
                new Card("7d"),
                new Card("8s"),
                new Card("6h"),
                new Card("7s")
        };
        Card[] board2 = {
                new Card("6c"),
                new Card("6d"),
                new Card("7c"),
                new Card("7d"),
                new Card("9s"),
                new Card("6h"),
                new Card("7s")
        };
        try {
            long board_int1 = Card.boardCards2long(board1);
            long board_int2 = Card.boardCards2long(board2);
            assertTrue(board_int1 != board_int2);

        }catch (Exception e){
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void compaierEquivlentTest(){
        System.out.println("compaierEquivlentTest");
        List<Card> board1_public = Arrays.asList(
                new Card("6c"),
                new Card("6d"),
                new Card("7c"),
                new Card("7d"),
                new Card("8s")
        );
        List<Card> board1_private = Arrays.asList(
                new Card("6h"),
                new Card("7s")
        );
        int[] board2_public = {
                (new Card("6c").getCardInt()),
                (new Card("6d").getCardInt()),
                (new Card("7c").getCardInt()),
                (new Card("7d").getCardInt()),
                (new Card("8s").getCardInt()),
        };
        int[] board2_private = {
                (new Card("6h").getCardInt()),
                (new Card("7s").getCardInt())
        };
        try {
            long board_int1 = se.compairer.get_rank(board1_private,board1_public);
            long board_int2 = se.compairer.get_rank(board2_private,board2_public);
            System.out.println(board_int1);
            System.out.println(board_int2);
            assertTrue(board_int1 == board_int2);

        }catch (Exception e){
            e.printStackTrace();
            assertTrue(false);
        }
        System.out.println("end compaierEquivlentTest");
    }

    @Test
    public void cfrSolverTest() throws BoardNotFoundException,Exception{
        System.out.println("solverTest");

        String player1RangeStr = "AA,KK,QQ,JJ,TT,99,88,77,66,AK,AQ,AJ,AT,A9,A8,A7,A6,KQ,KJ,KT,K9,K8,K7,K6,QJ,QT,Q9,Q8,Q7,Q6,JT,J9,J8,J7,J6,T9,T8,T7,T6,98,97,96,87,86,76";
        String player2RangeStr = "AA,KK,QQ,JJ,TT,99,88,77,66,AK,AQ,AJ,AT,A9,A8,A7,A6,KQ,KJ,KT,K9,K8,K7,K6,QJ,QT,Q9,Q8,Q7,Q6,JT,J9,J8,J7,J6,T9,T8,T7,T6,98,97,96,87,86,76";

        // String player1RangeStr = "87";
        // String player2RangeStr = "87";
        /*
        case 'c': return 0; // 梅花
        case 'd': return 1; // 方块
        case 'h': return 2; // 红桃
        case 's': return 3; // 黑桃
         */

        int[] initialBoard = new int[]{
                Card.strCard2int("Kd"),
                Card.strCard2int("Jd"),
                Card.strCard2int("Td"),
                Card.strCard2int("7s"),
                Card.strCard2int("8s")
        };

        PrivateCards[] player1Range = PrivateRangeConverter.rangeStr2Cards(player1RangeStr,initialBoard);
        PrivateCards[] player2Range = PrivateRangeConverter.rangeStr2Cards(player2RangeStr,initialBoard);
        // TODO 为什么QcTc和Qc8c的策略如此不相似
        //PrivateCards[] player1Range = new PrivateCards[]{new PrivateCards(Card.strCard2int("As"),Card.strCard2int("Ad"),1)};

        /*
        PrivateCards[] player1Range = new PrivateCards[]{
                new PrivateCards(Card.strCard2int("As"),Card.strCard2int("Ad"),1)
                ,new PrivateCards(Card.strCard2int("8h"),Card.strCard2int("7d"),1)
        };
        PrivateCards[] player2Range = new PrivateCards[]{
                //new PrivateCards(Card.strCard2int("6d"),Card.strCard2int("8s"),1)
                new PrivateCards(Card.strCard2int("6d"),Card.strCard2int("7d"),1)
                ,new PrivateCards(Card.strCard2int("6s"),Card.strCard2int("6d"),1)
        };
         */

        String logfile_name = "src/test/resources/outputs/outputs_log.txt";
        Solver solver = new CfrPlusRiverSolver(se.game_tree
                , player1Range
                , player2Range
                , initialBoard
                , se.compairer
                , se.deck
                ,1000
                ,false
                , 1
                ,logfile_name
                , DiscountedCfrTrainable.class
        );
        Map train_config = new HashMap();
        solver.train(train_config);

        String strategy_json = solver.getTree().dumps(false).toJSONString();

        String strategy_fname = "src/test/resources/outputs/outputs_strategy.json";

        File output_file = new File(strategy_fname);
        FileWriter writer = new FileWriter(output_file);
        writer.write(strategy_json);
        writer.flush();
        writer.close();

        System.out.println("end solverTest");
    }
}
