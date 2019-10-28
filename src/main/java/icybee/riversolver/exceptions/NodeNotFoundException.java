package icybee.riversolver.exceptions;

/**
 * Created by huangxuefeng on 2019/10/7.
 * This file contains code for runtime Node not found exception, where a program trys to creates a node that doesn't exist
 */
public class NodeNotFoundException extends RuntimeException{
    public NodeNotFoundException(String errmsg){
        super(errmsg);
    }
}
