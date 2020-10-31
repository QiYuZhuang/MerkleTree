package Merkle;

import java.util.ArrayList;
import java.util.List;

public class fileState {
    public List<Types >type;
    public List<String> filename;
//    public List<String> changeList = new ArrayList<>();

    public fileState(){ }

    public fileState(ArrayList<Types> t, ArrayList<String> f)
    {
        type = t;
        filename = f;
    }

}
