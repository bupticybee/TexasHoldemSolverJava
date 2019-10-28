package icybee.riversolver;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;


/**
 * Created by huangxuefeng on 2019/10/6.
 * Config parser
 */
public class Config {
    List<String> ranks;
    List<String> suits;

    // Compairer configures
    String compairer_type;
    String compairer_dic_dir;
    int compairer_lines;

    // Tree builder configures
    Boolean tree_builder = false;
    String tree_builder_json = null;
    String solver_type;

    Config(String input_file) throws FileNotFoundException,ClassNotFoundException{
        Yaml yaml_reader = new Yaml();
        File config_file = new File(input_file);
        FileInputStream fileInputStream = new FileInputStream(config_file);
        Map map = yaml_reader.loadAs(fileInputStream, Map.class);
        for (Object name:map.keySet()){
            String key = name.toString();
            Object value = map.get(key);
            switch(key){
                case "deck":{
                    Map deckdic = (Map)value;
                    ranks = (List<String>)((Map)deckdic.get("kwargs")).get("rank");
                    suits = (List<String>)((Map)deckdic.get("kwargs")).get("suit");
                    break;
                }
                case "compairer": {
                    Map kwargs = (Map)((Map)value).get("kwargs");
                    String dic_dir = (String)kwargs.get("dicfile");
                    String type = (String)((Map)value).get("type");
                    int lines = (Integer)((kwargs).get("lines"));
                    this.compairer_dic_dir = dic_dir;
                    this.compairer_type = type;
                    this.compairer_lines = lines;

                    break;
                }
                case "tree_builder":{
                    this.tree_builder = true;
                    Map kwargs = (Map)((Map)value).get("kwargs");
                    String json_file = (String)kwargs.get("json_file");
                    this.tree_builder_json = json_file;
                    break;
                }
                //TODO 写cfr的配置读取
                case "solver": {
                    String type = (String)((Map)value).get("type");
                    solver_type = type;
                }
            }
        }
    }
}

