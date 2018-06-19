package gui;

import core.LR1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class LR1_gui {
    private JPanel root;
    private JButton run;
    private JPanel inPanel;
    private JPanel outPanel;
    private JScrollPane input;
    private LR1 l;
    LR1_gui() {
        root.setPreferredSize(new Dimension(800, 600));
        outPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        inPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        in.setLineWrap(true);

        String[] col = {"步骤", "状态栈", "分析栈", "输入串", "动作"};

        run.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                out.setText("");
                try {
                    l = new LR1(in.getText());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                out.append("------------构造LR1项目集族------------\n");
                out.append(l.getC());
                out.append("------------构造分析表------------\n");
                out.append("ACTION\n");
                out.append(l.getACTION());
                out.append("-----------------------------\n");
                out.append("GOTO\n");
                out.append(l.getGOTO());
                out.append("------------分析------------\n");
                for (String s : col)
                    out.append(s + '\t');
                out.append("\n");
                int i = 0;
                while (!l.isFinish()) {
                    out.append(i + "\t");
                    out.append(l.getStatus() + '\t');
                    out.append(l.showAnalyzeStack() + '\t');
                    out.append(l.getStr() + '\t');
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
        JFrame frame = new JFrame("LR1_gui");
        frame.setContentPane(new LR1_gui().root);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private JScrollPane ouput;
    private JTextArea in;
    private JTextArea out;
}
