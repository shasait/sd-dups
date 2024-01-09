package de.hasait.sddups.ui;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridSelectionModel;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.AuthenticationUtil;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.hasait.sddups.Application;
import de.hasait.sddups.service.FileDupListener;
import de.hasait.sddups.service.FileDupService;
import de.hasait.util.dup.AbstractDupNode;
import de.hasait.util.dup.DupLeaf;
import de.hasait.util.dup.DupObject;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
@Route
@PermitAll
@SpringComponent
@UIScope
@PageTitle(Application.TITLE)
public class MainView extends VerticalLayout {

    private final FileDupService fileDupService;

    private final TextArea startPathField;
    private final TextArea excludePathRegexField;

    private final Button scanButton;
    private final Button refreshButton;
    private final Button clearButton;
    private final Text scanState;
    private final Grid<DupObject<File>> grid;
    private final Button deleteButton;
    private final TextArea selectionDetails;
    private final FileDupListener scannerListener = new FileDupListener() {

        @Override
        public void scanFinished() {
            VaadinUtil.accessUiOfComponent(scanState, () -> {
                scanState.setText("Scan finished");
                refreshDataProvider();
            }, () -> fileDupService.removeListener(scannerListener));
        }

        @Override
        public void scanStarted() {
            VaadinUtil.accessUiOfComponent(scanState, () -> scanState.setText("Scan started"), () -> fileDupService.removeListener(scannerListener));
        }

    };


    public MainView(FileDupService fileDupService, AuthenticationContext authContext) {
        this.fileDupService = fileDupService;

        setSizeFull();

        HorizontalLayout topLayout = new HorizontalLayout();
        topLayout.setWidthFull();

        H1 titleText = new H1(Application.TITLE);
        titleText.setWidthFull();
        topLayout.add(titleText);

        authContext.getAuthenticatedUser(UserDetails.class).ifPresent(user -> {
            Span span = new Span(user.getUsername());
            topLayout.add(span);
            Button logout = new Button("Logout", click -> authContext.logout());
            topLayout.add(logout);
        });

        add(topLayout);

        boolean admin = AuthenticationUtil.getSecurityHolderRoleChecker().apply("ADMIN");

        startPathField = new TextArea();
        startPathField.setSizeFull();
        startPathField.setPlaceholder("Enter path(s) to scan (one per line)");

        excludePathRegexField = new TextArea();
        excludePathRegexField.setSizeFull();
        excludePathRegexField.setPlaceholder("Enter regexps to exclude (one per line)");

        HorizontalLayout horizontalLayout = new HorizontalLayout(startPathField, excludePathRegexField);
        horizontalLayout.setWidthFull();
        horizontalLayout.setHeight(10, Unit.PERCENTAGE);
        add(horizontalLayout);

        scanButton = new Button("Scan");
        scanButton.addClickListener(event -> doScan());
        scanButton.setEnabled(admin);

        refreshButton = new Button("Refresh");
        refreshButton.addClickListener(event -> refreshDataProvider());

        clearButton = new Button("Clear");
        clearButton.addClickListener(event -> doClear());
        clearButton.setEnabled(admin);

        scanState = new Text("");

        add(new HorizontalLayout(scanButton, refreshButton, clearButton, scanState));

        grid = new Grid<>();
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addItemClickListener(this::onGridItemClicked);

        add(grid);
        setFlexGrow(1, grid);

        deleteButton = new Button("Delete");
        deleteButton.setEnabled(admin);
        deleteButton.addClickListener(event -> onDeleteClicked());
        add(deleteButton);

        selectionDetails = new TextArea();
        selectionDetails.setWidthFull();
        selectionDetails.setHeight(10, Unit.PERCENTAGE);
        add(selectionDetails);

        grid.addSelectionListener(event -> onGridSelectionChanged());
    }

    private void onGridItemClicked(ItemClickEvent<DupObject<File>> event) {
        DupObject<File> clickedItem = event.getItem();
        GridSelectionModel<DupObject<File>> selectionModel = grid.getSelectionModel();
        if (selectionModel instanceof GridMultiSelectionModel<DupObject<File>> multi) {
            // multi
            if (!event.isCtrlKey() && !event.isShiftKey() && !event.isAltKey() && !event.isMetaKey()) {
                // no modifier
                multi.updateSelection(Set.of(clickedItem), multi.getSelectedItems());
            } else if (event.isCtrlKey() && !event.isShiftKey() && !event.isAltKey() && !event.isMetaKey()) {
                // only ctrl
                if (selectionModel.isSelected(clickedItem)) {
                    selectionModel.deselect(clickedItem);
                } else {
                    selectionModel.select(clickedItem);
                }
            } else if (!event.isCtrlKey() && event.isShiftKey() && !event.isAltKey() && !event.isMetaKey()) {
                // only shift
                Set<DupObject<File>> addToSelection = new HashSet<>();
                AtomicInteger stateHolder = new AtomicInteger();
                grid.getListDataView().getItems().forEach(item -> {
                    int currentState = stateHolder.get();
                    if (currentState == 0) {
                        // before first selected item
                        if (multi.isSelected(item)) {
                            stateHolder.incrementAndGet();
                        }
                    } else if (currentState == 1) {
                        // after first selected item
                        addToSelection.add(item);
                        if (item == clickedItem) {
                            stateHolder.incrementAndGet();
                        }
                    }
                    // else after clicked item
                });
                multi.updateSelection(addToSelection, new HashSet<>());
            }
        } else {
            // single or custom
            if (selectionModel.isSelected(clickedItem)) {
                selectionModel.deselect(clickedItem);
            } else {
                selectionModel.select(clickedItem);
            }
        }

    }

    private void onDeleteClicked() {
        for (DupObject<File> selection : grid.getSelectedItems()) {
            fileDupService.deleteObject(selection);
        }
        refreshDataProvider();
    }

    private void onGridSelectionChanged() {
        StringBuilder sb = new StringBuilder();
        Iterator<DupObject<File>> selectedItems = grid.getSelectedItems().iterator();
        if (!selectedItems.hasNext()) {
            sb.append("No selection");
        }
        while (selectedItems.hasNext()) {
            DupObject<File> selectedItem = selectedItems.next();
            File selectedObject = selectedItem.getObject();
            sb.append(selectedObject.getPath()).append("\n");
            AbstractDupNode<File> selectedNode = selectedItem.getNode();
            if (selectedNode instanceof DupLeaf<File> leaf) {
                leaf.objectsStream().forEach(groupObject -> {
                    if (groupObject == selectedObject) {
                        sb.append("x ");
                    } else {
                        sb.append("* ");
                    }
                    sb.append(groupObject.getPath()).append("\n");
                });
            } else {
                sb.append("Not a group");
            }
        }
        selectionDetails.setValue(sb.toString());
    }

    @PostConstruct
    private void init() {
        grid.setMultiSort(true);

        Grid.Column<DupObject<File>> idColumn = grid.addColumn(DupObject::getNodeId);
        idColumn.setHeader("Group Id");
        idColumn.setSortable(true);
        idColumn.setFlexGrow(0);
        idColumn.setWidth("200px");

        Grid.Column<DupObject<File>> groupSizeColumn = grid.addColumn(DupObject::getNodeSize);
        groupSizeColumn.setHeader("Group Size");
        groupSizeColumn.setSortable(true);
        groupSizeColumn.setFlexGrow(0);
        groupSizeColumn.setWidth("200px");

        Grid.Column<DupObject<File>> fileSizeColumn = grid.addColumn(it -> it.getObject().length());
        fileSizeColumn.setHeader("File Size");
        fileSizeColumn.setSortable(true);
        fileSizeColumn.setFlexGrow(0);
        fileSizeColumn.setWidth("200px");

        Grid.Column<DupObject<File>> pathColumn = grid.addColumn(it -> it.getObject().getPath());
        pathColumn.setHeader("Path");
        pathColumn.setSortable(true);
        pathColumn.setFlexGrow(1);

        List<GridSortOrder<DupObject<File>>> sortOrders = new ArrayList<>();
        sortOrders.add(new GridSortOrder<>(groupSizeColumn, SortDirection.DESCENDING));
        sortOrders.add(new GridSortOrder<>(idColumn, SortDirection.ASCENDING));
        grid.sort(sortOrders);

        fileDupService.addListener(scannerListener);
    }

    @PreDestroy
    private void destroy() {
        fileDupService.removeListener(scannerListener);
    }

    private void doClear() {
        fileDupService.clear();
        refreshDataProvider();
    }

    private void doScan() {
        fileDupService.scan(startPathField.getValue(), excludePathRegexField.getValue());
    }

    private void refreshDataProvider() {
        ListDataProvider<DupObject<File>> dataProvider = DataProvider.ofCollection(fileDupService.collectObjects());
        // dataProvider.setFilter(leaf -> leaf.size() > 1);
        grid.setDataProvider(dataProvider);
        grid.recalculateColumnWidths();
    }

}
