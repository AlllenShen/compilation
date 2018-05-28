package core;

import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class LL1 {
}

class grammar {
    private ArrayList<token> nontermTokens;
    private ArrayList<token> termTokens;
    private ArrayList<Production> productions;

    grammar() throws IOException {
        nontermTokens = new ArrayList<>();
        termTokens = new ArrayList<>();
        productions = new ArrayList<>();
        load_resources();
    }

    void load_resources() throws IOException {
        BufferedReader token_f = new BufferedReader(new FileReader("tokens.txt"));
        String lineBuffer = token_f.readLine();
        for (String tw : lineBuffer.split(" "))
            nontermTokens.add(new token(tw, false));
        lineBuffer = token_f.readLine();
        for (String tw : lineBuffer.split(" "))
            termTokens.add(new token(tw, true));
        BufferedReader produ_f = new BufferedReader(new FileReader("productions.txt"));
        while ((lineBuffer = produ_f.readLine()) != null)
            productions.add(new Production(lineBuffer.split("->")[0], lineBuffer.split("->")[1]));
        for (Production p : productions){
            for (token t : nontermTokens)
                if (t.equals(p.left)){
                    t.setGenerate(p.right);
                    continue;
                }
            for (token t : termTokens)
                if (t.equals(p.left))
                    t.setGenerate(p.right);
        }
    }
    Boolean isTokens(String c) {
        for (token t : nontermTokens)
            if (t.getName().equals(c))
                return true;
        for (token t : termTokens)
            if (t.getName().equals(c))
                return true;
        return false;
    }
    token getToken(String s){
        for (token t : nontermTokens)
            if (t.getName().equals(s))
                return t;
        for (token t : termTokens)
            if (t.getName().equals(s))
                return t;
        return null;
    }

    public static void main(String[] args) throws IOException {
        grammar g = new grammar();
//        for (token t : g.nontermTokens.get(0).first())
//            System.out.println(t);
        ArrayList<token> a = g.nontermTokens.get(1).first();
    }
    class token{ //标识符
        private String name;
        private Boolean terminal;
        private ArrayList<Statement> generate;
        private ArrayList<token> FIRST;
        private ArrayList<token> FOLLOW;
        ArrayList<token> first(){
            if (FIRST.size() > 0)
                return FIRST;
            if (terminal) {
                FIRST.add(this);
                return FIRST;
            }
            int i = 0;
            for (Statement state : generate){ // 算法初步测试通过 需要其他测试用例
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

            return FIRST;
        }

        ArrayList<String> follow(){
            return null;
        }

        token(String n, Boolean t, ArrayList<Statement> g) {
            name = n;
            terminal = t;
            generate = g;
        }
        token(String n, Boolean t) {
            name = n;
            terminal = t;
            generate = new ArrayList<>();
            FIRST = new ArrayList<>();
            FOLLOW = new ArrayList<>();
        }

        public void setGenerate(ArrayList<Statement> generate) {
            this.generate = generate;
        }

        public String getName() {
            return name;
        }
    }

    class Statement { // 语句
        ArrayList<token> s;

        Statement(ArrayList<token> s_){
            s = s_;
        }
        Statement(String s_){
            s = new ArrayList<>();
            for (int i = 0; i < s_.length() ; i++) {
                if (isTokens(String.valueOf(s_.charAt(i))))
                    s.add(getToken(String.valueOf(s_.charAt(i))));
                else
                    throw new ValueException("标识符不存在:" + s_.charAt(i));
            }

        }
        token firstToken(){
            return s.get(0);
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            for (token t : s)
                str.append(t.getName());
            return str.toString();
        }
    }

    class Production { // 产生式
        token left;
        ArrayList<Statement> right;

        Production(String l, String r) {
            if (!isTokens(l))
                throw new ValueException("标识符错误: " + l);
            right = new ArrayList<>();
            ArrayList<String> r_strings = new ArrayList<>();
            if (r.contains("|"))
                for (String state : r.split("\\|"))
                    right.add(new Statement(state));
            else
                right.add(new Statement(r));
            left = getToken(l);
        }

        public token getLeft() {
            return left;
        }

        @Override
        public String toString() {
            String s = left.getName() + "->";
            for (Statement st : right)
                s += st + ",";
            return s;
        }
    }

}