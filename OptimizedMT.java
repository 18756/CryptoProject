import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class OptimizedMT {
    private final MessageDigest hashFun;
    private List<byte[]> merkleTree;
    private Map<String, Integer> idFromHash;
    private int sizeOfM;

    public OptimizedMT() throws NoSuchAlgorithmException {
        hashFun = MessageDigest.getInstance("SHA-256");
    }

    public Boolean verify(String x, List<Sibling> wit, Accumulator accX) {
        byte[] v = superHash(x);
        boolean hadM = false;
        boolean hadNM = false;

        for (Sibling sibl : wit) {
            hadM |= accX.mem.contains(stringOfHash(v));
            hadNM |= accX.notMem.contains(stringOfHash(v));
            v = superHash(concat(sibl.isLeft ? sibl.hash : v, sibl.isLeft ? v : sibl.hash));
        }
        if (accX.mem.contains(stringOfHash(v))) {
            if (hadNM) {
                return null;
            } else {
                return true;
            }
        }
        if (accX.notMem.contains(stringOfHash(v))) {
            if (hadM) {
                return null;
            } else {
                return false;
            }
        }
        return null;
    }

    public void setUp(Set<String> M) {
        sizeOfM = M.size();
        merkleTree = new ArrayList<>(2 * M.size() - 1);
        for (int i = 0; i < 2 * M.size() - 1; i++) {
            merkleTree.add(null);
        }
        idFromHash = new HashMap<>();
        int id = M.size() - 1;
        for (String m : M) {
            byte[] hash = superHash(m);
            merkleTree.set(id, hash);
            idFromHash.put(stringOfHash(hash), id);
            id++;
        }
        for (int i = M.size() - 2; i >= 0; i--) {
            byte[] leftH = merkleTree.get(2 * i + 1);
            byte[] rightH = merkleTree.get(2 * i + 2);
            merkleTree.set(i, superHash(concat(leftH, rightH)));
        }
    }

    public Accumulator accumulate(Set<String> X) {
        Set<Integer> xIds = new HashSet<>();
        for (String x : X) {
            byte[] h = superHash(x);
            int id = idFromHash.get(stringOfHash(h));
            xIds.add(id);
        }
        Set<Integer> R = new HashSet<>();
        for (int i = sizeOfM - 1; i < 2 * sizeOfM - 1; i++) {
            if (!xIds.contains(i)) {
                R.add(i);
            }
        }
        Set<DifferenceTrees> coverRes = SubsetDifference.cover(merkleTree, R);
        Set<String> mem = new HashSet<>();
        Set<String> notMem = new HashSet<>();
        for (DifferenceTrees differenceTrees : coverRes) {
            if (differenceTrees.i != -1) {
                mem.add(stringOfHash(merkleTree.get(differenceTrees.i)));
            }
            if (differenceTrees.j != -1) {
                notMem.add(stringOfHash(merkleTree.get(differenceTrees.j)));
            }
        }
        return new Accumulator(mem, notMem);
    }

    public List<Sibling> witGen(String x, Accumulator acc) {
        List<Sibling> siblings = new ArrayList<>();
        byte[] hash = superHash(x);
        int id = idFromHash.get(stringOfHash(hash));
        while (!acc.mem.contains(stringOfHash(merkleTree.get(id))) && !acc.notMem.contains(stringOfHash(merkleTree.get(id)))) {
            siblings.add(getSibling(id));
            id = (id - 1) / 2;
        }
        return siblings;
    }

    private Sibling getSibling(int id) {
        int parentId = (id - 1) / 2;
        if (id % 2 == 0) {
            return new Sibling(merkleTree.get(2 * parentId + 1), true);
        }
        return new Sibling(merkleTree.get(2 * parentId + 2), false);
    }


    private byte[] superHash(byte[] x) {
        return hashFun.digest(x);
    }

    private byte[] superHash(String x) {
        return superHash(x.getBytes());
    }

    private byte[] concat(byte[] left, byte[] right) {
        byte[] result = new byte[left.length + right.length];
        System.arraycopy(left, 0, result, 0, left.length);
        System.arraycopy(right, 0, result, left.length, right.length);
        return result;
    }

    private String stringOfHash(byte[] hash) {
        return Arrays.toString(hash);
    }

    public static class Accumulator {
        public Accumulator(Set<String> mem, Set<String> notMem) {
            this.mem = mem;
            this.notMem = notMem;
        }

        Set<String> mem;
        Set<String> notMem;
    }

    public static class DifferenceTrees {
        public DifferenceTrees(int i, int j) {
            this.i = i;
            this.j = j;
        }

        int i;
        int j;
    }


    public static class Sibling {
        public Sibling(byte[] hash, boolean isLeft) {
            this.hash = hash;
            this.isLeft = isLeft;
        }

        byte[] hash;
        boolean isLeft;
    }
}
