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
    private ArrayList<Token> nontermTokens;
    private ArrayList<Token> termTokens;
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
            nontermTokens.add(new Token(tw, false));
        lineBuffer = token_f.readLine();
        for (String tw : lineBuffer.split(" "))
            termTokens.add(new Token(tw, true));
        BufferedReader produ_f = new BufferedReader(new FileReader("productions.txt"));
        while ((lineBuffer = produ_f.readLine()) != null)
            productions.add(new Production(lineBuffer.split("->")[0], lineBuffer.split("->")[1]));
        for (Production p : productions){
            for (Token t : nontermTokens)
                if (t.equals(p.left)){
                    t.setGenerate(p.right);
                    continue;
                }
            for (Token t : termTokens)
                if (t.equals(p.left))
                    t.setGenerate(p.right);
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
    Token getToken(String s){
        for (Token t : nontermTokens)
            if (t.getName().equals(s))
                return t;
        for (Token t : termTokens)
            if (t.getName().equals(s))
                return t;
        return null;
    }

    public static void main(String[] args) throws IOException {
        grammar g = new grammar();
        g.nontermTokens.get(1).follow();
    }
    class Token { //标识符
        private String name;
        private Boolean terminal;
        private ArrayList<Statement> generate;
        private ArrayList<Token> FIRST;
        private ArrayList<Token> FOLLOW;
        ArrayList<Token> first(){
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
                    if (index == s.s.size() - 1 || s.s.get(index+1).inFIRST("ε"))
                        FOLLOW.addAll(p.left.follow());
                    else
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

        public void setGenerate(ArrayList<Statement> generate) {
            this.generate = generate;
        }

        public String getName() {
            return name;
        }
    }
    class Statement { // 语句
        ArrayList<Token> s;

        Statement(ArrayList<Token> s_){
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
        Integer hasToken(String t_str){
            for (Token t : s)
                if (t.name.equals(t_str))
                    return s.indexOf(t);
            return -1;
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            for (Token t : s)
                str.append(t.getName());
            return str.toString();
        }
    }
    class Production { // 产生式
        Token left;
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

        public Token getLeft() {
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