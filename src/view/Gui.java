package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.filechooser.FileNameExtensionFilter;
import rom_editor.ui.MainFrame;

public class Gui {
    public static void main(String[] args) {
        JFrame jf = new JFrame();
        jf.setResizable(false);
        jf.setSize(1000,750);
        jf.setTitle("rom to bitmap");
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setLayout(null);

        JLabel fileDic = new JLabel("none");
        fileDic.setBounds(50, 25, 775, 20);
        fileDic.setOpaque(true);
        fileDic.setBackground(new Color(240, 240, 240));
        fileDic.setForeground(Color.BLACK);
        fileDic.setBorder(BorderFactory.createRaisedBevelBorder());
        jf.add(fileDic);

        JButton selFile = new JButton("Select File");
        selFile.setBounds(850,25, 100, 20);

        JButton romEditBtn = new JButton("Edit in ROM Editor");
        romEditBtn.setBounds(850, 55, 150, 20);
        romEditBtn.setEnabled(false);

        final String[] selectedFilePath = {null};

        selFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter("Rom files", "bin");
                fileChooser.setFileFilter(filter);
                int result = fileChooser.showOpenDialog(jf);

                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    String Location = selectedFile.getAbsolutePath(); 
                    fileDic.setText(Location);
                    selectedFilePath[0] = Location;
                    romEditBtn.setEnabled(true);
                }
            }
        });

        romEditBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (selectedFilePath[0] != null) {
                    SwingUtilities.invokeLater(() -> {
                        MainFrame mainFrame = new MainFrame();
                        mainFrame.setVisible(true);
                        mainFrame.openFileDialogWithPath(selectedFilePath[0]);
                    });
                }
            }
        });

        jf.add(selFile);
        jf.add(romEditBtn);
        jf.setVisible(true);
    }
}