package Merkle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class TreeBuilder {
    private String path = "";
    public MerkleTree root = null;

    public TreeBuilder(String dirPath)
    {
        this.path = dirPath;
    }

    public void builder() throws IOException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            // Should never happen, we specified SHA, a valid algorithm
            assert false;
        }

        File[] fileList = new File(path).listFiles();
        List<Leaf> leafs = new ArrayList<Leaf>();
        assert fileList != null;
        Arrays.sort(fileList);
        for (File f: fileList)
        {
            String name = f.getName();
            File file = new File(path + "\\" + name);
            byte[] bytes = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(bytes);
            fis.close();

            List<byte[]> blocks = new ArrayList<>();
            blocks.add(name.getBytes());
            blocks.add(bytes);
            Leaf leaf = new Leaf(blocks, name);
            leafs.add(leaf);
//            System.out.println("filename: " + f.getName());
        }

        List<MerkleTree> branchs = new ArrayList<>();
        for (int i = 0; i < leafs.size(); i += 2)
        {
            MerkleTree tempBranch = new MerkleTree(md);
            if (i + 1 != leafs.size())
            {
                tempBranch.add(leafs.get(i), leafs.get(i + 1));
                leafs.get(i).setFather(tempBranch);
                leafs.get(i + 1).setFather(tempBranch);
                tempBranch.addFiles(leafs.get(i).getFilename());
                tempBranch.addFiles(leafs.get(i + 1).getFilename());
            }
            else {
                tempBranch.add(branchs.get(branchs.size() - 1), leafs.get(i));
                tempBranch.addFiles(branchs.get(branchs.size() - 1).getFileList());
                tempBranch.addFiles(leafs.get(i).getFilename());
                branchs.remove(branchs.size() - 1);
            }
            branchs.add(tempBranch);
        }

        while (branchs.size() != 1)
        {
            // recursive to root
            int levelSize = branchs.size();

            for (int i = 0; i < levelSize; i += 2)
            {
                MerkleTree tempBranch = new MerkleTree(md);
                if (i + 1 != levelSize)
                {
                    tempBranch.add(branchs.get(i), branchs.get(i + 1));
                    branchs.get(i).setFather(tempBranch);
                    branchs.get(i + 1).setFather(tempBranch);
                    tempBranch.addFiles(branchs.get(i).getFileList());
                    tempBranch.addFiles(branchs.get(i + 1).getFileList());
                    branchs.add(tempBranch);
                }
                else {
                    tempBranch.add(branchs.get(branchs.size() - 1), branchs.get(i));
                    branchs.get(branchs.size() - 1).setFather(tempBranch);
                    branchs.get(i).setFather(tempBranch);
                    tempBranch.addFiles(branchs.get(branchs.size() - 1).getFileList());
                    tempBranch.addFiles(branchs.get(i).getFileList());
                    branchs.add(tempBranch);
                }
            }

            if (levelSize > 0) {
                branchs.subList(0, levelSize).clear();
            }
        }

        root = branchs.get(0); // return the root
//        for (String f : root.getFileList())
//            System.out.println(f);
    }

    public Leaf searchFile(String filename) { return root.searchFile(filename); }

    public void addLeaf(String filename) throws IOException {
//        root.prettyPrint();
        File file = new File(path + "\\" + filename);
        byte[] bytes = new byte[(int) file.length()];
        FileInputStream fis = new FileInputStream(file);
        fis.read(bytes);
        fis.close();

        List<byte[]> blocks = new ArrayList<>();
        blocks.add(filename.getBytes());
        blocks.add(bytes);
        Leaf leaf = new Leaf(blocks, filename);

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            // Should never happen, we specified SHA, a valid algorithm
            assert false;
        }

        SortedSet<String> fileSet = new TreeSet<>(root.getFileList());
        SortedSet<String> tail = fileSet.tailSet(filename);
//        System.out.println(fileSet.size());
        if (tail.size() == 0) {
            // add to the right of the last file
            String nearest = fileSet.headSet(filename).last();
            Leaf closest = root.searchFile(nearest);
            MerkleTree father = closest.getFather();
            if (father.rightLeaf() == null && father.rightTree() == null) {
                // the right leaf is null
                leaf.setFather(father);
                father.add(closest, leaf);
            } else if (father.leftLeaf() == null && father.leftTree() == null){
                leaf.setFather(father);
                father.add(closest, leaf);
            } else {
                MerkleTree branch = new MerkleTree(md);
                branch.add(closest, leaf);
                branch.setFather(father);
                branch.addFiles(filename);
                branch.addFiles(closest.getFilename());
                closest.setFather(branch);
                leaf.setFather(branch);
                father.setRight((Leaf) null);
                if (father.leftTree() != null)
                {
                    father.add(father.leftTree(), branch);
                } else {
                    father.add(father.leftLeaf(), branch);
                }
            }
            father.addFiles(filename);
            MerkleTree node = father;
            father = father.getFather();
            while (father != null)
            {
                father.addFiles(filename);
                if (node == father.leftTree())
                    father.add(node, father.rightTree());
                else if (node == father.rightTree())
                    father.add(father.leftTree(), node);
                else
                    System.out.println("add leaf failed");
                node = father;
                father = father.getFather();
            }
        } else {
            // add to the left of the file, which is the smallest in the tailSet
            String nearest = tail.first();
//            System.out.println("filename: " + nearest);
            Leaf closest = root.searchFile(nearest);
            MerkleTree father = closest.getFather();
            father.addFiles(filename);
//            System.out.println(father.leftLeaf());
            if (closest == father.leftLeaf())
            {
//                System.out.println("right leaf: " + father.rightLeaf());
                if (father.rightLeaf() == null && father.rightTree() == null)
                {
//                    System.out.println("run this");
                    father.add(leaf, closest);
                } else {
                    MerkleTree branch = new MerkleTree(md);
                    branch.add(leaf, closest);
                    branch.addFiles(filename);
                    branch.addFiles(closest.getFilename());
                    branch.setFather(father);
                    closest.setFather(branch);
                    leaf.setFather(branch);
                    father.setLeft((Leaf) null);
                    if (father.rightTree() != null) {
                        father.add(branch, father.rightTree());
                    } else {
                        father.add(branch, father.rightLeaf());
                    }
                }
            } else {
//                System.out.println("aaa");
                if (father.leftLeaf() == null && father.leftTree() == null)
                {
                    father.add(leaf, closest);
                } else {
                    MerkleTree branch = new MerkleTree(md);
                    branch.add(leaf, closest);
                    branch.addFiles(filename);
                    branch.addFiles(closest.getFilename());
                    branch.setFather(father);
                    closest.setFather(branch);
                    leaf.setFather(branch);
                    father.setRight((Leaf) null);
                    if (father.leftTree() != null)
                    {
                        father.add(father.leftTree(), branch);
                    } else {
                        father.add(father.leftLeaf(), branch);
                    }
                }
            }
            MerkleTree node = father;
            father = father.getFather();
            while (father != null)
            {
                father.addFiles(filename);
                if (node == father.leftTree())
                    father.add(node, father.rightTree());
                else if (node == father.rightTree())
                    father.add(father.leftTree(), node);
                else
                    System.out.println("add leaf failed");
                node = father;
                father = father.getFather();
            }
        }
    }

    public void deleteLeaf(String filename)
    {
        Leaf leaf = root.searchFile(filename);
        MerkleTree father = leaf.getFather();
        MerkleTree grandfather = father.getFather();
        father.deleteFile(filename);
        grandfather.deleteFile(filename);

        if (father.leftLeaf() == leaf) {
            //右节点上提
            if (father.rightLeaf() != null) {
                //兄弟节点也是叶子节点
                if (grandfather.leftTree() == father) {
                    //父节点是祖父节点的左子树,用兄弟节点来取代祖父节点的左子树
                    if (grandfather.rightTree() != null) {
                        grandfather.add(father.rightLeaf(), grandfather.rightTree());
                    }
                    else {
                        grandfather.add(father.rightLeaf(), grandfather.rightLeaf());
                    }
                } else {
                    //父节点是祖父节点的右子树,用兄弟节点来取代祖父节点的右子树
                    if (grandfather.leftTree() != null) {
                        grandfather.add(grandfather.leftTree(), father.rightLeaf());
                    }
                    else {
                        grandfather.add(grandfather.leftLeaf(), father.rightLeaf());
                    }
                }
                father.rightLeaf().setFather(grandfather);
            } else {
                //兄弟节点是树节点
                if (grandfather.leftTree() == father) {
                    //父节点是祖父节点的左子树,用兄弟节点来取代祖父节点的左子树
                    if (grandfather.rightTree() != null)
                        grandfather.add(father.rightTree(), grandfather.rightTree());
                    else
                        grandfather.add(father.rightTree(), grandfather.rightLeaf());
                } else {
                    //父节点是祖父节点的右子树,用兄弟节点来取代祖父节点的右子树
                    if (grandfather.leftTree() != null)
                        grandfather.add(grandfather.leftTree(), father.rightTree());
                    else
                        grandfather.add(grandfather.leftLeaf(), father.rightTree());
                }
                father.rightTree().setFather(grandfather);
            }
        } else {
            //左节点上提
            if (father.leftLeaf() != null) {
                //兄弟节点也是叶子节点
                if (grandfather.leftTree() == father) {
                    //父节点是祖父节点的左子树,用兄弟节点来取代祖父节点的左子树
                    if (grandfather.rightTree() != null)
                        grandfather.add(father.leftLeaf(), grandfather.rightTree());
                    else
                        grandfather.add(father.leftLeaf(), grandfather.rightLeaf());
                    grandfather.setLeft((MerkleTree) null);
                } else {
                    //父节点是祖父节点的右子树,用兄弟节点来取代祖父节点的右子树
                    if (grandfather.leftTree() != null)
                        grandfather.add(grandfather.leftTree(), father.leftLeaf());
                    else
                        grandfather.add(grandfather.leftLeaf(), father.leftLeaf());
                    grandfather.setRight((MerkleTree) null);
                }
                father.leftLeaf().setFather(grandfather);
            } else {
                //兄弟节点是树节点
                if (father.getFather().leftTree() == father) {
                    //父节点是祖父节点的左子树,用兄弟节点来取代祖父节点的左子树
                    if (grandfather.rightTree() != null)
                        grandfather.add(father.leftTree(), grandfather.rightTree());
                    else
                        grandfather.add(father.leftTree(), grandfather.rightLeaf());
                } else {
                    //父节点是祖父节点的右子树,用兄弟节点来取代祖父节点的右子树
                    if (grandfather.leftTree() != null)
                        grandfather.add(grandfather.leftTree(), father.leftTree());
                    else
                        grandfather.add(grandfather.leftLeaf(), father.leftTree());
                }
                father.leftTree().setFather(grandfather);
            }
        }
        //回溯更新哈希值
        father = grandfather;
        grandfather = grandfather.getFather();
        while(grandfather != null)
        {
            grandfather.deleteFile(filename);
            if(grandfather.leftTree()== father)
            {
                if(grandfather.rightLeaf()!=null)
                    grandfather.add(father,grandfather.rightLeaf());
                else grandfather.add(father,grandfather.rightTree());
            }else{
                if(grandfather.leftLeaf()!=null)
                    grandfather.add(grandfather.leftLeaf(),father);
                else grandfather.add(grandfather.leftTree(),father);
            }
            father = grandfather;
            grandfather = grandfather.getFather();
        }
//        System.out.println("Successful delete leaf from tree!");
    }

    public fileState compareTree(TreeBuilder t)
    {
        ArrayList<String> editList = new ArrayList<String>();
        ArrayList<Types> typesList = new ArrayList<Types>();
        if (t.root.digestEqual(root))
        {
            typesList.add(Types.Equal);
            return new fileState(typesList, editList);
        }
        List<String> filenameList = new ArrayList<String>(t.root.getFileList());

        if (root.getFileList().containsAll(filenameList) && filenameList.containsAll(root.getFileList()))
        {
            for (String f: filenameList)
            {
                Leaf targetLeaf = searchFile(f);
                Leaf optionLeaf = t.searchFile(f);
                if (!optionLeaf.blockEqual(targetLeaf))
                {
                    editList.add(f);
                    typesList.add(Types.Edit);
                }
                editList.add(f);
                typesList.add(Types.Equal);
            }
            return new fileState(typesList, editList);
        } else if (filenameList.containsAll(root.getFileList())) {
            // Add files
            for (String f: filenameList)
            {
                Leaf targetLeaf = searchFile(f);
                Leaf optionLeaf = t.searchFile(f);
                if (targetLeaf == null)
                {
                    editList.add(f);
                    typesList.add(Types.Add);
                } else {
                    if (!optionLeaf.blockEqual(targetLeaf))
                    {
                        editList.add(f);
                        typesList.add(Types.Edit);
                    }
                    editList.add(f);
                    typesList.add(Types.Equal);
                }
            }
            return new fileState(typesList, editList);
        } else {
            for (String f: root.getFileList())
            {
                Leaf targetLeaf = searchFile(f);
                Leaf optionLeaf = t.searchFile(f);
                if (filenameList.contains(f))
                {
                    if (!optionLeaf.blockEqual(targetLeaf))
                    {
                        editList.add(f);
                        typesList.add(Types.Edit);
                    }
                    editList.add(f);
                    typesList.add(Types.Equal);
                } else {
                    editList.add(f);
                    typesList.add(Types.Delete);
                }
            }
            for (String f: filenameList) {
                if (!root.getFileList().contains(f)) {
                    editList.add(f);
                    typesList.add(Types.Add);
                }
            }
            return new fileState(typesList, editList);
        }
    }

    public void Synchronize(TreeBuilder t) throws IOException {
        fileState changes;
        changes = this.compareTree(t);
        for (int i = 0; i < changes.type.size(); i++) {
            if (changes.type.get(i) == Types.Add) {
                File file = new File(t.path + "\\" + changes.filename.get(i));
                if (file.delete())
                    System.out.println(changes.filename.get(i) + " has been deleted!");
                else
                    System.out.println("Delete failed!");
            } else if (changes.type.get(i) == Types.Edit) {
                File sourceFile = new File(path + "\\" + changes.filename.get(i));
                File destFile = new File(t.path + "\\" + changes.filename.get(i));
                destFile.delete();
                Files.copy(sourceFile.toPath(), destFile.toPath());
                System.out.println(changes.filename.get(i) + " has been rewrite!");
            } else if (changes.type.get(i) == Types.Delete) {
                File sourceFile = new File(path + "\\" + changes.filename.get(i));
                File destFile = new File(t.path + "\\" + changes.filename.get(i));
                Files.copy(sourceFile.toPath(), destFile.toPath());
                System.out.println(changes.filename.get(i) + " has been recover!");
            }
        }
    }
}
