package ui.components.pickers;

import backend.resource.TurboIssue;
import backend.resource.TurboLabel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Logger;

import ui.UI;
import util.HTLog;

/**
 * Serves as a presenter that synchronizes changes in labels with dialog view  
 */
public class LabelPickerDialog extends Dialog<List<String>> {

    private static final int ELEMENT_MAX_WIDTH = 400;
    private static final Insets GROUPLESS_PAD = new Insets(5, 0, 0, 0);
    private static final Insets GROUP_PAD = new Insets(0, 0, 10, 10);
    private static final Logger logger = HTLog.get(LabelPickerDialog.class);

    private final List<TurboLabel> allLabels;
    private final TurboIssue issue;
    private LabelPickerState state;

    @FXML
    private VBox mainLayout;
    @FXML
    private Label title;
    @FXML
    private FlowPane assignedLabels;
    @FXML
    private TextField queryField;
    @FXML
    private VBox feedbackLabels;

    LabelPickerDialog(TurboIssue issue, List<TurboLabel> allLabels, Stage stage) {
        this.allLabels = allLabels;
        this.issue = issue;

        initUI(stage, issue);
        Platform.runLater(queryField::requestFocus);
    }

    // Initialisation of UI

    @FXML
    public void initialize() {
        queryField.textProperty().addListener(
            (observable, oldText, newText) -> handleUserInput(queryField.getText()));
    }

    private void initUI(Stage stage, TurboIssue issue) {
        initialiseDialog(stage, issue);
        setDialogPaneContent(issue);
        title.setTooltip(createTitleTooltip(issue));
        createButtons();

        state = new LabelPickerState(new HashSet<>(issue.getLabels()), allLabels, "");
        populatePanes(state);
    }

    private void initialiseDialog(Stage stage, TurboIssue issue) {
        initOwner(stage);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Edit Labels for " + (issue.isPullRequest() ? "PR #" : "Issue #") +
                issue.getId() + " in " + issue.getRepoId());
    }

    private void setDialogPaneContent(TurboIssue issue) {
        createMainLayout();
        setTitleLabel(issue);
        getDialogPane().setContent(mainLayout);
    }

    // Population of UI elements

    /**
     * Populates respective panes with labels that matches current user input
     * @param state
     */
    private final void populatePanes(LabelPickerState state) {
        // Population of UI elements
        populateAssignedLabels(state.getInitialLabels(), state.getRemovedLabels(), state.getAddedLabels(),
                               state.getCurrentSuggestion());
        populateFeedbackLabels(state.getAssignedLabels(), state.getMatchedLabels(), state.getCurrentSuggestion());
        // Ensures dialog pane resize according to content
        getDialogPane().getScene().getWindow().sizeToScene();
    }

    private final void populateAssignedLabels(List<String> initialLabels, List<String> removedLabels,
                                        List<String> addedLabels, Optional<String> suggestion) {
        assignedLabels.getChildren().clear();
        populateInitialLabels(initialLabels, removedLabels, suggestion);
        populateToBeAddedLabels(addedLabels, suggestion);
        if (initialLabels.isEmpty()) {
            assignedLabels.getChildren().add(createTextLabel("No currently selected labels. "));
        }
    }

    private final void populateInitialLabels(List<String> initialLabels, List<String> removedLabels,
                                       Optional<String> suggestion) {
        initialLabels.stream()
            .forEach(label -> assignedLabels.getChildren()
            .add(processInitialLabel(label, removedLabels, suggestion)));
    }

    private final Node processInitialLabel(String initialLabel, List<String> removedLabels, 
                                            Optional<String> suggestion) {
        TurboLabel repoInitialLabel = TurboLabel.getFirstMatchingTurboLabel(allLabels, initialLabel);
        if (!removedLabels.contains(initialLabel)) {
            if (suggestion.isPresent() && initialLabel.equals(suggestion.get())) {
                return getPickerLabelNode(
                    new PickerLabel(repoInitialLabel, true).faded(true).removed(true));
            }
            return getPickerLabelNode(new PickerLabel(repoInitialLabel, true));
        }

        if (suggestion.isPresent() && initialLabel.equals(suggestion.get())) {
            return getPickerLabelNode(new PickerLabel(repoInitialLabel, true).faded(true));
        }

        return getPickerLabelNode(new PickerLabel(repoInitialLabel, true).removed(true));
    }

    /**
     * @param label
     * @return Node from label after registering mouse handler
     */
    private final Node getPickerLabelNode(PickerLabel label) {
        Node node = label.getNode();
        node.setOnMouseClicked(e -> handleLabelClick(label));
        return node;
    }

    private final void populateToBeAddedLabels(List<String> addedLabels, Optional<String> suggestion) {
        if (!addedLabels.isEmpty() || hasNewSuggestion(addedLabels, suggestion)) {
            assignedLabels.getChildren().add(new Label("|"));
        }
        populateAddedLabels(addedLabels, suggestion);
        populateSuggestedLabel(addedLabels, suggestion);
    }

    private final void populateAddedLabels(List<String> addedLabels, Optional<String> suggestion) {
        addedLabels.stream()
                .forEach(label -> {
                    assignedLabels.getChildren().add(processAddedLabel(label, suggestion));
                });
    }

    private final Node processAddedLabel(String addedLabel, Optional<String> suggestion) {
        if (!suggestion.isPresent() || !addedLabel.equals(suggestion.get())) {
            return getPickerLabelNode(
                new PickerLabel(TurboLabel.getFirstMatchingTurboLabel(allLabels, addedLabel), true));
        }
        return getPickerLabelNode(
                new PickerLabel(TurboLabel.getFirstMatchingTurboLabel(allLabels, addedLabel), true)
                .faded(true).removed(true));
    }

    private final void populateSuggestedLabel(List<String> addedLabels, Optional<String> suggestion) {
        if (hasNewSuggestion(addedLabels, suggestion)) {
            assignedLabels.getChildren().add(processSuggestedLabel(suggestion.get()));
        }
    }

    private final boolean hasNewSuggestion(List<String> addedLabels, Optional<String> suggestion) {
        return suggestion.isPresent() 
            && !(issue.getLabels()).contains(suggestion.get()) && !addedLabels.contains(suggestion.get());
    }

    private final Node processSuggestedLabel(String suggestedLabel) {
        return getPickerLabelNode(
             new PickerLabel(TurboLabel.getFirstMatchingTurboLabel(allLabels, suggestedLabel), true)
             .faded(true));
    }

    private final void populateFeedbackLabels(List<String> assignedLabels, List<String> matchedLabels,
                                        Optional<String> suggestion) {
        feedbackLabels.getChildren().clear();
        populateGroupLabels(assignedLabels, matchedLabels, suggestion);
        populateGrouplessLabels(assignedLabels, matchedLabels, suggestion);
    }

    private final void populateGroupLabels(List<String> finalLabels, List<String> matchedLabels,
                                     Optional<String> suggestion) {

        Map<String, FlowPane> groupContent = getGroupContent(finalLabels, matchedLabels, suggestion);
        groupContent.entrySet().forEach(entry -> {
            feedbackLabels.getChildren().addAll(
                    createGroupTitle(entry.getKey()), entry.getValue());
        });
    }

    private final Map<String, FlowPane> getGroupContent(List<String> finalLabels, List<String> matchedLabels,
                                                  Optional<String> suggestion) {
        Map<String, FlowPane> groupContent = new HashMap<>();
        allLabels.stream().sorted()
                .filter(label -> label.isInGroup())
                .map(label -> new PickerLabel(label, false))
                .forEach(label -> {
                    String group = label.getGroupName();
                    if (!groupContent.containsKey(group)) {
                        groupContent.put(group, createGroupPane(GROUP_PAD));
                    }
                    groupContent.get(group).getChildren().add(processMatchedLabel(
                        label.getFullName(), matchedLabels, finalLabels, suggestion));
                });
        return groupContent;
    }

    private final void populateGrouplessLabels(List<String> finalLabels, List<String> matchedLabels,
                                         Optional<String> suggestion) {
        FlowPane groupless = createGroupPane(GROUPLESS_PAD);
        allLabels.stream()
                .filter(label -> !label.isInGroup())
                .map(label -> new PickerLabel(label, false))
                .forEach(label -> groupless.getChildren().add(processMatchedLabel(
                    label.getFullName(), matchedLabels, finalLabels, suggestion)));

        feedbackLabels.getChildren().add(groupless);
    }

    private final Node processMatchedLabel(String repoLabel, List<String> matchedLabels, 
                                            List<String> assignedLabels, Optional<String> suggestion) {

        return getPickerLabelNode(
            new PickerLabel(TurboLabel.getFirstMatchingTurboLabel(allLabels, repoLabel), false)
                .faded(!matchedLabels.contains(repoLabel))
                .highlighted(suggestion.isPresent() && suggestion.get().equals(repoLabel))
                .selected(assignedLabels.contains(repoLabel)));
    }

    private void createMainLayout() {
        FXMLLoader loader = new FXMLLoader(UI.class.getResource("fxml/LabelPickerView.fxml"));
        loader.setController(this);
        try {
            mainLayout = (VBox) loader.load();
        } catch (IOException e) {
            logger.error("Failure to load FXML. " + e.getMessage());
            close();
        }
    }

    private void createButtons() {
        ButtonType confirmButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // defines what happens when user confirms/presses enter
        setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                // Ensures the last keyword in the query is toggled after confirmation
                queryField.appendText(" ");
                return state.getAssignedLabels();
            }
            return null;
        });
    }

    private Tooltip createTitleTooltip(TurboIssue issue) {
        Tooltip titleTooltip = new Tooltip(
                (issue.isPullRequest() ? "PR #" : "Issue #") + issue.getId() + ": " + issue.getTitle());
        titleTooltip.setWrapText(true);
        titleTooltip.setMaxWidth(500);
        return titleTooltip;
    }

    private void setTitleLabel(TurboIssue issue) {
        title.setText((issue.isPullRequest() ? "PR #" : "Issue #")
                + issue.getId() + ": " + issue.getTitle());
    }

    private Label createTextLabel(String input) {
        Label label = new Label(input);
        label.setPadding(new Insets(2, 5, 2, 5));
        return label;
    }


    private Label createGroupTitle(String name) {
        Label groupName = new Label(name);
        groupName.setPadding(new Insets(0, 5, 5, 0));
        groupName.setMaxWidth(ELEMENT_MAX_WIDTH - 10);
        groupName.setStyle("-fx-font-size: 110%; -fx-font-weight: bold; ");
        return groupName;
    }

    private FlowPane createGroupPane(Insets padding) {
        FlowPane group = new FlowPane();
        group.setHgap(5);
        group.setVgap(5);
        group.setPadding(padding);
        return group;
    }

    // Event handling 

    /**
     * Updates state of the label picker based on the entire query
     */
    private final void handleUserInput(String query) {
        state = new LabelPickerState(new HashSet<>(issue.getLabels()), allLabels, query.toLowerCase());
        populatePanes(state);
    }

    private void handleLabelClick(PickerLabel label) {
        queryField.setDisable(true);
        state.updateAssignedLabels(label);
        populatePanes(state);
    }
}
