import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Table implements Serializable {
    static int PAGE_SIZE;
    String name;
//    String pk;
    //private int pageCount;
    Vector<Integer> pageNums;
    public Table(String name) {
        //pages = new Vector<>();
        this.name = name;
        this.pageNums = new Vector<>();
    }

    public void insertRec(Tuple t) throws DBAppException {
            if (this.pageNums.size() == 0) {
                createPage();
                Page p = getPageByNumber(this.pageNums.get(0));
                p.getTuples().add(t);
                savePage(p);
                return;
            }
            Page p = getPageByTuple(t);
            // if condition if p = null <<<<
            if (p == null) {
                Page page = getPageByNumber(this.pageNums.get(this.pageNums.size() - 1));
                if (page.getTuples().size() < Table.PAGE_SIZE) {
                    page.getTuples().add(t);
                    savePage(page);
                    return;
                }
                createPage();
//                Page pg = getPageByNumber(this.pageNums.size() - 1);
                Page pg = getPageByNumber(this.pageNums.get(this.pageNums.size() - 1));
                pg.getTuples().add(t);
                savePage(pg);
                return;
            }
            Tuple first = p.getTuples().get(0);
            if (t.compareTo(first) < 0){
                for (int i = 0 ; i < pageNums.size() ; i++){
                    int pgNum = pageNums.get(i);
                    if (pgNum == p.getPageNumber() && i!=0){
                        Page prevPage = getPageByNumber(i-1);
                        if (prevPage.getTuples().size() < Table.PAGE_SIZE){
                            prevPage.getTuples().add(t);
                            savePage(prevPage);
                            return;
                        }
                    }
                }
            }
            this.insertRec(t, p);
    }
    public void createPage() throws DBAppException{
        if (pageNums.size() == 0){
            Page p = new Page(1);
            this.pageNums.add(1);
            savePage(p);
            return;
        }
        Page p = new Page(pageNums.get(pageNums.size() - 1)+1);
        this.pageNums.add(p.getPageNumber());
        //pageCount++;
        // serialize page
        savePage(p);
    }
    public void savePage(Page p) throws DBAppException{
        try {
            // Serialize the object to a file
            FileOutputStream fileOut = new FileOutputStream("Pages/" + this.name + "" + p.getPageNumber() + ".class");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(p);
            out.close();
            fileOut.close();
            p = null;
            System.gc();
        } catch(IOException e) {
            throw new DBAppException(e);
        }
    }
    public Page getPageByNumber(int pageNumber) throws DBAppException {
        try {
            Page p = null;
            FileInputStream fileIn = new FileInputStream("Pages/" + this.name + "" + pageNumber + ".class");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            p = (Page) in.readObject();
            in.close();
            fileIn.close();
            return p;
        }
        catch (Exception e){
            throw new DBAppException(e);
        }
    }
    public Page getPageByTuple(Tuple t) throws DBAppException {
        for (int i = 0 ; i < this.pageNums.size() ; i++){
            int pgNum = pageNums.get(i);
            Page p = getPageByNumber(pgNum);
            if (p.getTuples().get(p.getTuples().size() - 1).compareTo(t) >= 0) {
//                if (p.getTuples().get(0).compareTo(t) > 0 && i != 0){
//                    Page prev = getPageByNumber(pageNums.get(i-1));
//                    if (prev.getTuples().size() < PAGE_SIZE)
//                        return prev;
//                }
                return p;
            }
        }
        return null; // bigger than largest record in last page
    }

    public void insertRec(Tuple t , Page p) throws DBAppException {
        Vector<Tuple> tuples = p.getTuples();
        if (tuples.isEmpty()){
            tuples.add(t);
            savePage(p);
            return;
        }
        int low = 0 , high = tuples.size() - 1 , mid = (low + high) / 2;
        while (low <= high){
            mid = (low + high) / 2;
            Tuple tempTup = tuples.get(mid);
            int com = t.compareTo(tempTup);
            if (com == 0){
                throw new DBAppException("Existing PK");
            }
            if (com < 0)
                high = mid - 1;
            else
                low = mid + 1;
        }
        // now we insert it at mid value and shift all other record on cell down
        Tuple tempTup = tuples.get(mid);
        if (t.compareTo(tempTup) > 0)
            mid++;
        // if the page was full before inserting
        if (tuples.size() == Table.PAGE_SIZE) {
            // insert the last record to the next page
            if (p.getPageNumber() == this.pageNums.get(this.pageNums.size() - 1))
                createPage();
            int pageNum = 0;
            for (int i = 0 ; i < pageNums.size() ; i++){
                int pgNum = pageNums.get(i);
                if (p.getPageNumber() == pgNum){
                    pageNum = pageNums.get(i+1);
                    break;
                }
            }
            Page nextPage = getPageByNumber(pageNum); // edit
            this.insertRec(tuples.get(tuples.size() - 1), nextPage);
            for (int i = tuples.size()-1 ; i > mid ; i--){
                tuples.set(i , tuples.get(i-1));
            }
            tuples.set(mid , t);
            savePage(p);
            return;
        }
        Tuple last = tuples.get(tuples.size() - 1);
        for (int i = tuples.size()-1 ; i > mid ; i--){
            tuples.set(i , tuples.get(i-1));
        }
        tuples.set(mid , t);
        tuples.add(last);
        savePage(p);
    }
    public void deleteRecs(Hashtable<String , Object> htblColNameVal) throws DBAppException {
//        ArrayList<Hashtable<String , String>> dataOfTable = DBApp.readDataOfTable(this.name); //type max min pk
//        String pk = dataOfTable.get(3).get("pk");
        String pk = DBApp.getPK(this.name);
        if (htblColNameVal.keySet().contains(pk)){
            Tuple t = new Tuple(htblColNameVal , this.name);
            Page p = getPageByTuple(t);
            if (p==null){
                throw new DBAppException("NOT EXISTING PK");
            }
            int index = binSearchRecs(p,t);
            if (index == -1)
                throw new DBAppException("NOT EXISTING PK");
            Tuple tup = p.getTuples().get(index);
            if (checkDelete(htblColNameVal , tup))
                deleteRec(index , p , true);
            return;
        }
        for (int i = 0 ; i < pageNums.size() ; i++){
            int size = pageNums.size();
            int pgNum = pageNums.get(i);
            Page p = getPageByNumber(pgNum);
            boolean pageAva = true;
            for (int j = 0 ; j < p.getTuples().size() ; j++){
                if (checkDelete(htblColNameVal , p.getTuples().get(j))){
                    deleteRec(j,p , pageAva);
                    j--;
                }
            }
            if (pageNums.size() < size)
                i--;
        }



    }
    public boolean checkDelete(Hashtable<String,Object> htblColNameVal , Tuple t){
        for (String key : htblColNameVal.keySet()){
            Object val = htblColNameVal.get(key);
            if (val instanceof Integer){
                Integer value = (Integer) val;
                if (value != (Integer) t.getData().get(key))
                    return false;
            }
            if (val instanceof Double){
                Double value = (Double) val;
                Double test = (Double) t.getData().get(key);
                if (!value.equals(test))
                    return false;
            }
            if (val instanceof String){
                String value = (String) val;
                if (!value.equals((String) t.getData().get(key)))
                    return false;
            }
            if (val instanceof Date){
                Date value = (Date) val;
                if (!value.equals ((Date) t.getData().get(key)))
                    return false;
            }
        }
        return true;
    }
    public int binSearchRecs(Page p , Tuple t) throws DBAppException {
        int low = 0 , high = p.getTuples().size() - 1 , mid = (low + high) / 2;
        while (low <= high){
            mid = (low + high) / 2;
            Tuple tempTup = p.getTuples().get(mid);
            int com = t.compareTo(tempTup);
            if (com == 0){
                return mid;
            }
            if (com < 0)
                high = mid - 1;
            else
                low = mid + 1;
        }
        return -1;
    }
    public void deleteRec(int i,Page p , boolean pageAva) throws DBAppException {
        //Page p = getPageByTuple(t);
        p.getTuples().remove(i);
        if (p.getTuples().size() == 0){
            String path = "Pages/" + this.name + "" + p.getPageNumber() + ".class";
            File file = new File(path);
            System.out.println(file.delete());
            pageAva = false;
            for (int j = 0 ; j < pageNums.size() ; j++) {
                int pg = pageNums.get(j);
                if (pg==p.getPageNumber()) {
                    this.pageNums.remove(j);
                    break;
                }
            }
        }
        else {
            savePage(p);
        }

    }
    public void updateRec(Tuple t,String val) throws DBAppException {
        String pk = DBApp.getPK(this.name);
            Page p = getPageContainingKey(val);
            if (p == null) {
                throw new DBAppException("PK DOESNT EXIST");
            }
            int low = 0, high = p.getTuples().size() - 1, mid = (low + high) / 2;
            while (low <= high) {
                mid = (low + high) / 2;
                Tuple tempTup = p.getTuples().get(mid);
                Object midVal = tempTup.getValue(pk);
                int com = comparePKs(val , midVal);
                if (com == 0) {
                    for (String str : t.getData().keySet()) {
                        tempTup.getData().replace(str, t.getData().get(str));
                    }
                    savePage(p);
                    return;
                }
                if (com < 0)
                    high = mid - 1;
                else
                    low = mid + 1;
            }
            throw new DBAppException("PK DOESNT EXIST");
    }
    public Page getPageContainingKey(String key) throws DBAppException {
        String pk = DBApp.getPK(this.name);
        ArrayList<Hashtable<String , String>> a = DBApp.readDataOfTable(this.name);//type max min pk
        for (int i = 0 ; i < pageNums.size() ; i++){
            Page page = getPageByNumber(pageNums.get(i));
            if (comparePKs(key , page.getTuples().get(0).getData().get(pk)) >= 0 &&
                    comparePKs(key , page.getTuples().get(page.getTuples().size() - 1).getData().get(pk)) <= 0)
                return page;
        }

        return null;
    }
    public int comparePKs(String stringPK , Object realPK) throws DBAppException {
        if (realPK instanceof String){
            return stringPK.compareTo((String) realPK);
        }
        else if (realPK instanceof Integer){
            Integer sPK = Integer.parseInt(stringPK);
            Integer rPK = (Integer) realPK;
            return sPK.compareTo(rPK);
        }
        if (realPK instanceof Double){
            Double sPK = Double.parseDouble(stringPK);
            Double rPK = (Double) realPK;
            return sPK.compareTo(rPK);
        }
        else{
            SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-DD");
            try {
                Date sPK = dateFormat.parse(stringPK);
                Date rPK = (Date) realPK;
                return sPK.compareTo(rPK);
            } catch (Exception e) {
                throw new DBAppException("NOT A DATE");
            }
        }

    }


}