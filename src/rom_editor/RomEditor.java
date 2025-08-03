package rom_editor;

import javax.swing.SwingUtilities;
import rom_editor.ui.MainFrame;

public class RomEditor {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}
