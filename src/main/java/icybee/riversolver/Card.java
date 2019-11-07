package icybee.riversolver;

import icybee.riversolver.exceptions.BoardNotFoundException;
import icybee.riversolver.exceptions.CardsNotFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by huangxuefeng on 2019/10/6.
 * created to hold or convert a card to int
 */
public class Card {
    public String getCard() {
        return card;
    }

    String card;
    Card(String card){
        this.card = card;
    }

    public int getCardInt(){
        return Card.strCard2int(this.card);
    }

    public static int card2int(Card card){
        return strCard2int(card.getCard());
    }

    public static int strCard2int(String card){
        char rank = card.charAt(0);
        char suit = card.charAt(1);
        if(card.length() != 2){
            throw new CardsNotFoundException(String.format("card %s not found",card));
        }
        return (rankToInt(rank) - 2) * 4 + suitToInt(suit);
    }

    public static String intCard2Str(int card)
    {
        int rank = card / 4 + 2;
        int suit = card - (rank-2)*4;
        return rankToString(rank) + suitToString(suit);
    }

    public static long boardCards2long(String[] cards) throws BoardNotFoundException {
        Card[] cards_objs = new Card[cards.length];
        for(int i = 0;i < cards.length;i++){
            cards_objs[i] = new Card(cards[i]);
        }
        return boardCards2long(cards_objs);
    }

    public static long boardCards2long(Card[] cards) throws BoardNotFoundException{
        int[] board_int = new int[cards.length];
        for(int i = 0;i < cards.length;i++){
            board_int[i] = Card.card2int(cards[i]);
        }
        return boardInts2long(board_int);
    }

    public static boolean boardsHasIntercept(long board1,long board2){
        return ((board1 & board2) != 0);
    }

    public static long boardInts2long(List<Integer> board) throws BoardNotFoundException {
        int[] array = board.stream().mapToInt(i->i).toArray();
        return boardInts2long(array);
    }

    public static long boardInts2long(int[] board){
        if(board.length < 1 || board.length > 7){
            throw new RuntimeException(board.toString());
        }
        long board_long = 0;
        for(int one_card: board){
            // 这里hard code了一副扑克牌是52张
            if(one_card < 0 || one_card >= 52){
                throw new RuntimeException(String.format("Card with id %d not found",one_card));
            }
            // long d
            // long 的range 在- 2 ^ 63 - 1 ~ + 2^ 63之间,所以不用太担心溢出问题
            board_long += (Long.valueOf(1) << one_card);
        }
        return board_long;
    }

    public static int[] long2board(long board_long) throws BoardNotFoundException{
        List<Integer> board = new ArrayList<>();
        for(int i = 0;i < 52;i ++){
            if((board_long & 1) == 1){
                board.add(i);
            }
            board_long = board_long >> 1;
        }
        if (board.size() < 1 || board.size() > 7){
            throw new BoardNotFoundException(String.format("board length not correct, board length %d, boards %s",board.size(),board.toString()));
        }
        int[] retval = new int[board.size()];
        for(int i = 0;i < board.size();i ++){
            retval[i] = board.get(i);
        }
        return retval;
    }

    public static Card[] long2boardCards(long board_long) throws BoardNotFoundException{
        int[] board = long2board(board_long);
        List<Card> board_cards = new ArrayList<>();
        for(int one_board:board){
            board_cards.add(new Card(intCard2Str(one_board)));
        }
        if (board_cards.size() < 1 || board_cards.size() > 7){
            throw new BoardNotFoundException(String.format("board length not correct, board length %d, boards %s",board_cards.size(),board.toString()));
        }
        Card retval[] = new Card[board_cards.size()];
        for(int i = 0;i < board_cards.size();i ++){
            retval[i] = board_cards.get(i);
        }
        return retval;
    }

    static String suitToString(int suit)
    {
        switch(suit)
        {
            case 0: return "c";
            case 1: return "d";
            case 2: return "h";
            case 3: return "s";
            default: return "c";
        }
    }

    static String rankToString(int rank)
    {
        switch(rank)
        {
            case 2: return "2";
            case 3: return "3";
            case 4: return "4";
            case 5: return "5";
            case 6: return "6";
            case 7: return "7";
            case 8: return "8";
            case 9: return "9";
            case 10: return "T";
            case 11: return "J";
            case 12: return "Q";
            case 13: return "K";
            case 14: return "A";
            default: return "2";
        }
    }

    static int rankToInt(char rank)
    {
        switch(rank)
        {
            case '2': return 2;
            case '3': return 3;
            case '4': return 4;
            case '5': return 5;
            case '6': return 6;
            case '7': return 7;
            case '8': return 8;
            case '9': return 9;
            case 'T': return 10;
            case 'J': return 11;
            case 'Q': return 12;
            case 'K': return 13;
            case 'A': return 14;
            default: return 2;
        }
    }

    static String[] getRanks(){
        return new String[]{"2","3","4","5","6","7","8","9","T","J","Q","K","A"};
    }

    static int suitToInt(char suit)
    {
        switch(suit)
        {
            case 'c': return 0;
            case 'd': return 1;
            case 'h': return 2;
            case 's': return 3;
            default: return 0;
        }
    }

    public static String[] getSuits(){
        return new String[]{"c","d","h","s"};
    }

}
