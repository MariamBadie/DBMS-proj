import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import javax.swing.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class DBApp {
    HashSet<String> tableNames = new HashSet<>();

    public void readTableNames() throws DBAppException {
        BufferedReader br = null;
        String itemsPath = "metadata.csv";
        try {
            br = new BufferedReader(new FileReader(itemsPath));
            String line = br.readLine();
            while (line != null) {
                String[] data = line.split(",");
                String name = data[0];
                tableNames.add(name);
                line = br.readLine();
            }
        } catch (Exception e) {
            throw new DBAppException(e);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                throw new DBAppException(e);
            }
        }
        for (String name : tableNames) {

        }
    }

    public void writeNewColumn(String tableName, String columnName, String columnType,
                               String clusteringKey, String indexName, String indexType,
                               String min, String max) throws DBAppException {
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter pw = null;
        String filename = "metadata.csv";
        try {
            fw = new FileWriter(filename, true);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);
            String toWrite = tableName + "," + columnName + "," + columnType + "," +
                    clusteringKey + "," + indexName + "," + indexType + "," + min + "," + max;
            pw.println(toWrite);
            pw.flush();


        } catch (Exception e) {
            throw new DBAppException(e);
        } finally {
            try {
                pw.close();
                bw.close();
                fw.close();
            } catch (Exception e) {

                throw new DBAppException();
            }
        }
    }


    public void init() throws DBAppException {
        Properties props = new Properties();
        try {
            InputStream input = new FileInputStream("resources/DBApp.config");
            props.load(input);
            Table.PAGE_SIZE = Integer.parseInt(props.getProperty("MaximumRowsCountinTablePage"));
            System.out.println(Table.PAGE_SIZE);
        } catch (Exception e) {
            throw new DBAppException(e);
        }
        File file = new File("metadata.csv");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new DBAppException(e);
            }
        }
        File tablesFolder = new File("Tables");
        if (!tablesFolder.isDirectory())
            tablesFolder.mkdir();
        File pagesFolder = new File("Pages");
        if (!pagesFolder.isDirectory())
            pagesFolder.mkdir();

    }

    public void createTable(String strTableName,
                            String strClusteringKeyColumn, // id
                            Hashtable<String, String> htblColNameType,
                            Hashtable<String, String> htblColNameMin,
                            Hashtable<String, String> htblColNameMax)
            throws DBAppException {
        if (tableNames.size() == 0)
            readTableNames();
        if (tableNames.contains(strTableName)) {
            throw new DBAppException("ALREADY EXISTING TABLE");
        } else {
            if (htblColNameType.size() != htblColNameMin.size() || htblColNameType.size() != htblColNameMax.size()) {
                throw new DBAppException("MISSING INFO");
            }
            for (String columnName : htblColNameType.keySet()) { // columnName : id , name , gpa
                String type = htblColNameType.get(columnName);
                String min = htblColNameMin.get(columnName);
                String max = htblColNameMax.get(columnName);
                boolean clustering = columnName.equals(strClusteringKeyColumn);
                writeNewColumn(strTableName, columnName, type, clustering + "", "null", "null", min, max);
            }
            Table t = new Table(strTableName);
            try {
                // Serialize the object to a file
                System.out.println(tableNames.size());
                FileOutputStream fileOut = new FileOutputStream("Tables/" + strTableName + ".class");
                ObjectOutputStream out = new ObjectOutputStream(fileOut);
                out.writeObject(t);
                out.close();
                fileOut.close();
            } catch (IOException e) {
                throw new DBAppException(e);
            }
            tableNames.add(strTableName);

        }
        // check tablename existence
    }

    public void insertIntoTable(String strTableName,
                                Hashtable<String, Object> htblColNameValue)
            throws DBAppException {
        String pk = getPK(strTableName);
        if (!htblColNameValue.keySet().contains(pk)) {
            throw new DBAppException("CANT INSERT WITHOUT PK");
        }
        ArrayList<Hashtable<String, String>> dataOfTable = readDataOfTable(strTableName); //type max min pk
        Hashtable<String, String> htblColNameType = dataOfTable.get(0);
//        String pk = dataOfTable.get(3).get("pk");

        for (String str : htblColNameType.keySet()) {
            if (!htblColNameValue.keySet().contains(str)) {
                htblColNameValue.put(str, Values.NULL);
            }
        }
        verifyTuple(strTableName, htblColNameValue);
        Table t1 = getTable(strTableName);
        Tuple t = new Tuple(htblColNameValue, strTableName);
        t1.insertRec(t);
        saveTable(t1);
    }

    public void verifyTuple(String strTableName, Hashtable<String, Object> htblColNameValue) throws DBAppException {
        BufferedReader br = null;
        ArrayList<Hashtable<String, String>> dataOfTable = readDataOfTable(strTableName); //type max min pk
        Hashtable<String, String> htblColNameType = dataOfTable.get(0);
        Hashtable<String, String> htblColNameMax = dataOfTable.get(1);
        Hashtable<String, String> htblColNameMin = dataOfTable.get(2);
        String pk = dataOfTable.get(3).get("pk");

        for (String str : htblColNameValue.keySet()) {
            if (!htblColNameType.keySet().contains(str))
                throw new DBAppException("THIS COLUMN DOESNT EXIST");
        }
        for (String str : htblColNameType.keySet()) {
            if (htblColNameValue.get(str) == Values.NULL && !str.equals(pk))
                continue;
            else if (htblColNameValue.get(str) == Values.NULL && str.equals(pk))
                throw new DBAppException("PK CAN'T BE NULL");
            if (htblColNameType.get(str).toLowerCase().equals("java.lang.string")) {
                if (!(htblColNameValue.get(str) instanceof String))
                    throw new DBAppException("Invalid DataTypes");
                String val = (String) htblColNameValue.get(str);
                String min = htblColNameMin.get(str);
                String max = htblColNameMax.get(str);
                if (val.compareToIgnoreCase(min) < 0 || val.compareToIgnoreCase(max) > 0)
                    throw new DBAppException("VALUE OUT OF RANGE");
            } else if (htblColNameType.get(str).toLowerCase().equals("java.lang.integer")) {
                if (!(htblColNameValue.get(str) instanceof Integer))
                    throw new DBAppException("Invalid DataTypes");
                Integer val = (Integer) htblColNameValue.get(str);
                Integer min = Integer.parseInt(htblColNameMin.get(str));
                Integer max = Integer.parseInt(htblColNameMax.get(str));
                if (val.compareTo(min) < 0 || val.compareTo(max) > 0)
                    throw new DBAppException("VALUE OUT OF RANGE");
            } else if (htblColNameType.get(str).toLowerCase().equals("java.lang.double")) {
                if (!(htblColNameValue.get(str) instanceof Double || htblColNameValue.get(str) instanceof Integer))
                    throw new DBAppException("Invalid DataTypes");
                Double val = 1.0;
                if (htblColNameValue.get(str) instanceof Integer) {
                    val = Double.valueOf((Integer) htblColNameValue.get(str));
                } else
                    val = (Double) htblColNameValue.get(str);
                Double min = Double.parseDouble(htblColNameMin.get(str));
                Double max = Double.parseDouble(htblColNameMax.get(str));
                if (val.compareTo(min) < 0 || val.compareTo(max) > 0)
                    throw new DBAppException("VALUE OUT OF RANGE");
            }
            if (htblColNameType.get(str).toLowerCase().equals("java.util.date")) {
                if (!(htblColNameValue.get(str) instanceof Double))
                    throw new DBAppException("Invalid DataTypes");
                Date val = (Date) htblColNameValue.get(str);
                Date min = null;
                Date max = null;
                SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-DD");
                try {
                    min = dateFormat.parse(htblColNameMin.get(str));
                    max = dateFormat.parse(htblColNameMax.get(str));
                } catch (Exception e) {
                    throw new DBAppException(e);
                }
                if (val.compareTo(min) < 0 || val.compareTo(max) > 0)
                    throw new DBAppException("VALUE OUT OF RANGE");
            }


        }
    }

    public static ArrayList<Hashtable<String, String>> readDataOfTable(String strTableName) throws DBAppException {
        BufferedReader br = null;
        String itemsPath = "metadata.csv";
        Hashtable<String, String> htblColNameType = new Hashtable<>();
        Hashtable<String, String> htblColNameMax = new Hashtable<>();
        Hashtable<String, String> htblColNameMin = new Hashtable<>();
        String pk = "";
        try {
            br = new BufferedReader(new FileReader(itemsPath));
            String line = br.readLine();
            while (line != null) {
                String[] data = line.split(",");
                String name = data[0];
                if (name.equals(strTableName)) {
                    htblColNameType.put(data[1], data[2]);
                    htblColNameMin.put(data[1], data[6]);
                    htblColNameMax.put(data[1], data[7]);
                    if (data[3].equals("true"))
                        pk = data[1];
                }
                line = br.readLine();
            }
            ArrayList<Hashtable<String, String>> res = new ArrayList<>();
            res.add(htblColNameType);
            res.add(htblColNameMax);
            res.add(htblColNameMin);
            Hashtable<String, String> pktbl = new Hashtable<>();
            pktbl.put("pk", pk);
            res.add(pktbl);
            return res;
        } catch (Exception e) {
            throw new DBAppException(e);
        }

    }

    public void updateTable(String strTableName,
                            String strClusteringKeyValue,
                            Hashtable<String, Object> htblColNameValue)
            throws DBAppException {
        try {
            Table t = getTable(strTableName);
            verifyTuple(strTableName, htblColNameValue);
            verifyPK(strTableName, strClusteringKeyValue);
            Tuple tuple = new Tuple(htblColNameValue, strTableName);
            t.updateRec(tuple, strClusteringKeyValue);
            saveTable(t);
        } catch (Exception e) {
            throw new DBAppException(e);
        }
    }

    private void verifyPK(String strTableName, String strClusteringKeyValue) throws DBAppException {
        ArrayList<Hashtable<String, String>> data = readDataOfTable(strTableName); //type max min pk
        Hashtable<String, String> type = data.get(0);
        String pk = data.get(3).get("pk");
        String pkType = type.get(pk);
        if (pkType.toLowerCase().equals("java.lang.string")) {
            return;
        } else if (pkType.toLowerCase().equals("java.lang.integer")) {
            try {
                Integer test = Integer.parseInt(strClusteringKeyValue);
            } catch (Exception e) {
                throw new DBAppException("Incompatible types String and integer");
            }
        } else if (pkType.toLowerCase().equals("java.lang.double")) {
            try {
                Double test = Double.parseDouble(strClusteringKeyValue);
            } catch (Exception e) {
                throw new DBAppException("Incompatible types string and double");
            }
        } else if (pkType.toLowerCase().equals("java.util.date")) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-DD");
            try {
                Date test = dateFormat.parse(strClusteringKeyValue);
            } catch (Exception e) {
                throw new DBAppException("NOT A DATE");
            }
        }

    }

    public void deleteFromTable(String strTableName,
                                Hashtable<String, Object> htblColNameValue)
            throws DBAppException {
        Table table = getTable(strTableName);
        table.deleteRecs(htblColNameValue);
        saveTable(table);
    }

    public static void main(String[] args) throws Exception {
        DBApp t = new DBApp();
        t.init();
        Hashtable htblColNameType = new Hashtable();
        htblColNameType.put("id", "java.lang.Integer");
        htblColNameType.put("name", "java.lang.String");
        htblColNameType.put("gpa", "java.lang.double");
        Hashtable htblColNameMin = new Hashtable();
        Hashtable htblColNameMax = new Hashtable();
        htblColNameMin.put("id", "0");
        htblColNameMin.put("name", "a");
        htblColNameMin.put("gpa", "0.7");
        htblColNameMax.put("gpa", "5");
        htblColNameMax.put("id", "999000000");
        htblColNameMax.put("name", "ZZZZZ");


        String strTableName = "Student";
        DBApp dbApp = new DBApp();
        dbApp.createTable(strTableName, "id", htblColNameType, htblColNameMin, htblColNameMax);
//dbApp.createIndex( strTableName, new String[] {"gpa"} );
        Hashtable htblColNameValue = new Hashtable();
        htblColNameValue.put("id", new Integer(2343432));
        htblColNameValue.put("name", new String("Ahmed Noor"));
        htblColNameValue.put("gpa", new Double(0.95));
        dbApp.insertIntoTable(strTableName, htblColNameValue);
        htblColNameValue.clear();
        htblColNameValue.put("id", new Integer(453455));
        htblColNameValue.put("name", new String("Ahmed Noor"));
        htblColNameValue.put("gpa", new Double(0.95));
        dbApp.insertIntoTable(strTableName, htblColNameValue);
        htblColNameValue.clear();
        htblColNameValue.put("id", new Integer(5674567));
        htblColNameValue.put("name", new String("Dalia Noor"));
        htblColNameValue.put("gpa", new Double(1.25));
        dbApp.insertIntoTable(strTableName, htblColNameValue);
        htblColNameValue.clear();
        htblColNameValue.put("id", new Integer(23498));
        htblColNameValue.put("name", new String("John Noor"));
        htblColNameValue.put("gpa", new Double(1.5));
        dbApp.insertIntoTable(strTableName, htblColNameValue);
        htblColNameValue.clear();
        htblColNameValue.put("id", new Integer(78452));
        htblColNameValue.put("name", new String("Zaky Noor"));
        htblColNameValue.put("gpa", new Double(0.88));
        dbApp.insertIntoTable(strTableName, htblColNameValue);
        t.displayTable(strTableName);
    }

    public Table getTable(String tableName) throws DBAppException {
        try {
            Table t = null;
            FileInputStream fileIn = new FileInputStream("Tables/" + tableName + ".class");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            t = (Table) in.readObject();
            in.close();
            fileIn.close();
            return t;
        } catch (Exception e) {
            throw new DBAppException(e);
        }
    }

    public void saveTable(Table t) throws DBAppException {
        try {
            // Serialize the object to a file
            FileOutputStream fileOut = new FileOutputStream("Tables/" + t.name + ".class");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(t);
            out.close();
            fileOut.close();
            t = null;
            System.gc();
        } catch (IOException e) {
            throw new DBAppException(e);
        }
    }

    public void processUpdate(String sql) throws DBAppException {
        CharStream input = CharStreams.fromString(sql);
        gLexer t = new gLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(t);
        gParser parser = new gParser(tokens);
        ParseTree tree = parser.updateStatement();
        ParseTree set = tree.getChild(3);
        System.out.println(set.getChildCount());
        Hashtable<String, Object> htblColNameVal = new Hashtable<>();
        for (int i = 0; i < set.getChildCount(); i++) {
            if (set.getChild(i).toString().contains(","))
                continue;
            ParseTree setItem = set.getChild(i);
            String col = setItem.getChild(0).getChild(0).toString();
            String op = setItem.getChild(1).toString();
            Object val = setItem.getChild(2).getChild(0).getChild(0);
            if (!op.trim().equals("=")) {
                System.out.println(op);
                System.out.println("ERROR");
                return;
            }
            htblColNameVal.put(col, val);
        }
        ParseTree condition = tree.getChild(5).getChild(0);
        System.out.println("-----------------------------------------------------------");
        String op = condition.getChild(1).getChild(0).toString();
        System.out.println(op + "FJPFP");
        if (!op.trim().equals("=")) {
            System.out.println(op);
            System.out.println("ERROR");
            return;
        }
        String pkName = condition.getChild(0).getChild(0).getChild(0).toString();
        String tableName = tree.getChild(1).getChild(0).toString();
        System.out.println(pkName);
        System.out.println(tableName);
        //Table table = getTable("aa");
//        if (!table.pk.equals(pkName)){
//            System.out.println("ERROR");
//            return;
//        }
        String pkVal = condition.getChild(2).getChild(0).getChild(0).toString();

    }

    public void processInsert(String sql) {
        CharStream input = CharStreams.fromString(sql);
        gLexer t = new gLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(t);
        gParser parser = new gParser(tokens);
        ParseTree tree = parser.insertStatement();
        ParseTree columns = tree.getChild(4);
        ParseTree values = tree.getChild(8);
        if (columns.getChildCount() != values.getChildCount()) {
            System.out.println(columns.getChildCount());
            System.out.println(values.getChildCount());
            System.out.println("ERROR !!!!!!!!!!!!!!!!!");
            return;
        }
        String tableName = tree.getChild(2).getChild(0).toString();
        System.out.println(tableName);
        Hashtable<String, Object> htblColNameValue = new Hashtable<>();
        for (int i = 0; i < values.getChildCount(); i++) {
            if (values.getChild(i).toString().contains(","))
                continue;
            String column = columns.getChild(i).getChild(0).toString();
            Object value = values.getChild(i).getChild(0).getChild(0);
            //System.out.println("Column" + i + ":" + column);
            //System.out.println("Value" + i + ": " + value);
            htblColNameValue.put(column, value);
        }
        for (String col : htblColNameValue.keySet()) {
            System.out.println(col + "  " + htblColNameValue.get(col));
        }


    }

    public void processDelete(String sql) {
        CharStream input = CharStreams.fromString(sql);
        gLexer t = new gLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(t);
        gParser parser = new gParser(tokens);
        ParseTree tree = parser.deleteStatement();
        ParseTree condition = tree.getChild(4);
        Hashtable<String, Object> htblColNameValue = new Hashtable<>();
        for (int i = 0; i < condition.getChildCount(); i++) {
            if (condition.getChild(i).toString().contains(","))
                continue;
            //String name = condition.getChild(i).getChild(0).getChild(0).getChild(0).getChild(0).toString();
            //Object value = condition.getChild(i).getChild(0).getChild(2).getChild(0).getChild(0);
            String name = condition.getChild(i).getChild(0).getChild(0).getChild(0).getChild(0).toString();
            String op = condition.getChild(i).getChild(0).getChild(1).getChild(0).toString();
            Object value = condition.getChild(i).getChild(0).getChild(2).getChild(0).getChild(0);
            if (!op.trim().equals("=")) {
                System.out.println("ERROR");
                return;
            }
            htblColNameValue.put(name, value);
        }
        for (String col : htblColNameValue.keySet())
            System.out.println(col + "   " + htblColNameValue.get(col));
    }

    public void displayTable(String tableName) throws DBAppException {
        Table t = getTable(tableName);
        for (int i = 0; i < t.pageNums.size(); i++) {
            int pgNum = t.pageNums.get(i);
            Page p = t.getPageByNumber(pgNum);
            System.out.println("----------- Start of page " + i + "----------------");
            for (int j = 0; j < p.getTuples().size(); j++) {
                Hashtable<String, Object> htbl = p.getTuples().get(j).getData();
                for (String col : htbl.keySet()) {
                    System.out.println(col + ":" + htbl.get(col));
                }
                System.out.println("-------------------------------------------------");
            }
            System.out.println("----------- end of page " + i + "-----------------");
        }
    }

    public static String getPK(String strTableName) throws DBAppException {
        BufferedReader br = null;
        String itemsPath = "metadata.csv";
        String pk = "";
        try {
            br = new BufferedReader(new FileReader(itemsPath));
            String line = br.readLine();
            while (line != null) {
                String[] data = line.split(",");
                String name = data[0];
                if (name.equals(strTableName)) {
                    if (data[3].equals("true"))
                        return data[1];
                }
                line = br.readLine();
            }
            return pk;
        } catch (Exception e) {
            throw new DBAppException(e);
        }

    }


}