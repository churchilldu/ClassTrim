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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.table.JBTable;
import org.classtrim.core.engine.RefactoringSuggestion;
import org.classtrim.model.JavaMethod;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool-window panel that displays move-method suggestions and supports:
 * <ul>
 *   <li><strong>Single-click navigation</strong> — selecting a row navigates
 *       the editor to the method's declaration and highlights it.</li>
 *   <li><strong>Per-row "Move" button</strong> — invokes IntelliJ's standard
 *       Move Instance Method refactoring dialog on the selected suggestion.</li>
 * </ul>
 */
public class ClassTrimToolWindowPanel extends JPanel {
    private static final Map<String, ClassTrimToolWindowPanel> PANELS = new ConcurrentHashMap<>();

    private final Project project;
    private final DefaultTableModel model;
    private final JBTable table;
    private final List<RefactoringSuggestion> suggestions = new ArrayList<>();

    public ClassTrimToolWindowPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        this.model = new DefaultTableModel(new Object[]{"Method", "From", "To", "Action"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Only the "Action" column (index 3) is editable (for the button click).
                return column == 3;
            }
        };
        this.table = new JBTable(model);

        // Set up the "Action" column with a button renderer + editor.
        table.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor(this));
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setMaxWidth(90);

        // Single-click navigation: when the selection changes, navigate to the method.
        table.getSelectionModel().addListSelectionListener(this::onRowSelected);

        add(new JScrollPane(table), BorderLayout.CENTER);
        PANELS.put(project.getLocationHash(), this);
    }

    /**
     * Replaces the current suggestion list with a new one. Called from the
     * coordinator's success path on the EDT.
     */
    public static void updateSuggestions(Project project, List<RefactoringSuggestion> newSuggestions) {
        ClassTrimToolWindowPanel panel = PANELS.get(project.getLocationHash());
        if (panel == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            panel.suggestions.clear();
            panel.suggestions.addAll(newSuggestions);
            panel.model.setRowCount(0);
            for (RefactoringSuggestion s : newSuggestions) {
                panel.model.addRow(new Object[]{
                        s.getMethod().toString(),
                        s.getSourceClass().toString(),
                        s.getTargetClass().toString(),
                        "Move"
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
        String classFqn = suggestion.getSourceClass().toString(); // e.g. "org.example.Foo"
        String methodName = suggestion.getMethod().getName();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            PsiMethod psiMethod = ReadAction.compute(() -> findPsiMethod(classFqn, methodName));
            if (psiMethod == null) return;

            // Navigate on the EDT.
            ApplicationManager.getApplication().invokeLater(() -> {
                psiMethod.navigate(true); // opens the file, scrolls, and highlights
            });
        });
    }

    // -------------------------------------------------------------------------
    // Apply: per-row "Move" button invokes Move Instance Method refactoring.
    // -------------------------------------------------------------------------

    void applyMove(int row) {
        if (row < 0 || row >= suggestions.size()) return;
        RefactoringSuggestion suggestion = suggestions.get(row);

        String sourceClassFqn = suggestion.getSourceClass().toString();
        String targetClassFqn = suggestion.getTargetClass().toString();
        String methodName = suggestion.getMethod().getName();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            PsiMethod psiMethod = ReadAction.compute(() -> findPsiMethod(sourceClassFqn, methodName));
            PsiClass targetClass = ReadAction.compute(() -> findPsiClass(targetClassFqn));

            if (psiMethod == null) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Messages.showWarningDialog(project,
                                "Cannot find method '" + methodName + "' in " + sourceClassFqn,
                                "ClassTrim"));
                return;
            }
            if (targetClass == null) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Messages.showWarningDialog(project,
                                "Cannot find target class '" + targetClassFqn + "'",
                                "ClassTrim"));
                return;
            }

            // Navigate to the method first so the editor is focused on it,
            // then invoke the Move refactoring dialog.
            ApplicationManager.getApplication().invokeLater(() -> {
                psiMethod.navigate(true);

                // Give the editor a moment to focus, then invoke the refactoring.
                ApplicationManager.getApplication().invokeLater(() -> {
                    invokeMove(psiMethod);
                });
            });
        });
    }

    /**
     * Invokes IntelliJ's Move refactoring on the given method. This opens
     * the standard "Move Instance Method" dialog where the user can confirm
     * the target, review visibility changes, and apply.
     */
    private void invokeMove(PsiMethod method) {
        RefactoringActionHandler handler =
                RefactoringActionHandlerFactory.getInstance().createMoveHandler();
        if (handler == null) return;

        // Build a DataContext pointing at the method's containing file + editor.
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
    // Button renderer + editor for the "Action" column.
    // -------------------------------------------------------------------------

    /**
     * Renders a "Move" button in each row of the Action column.
     */
    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setText(value == null ? "Move" : value.toString());
            return this;
        }
    }

    /**
     * Handles clicks on the "Move" button in the Action column.
     */
    private static class ButtonEditor extends DefaultCellEditor {
        private final ClassTrimToolWindowPanel panel;
        private int currentRow;

        ButtonEditor(ClassTrimToolWindowPanel panel) {
            super(new JCheckBox()); // DefaultCellEditor requires a component
            this.panel = panel;

            JButton button = new JButton("Move");
            button.setOpaque(true);
            button.addActionListener(e -> {
                fireEditingStopped();
                panel.applyMove(currentRow);
            });
            editorComponent = button;
            setClickCountToStart(1);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            currentRow = row;
            ((JButton) editorComponent).setText(value == null ? "Move" : value.toString());
            return editorComponent;
        }

        @Override
        public Object getCellEditorValue() {
            return "Move";
        }
    }
}
