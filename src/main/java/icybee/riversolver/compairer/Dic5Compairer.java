package icybee.riversolver.compairer;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import icybee.riversolver.Card;
import icybee.riversolver.exceptions.BoardNotFoundException;
import icybee.riversolver.exceptions.CardsNotFoundException;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.lang3.ArrayUtils;
import org.paukov.combinatorics3.Generator;
import org.paukov.combinatorics3.IGenerator;

/**
 * Created by huangxuefeng on 2019/10/6.
 * This file contains code for a card compairer
 */
public class Dic5Compairer extends Compairer {
    Map<Set<String>,Integer> cards2rank = (Map<Set<String>,Integer>)new HashMap<Set<String>,Integer>();
    Map<Long,Integer> cardslong2rank = (Map<Long,Integer>)new HashMap<Long,Integer>();
    public Dic5Compairer(String dic_dir,int lines) throws IOException{
        super(dic_dir,lines);

        cards2rank = (Map<Set<String>,Integer>)new Hashtable<Set<String>,Integer>(lines * 50);
        cardslong2rank = (Map<Long,Integer>)new Hashtable<Long,Integer>(lines * 50);

        BufferedReader bufferedReader = new BufferedReader(new FileReader(dic_dir));
        String str;
        ProgressBar pb = new ProgressBar("Dic5Comapirer Load",lines);
        pb.start();
        int ind = 0;
        while ((str = bufferedReader.readLine()) != null) {
            String[] linesp = str.trim().split(",");
            String cards_str = linesp[0];
            String[] cards = cards_str.split("-");
            assert(cards.length == 5);

            Set<String> cards_set = new HashSet<>(Arrays.asList(cards));

            int rank = Integer.valueOf(linesp[1]);
            cards2rank.put(cards_set,rank);
            if(cardslong2rank.containsKey(Card.boardCards2long(cards))){
                String err_info = "";
                for(String one_card:cards) err_info += (" " + one_card);
                throw new RuntimeException(
                        String.format(
                                "cards long already exist: %s ,existed long: %d",
                                err_info,cardslong2rank.get(Card.boardCards2long(cards))
                        )
                );
            }
            cardslong2rank.put(Card.boardCards2long(cards),rank);
            ind += 1;
            if(ind % 100 == 0) {
                pb.stepBy(100);
            }
        }
        pb.stop();
    }

    @SuppressWarnings("all")
    public static <T> List<T> merge(List<T>... args) {
        final List<T> result = new ArrayList<>();

        for (List<T> list : args) {
            result.addAll(list);
        }

        return result;
    }

    int getRank(List<Card> cards) throws CardsNotFoundException{
        // inf here
        IGenerator<List<Card>> cards_gen = Generator.combination(cards).simple(5);
        List<Integer> rank_list = cards_gen.stream().map(comb_cards -> {
            List<String> cards_str = comb_cards.stream().map(
                    Card::getCard
            ).collect(Collectors.toList());
            Set<String> cards_set = new HashSet<>(cards_str);
            Integer rank = cards2rank.get(cards_set);

            if (rank == null){
                throw new CardsNotFoundException(cards_set.toString());
            }
            return rank;
        }).collect(Collectors.toList());
        return Collections.min(rank_list);
    }

    int getRank(int[] cards) throws CardsNotFoundException{
        // inf here
        List<Integer> cards_list = IntStream.of(cards)
                .boxed().collect(Collectors.toCollection(ArrayList::new));
        IGenerator<List<Integer>> cards_gen = Generator.combination(cards_list).simple(5);

        List<Integer> rank_list = cards_gen.stream().map(comb_cards -> {

            long board_cards;
            try {
                board_cards = Card.boardInts2long(comb_cards);
            }catch(Exception e){
                throw new CardsNotFoundException("rank is null");
            }
            Integer rank = cardslong2rank.get(board_cards);

            if (rank == null){
                throw new CardsNotFoundException("rank is null");
            }
            return rank;
        }).collect(Collectors.toList());
        return Collections.min(rank_list);
    }

    CompairResult compairRanks(int rank_former,int rank_latter) {
        if (rank_former < rank_latter) {
            // rank更小的牌更大，0是同花顺
            return CompairResult.LARGER;
        } else if (rank_former > rank_latter) {
            return CompairResult.SMALLER;
        } else {
            // rank_former == rank_latter
            return CompairResult.EQUAL;
        }
    }

    @Override
    @SuppressWarnings("all")
    public CompairResult compair(List<Card> private_former,List<Card> private_latter,List<Card> public_board) throws CardsNotFoundException {
        assert(private_former.size() == 2);
        assert(private_latter.size() == 2);
        assert(public_board.size() == 5);
        List<Card> former_cards =  merge(private_former,public_board);
        List<Card> latter_cards =  merge(private_latter,public_board);

        int rank_former = this.getRank(former_cards);
        int rank_latter = this.getRank(latter_cards);

        return compairRanks(rank_former,rank_latter);

    }
    @Override
    public CompairResult compair(int[] private_former,int[] private_latter,int[] public_board) throws CardsNotFoundException, BoardNotFoundException{
        assert(private_former.length == 2);
        assert(private_latter.length == 2);
        assert(public_board.length == 5);
        int[] former_cards =  ArrayUtils.addAll(private_former,public_board);
        int[] latter_cards =  ArrayUtils.addAll(private_latter,public_board);

        int rank_former = this.getRank(former_cards);
        int rank_latter = this.getRank(latter_cards);

        return compairRanks(rank_former,rank_latter);
    }

    @Override
    @SuppressWarnings("all")
    public int get_rank(List<Card> private_hand, List<Card> public_board){
        return this.getRank(merge(private_hand,public_board));
    }

    @Override
    public int get_rank(int[] private_hand, int[] public_board){
        return getRank(ArrayUtils.addAll(private_hand,public_board));
    }

    @Override
    public int get_rank(long private_hand, long public_board) {
        return this.get_rank(Card.long2board(private_hand),Card.long2board(public_board));
    }
}

