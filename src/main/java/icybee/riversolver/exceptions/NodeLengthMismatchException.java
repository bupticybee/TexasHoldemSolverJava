package icybee.riversolver.exceptions;

/**
 * Created by huangxuefeng on 2019/10/7.
 * This file contains code for a runtime exception when a game tree json's node has different child action and child node length
 */
public class NodeLengthMismatchException extends RuntimeException{
    public NodeLengthMismatchException(String errmsg){
        super(errmsg);
    }
}
