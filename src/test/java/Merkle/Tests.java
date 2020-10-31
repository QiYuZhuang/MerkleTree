package Merkle;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Tests {
    TreeBuilder tree_1 = new TreeBuilder("E:\\Lab3\\dir1");
    TreeBuilder tree_2 = new TreeBuilder("E:\\Lab3\\dir2");

    @Test
    public void build() throws IOException {
        tree_1.builder();
        tree_1.root.prettyPrint();
    }

    @Test
    public void delete() throws IOException {
        tree_1.builder();
        tree_1.deleteLeaf(tree_1.root.getFileList().get(0));
        tree_1.deleteLeaf(tree_1.root.getFileList().get(3));
        tree_1.root.prettyPrint();
    }

    @Test
    public void insert() throws IOException {
        tree_1.builder();
        tree_1.root.prettyPrint();
        tree_1.deleteLeaf(tree_1.root.getFileList().get(0));
        tree_1.deleteLeaf(tree_1.root.getFileList().get(3));
        System.out.println("After delete: ");
        tree_1.root.prettyPrint();
        tree_1.addLeaf("file1.txt");
        tree_1.addLeaf("file5.txt");
        System.out.println("After insert: ");
        tree_1.root.prettyPrint();
    }

    @Test
    public void synchronize() throws IOException {
        tree_1.builder();
        tree_2.builder();
        fileState changes;
        changes = tree_1.compareTree(tree_2);
        System.out.println("After compare: ");
        for (int i = 0; i < changes.type.size(); i++) {
            if (changes.type.get(i) == Types.Equal)
                System.out.println("Equal: " + changes.filename.get(i));
            else if (changes.type.get(i) == Types.Edit)
                System.out.println("Edit: " + changes.filename.get(i));
            else if (changes.type.get(i) == Types.Add)
                System.out.println("Add: " + changes.filename.get(i));
            else
                System.out.println("Delete: " + changes.filename.get(i));
        }
        System.out.println("After synchronize: ");
        tree_1.Synchronize(tree_2); // tree 1 is the
        tree_2.builder();
        tree_2.root.prettyPrint();
    }
}
