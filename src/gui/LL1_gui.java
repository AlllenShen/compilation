package gui;

import core.LL1;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;


public class LL1_gui {
    private JPanel root;
    private JTextArea input;
    private JButton run;
    private JTextArea out;
    private JPanel inPanel;
    private JPanel outPanel;
    private JScrollPane inScroll;
    private LL1 l;

    LL1_gui(){
        root.setPreferredSize(new Dimension(800, 600));
        outPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        inPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        input.setLineWrap(true);
        String[] col = {"步骤", "分析栈", "输入串", "使用产生式", "动作"};

        run.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                out.setText("");
                try {
                    l = new LL1(input.getText());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                out.append("----------------构造FIRST集-----------------\n");
                out.append(l.showFirst());
                out.append("----------------构造FOLLOW集-----------------\n");
                out.append(l.showFollow());
                out.append("----------------构造分析表-----------------\n");
                out.append(l.showM());
                out.append("----------------分析-----------------\n");
                for (String s : col)
                    out.append(s + '\t');
                out.append("\n");
                int i = 0;
                while (!l.isFinish()) {
                    out.append(i + "\t");
                    out.append(l.showAnalyzeStack() + '\t');
                    out.append(l.getStr() + '\t');
                    out.append(l.getUseP() + '\t');
                    out.append(l.getAction() + '\t');
                    out.append("\n");
                    i++;
                    l.analyze();
                }
                out.append("分析完成");
                }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("LL1_gui");
        frame.setContentPane(new LL1_gui().root);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
