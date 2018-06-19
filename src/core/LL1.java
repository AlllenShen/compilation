package core;

import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;

import javax.xml.stream.FactoryConfigurationError;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Stack;

// i+i-(i*i/(i+(i-i))
public class LL1 {
    private String str;
    private Stack<Grammar.Token> analyzeStack;
    private Grammar g;
    private int strP;
    private Exception error;
    private String action;
    private String useP;
    private boolean finish;
    private Grammar.Token begin;

    public LL1(String s) throws IOException {
        str = s + "#";
        g = new Grammar();
        strP = 0;
        action = "初始化";
        finish = false;
        analyzeStack = new Stack<>();
        begin = g.getToken("E");
        analyzeStack.push(g.getToken("#"));
        analyzeStack.push(begin);
    }

    public boolean analyze(){
        if (analyzeStack.peek().getName().equals("#")
                && str.charAt(strP) == '#') {
            finish = true;
            return true;
        }
        if (analyzeStack.peek().getName().equals(String.valueOf(str.charAt(strP)))
                && str.charAt(strP) != '#'){
            analyzeStack.pop();
            action = "栈顶匹配";
            next();
            finish = false;
            return false;
        }
        if (!analyzeStack.peek().isTerminal()){
            int nonIndex = g.indexOfnonT(analyzeStack.peek().getName());
            int termIndex = g.indexOftermT(String.valueOf(str.charAt(strP)));
            if (termIndex == -1) {
                error = new ValueException("输入字符不匹配");
                action = "error:输入字符不匹配";
                finish = true;
                return true;
            }
            Grammar.Production p = g.M(nonIndex, termIndex);
            if (p == null) {
                error = new ValueException("输入字符不匹配");
                action = "error:输入字符不匹配";
                finish = true;
                return true;
            }
            analyzeStack.pop();
            useP = p.toString();
            String s = "";
            for (int i=p.right.get(0).s.size() - 1; i >= 0 ; i--) {
                Grammar.Token t = p.right.get(0).s.get(i);
                if (!t.getName().equals("ε"))
                    analyzeStack.push(t);
                s += t.getName();
            }
            action = String.format("POP(), PUSH(%s)", s);
            finish = false;
            return false;
        }
        finish = true;
        return true;
    }

    public boolean isFinish() {
        return finish;
    }

    String next(){
        strP += 1;
        return String.valueOf(str.charAt(strP));
    }
    public String getStr() {
        String s = "";
        for (int i = strP; i < str.length(); i++)
            s += str.charAt(i);
        return s;
    }

    public String showFirst(){
        return g.getFIRS();
    }
    public String showFollow(){
        return g.getFOLLOW();
    }
    public String showM(){return g.getM();}
    public String showAnalyzeStack(){
        String s = "";
        Stack<Grammar.Token> temp = (Stack<Grammar.Token>) analyzeStack.clone();
        while (!temp.empty()){
            s += temp.peek();
            temp.pop();
        }
        return new StringBuffer(s).reverse().toString();
    }

    public String getAction() {
        return action;
    }

    public String getUseP() {
        return useP;
    }
}

class Grammar {
    protected ArrayList<Token> nontermTokens;
    protected ArrayList<Token> termTokens;
    protected ArrayList<Production> productions;
    private Production[][] M;

    Grammar() throws IOException {
        nontermTokens = new ArrayList<>();
        termTokens = new ArrayList<>();
        productions = new ArrayList<>();
        load_resources();
        M = new Production[nontermTokens.size()][termTokens.size()];
        geneM();
    }

    void load_resources() throws IOException {
        BufferedReader token_f = new BufferedReader(new FileReader("tokens.txt"));
        String lineBuffer = token_f.readLine();
        for (String tw : lineBuffer.split(" "))
            nontermTokens.add(new Token(tw, false));
        lineBuffer = token_f.readLine();
        for (String tw : lineBuffer.split(" "))
            termTokens.add(new Token(tw, true));
        BufferedReader produ_f = new BufferedReader(new FileReader("productions.txt"));
        while ((lineBuffer = produ_f.readLine()) != null){
            String left = lineBuffer.split("->")[0];
            String right = lineBuffer.split("->")[1];
            productions.add(new Production(lineBuffer.split("->")[0], lineBuffer.split("->")[1]));
        }
        out: for (Production p : productions){
            for (Token t : nontermTokens) {
                if (t.toString().equals(p.left.toString())) {
                    for (Statement s : p.right)
                        t.generate.add(new Statement(s.s));
                    continue out;
                }
            }
            for (Token t : termTokens)
                if (t.equals(p.left))
                    for (Statement s : p.right)
                        t.generate.add(new Statement(s.s));
        }
        for (Token t : nontermTokens) {
            t.first();
            t.follow();
        }
        for (Token t : termTokens) {
            t.first();
            t.follow();
        }
    }
    Boolean isTokens(String c) {
        for (Token t : nontermTokens)
            if (t.getName().equals(c))
                return true;
        for (Token t : termTokens)
            if (t.getName().equals(c))
                return true;
        return false;
    }
    public Token getToken(String s){
        for (Token t : nontermTokens)
            if (t.getName().equals(s))
                return t;
        for (Token t : termTokens)
            if (t.getName().equals(s))
                return t;
        return null;
    }
    void geneM(){
        for (Production p : productions)
            for (Statement s : p.right){
                    s.first();
                    for (Token termT : termTokens) {
                        if (termT.terminal && s.FIRST.contains(termT)
                                && !termT.name.equals("#") && !termT.name.equals("ε"))
                            M[nontermTokens.indexOf(p.left)][termTokens.indexOf(termT)] =
                                    new Production(p.left, s);
                        if (s.FIRST.contains(getToken("ε")) && termT.name.equals("#"))
                            M[nontermTokens.indexOf(p.left)][termTokens.indexOf(termT)] =
                                    new Production(p.left, new Statement("ε"));
                    }
                    if (s.FIRST.contains(getToken("ε")))
                        for (Token b : p.left.FOLLOW)
                            if (!b.name.equals("#") && !b.name.equals("ε"))
                                M[nontermTokens.indexOf(p.left)][termTokens.indexOf(b)] =
                                        new Production(p.left, s);

            }
    }
    void showM(){
        System.out.printf("%-24s", ' ');
        for (Token t : termTokens)
            System.out.printf("%-24s", t);
        System.out.println();
        for (int i=0; i < nontermTokens.size(); i++){
            System.out.printf("%-24s", nontermTokens.get(i));
            for (int j=0; j < termTokens.size(); j++) {
                if (M[i][j] != null)
                    System.out.printf("%-24s", M[i][j]);
                else
                    System.out.printf("%-24s", '-');
            }
            System.out.println();
        }

    }
    String getM(){
        String s = "";
        s += '\t'; // String.format("%-24s", "");
        for (Token t : termTokens)
            s += t.toString() + '\t'; // String.format("%-24s", t);
        s += '\n';
        System.out.println();
        for (int i=0; i < nontermTokens.size(); i++){
            s += nontermTokens.get(i).toString() + '\t'; // String.format("%-24s", nontermTokens.get(i));
            for (int j=0; j < termTokens.size(); j++) {
                if (M[i][j] != null)
                    s += M[i][j].toString() + '\t'; // String.format("%-24s", M[i][j]);
                else
                    s += "-\t"; // String.format("%-24s", '-');
            }
            s += '\n';
        }
        return s;
    }
    int indexOfnonT(String s){
        for (int i=0; i < nontermTokens.size(); i++)
            if (nontermTokens.get(i).name.equals(s))
                return i;
        return -1;
    }
    int indexOftermT(String s){
        for (int i=0; i < termTokens.size(); i++)
            if (termTokens.get(i).name.equals(s))
                return i;
        return -1;
    }
    int indexOfProduction(Production p){
        for (int i = 0; i < productions.size(); i++){
            Production pr = productions.get(i);
            if (p.same(pr))
                return i;
            else
                for (Statement s : pr.right)
                    if (p.equals(new Production(pr.left, s)))
                        return i;
        }
        return -1;
    }
    Production getProductionAt(int i){
        if (i >= productions.size())
            return null;
        return productions.get(i);
    }
    Production M(int i, int j){
        return M[i][j];
    }
    String getFIRS(){
        String s = "";
        for (Token t : nontermTokens) {
            String first = "";
            for (Token temp : t.FIRST)
                first += temp.toString() + " ";
            s += String.format("FIRST(%s) = { %s }\n", t.name, first);
        }
        return s;
    }
    String getFOLLOW(){
        String s = "";
        for (Token t : nontermTokens) {
            String follow = "";
            for (Token temp : t.FOLLOW)
                follow += temp.toString() + ' ';
            s += String.format("FOLLOW(%s) = { %s }\n", t.name, follow);
        }
        return s;
    }
    public static void main(String[] args) throws Exception {
        Grammar g = new Grammar();
        for (Token t : g.nontermTokens) {
            t.showFIRST();
            t.showFOLLOW();
        }
        g.showM();
    }

    class Token  { //标识符
        private String name;
        private Boolean terminal;
        private ArrayList<Statement> generate;
        private ArrayList<Token> FIRST;
        private ArrayList<Token> FOLLOW;
        // 构造函数
        Token(){
            name = "";
            terminal = false;
            generate = new ArrayList<>();
            FIRST = new ArrayList<>();
            FOLLOW = new ArrayList<>();
        }
        Token(String n, Boolean t, ArrayList<Statement> g) {
            name = n;
            terminal = t;
            generate = g;
        }
        Token(String n, Boolean t) {
            name = n;
            terminal = t;
            generate = new ArrayList<>();
            FIRST = new ArrayList<>();
            FOLLOW = new ArrayList<>();
        }

        ArrayList<Token> first(){
            if (FIRST.size() > 0)
                return FIRST;
            if (terminal) {
                FIRST.add(this);
                return FIRST;
            }
            for (Statement state : generate){
                int i = 0;
                if (state.get(0).isTerminal()) {
                    FIRST.add(state.get(0));
                    continue;
                }
                try {
                    if (state.s.get(i).equals(this))
                        continue;
                }catch (IndexOutOfBoundsException e){
                    continue;
                }
                FIRST.addAll(state.s.get(i).first());
                //if (FIRST.contains(getToken("ε")))
                // 如果当前项包含空串 向下一个token递归
                // 没有这个判断 好像也没啥问题...
                // 换个用例会不会出问题我也不知道...
                while (state.s.get(i).FIRST.contains(getToken("ε"))) { // 当token包含空串时 向后递归
                    i++;
                    if (i >= state.s.size())
                        break;
                    FIRST.addAll(state.s.get(i).first());
                }
            }
            clean();
            return FIRST;
        }
        ArrayList<Token> follow(){
            if (FOLLOW.size() > 0)
                return FOLLOW;
            FOLLOW.add(getToken("#"));
            for (Production p : productions){
                for (Statement s : p.right){
                    int index = s.hasToken(name);
                    if (index == -1)
                        continue;
                    if (index == s.s.size() - 1 ||
                            index < s.s.size() - 1 && s.s.get(index+1).inFIRST("ε"))
                        // token位于末尾或不位于末尾且下一个token FIRST中有ε
                        FOLLOW.addAll(p.left.follow());
                    if (index < s.s.size() - 1)
                        // token不在末尾
                        FOLLOW.addAll(s.s.get(index+1).first());
                }
            }
            clean();
            return FOLLOW;
        }
        Boolean inFIRST(String s){
            if (FIRST.size() == 0)
                first();
            for (Token t : FIRST)
                if (t.name.equals(s))
                    return true;
            return false;
        }
        Boolean inFOLLOW(String s){
            if (FOLLOW.size() == 0)
                first();
            for (Token t : FOLLOW)
                if (t.name.equals(s))
                    return true;
            return false;
        }
        void clean(){
            for(int i = 0; i < FIRST.size(); i++)
                for(int j = i + 1; j < FIRST.size(); j++)
                    if(FIRST.get(i) == FIRST.get(j)){
                        FIRST.remove(j);
                        j--;
                    }
            for(int i = 0; i < FOLLOW.size(); i++)
                for(int j = i + 1; j < FOLLOW.size(); j++)
                    if(FOLLOW.get(i) == FOLLOW.get(j)){
                        FOLLOW.remove(j);
                        j--;
                    }
        }
        void showFIRST(){
            System.out.print(name + ": ");
            for (Token t : FIRST)
                System.out.print(t);
            System.out.println();
        }
        void showFOLLOW(){
            System.out.print(name + ": ");
            for (Token t : FOLLOW)
                System.out.print(t);
            System.out.println();
        }
        boolean isTerminal(){
            return terminal;
        }

        public void setGenerate(ArrayList<Statement> generate) {
            this.generate = generate;
        }
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            try {
                Token t = (Token) obj;
                return name.equals(t.name) && terminal.equals(t.terminal);
            }catch (java.lang.ClassCastException e){
                return false;
            }
        }
    }
    class Statement { // 语句
        ArrayList<Token> s;
        private ArrayList<Token> FIRST;

        Statement(ArrayList<Token> s_){
            s = s_;
            FIRST = new ArrayList<>();
        }
        Statement(String s_){
            s = new ArrayList<>();
            FIRST = new ArrayList<>();
            for (int i = 0; i < s_.length() ; i++) {
                if (isTokens(String.valueOf(s_.charAt(i))))
                    s.add(getToken(String.valueOf(s_.charAt(i))));
                else
                    throw new ValueException("标识符不存在:" + s_.charAt(i));
            }

        }
        Integer hasToken(String t_str){
            for (Token t : s)
                if (t.name.equals(t_str))
                    return s.indexOf(t);
            return -1;
        }
        ArrayList<Token> first(){
            if (FIRST.size() > 0)
                return FIRST;
            FIRST.addAll(s.get(0).first());
            for (int i = 0; s.get(i).first().contains(getToken("ε")); i++) {
                if (i + 1 >= s.size())
                    break;
                FIRST.addAll(s.get(i + 1).first());
            }
            return FIRST;
        }
        public Token get(int i){
            if (i < 0)
                return null;
            if (i < s.size())
                return s.get(i);
            else
                return getToken("ε");

        }
        public int size(){
            return s.size();
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            for (Token t : s)
                str.append(t.getName());
            return str.toString();
        }

        @Override
        public boolean equals(Object obj) {
            try {
                Statement st = (Statement) obj;
                if (s.size() != st.size())
                    return false;
                for (int i = 0; i < s.size(); i++)
                    if (!s.get(i).equals(st.get(i)))
                        return false;
                return true;
            }catch (ClassCastException e){
                return false;
            }
        }
    }
    class Production { // 产生式
        Token left;
        ArrayList<Statement> right;
        boolean geneFirst;
        boolean geneFollow;

        Production(){
            left = new Token();
            right = new ArrayList<>();
            geneFirst = false;
            geneFollow = false;
        }
        Production(Token l, Statement r){
            left = l;
            right = new ArrayList<>();
            right.add(r);
            geneFirst = false;
            geneFollow = false;
        }
        Production(String l, String r) {
            if (!isTokens(l))
                throw new ValueException("标识符错误: " + l);
            right = new ArrayList<>();
            if (r.contains("|"))
                for (String state : r.split("\\|"))
                    right.add(new Statement(state));
            else
                right.add(new Statement(r));
            left = getToken(l);
            geneFirst = false;
            geneFollow = false;
        }

        public Token getLeft() {
            return left;
        }

        public boolean same(Production p){
            // 用于与子类比较
            if (!left.equals(p.left))
                return false;
            out: for (Statement s : p.right){
                for (Statement st : right)
                    if (st.equals(s))
                        continue out;
                return false; // 没有匹配才会运行
            }
            return true;
        }

        @Override
        public String toString() {
            String s = left.getName() + "->";
            for (Statement st : right) {
                if (right.indexOf(st) == right.size() - 1)
                    s += st;
                else
                    s += st + ",";
            }
            return s;
        }

        @Override
        public boolean equals(Object obj) {
            if ( this == obj ) return true;
            if ( obj == null || obj.getClass() != this.getClass() ) return false;
            Production p = (Production) obj;
            if (!left.equals(p.left))
                return false;
            out: for (Statement s : p.right){
                for (Statement st : right)
                    if (st.equals(s))
                        continue out;
                return false; // 没有匹配才会运行
            }
            return true;
        }
    }
}

//E->TG
//G->+TG|-TG|ε
//T->FS
//S->*FS|/FS|ε
//F->(E)|i|ε

//E->TK
//K->+T|ε
//T->FM
//M->*F|ε
//F->(E)|i

//E T G T S F
//( ) i + - ε * / ε #

//E K T M F

//G->S
//S->BB
//B->aB
//B->b

//E->E+T
//E->T
//T->T*F
//T->F
//F->(E)
//F->i