import java.util.ArrayList;

public class Directory implements Comparable<Directory> {
    private int dirAttribute, size, nextFreeOffset, nextFreeCluster, offsetInParent;
    private String name, low, high;
    private ArrayList<Directory> children = new ArrayList<>();
    private Directory parent;
    private boolean file;


    public Directory(String name, int dirAttribute, int size, String low, String high, Directory parent){
        this.name = name;
        this.dirAttribute = dirAttribute;
        this.size = size;
        this.low = low;
        this.high = high;
        this.file = !(dirAttribute == 8 || dirAttribute == 16);
        this.parent = parent;
    }

    public Directory(String name, int dirAttribute, int size, String low, String high, Directory parent, int offsetInParent){
        this.name = name;
        this.dirAttribute = dirAttribute;
        this.size = size;
        this.low = low;
        this.high = high;
        this.file = !(dirAttribute == 8 || dirAttribute == 16);
        this.parent = parent;
        this.offsetInParent = offsetInParent;
    }

    public int getDirAttribute() {
        return dirAttribute;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public String getHigh() {
        return high;
    }

    public String getLow() {
        return low;
    }

    public ArrayList<Directory> getChildren() {
        return children;
    }

    public Directory getParent() {
        return parent;
    }

    public boolean isFile() {
        return file;
    }

    public void setNextFreeOffset(int nextFreeOffset) {
        if(!isFile()) {
            this.nextFreeOffset = nextFreeOffset;
        }
        else this.nextFreeOffset = Integer.parseInt(null);
    }

    public int getNextFreeOffset() {
        return nextFreeOffset;
    }

    public void setNextFreeCluster(int nextFreeCluster) {
        this.nextFreeCluster = nextFreeCluster;
    }

    public int getNextFreeCluster() {
        return nextFreeCluster;
    }

    @Override
    public int compareTo(Directory o) {
        return this.name.compareTo(o.name);
    }

    public void setOffsetInParent(int offsetInParent) {
        this.offsetInParent = offsetInParent;
    }

    public int getOffsetInParent() {
        return offsetInParent;
    }


}
