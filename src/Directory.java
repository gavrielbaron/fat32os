public class Directory {
    private String name;
    private int dirAttribute;
    private int size;
    private String low;
    private String high;
    private Directory children;


    public Directory(String name, int dirAttribute, int size, String low, String high){
        this.name = name;
        this.dirAttribute = dirAttribute;
        this.size = size;
        this.low = low;
        this.high = high;
        this.children = null;
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
}
