package org.classtrim.plugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringActionHandlerFactory;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.psi.PsiFile;
import com.intellij.ui.table.JBTable;
import org.classtrim.core.engine.RefactoringSuggestion;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool-window panel that displays move-method suggestions with:
 * <ul>
 *   <li>A checkbox column for selecting rows</li>
 *   <li>Single-click navigation to the method declaration</li>
 *   <li>A toolbar with "Select All" and "Move Selected" buttons</li>
 *   <li>Applied rows are greyed out and cannot be re-selected</li>
 * </ul>
 */
public class ClassTrimToolWindowPanel extends JPanel {
    private static final Map<String, ClassTrimToolWindowPanel> PANELS = new ConcurrentHashMap<>();

    private final Project project;
    private final DefaultTableModel model;
    private final JBTable table;
    private final List<RefactoringSuggestion> suggestions = new ArrayList<>();
    private final Set<Integer> appliedRows = new HashSet<>();
    private final JButton moveButton;
    private final JButton selectAllButton;

    public ClassTrimToolWindowPanel(Project project) {
        super(new BorderLayout());
        this.project = project;

        // Table model: columns are [✓, Method, From, To]
        this.model = new DefaultTableModel(new Object[]{"✓", "Method", "From", "To"}, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                // Only the checkbox column is editable, and only if the row hasn't been applied.
                return column == 0 && !appliedRows.contains(row);
            }
        };

        this.table = new JBTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(0).setMaxWidth(40);

        // Grey out applied rows.
        table.setDefaultRenderer(Object.class, new AppliedRowRenderer());
        table.setDefaultRenderer(Boolean.class, new AppliedCheckboxRenderer());

        // Single-click navigation (fires on selection change, not on checkbox toggle).
        table.getSelectionModel().addListSelectionListener(this::onRowSelected);

        // Toolbar with Select All + Move Selected buttons.
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectAllButton = new JButton("Select All");
        selectAllButton.addActionListener(e -> toggleSelectAll());
        toolbar.add(selectAllButton);

        moveButton = new JButton("Move Selected");
        moveButton.addActionListener(e -> moveSelected());
        moveButton.setEnabled(false);
        toolbar.add(moveButton);

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Listen for checkbox changes to enable/disable the Move button.
        model.addTableModelListener(e -> updateMoveButtonState());

        PANELS.put(project.getLocationHash(), this);
    }

    /**
     * Replaces the current suggestion list with a new one.
     */
    public static void updateSuggestions(Project project, List<RefactoringSuggestion> newSuggestions) {
        ClassTrimToolWindowPanel panel = PANELS.get(project.getLocationHash());
        if (panel == null) return;
        SwingUtilities.invokeLater(() -> {
            panel.suggestions.clear();
            panel.suggestions.addAll(newSuggestions);
            panel.appliedRows.clear();
            panel.model.setRowCount(0);
            for (RefactoringSuggestion s : newSuggestions) {
                panel.model.addRow(new Object[]{
                        Boolean.FALSE,
                        s.getMethod().toString(),
                        s.getSourceClass().toString(),
                        s.getTargetClass().toString()
                });
            }
            panel.updateMoveButtonState();
            panel.selectAllButton.setText("Select All");
        });
    }

    // -------------------------------------------------------------------------
    // Toolbar actions.
    // -------------------------------------------------------------------------

    private void toggleSelectAll() {
        // Check if any non-applied row is currently unchecked
        boolean anyUnchecked = false;
        for (int i = 0; i < model.getRowCount(); i++) {
            if (!appliedRows.contains(i) && !Boolean.TRUE.equals(model.getValueAt(i, 0))) {
                anyUnchecked = true;
                break;
            }
        }
        if (anyUnchecked) {
            // Select all non-applied rows
            for (int i = 0; i < model.getRowCount(); i++) {
                if (!appliedRows.contains(i)) {
                    model.setValueAt(Boolean.TRUE, i, 0);
                }
            }
            selectAllButton.setText("Unselect All");
        } else {
            // Unselect all
            for (int i = 0; i < model.getRowCount(); i++) {
                if (!appliedRows.contains(i)) {
                    model.setValueAt(Boolean.FALSE, i, 0);
                }
            }
            selectAllButton.setText("Select All");
        }
    }

    private void moveSelected() {
        // Collect checked, non-applied row indices.
        List<Integer> toMove = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            if (Boolean.TRUE.equals(model.getValueAt(i, 0)) && !appliedRows.contains(i)) {
                toMove.add(i);
            }
        }
        if (toMove.isEmpty()) return;

        // Process one at a time sequentially. Each invocation opens the Move dialog;
        // the user confirms or cancels per suggestion.
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            for (int row : toMove) {
                RefactoringSuggestion suggestion = suggestions.get(row);
                String sourceClassFqn = suggestion.getSourceClass().toString();
                String methodName = suggestion.getMethod().getName();

                PsiMethod psiMethod = ReadAction.compute(() -> findPsiMethod(sourceClassFqn, methodName));
                if (psiMethod == null) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Messages.showWarningDialog(project,
                                    "Cannot find method '" + methodName + "' in " + sourceClassFqn,
                                    "ClassTrim"));
                    continue;
                }

                // Invoke the Move dialog on the EDT and wait for it to complete.
                final int currentRow = row;
                ApplicationManager.getApplication().invokeAndWait(() -> {
                    psiMethod.navigate(true);
                    invokeMove(psiMethod);
                    // Mark as applied after the dialog closes (whether user confirmed or cancelled,
                    // we grey it out to indicate it was processed).
                    appliedRows.add(currentRow);
                    model.setValueAt(Boolean.FALSE, currentRow, 0);
                    model.fireTableRowsUpdated(currentRow, currentRow);
                    updateMoveButtonState();
                });
            }
        });
    }

    // -------------------------------------------------------------------------
    // Navigation: single-click jumps to the method declaration.
    // -------------------------------------------------------------------------

    private void onRowSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        int row = table.getSelectedRow();
        if (row < 0 || row >= suggestions.size()) return;

        RefactoringSuggestion suggestion = suggestions.get(row);
        navigateToMethod(suggestion);
    }

    private void navigateToMethod(RefactoringSuggestion suggestion) {
        String classFqn = suggestion.getSourceClass().toString();
        String methodName = suggestion.getMethod().getName();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            PsiMethod psiMethod = ReadAction.compute(() -> findPsiMethod(classFqn, methodName));
            if (psiMethod == null) return;
            ApplicationManager.getApplication().invokeLater(() -> psiMethod.navigate(true));
        });
    }

    // -------------------------------------------------------------------------
    // Move refactoring invocation.
    // -------------------------------------------------------------------------

    private void invokeMove(PsiMethod method) {
        RefactoringActionHandler handler =
                RefactoringActionHandlerFactory.getInstance().createMoveHandler();
        if (handler == null) return;

        PsiFile file = method.getContainingFile();
        if (file == null || file.getVirtualFile() == null) return;

        Editor editor = FileEditorManager.getInstance(project)
                .openTextEditor(new OpenFileDescriptor(project, file.getVirtualFile(),
                        method.getTextOffset()), true);
        if (editor == null) return;

        DataContext dataContext = SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, project)
                .add(CommonDataKeys.EDITOR, editor)
                .add(CommonDataKeys.PSI_ELEMENT, method)
                .add(CommonDataKeys.PSI_FILE, file)
                .build();

        handler.invoke(project, editor, file, dataContext);
    }

    // -------------------------------------------------------------------------
    // UI state helpers.
    // -------------------------------------------------------------------------

    private void updateMoveButtonState() {
        boolean anyChecked = false;
        for (int i = 0; i < model.getRowCount(); i++) {
            if (Boolean.TRUE.equals(model.getValueAt(i, 0)) && !appliedRows.contains(i)) {
                anyChecked = true;
                break;
            }
        }
        moveButton.setEnabled(anyChecked);
    }

    // -------------------------------------------------------------------------
    // PSI lookup helpers.
    // -------------------------------------------------------------------------

    private PsiMethod findPsiMethod(String classFqn, String methodName) {
        PsiClass psiClass = findPsiClass(classFqn);
        if (psiClass == null) return null;
        PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
        return methods.length > 0 ? methods[0] : null;
    }

    private PsiClass findPsiClass(String fqn) {
        return JavaPsiFacade.getInstance(project)
                .findClass(fqn, GlobalSearchScope.projectScope(project));
    }

    // -------------------------------------------------------------------------
    // Custom renderers for greying out applied rows.
    // -------------------------------------------------------------------------

    /**
     * Renders text cells with a grey foreground + strikethrough-like dimming
     * for rows that have already been applied.
     */
    private class AppliedRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (appliedRows.contains(row)) {
                c.setForeground(Color.GRAY);
                c.setEnabled(false);
            } else {
                c.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                c.setEnabled(true);
            }
            return c;
        }
    }

    /**
     * Renders the checkbox column: disabled + unchecked for applied rows.
     */
    private class AppliedCheckboxRenderer extends JCheckBox implements TableCellRenderer {
        AppliedCheckboxRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setSelected(value instanceof Boolean && (Boolean) value);
            setEnabled(!appliedRows.contains(row));
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }
}
