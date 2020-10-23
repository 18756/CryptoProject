import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SubsetDifference {
    public static Set<OptimizedMT.DifferenceTrees> cover(List<byte[]> tree, Set<Integer> R) {
        // building steiner tree
        boolean[] steinerTree = new boolean[tree.size()];
        steinerTree[0] = true;
        for (int id : R) {
            while (!steinerTree[id]) {
                steinerTree[id] = true;
                id = (id - 1) / 2;
            }
        }
        Set<OptimizedMT.DifferenceTrees> CV = new HashSet<>();

        if (R.isEmpty()) {
            CV.add(new OptimizedMT.DifferenceTrees(0, -1));
            return CV;
        }

        while (R.size() >= 2) {
            // finding 2 leaves
            TwoLeavesWithLca twoLeavesWithLca = findTwoLeaves(steinerTree, R);
            int leftId = twoLeavesWithLca.leftId;
            int rightId = twoLeavesWithLca.rightId;
            int lca = twoLeavesWithLca.lca;
            if (2 * lca + 1 != leftId) {
                CV.add(new OptimizedMT.DifferenceTrees(2 * lca + 1, leftId));
            }
            if (2 * lca + 2 != rightId) {
                CV.add(new OptimizedMT.DifferenceTrees(2 * lca + 2, rightId));
            }
            // removing descendants of lca
            steinerTree[2 * lca + 1] = false;
            steinerTree[2 * lca + 2] = false;
            R.remove(leftId);
            R.remove(rightId);
            R.add(lca);
            // now lca is a new leaf
        }
        // left only 1 leaf
        int lastLeaf = R.iterator().next();

        if (lastLeaf == 0) {
            CV.add(new OptimizedMT.DifferenceTrees(-1, 0));
        } else {
            CV.add(new OptimizedMT.DifferenceTrees(0, lastLeaf));
        }
        return CV;
    }

    private static TwoLeavesWithLca findTwoLeaves(boolean[] steinerTree, Set<Integer> R) {
        // getting any leaf id
        int id = R.iterator().next();
        // going to 'Node' with 2 children
        id = (id - 1) / 2;
        while (!(steinerTree[2 * id + 1] && steinerTree[2 * id + 2])) {
            id = (id - 1) / 2;
        }

        int lca = id;
        int leftId  = 2 * id + 1;
        int rightId = 2 * id + 2;
        while (!isLeaf(leftId, R) || !isLeaf(rightId, R)) {
            boolean isLeftLeaf = isLeaf(leftId, R);
            boolean isRightLeaf = isLeaf(rightId, R);
            if (!isLeftLeaf && steinerTree[2 * leftId + 1] && steinerTree[2 * leftId + 2]) {
                lca = leftId;
                rightId = 2 * leftId + 2;
                leftId = 2 * leftId + 1;
            } else if (!isRightLeaf && steinerTree[2 * rightId + 1] && steinerTree[2 * rightId + 2]) {
                lca = rightId;
                leftId = 2 * rightId + 1;
                rightId = 2 * rightId + 2;
            } else {
                if (!isLeftLeaf) {
                    leftId = steinerTree[2 * leftId + 1] ? 2 * leftId + 1 : 2 * leftId + 2;
                }
                if (!isRightLeaf) {
                    rightId = steinerTree[2 * rightId + 1] ? 2 * rightId + 1 : 2 * rightId + 2;
                }
            }
        }
        return new TwoLeavesWithLca(leftId, rightId, lca);
    }

    private static boolean isLeaf(int id, Set<Integer> R) {
        return R.contains(id);
    }


    static class TwoLeavesWithLca {
        public TwoLeavesWithLca(int leftId, int rightId, int lca) {
            this.leftId = leftId;
            this.rightId = rightId;
            this.lca = lca;
        }

        int leftId;
        int rightId;
        int lca;
    }
}