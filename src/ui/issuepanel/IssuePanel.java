package ui.issuepanel;

import java.lang.ref.WeakReference;
import java.util.Optional;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.util.Callback;
import model.Model;
import model.TurboIssue;
import ui.UI;
import ui.issuecolumn.ColumnControl;
import ui.issuecolumn.IssueColumn;
import ui.sidepanel.SidePanel;
import util.events.IssueSelectedEvent;
import util.events.IssueSelectedEventHandler;

import command.TurboCommandExecutor;

public class IssuePanel extends IssueColumn {

	private final Stage mainStage;
	private final Model model;
	private final ColumnControl parentColumnControl;
	private final int columnIndex;
	private final SidePanel sidePanel;
	private final UI ui;

	private ListView<TurboIssue> listView;
	private Optional<Integer> selectedId = Optional.empty();
	
	public IssuePanel(UI ui, Stage mainStage, Model model, ColumnControl parentColumnControl, SidePanel sidePanel, int columnIndex, TurboCommandExecutor dragAndDropExecutor) {
		super(ui, mainStage, model, parentColumnControl, sidePanel, columnIndex, dragAndDropExecutor);
		this.mainStage = mainStage;
		this.model = model;
		this.parentColumnControl = parentColumnControl;
		this.columnIndex = columnIndex;
		this.sidePanel = sidePanel;
		this.ui = ui;
		
		listView = new ListView<>();
		setupListView();
		getChildren().add(listView);
		
		ui.registerEvent(new IssueSelectedEventHandler() {
			@Override
			public void handle(IssueSelectedEvent e) {
				selectedId = Optional.of(e.id);
			}
		});
		
		refreshItems();
	}
	
	@Override
	public void deselect() {
		listView.getSelectionModel().clearSelection();
		selectedId = Optional.empty();
	}
	
	@Override
	public void refreshItems() {
		super.refreshItems();
		WeakReference<IssuePanel> that = new WeakReference<IssuePanel>(this);
		
		// Set the cell factory every time - this forces the list view to update
		listView.setCellFactory(new Callback<ListView<TurboIssue>, ListCell<TurboIssue>>() {
			@Override
			public ListCell<TurboIssue> call(ListView<TurboIssue> list) {
				if(that.get() != null){
					return new IssuePanelCell(ui, mainStage, model, that.get(), columnIndex, sidePanel, parentColumnControl);
				} else{
					return null;
				}
			}
		});
		
		// Supposedly this also causes the list view to update - not sure
		// if it actually does on platforms other than Linux...
		listView.setItems(null);
		listView.setItems(getIssueList());
	}
	
	/**
	 * Given an issue id, returns the index of that issue's cell in this panel.
	 */
	private int getIndexOfIssue(int issueId) {
		assert selectedId.isPresent() : "There has to be a previously selected id by this point";
		int i = 0;
		for (TurboIssue issue : getIssueList()) {
			if (issue.getId() == selectedId.get()) {
				return  i;
			}
			i++;
		}
		assert false : "This method should be called with an issue index that is in this panel! " + issueId;
		return -1;
	}

	private void setupListView() {
		setVgrow(listView, Priority.ALWAYS);
		setOnKeyReleased((e) -> {
			if (e.getCode().equals(KeyCode.DOWN) || e.getCode().equals(KeyCode.UP)) {
				handleUpDownNavigation(e.getCode().equals(KeyCode.DOWN), e.isShiftDown());
			}
		}); 
	}

	/**
	 * This method deals with the problem of list view selection 'jumping' to the top of the 
	 * list when the list refreshes. The reason for this is that the data structure backing
	 * the list is cleared when the refresh happens and the list cells recreated, so the
	 * selection model is reset.
	 * 
	 * The problem is fixed by maintaining our own 'selection model': the selectedId field.
	 * It tracks where the selection should be at any point in time. This allows us to set it if
	 * it ever jumps to the top.
	 * 
	 * The rest of it is dealing with edge cases and interpreting what the list view is trying to
	 * tell us.
	 */
	private void handleUpDownNavigation(boolean isDownKey, boolean isShiftPressed) {
		
		// This panel is only considered to be selected if there was a previously-selected
		// id, and if it's the currently-selected column.
		// Otherwise it is not selected.
		boolean panelNotSelected = !(selectedId.isPresent()
			&& parentColumnControl.getCurrentlySelectedColumn().isPresent()
			&& parentColumnControl.getCurrentlySelectedColumn().get() == columnIndex);
		
		if (panelNotSelected) {
			// Do nothing
			assert listView.getSelectionModel().getSelectedItem() == null
				: "There can't be a selected item if there is no previous selectedId";
			return;
		}
		
		// Compute the next index based on the direction key, then clamp it to the size of the list
		int correctIndex = getIndexOfIssue(selectedId.get()) + (isDownKey ? 1 : -1);
		correctIndex = Math.max(0, Math.min(getIssueList().size()-1, correctIndex));
		
		// Model index is inconsistent with correct index => selection has jumped to the top.
		// Select the correct item.
		int modelIndex = listView.getSelectionModel().getSelectedIndex();
		if (modelIndex != correctIndex) {
			listView.getSelectionModel().clearAndSelect(correctIndex);
		}
		
		// If jump happened, the selected item may be null
		if (listView.getSelectionModel().getSelectedItem() != null) {
			// If it's not, the selection model can be trusted, so we use that to update
			// the selected id
			selectedId = Optional.of(listView.getSelectionModel().getSelectedItem().getId());
		} else {
			// If it's null, we keep the previous selected id and do nothing
		}
		
		// Trigger selection event for the right issue
		if (!isShiftPressed) {
			ui.triggerEvent(new IssueSelectedEvent(selectedId.get(), columnIndex));
		}
	}
}
