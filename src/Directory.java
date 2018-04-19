import java.util.ArrayList;

public class Directory {
    private String name;
    private int dirAttribute;
    private int size;
    private String low;
    private String high;
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
}
