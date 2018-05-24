package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileWriter;
import java.io.IOException;

import core.lexical;

public class lexical_gui {
    private JTextArea input;
    private JButton run;
    private JTextArea output;
    private JPanel root;
    private JPanel in;
    private JPanel out;

    lexical_gui(){
        root.setPreferredSize(new Dimension(800, 600));
        out.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        in.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        input.setLineWrap(true);
        run.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String src = input.getText();
                try {
                    FileWriter fw = new FileWriter("source.c");
                    fw.write(src);
                    fw.close();
                    lexical l = new lexical();
                    l.load_src_file("source.c");
                    l.pre_handle();
                    l.analyze();
                    for (lexical.token token : l.getTokens())
                        output.append(token.toString());
                    if (l.getErrorMsg() != null)
                        output.append(l.getErrorMsg());
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("lexical_gui");
        frame.setContentPane(new lexical_gui().root);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
