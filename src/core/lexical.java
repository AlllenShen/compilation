package core;
        import javax.xml.stream.FactoryConfigurationError;
        import java.io.*;
        import java.lang.*;
        import java.util.ArrayList;
        import java.util.concurrent.ArrayBlockingQueue;

public class lexical {
    private String srcPath;
    private BufferedReader src;

    private Integer row;
    private Integer col;

    private ArrayList<String> keyw;
    private ArrayList<String> identifier;
    private ArrayList<String> constant;
    private ArrayList<String> opr;
    private ArrayList<String> delimiter;

    private Character currentChar;
    private String  strBuffer;

    private ArrayBlockingQueue<mark> pre_mark;
    private ArrayList<token> tokens;
    private String errorMsg;

    private Boolean exception;

    public lexical() throws Exception {
        keyw = new ArrayList<>();
        identifier = new ArrayList<>();
        constant = new ArrayList<>();
        opr = new ArrayList<>();
        delimiter = new ArrayList<>();
        tokens = new ArrayList<>();
        row = 1;
        col = 1;
        pre_mark = new ArrayBlockingQueue<>(100);
        strBuffer = "";
        errorMsg = "";
        load_delimiter();
        load_keyw();
        load_opr();
        exception = false;
    }
    // 加载资源
    void load_keyw() throws Exception{
        BufferedReader in = new BufferedReader(new InputStreamReader(
                new FileInputStream("keywords.txt")));
        String kw;
        while ((kw=in.readLine()) != null) {
            // System.out.println(kw + keyw.size());
            keyw.add(kw);
        }
    }
    void load_delimiter() throws Exception{
        BufferedReader in = new BufferedReader(new InputStreamReader(
                new FileInputStream("delimiters.txt")));
        String kw;
        while ((kw=in.readLine()) != null) {
            // System.out.println(kw + delimiter.size());
            delimiter.add(kw);
        }
    }
    void load_opr() throws Exception{
        BufferedReader in = new BufferedReader(new InputStreamReader(
                new FileInputStream("operations.txt")));
        String kw;
        while ((kw=in.readLine()) != null) {
            // System.out.println(kw + opr.size());
            opr.add(kw);
        }
    }
    public void load_src_file(String path) throws Exception {
        srcPath = path;
        File f = new File(path);
        src = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        // System.out.println(src.readLine());
        row = 1;
        col = 1;
    }
    // 预处理
    public void pre_handle() throws Exception{
        scan_script();
    }
    public void scan_script() throws Exception{
        if (src == null) {
            System.out.println("未打开源文件");
        }
        move();
        while (currentChar != null && currentChar!= '\uffff') {
            if (currentChar == '#') {
                System.out.println("<" + row + " ," + col + ", #>");
                pre_mark.add(new mark(row, col, "#"));
                next_line();
                continue;
            }
            if (currentChar == '/') {
                getChar();
                if (currentChar == '/') {
                    System.out.println("<" + row + " ," + (col - 1) + ", //>");
                    pre_mark.add(new mark(row, col - 1, "/"));
                    next_line();
                    continue;
                }
                if (currentChar == '*') {
                    System.out.println("<" + row + " ," + (col - 1) + ", /*>");
                    pre_mark.add(new mark(row, col - 1, "*"));
                    move_to("*/", null);
                    System.out.println("<" + row + " ," + col + ", */>");
                    pre_mark.add(new mark(row, col, "*"));
                }
            }
            if (currentChar == '\"') {
                System.out.println("<" + row + " ," + col + ", \">");
                pre_mark.add(new mark(row, col, "\""));
            }
            getChar();
            move();
        }
        load_src_file("source.c");
    }
    // 扫描
    void scanWord() throws Exception {
        while (currentChar == '_' || Character.isDigit(currentChar)
                || Character.isAlphabetic(currentChar))
        {
            strBuffer += currentChar;
            getChar();
        }
        move();

        int kw_index = match_keyw(strBuffer);
        if (kw_index == -1)
        {
            int id_index = match_identifier(strBuffer, true);
            System.out.println(strBuffer + "\t < 标识符, " + id_index + ">");
            tokens.add(new token(strBuffer,"标识符", id_index, row, col));
            strBuffer = "";
            return;
        }
        System.out.println(strBuffer + "\t < " + strBuffer + ", -->");
        tokens.add(new token(strBuffer, "关键字", -1, row, col));
        strBuffer = "";
    }
    void scanNum() throws Exception {
        while (Character.isDigit(currentChar)
                || currentChar == '.' && strBuffer.indexOf('.') == -1)
        {
            strBuffer += currentChar;
            getChar();
        }
        //getChar();
        if (may_deli(currentChar).size() > 0 || currentChar == ' ') {
            int const_index = match_constant(strBuffer, true);
            System.out.println(strBuffer + "\t < 常数, " + const_index + ">");
            tokens.add(new token(strBuffer,"常数", const_index, row, col));
        }
        else
            raiseError("非法标识");
        strBuffer = "";
        move();
    }
    void scanOpr_Deli(ArrayList<String> may, Boolean type) throws Exception {
        // type 0-op 1-deli
        strBuffer += currentChar;
        while ( may_opr(currentChar).size() > 0
                || may_deli(currentChar).size() > 0) {
            getChar();
            String buff = strBuffer;
            buff += currentChar;
            ArrayList<String> s = begin_with(may, buff);
            if (s.size() == 0) {// 匹配结束
                if (may.indexOf(strBuffer) < 0) {
                    raiseError(null);
                    break;
                }
                else {
                    System.out.println(strBuffer + "\t < " + strBuffer + ", -->");
                    if (type)
                        tokens.add(new token(strBuffer, "界符", -1, row, col));
                    else
                        tokens.add(new token(strBuffer, "运算符", -1, row, col));
                    strBuffer = "";
                    move();
                    break;
                }
            }
            strBuffer = buff;
            may = s;
        }
    }
    // 查询
    Integer match_keyw(String kw){
        return keyw.indexOf(kw);
    }
    Integer match_identifier(String id, Boolean add) {
        int i = identifier.indexOf(id);
        if (add == null)
            add = true;
        if (i == -1 && add == true) {
            identifier.add(id);
            return identifier.size() - 1;
        }
        return i;
    }
    Integer match_constant(String cons, Boolean add) {
        int i = identifier.indexOf(cons);
        if (add == null)
            add = true;
        if (i == -1 && add == true) {
            constant.add(cons);
            return constant.size() - 1;
        }
        return i;
    }
    ArrayList<String> may_opr(char op){
        return begin_with(opr, String.valueOf(op));
    }
    ArrayList<String> may_deli(char deli){ return begin_with(delimiter, String.valueOf(deli));}
    // 工具方法
    ArrayList<String> begin_with(ArrayList<String> v, String s){
        ArrayList<String> l = new ArrayList<>();
        for (String item : v) {
            if (s.length() > item.length())
                continue;
            Boolean flag = true;
            for (int i = 0; i < s.length(); i++)
                if (item.charAt(i) != s.charAt(i)) {
                    flag = false;
                    break;
                }
            if (flag)
                l.add(item);
        }
        return l;
    }
    Character getChar() throws Exception {
        currentChar = (char) src.read();
        if (currentChar == '\r') {
            getChar();
        }
        else if (currentChar == '\n'){
            row += 1;
            col = 1;
            src.mark(65535);
        }

        else if (currentChar == '\t')
            col += 4;
        else
            col += 1;
        return currentChar;
    }
    void move() throws Exception{
        while (currentChar == null
                ||currentChar == ' '
                || currentChar == '\n'
                || currentChar == '\t')
            getChar();
    }
    String move_to(Integer r, Integer c) throws Exception {
        String str_buffer = "";
        str_buffer += currentChar;

        if (r == row && c < col || r < row)
            return null; // 目前仅支持向后移动
        while (row == r && col < c || row < r)
            str_buffer += getChar();
        return str_buffer;
    }
    String move_to(String w, String str_buffer) throws Exception {
        if (str_buffer == null)
            str_buffer = "";
        if (currentChar == null)
            getChar();
        str_buffer += currentChar;
        getChar();
        while (currentChar != w.charAt(0) && currentChar != null)
            str_buffer += getChar();
        if (currentChar == null)
            return str_buffer;
        for (int i = 1; i < w.length(); i++)
            if (getChar() != w.charAt(i))
            {
                str_buffer += currentChar;
                move_to(w, str_buffer);
            }
        return str_buffer;
    }
    String next_line() throws Exception {
        String s = src.readLine();
        row += 1;
        move();
        getChar();
        return s;
    }
    // 分析
    public void analyze() throws Exception {
        if (src == null) {
            System.out.println("未打开源文件");
        }
        getChar();
        move();
        ArrayBlockingQueue<mark> _pre_mark = pre_mark;
        mark top_mark = _pre_mark.peek();

        while (currentChar != null && currentChar!= '\uffff' ) {
            if (top_mark != null && row == top_mark.getRow() && col == top_mark.getCol()) {
                // 处理预编译信息
                String c = top_mark.getSymble();
                if (c == "#" || c == "/") {
                    next_line();
                    move();
                }
                else if (c == "*" || c == "\"") {
                    _pre_mark.poll();
                    top_mark = _pre_mark.peek();
                    if (c == "*" && top_mark.getSymble() != "*"
                            || c == "\"" && top_mark.getSymble() != "\"")
                        raiseError("符号不匹配");
                    move_to(top_mark.getRow(), top_mark.getCol());
                    getChar();
                    move();
                }
                _pre_mark.poll();
                if (!_pre_mark.isEmpty())
                    top_mark = _pre_mark.peek(); //更新top
                continue;
            }
            if (Character.isAlphabetic(currentChar) || currentChar == '_')
                scanWord();
            else if (Character.isDigit(currentChar))
                scanNum();
            else if (may_opr(currentChar).size() > 0)
                scanOpr_Deli(may_opr(currentChar), false);
            else if (may_deli(currentChar).size() > 0)
                scanOpr_Deli(may_deli(currentChar), true);
            else {
                raiseError("非法标识");
                break;
            }
            if (exception)
                break;
        }
    }
    String raiseError(String msg) throws IOException {
        src.reset();
        errorMsg += src.readLine().replace("\t", "    ") + '\n';
        for (int i=1; i < col - 1; i++)
            errorMsg += ' ';
        errorMsg += "^\n";
        errorMsg += "词法错误：(" + row + ", " + col + ") " + msg;
        System.out.println(errorMsg);
        exception = true;
        return errorMsg;
    }

    public ArrayList<token> getTokens() {
        return tokens;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public static void main(String[] args) throws Exception {
        lexical l = new lexical();
        l.load_keyw();
        l.load_opr();
        l.load_delimiter();
        System.out.println("加载文件...");
        l.load_src_file("source.c");
        //System.out.println(l.next_line());
        //System.out.println(l.row + ", " + l.col + l.currentChar)
        System.out.println("预处理...");
        l.scan_script();
        System.out.println("词法分析...");
        l.analyze();
        for (lexical.token token : l.getTokens())
            System.out.println(token.toString());
        if (l.getErrorMsg() != null)
            System.out.println(l.getErrorMsg());
        //System.out.println(l.errorMsg);
    }

    public class token {
        private String str;
        private String type;
        private Integer pos;
        private Integer row;
        private Integer col;

        token(String s, String t, Integer p, Integer r, Integer c){
            str = s;
            type = t;
            pos = p;
            row = r;
            col = c;
        }

        public String getType() {
            return type;
        }
        public Integer getPos() {
            return pos;
        }
        public String getStr() {
            return str;
        }

        @Override
        public String toString() {
            if (pos > 0)
                return str + "\t< " + str + ", " + pos + ">\t" + type + "\t( " + row + ", " + col + ")\n";
            return str + "\t< " + str + ", " + "-->\t" + type + "\t( " + row + ", " + col + ")\n";
        }
    }
    class mark {
        Integer row;
        Integer col;
        String symble;

        mark(Integer r, Integer c, String s) {
            row = r;
            col = c;
            symble = s;
        }

        public Integer getRow() {
            return row;
        }

        public Integer getCol() {
            return col;
        }

        public String getSymble() {
            return symble;
        }
    }
}

