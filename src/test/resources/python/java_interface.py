from jpype import *

startJVM(getDefaultJVMPath(), "-ea", "-Djava.class.path=%s" % "./RiverSolver.jar")

PokerSolver = JClass('icybee.riversolver.runtime.PokerSolver')

ps = PokerSolver("Dic5Compairer",
                 "./resources/compairer/card5_dic_sorted_shortdeck.txt",
                 376993,
                 ['A', 'K', 'Q', 'J', 'T', '9', '8', '7', '6'],
                 ['h', 's', 'd', 'c']
                )

ps.build_game_tree("./resources/gametree/part_tree_turn_withallin.km")

ps.train(
    "AA,KK,QQ,JJ,TT,99,88,77,66,AK,AQ,AJ,AT,A9,A8,A7,A6,KQ,KJ,KT,K9,K8,K7,K6,QJ,QT,Q9,Q8,Q7,Q6,JT,J9,J8,J7,J6,T9,T8,T7,T6,98,97,96,87,86,76",
    "AA,KK,QQ,JJ,TT,99,88,77,66,AK,AQ,AJ,AT,A9,A8,A7,A6,KQ,KJ,KT,K9,K8,K7,K6,QJ,QT,Q9,Q8,Q7,Q6,JT,J9,J8,J7,J6,T9,T8,T7,T6,98,97,96,87,86,76",
    #"Kd,Jd,Td,7s,8s",
    "Kd,Jd,Td,7s",
    100,
    10,
    False, # debug
    True, # parallel
    "outputs_strategy.json",
    "log.txt",
    "discounted_cfr",
    "none",
    -1,
    1)