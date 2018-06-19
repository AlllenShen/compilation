package core;

        import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;

        import java.io.*;
        import java.util.*;

public class LR1 {
    private String str;
    private Stack<Integer> status;
    private Stack<Grammar.Token> analyzeStack;
    private LR1Grammar g;
    private int strP;
    private Exception error;
    private String action;
    private boolean finish;
    private String useP;

    public LR1(String s) throws IOException {
        str = s + "#";
        g = new LR1Grammar();
        strP = 0;
        action = "初始化";
        useP = "";
        finish = false;
        analyzeStack = new Stack<>();
        analyzeStack.push(g.getToken("#"));
        status = new Stack<>();
        status.push(0);
    }

    public void analyze(){
        LR1Grammar.Status s = g.ACTION[status.peek()][g.indexOftermT(String.valueOf(str.charAt(strP)))];
        if (s.getStatus() == 's'){
            // 移进
            analyzeStack.push(g.getToken(String.valueOf(str.charAt(strP))));
            status.push(s.getTarget());
            action = "移进";
            next();
        }
        else if (s.getStatus() == 'r'){
            // 归约
            Grammar.Production p = g.getProductionAt(s.getTarget());
            // 暂时不支持多个右部的产生式 懒得写
            for (int i =0; i < p.right.get(0).size(); i++){
                analyzeStack.pop();
                status.pop();
            }
            analyzeStack.push(p.left);
            status.push(g.GOTO[status.peek()][g.indexOfnonT(p.left.getName())]);
            action = "归约 使用：" + p.toString();
        }
        else if (s.getStatus() == 'a'){
            action = "完成";
            finish = true;
        }
        else {
            action = "语法错误";
            finish = true;
        }
    }
    public boolean isFinish() {
        return finish;
    }
    public String getStatus(){
        String s = "";
        Stack<Integer> temp = (Stack<Integer>) status.clone();
        while (!temp.empty()){
            s += temp.peek();
            temp.pop();
        }
        return new StringBuffer(s).reverse().toString();
    }
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
    public String getC(){return g.showC();}
    public String getACTION() {return g.showACTION();}
    public String getGOTO() {return g.showGOTO();}
}

class LR1Grammar extends Grammar {
    private Set<Project> projects; // 项目
    private Set<LR1Project> LR1Projects; // LR1项目
    private Set<LR1ProjectSet> C; // 项目集族
    protected Status[][] ACTION;
    protected int[][] GOTO;
    private Token initToken; // 初始化状态符号

    public LR1Grammar() throws IOException{
        super();
        projects = new HashSet<>();
        LR1Projects = new HashSet<>();
        C = new HashSet<>();
        initToken = getToken("E");

        geneProjects();
        LR1ProjectSet i0 = new LR1ProjectSet(0, new HashSet<>());
        i0.add(new LR1Project(productions.get(0).left.toString(),
                productions.get(0).right.get(0).toString(), 0, "#"));
        i0.closure();
        C.add(i0);
        geneC();

        ACTION = new Status[C.size()][termTokens.size()];
        GOTO = new int[C.size()][nontermTokens.size()];

//        geneACTIONandGOTO();
        System.out.println(showC());
        geneACTIONandGOTO();
        System.out.println(showACTION());
        System.out.println(showGOTO());
    }

    String showC(){
        String s = "";
        ArrayList<LR1ProjectSet> temp = new ArrayList<>();
        for (int i = 0; i < C.size(); i++) // 排序
            for (LR1ProjectSet set : C)
                if (set.id == i)
                    temp.add(set);
        for (LR1ProjectSet set : temp){
            s += "I" + set.id + ":" + '\t';
            for (Map.Entry<Token, LR1ProjectSet> entry : set.map.entrySet())
                s += entry.getKey().toString() + "=>" + "I" + entry.getValue().id + '\t';
            s += '\n';
            for (LR1Project p : set.projects)
                s += "\t" + p.toString() + '\n';
        }
        return s;
    }

    String showACTION(){
        String s = "\t";
        for (Token t : termTokens)
            s += t.toString() + '\t';
        s += '\n';
        for (int i = 0; i < C.size(); i++){
            s += String.valueOf(i) + '\t';

            for (int j = 0; j < termTokens.size() - 1; j++) {
                if (ACTION[i][j] != null)
                    s += ACTION[i][j].toString() + '\t';
                else
                    s += "-" + '\t';
            }
            s += '\n';
        }
        return s;
    }

    String showGOTO(){
        String s = "\t";
        for (Token t : nontermTokens)
            s += t.toString() + '\t';
        s += '\n';
        for (int i = 0; i < C.size(); i++){
            s += String.valueOf(i) + '\t';
            for (int j = 0; j < nontermTokens.size() - 1; j++)
                s += String.valueOf(GOTO[i][j])  + '\t';
            s += '\n';
        }
        return s;
    }

    Set<Project> geneProjects(){
        for (Production p : productions)
            for (Statement s : p.right)
                for (int i=0; i <= s.s.size(); i++)
                    projects.add(new Project(p.left.toString(), s.toString(), i));
        return projects;
    }

    LR1ProjectSet getInitSet(){
        for (LR1ProjectSet set : C){
            if (set.id != 0)
                continue;
            return set.map.get(initToken);
        }
        throw new ValueException("没有I0集");
    }

    void geneACTIONandGOTO(){
        for (LR1Project p : LR1Projects){
            ArrayList<LR1ProjectSet> sets = getLR1Setswith(p);
            Token behind = p.get(0, p.project.dotPos);
            if (behind == null)
                for (LR1ProjectSet set : sets) {
                    ACTION[set.id][indexOftermT(p.forward.getName())] =
                            new Status('r', indexOfProduction(p.project));
                    Set<LR1Project> go = set.go(p.project.left);
                }
            else if (behind.isTerminal()){
                for (LR1ProjectSet set : sets){
                    Set<LR1Project> go = set.go(behind);
                    LR1ProjectSet ij = getLR1Setwith(go);
                    if (ij != null) {
                        ACTION[set.id][indexOftermT(behind.getName())] =
                                new Status('s', ij.id);
                    }
                }
            }
        }
        // 处理goto
        for (LR1ProjectSet set : C)
            for (Map.Entry<Token, LR1ProjectSet> entry : set.map.entrySet())
                if (!entry.getKey().isTerminal())
                    GOTO[set.id][indexOfnonT(entry.getKey().getName())] = entry.getValue().id;
        LR1ProjectSet initSet = getInitSet();
        ACTION[initSet.id][indexOftermT("#")] = new Status('a', 0);
    }

    ArrayList<LR1ProjectSet> getLR1Setswith(LR1Project p){
        ArrayList<LR1ProjectSet> sets = new ArrayList<>();
        for (LR1ProjectSet i : C)
            if (i.contain(p))
                sets.add(i);
        return sets;
    }

    Set<LR1ProjectSet> geneC(){
        while (true){
            Set<LR1ProjectSet> tempSet = new HashSet<>();
            for (LR1ProjectSet i : C){
                geneNewSet(tempSet, i, nontermTokens);
                geneNewSet(tempSet, i, termTokens);
            }
            C.addAll(tempSet);
            if (tempSet.size() == 0)
                break;
        }
        for (LR1ProjectSet set : C)
            LR1Projects.addAll(set.projects);
        return C;
    }

    private void geneNewSet(Set<LR1ProjectSet> tempSet, LR1ProjectSet i, ArrayList<Token> tokens) {
        for (Token t : tokens){
            if (t.getName().equals("ε"))
                continue;
            Set<LR1Project> go = i.go(t);
            if (go.size() == 0)
                continue;
            LR1ProjectSet newSet = new LR1ProjectSet();
            newSet.id = C.size() + tempSet.size();
            newSet.CLOSURE = newSet.projects = go;
            if (!inC(go)) {
                i.map.put(t, newSet);
                tempSet.add(newSet);
            }
            else
                i.map.put(t, getLR1Setwith(go));
        }
    }

    boolean inC(Set<LR1Project> p){
        for (LR1ProjectSet ps : C) // 对C中每一个项目集
            if (ps.same(p))
                return true;
        return false;
    }

    LR1ProjectSet getLR1Setwith(Set<LR1Project> p){
        for (LR1ProjectSet ps : C) // 对C中每一个项目集
            if (ps.same(p))
                return ps;
        return null;
    }

    public static void main(String[] args) throws IOException, CloneNotSupportedException, ClassNotFoundException {
        LR1Grammar g = new LR1Grammar();

    }

    class Project extends Production {

        private int dotPos; // 分隔符位置 第i个字符后
        Project(){
            super();
            dotPos = -1;
        }
        Project(String l, String r, int dot) {
            super(l, r);
            dotPos = dot;
        }
        Project(Production p, int dot){
            super("#", "#");
            left = p.left;
            right = p.right;
            dotPos = dot;
        }
        int posOf(Token t){
            // 返回t在产生式右部中的索引 没有则为-1
            for (int i = 0; i < right.get(0).size(); i++)
                if (right.get(0).get(i) == t)
                    return i;
            return -1;
        }

        @Override
        public String toString() {
            String s = left.toString() + "->";
            String r = right.get(0).toString();
            for (int i=0; i < r.length(); i++){
                if (i == dotPos)
                    s += '.';
                s += r.charAt(i);
            }
            if (dotPos == r.length())
                s += '.';
            return s;
        }

        public boolean equals(Object obj) {
            if ( this == obj ) return true;
            if ( obj == null || obj.getClass() != this.getClass() ) return false;
            Project that = (Project) obj;
            return that.left.toString().equals(left.toString())
                    && that.right.toString().equals(right.toString())
                    && that.dotPos == dotPos;
        }
    }

    class LR1Project {
        private Project project;
        private Token forward;
        LR1Project(){
            project = new Project();
            forward = new Token();
        }
        LR1Project(String l, String r, int dot, String f){
            project = new Project(l, r, dot);
            forward = getToken(f);
        }
        LR1Project(Project p, String f){
            project = p;
            forward = getToken(f);
        }
        LR1Project newWithDotPos(int i){
            // 返回一个复制的 改变dot位置的对象
            LR1Project p = new LR1Project(new Project(new Production(project.left, project.right.get(0)), i),
                    forward.toString());// this.deepClone();
//            p.project.dotPos = i;
            return p;
        }
        Token get(int i, int j){
            if (i >= project.right.size() || j >= project.right.get(i).size())
                return null;
            return project.right.get(i).get(j);
        }

        @Override
        public String toString() {
            return '<' + project.toString() + ", " +  forward + '>';
        }

        @Override
        public boolean equals(Object obj) {
            try {
                LR1Project p = (LR1Project) obj;
                return project.equals(p.project) && forward.equals(p.forward);
            }catch (java.lang.ClassCastException e){
                return false;
            }
        }

        protected LR1Project deepClone(){
            LR1Project that = new LR1Project();
            that.project = project;
            that.forward = forward;
            return that;
        }
    }

    class LR1ProjectSet {
        private int id;
        private Set<LR1Project> projects;
        private Set<LR1Project> CLOSURE;
        private Map<Token, LR1ProjectSet> map;

        LR1ProjectSet(){
            id = -1;
            projects = new HashSet<>();
            CLOSURE = new HashSet<>();
            map = new HashMap<>();
        }

        LR1ProjectSet(int i, Set<LR1Project> p){
            id = i;
            projects = CLOSURE = p;
            map = new HashMap<>();
        }
        void add(LR1Project p){
            projects.add(p);
        }
        Set<LR1Project> closure(){
//            int before;
            while (true) {
                Set<LR1Project> tempSet = new HashSet<>();
                for (LR1Project p : CLOSURE) {
                    Token behind = p.project.right.get(0).get(p.project.dotPos); // 活前缀后第一个token
                    if (behind.isTerminal())
                        continue;
                    ArrayList<Token> temp = new ArrayList<>();
                    if (p.project.dotPos + 1 > p.project.right.get(0).size())
                        temp.add(getToken("ε"));
                    else
                        temp.add(p.project.right.get(0).get(p.project.dotPos + 1));
                    temp.add(p.forward);
                    Statement s = new Statement(temp);
                    s.first();
                    for (Production pr : productions) {
                        if (!pr.left.getName().equals(behind.getName()))
                            continue;
                        for (Token t : s.first()) {
                            if (!t.isTerminal() || t.getName().equals("ε"))
                                continue;
                            if (!inClosure(new Project(pr, 0), t.toString()))
                                tempSet.add(new LR1Project(new Project(pr, 0), t.toString()));
                        }
                    }
                }
                CLOSURE.addAll(tempSet);
                if (tempSet.size() == 0)
                    return CLOSURE;
            }
        }
//        void _closure(){
//            // 遍历现有CLOSURE 用于重复调用
//            CLOSURE.addAll(projects);
//            for (LR1Project p : CLOSURE){
//                Token behind = p.project.right.get(0).get(p.project.dotPos); // 活前缀后第一个token
//                if (behind.isTerminal())
//                    continue;
//                ArrayList<Token> temp = new ArrayList<>();
//                if (p.project.dotPos + 1 >= p.project.right.get(0).size())
//                    temp.add(getToken("ε"));
//                else
//                    temp.add(p.project.right.get(0).get(p.project.dotPos + 1));
//                temp.add(p.forward);
//                Statement s = new Statement(temp);
//                s.first();
//                for (Production pr : productions){
//                    if (!pr.left.getName().equals(behind.getName()))
//                        continue;
//                    for (Token t : s.first()){
//                        if (!t.isTerminal() || t.getName().equals("ε"))
//                            continue;
//                        if (!inClosure(new Project(pr, 0), t.toString()))
//                            projects.add(new LR1Project(new Project(pr, 0), t.toString()));
//                    }
//                }
//            }
//        }

        Set<LR1Project> go(Token t){
            Set<LR1Project> s = new HashSet<>();
            for (LR1Project p : projects)
                if (p.project.right.get(0).get(p.project.dotPos).equals(t)){
                    // ·X
                    LR1Project newp = p.newWithDotPos(p.project.dotPos + 1);
                    if (!inSet(s, newp))
                        s.add(newp);
                }
            LR1ProjectSet ps = new LR1ProjectSet(-1, s);
            return ps.closure();
        }

        boolean inSet(Set<LR1Project> set, LR1Project value){
            for (LR1Project p : set)
                if (p.equals(value))
                    return true;
            return false;
        }

        boolean inClosure(Project pr, String f){
            if (pr == null)
                throw new NullPointerException();
            for (LR1Project p : CLOSURE)
                if (p.project.equals(pr) && p.forward.toString().equals(f))
                    return true;
            return false;
        }

        boolean same(Set<LR1Project> p){
            if (p.size() != projects.size())
                return false;
            for (LR1Project p1 : p) {
                if (this.contain(p1))
                    continue; // 如果包含continue 如果不包含 直接return false
                return false;
            }
            return true;
        }

        boolean contain(LR1Project p){
            for (LR1Project p1 : projects)
                if (p1.equals(p))
                    return true;
            return false;
        }


    }

    static class Status{
        private char status; // a s r
        private int to;

        public Status(char s, int t){
            if (s != 'a' && s != 's' && s != 'r')
                throw new ValueException("错误的状态名");
            status = s;
            to = t;
        }

        public int getTarget() {
            return to;
        }

        public char getStatus() {
            return status;
        }

        @Override
        public String toString() {
            if (status == 'a')
                return "acc";
            return status + String.valueOf(to);
        }
    }
}
