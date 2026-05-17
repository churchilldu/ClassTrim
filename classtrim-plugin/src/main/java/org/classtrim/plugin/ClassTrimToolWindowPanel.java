package org.classtrim.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.ui.table.JBTable;
import org.classtrim.core.engine.RefactoringSuggestion;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClassTrimToolWindowPanel extends JPanel {
    private static final Map<String, ClassTrimToolWindowPanel> PANELS = new ConcurrentHashMap<>();
    private final DefaultTableModel model;

    public ClassTrimToolWindowPanel(Project project) {
        super(new BorderLayout());
        this.model = new DefaultTableModel(new Object[]{"Method", "From", "To"}, 0);
        JBTable table = new JBTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);
        PANELS.put(project.getLocationHash(), this);
    }

    public static void updateSuggestions(Project project, List<RefactoringSuggestion> suggestions) {
        ClassTrimToolWindowPanel panel = PANELS.get(project.getLocationHash());
        if (panel == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            panel.model.setRowCount(0);
            for (RefactoringSuggestion suggestion : suggestions) {
                panel.model.addRow(new Object[]{
                        suggestion.getMethod().toString(),
                        suggestion.getSourceClass().toString(),
                        suggestion.getTargetClass().toString()
                });
            }
        });
    }
}
