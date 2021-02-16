from jpype import *
import yaml
import numpy as np
import sys
sys.path.append("resources")
from python.TreeBuilder import *
import unittest
startJVM(getDefaultJVMPath(), "-ea", "-Djava.class.path=%s" % "./RiverSolver.jar")
PokerSolver = JClass('icybee.solver.runtime.PokerSolver')

class TestSolver(unittest.TestCase):
    @classmethod
    def setUpClass(self):
        with open('resources/yamls/general_rule.yaml') as fhdl:
            self.conf = yaml.load(fhdl)
        self.ps_shortdeck = PokerSolver("Dic5Compairer",
                                        "./resources/compairer/card5_dic_sorted_shortdeck.txt",
                                        376993,
                                        ['A', 'K', 'Q', 'J', 'T', '9', '8', '7', '6'],
                                        ['h', 's', 'd', 'c']
                                        )
        self.ps_holdem = PokerSolver("Dic5Compairer",
                                     "./resources/compairer/card5_dic_sorted.txt",
                                     2598961,
                                     ['A', 'K', 'Q', 'J', 'T', '9', '8', '7', '6', '5', '4', '3'],
                                     ['h', 's', 'd', 'c']
                                     )

    @classmethod
    def tearDownClass(self):
        pass

    def setUp(self):
        pass

    def tearDown(self):
        pass


    def test_shortdeck_flop(self):
        rule = RulesBuilder(
            self.conf,
            current_commit = [2,2],
            current_round =  2,
            raise_limit = 1,
            check_limit = 2,
            small_blind = 0.5,
            big_blind = 1,
            stack = 10,
            bet_sizes = ["1_pot"],
        )


        gameTree = PartGameTreeBuilder(rule)


        depth = np.inf
        json = gameTree.gen_km_json("./.tree.km".format(depth),limit=depth,ret_json=True)

        self.ps_shortdeck.build_game_tree("./.tree.km")

        result_json = self.ps_shortdeck.train(
            "AA,KK,QQ,JJ,TT,99,88,77,66,AK,AQ,AJ,AT,A9,A8,A7,A6,KQ,KJ,KT,K9,K8,K7,K6,QJ,QT,Q9,Q8,Q7,Q6,JT,J9,J8,J7,J6,T9,T8,T7,T6,98,97,96,87,86,76",
            "AA,KK,QQ,JJ,TT,99,88,77,66,AK,AQ,AJ,AT,A9,A8,A7,A6,KQ,KJ,KT,K9,K8,K7,K6,QJ,QT,Q9,Q8,Q7,Q6,JT,J9,J8,J7,J6,T9,T8,T7,T6,98,97,96,87,86,76",
            "Kd,Jd,Td",
            50, # iterations
            10, # print_interval
            False, # debug
            True, # parallel
            None,
            "log.txt",
            "discounted_cfr",
            "none",
            -1, # threads
            1, # action fork prob
            1, # chance fork prob
            1, # fork every tree depth
            4, # fork minimal size
        )

    def test_shortdeck_turn(self):
        rule = RulesBuilder(
            self.conf,
            current_commit = [2,2],
            current_round =  3,
            raise_limit = 1,
            check_limit = 2,
            small_blind = 0.5,
            big_blind = 1,
            stack = 10,
            bet_sizes = ["0.5_pot","1_pot","2_pot","all-in"],
        )

        gameTree = PartGameTreeBuilder(rule)

        depth = np.inf
        json = gameTree.gen_km_json("./.tree.km".format(depth),limit=depth,ret_json=True)

        self.ps_shortdeck.build_game_tree("./.tree.km")

        result_json = self.ps_shortdeck.train(
            "AA,KK,QQ,JJ,TT,99,88,77,66,AK,AQ,AJ,AT,A9,A8,A7,A6,KQ,KJ,KT,K9,K8,K7,K6,QJ,QT,Q9,Q8,Q7,Q6,JT,J9,J8,J7,J6,T9,T8,T7,T6,98,97,96,87,86,76",
            "AA,KK,QQ,JJ,TT,99,88,77,66,AK,AQ,AJ,AT,A9,A8,A7,A6,KQ,KJ,KT,K9,K8,K7,K6,QJ,QT,Q9,Q8,Q7,Q6,JT,J9,J8,J7,J6,T9,T8,T7,T6,98,97,96,87,86,76",
            "Kd,Jd,Td,7s",
            50, # iterations
            10, # print_interval
            False, # debug
            True, # parallel
            None,
            "log.txt",
            "discounted_cfr",
            "none",
            -1, # threads
            1, # action fork prob
            1, # chance fork prob
            1, # fork every tree depth
            4, # fork minimal size
        )

    def test_shortdeck_river(self):
        rule = RulesBuilder(
            self.conf,
            current_commit = [2,2],
            current_round = 4,
            raise_limit = 1,
            check_limit = 2,
            small_blind = 0.5,
            big_blind = 1,
            stack = 10,
            bet_sizes = ["0.5_pot","1_pot","2_pot","all-in"],
        )

        gameTree = PartGameTreeBuilder(rule)

        depth = np.inf
        json = gameTree.gen_km_json("./.tree.km".format(depth),limit=depth,ret_json=True)

        self.ps_shortdeck.build_game_tree("./.tree.km")

        result_json = self.ps_shortdeck.train(
            "AA,KK,QQ,JJ,TT,99,88,77,66,AK,AQ,AJ,AT,A9,A8,A7,A6,KQ,KJ,KT,K9,K8,K7,K6,QJ,QT,Q9,Q8,Q7,Q6,JT,J9,J8,J7,J6,T9,T8,T7,T6,98,97,96,87,86,76",
            "AA,KK,QQ,JJ,TT,99,88,77,66,AK,AQ,AJ,AT,A9,A8,A7,A6,KQ,KJ,KT,K9,K8,K7,K6,QJ,QT,Q9,Q8,Q7,Q6,JT,J9,J8,J7,J6,T9,T8,T7,T6,98,97,96,87,86,76",
            "Kd,Jd,Td,7s,8s",
            50, # iterations
            10, # print_interval
            False, # debug
            True, # parallel
            None,
            "log.txt",
            "discounted_cfr",
            "none",
            -1, # threads
            1, # action fork prob
            1, # chance fork prob
            1, # fork every tree depth
            4, # fork minimal size
        )

    def test_holdem_turn(self):
        rule = RulesBuilder(
            self.conf,
            current_commit = [2,2],
            current_round =  3,
            raise_limit = 1,
            check_limit = 2,
            small_blind = 0.5,
            big_blind = 1,
            stack = 10,
            bet_sizes = ["0.5_pot","1_pot","2_pot","all-in"],
        )

        gameTree = PartGameTreeBuilder(rule)

        depth = np.inf
        json = gameTree.gen_km_json("./.tree.km".format(depth),limit=depth,ret_json=True)

        self.ps_holdem.build_game_tree("./.tree.km")

        result_json = self.ps_holdem.train(
            "AA,KK,QQ,JJ,TT,99,88,77,66,AK,AQ,AJ,AT,A9,A8,A7,A6,KQ,KJ,KT,K9,K8,K7,K6,QJ,QT,Q9,Q8,Q7,Q6,JT,J9,J8,J7,J6,T9,T8,T7,T6,98,97,96,87,86,76,83",
            "AA,KK,QQ,JJ,TT,99,88,77,66,AK,AQ,AJ,AT,A9,A8,A7,A6,KQ,KJ,KT,K9,K8,K7,K6,QJ,QT,Q9,Q8,Q7,Q6,JT,J9,J8,J7,J6,T9,T8,T7,T6,98,97,96,87,86,76,83",
            "Kd,Jd,Td,7s",
            50, # iterations
            10, # print_interval
            False, # debug
            True, # parallel
            None,
            "log.txt",
            "discounted_cfr",
            "none",
            -1, # threads
            1, # action fork prob
            1, # chance fork prob
            1, # fork every tree depth
            4, # fork minimal size
        )

    def test_holdem_river(self):
        rule = RulesBuilder(
            self.conf,
            current_commit = [2,2],
            current_round = 4,
            raise_limit = 1,
            check_limit = 2,
            small_blind = 0.5,
            big_blind = 1,
            stack = 10,
            bet_sizes = ["0.5_pot","1_pot","2_pot","all-in"],
        )

        gameTree = PartGameTreeBuilder(rule)

        depth = np.inf
        json = gameTree.gen_km_json("./.tree.km".format(depth),limit=depth,ret_json=True)

        self.ps_holdem.build_game_tree("./.tree.km")

        result_json = self.ps_holdem.train(
            "AA,KK,QQ,JJ,TT,99,88,77,66,AK,AQ,AJ,AT,A9,A8,A7,A6,KQ,KJ,KT,K9,K8,K7,K6,QJ,QT,Q9,Q8,Q7,Q6,JT,J9,J8,J7,J6,T9,T8,T7,T6,98,97,96,87,86,76,83",
            "AA,KK,QQ,JJ,TT,99,88,77,66,AK,AQ,AJ,AT,A9,A8,A7,A6,KQ,KJ,KT,K9,K8,K7,K6,QJ,QT,Q9,Q8,Q7,Q6,JT,J9,J8,J7,J6,T9,T8,T7,T6,98,97,96,87,86,76,83",
            "Kd,Jd,Td,7s,8s",
            50, # iterations
            10, # print_interval
            False, # debug
            True, # parallel
            None,
            "log.txt",
            "discounted_cfr",
            "none",
            -1, # threads
            1, # action fork prob
            1, # chance fork prob
            1, # fork every tree depth
            4, # fork minimal size
        )

    def test_holdem_river_range_with_float(self):
        rule = RulesBuilder(
            self.conf,
            current_commit = [2,2],
            current_round = 4,
            raise_limit = 1,
            check_limit = 2,
            small_blind = 0.5,
            big_blind = 1,
            stack = 10,
            bet_sizes = ["0.5_pot","1_pot","2_pot","all-in"],
        )

        gameTree = PartGameTreeBuilder(rule)

        depth = np.inf
        json = gameTree.gen_km_json("./.tree.km".format(depth),limit=depth,ret_json=True)

        self.ps_holdem.build_game_tree("./.tree.km")

        result_json = self.ps_holdem.train(
            "AA:0.7,KK:0.6,QQ:0.5,76:0.4,83:0.9",
            "AA:0.87,KK:0.9,QQ:0.2,76:0.5,83:0.4",
            "Kd,Jd,Td,7s,8s",
            50, # iterations
            10, # print_interval
            False, # debug
            True, # parallel
            None,
            "log.txt",
            "discounted_cfr",
            "none",
            -1, # threads
            1, # action fork prob
            1, # chance fork prob
            1, # fork every tree depth
            4, # fork minimal size
        )

if __name__ == '__main__':
    unittest.main(verbosity=1)
