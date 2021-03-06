package mediathek.tool.table;

import mediathek.config.MVConfig;
import mediathek.tool.Log;
import mediathek.tool.models.TModel;

import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("serial")
public abstract class MVTable extends JTable {
    private static final String FELDTRENNER = "|";
    private static final String SORT_ASCENDING = "ASCENDING";
    private static final String SORT_DESCENDING = "DESCENDING";
    protected final int[] breite;
    protected final int[] reihe;
    /**
     * This is the UI provided default font used for calculating the size area
     */
    private final Font defaultFont = UIManager.getDefaults().getFont("Table.font");
    public boolean useSmallSenderIcons = false;
    protected int maxSpalten;
    protected int indexSpalte = 0;
    protected int[] selRows;
    protected int[] selIndexes = null;
    protected int selRow = -1;
    protected boolean[] spaltenAnzeigen;
    protected MVConfig.Configs nrDatenSystem = null;
    protected MVConfig.Configs iconAnzeigenStr = null;
    protected MVConfig.Configs iconKleinStr = null;
    private boolean showSenderIcon = false;
    private boolean lineBreak = true;
    private List<? extends RowSorter.SortKey> listeSortKeys = null;

    public MVTable() {
        setAutoCreateRowSorter(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        setupTableType();

        getTableHeader().addMouseListener(new WidthAdjuster(this));


        breite = new int[maxSpalten];
        Arrays.fill(breite,-1);

        reihe = new int[maxSpalten];
        Arrays.fill(reihe, -1);

        if (iconAnzeigenStr != null) {
            showSenderIcon = Boolean.parseBoolean(MVConfig.get(iconAnzeigenStr));
        }
        if (iconKleinStr != null) {
            useSmallSenderIcons = Boolean.parseBoolean(MVConfig.get(iconKleinStr));
        }

        setHeight();
    }

    private static SortKey sortKeyLesen(String s, String upDown) {
        SortKey sk;

        try {
            final int sp = Integer.parseInt(s);
            if (upDown.equals(SORT_DESCENDING)) {
                sk = new SortKey(sp, SortOrder.DESCENDING);
            } else {
                sk = new SortKey(sp, SortOrder.ASCENDING);
            }
        } catch (Exception ex) {
            return null;
        }
        return sk;
    }

    public boolean showSenderIcons() {
        return showSenderIcon;
    }

    public void setShowIcon(boolean newVal) {
        showSenderIcon = newVal;
    }

    /**
     * Setup table specific stuff here.
     */
    protected abstract void setupTableType();

    public boolean isLineBreak() { return lineBreak;}

    public void setLineBreak(boolean lb) {
        lineBreak = lb;
    }

    public void invertSelection() {
        final ListSelectionModel mdl = getSelectionModel();
        final int[] selected = getSelectedRows();
        mdl.setValueIsAdjusting(true);
        mdl.setSelectionInterval(0, getRowCount() - 1);
        for (int i : selected) {
            mdl.removeSelectionInterval(i, i);
        }
        mdl.setValueIsAdjusting(false);
    }

    protected int getSizeArea() {
        int sizeArea = 0;

        if (lineBreak)
            sizeArea = defaultFont.getSize() * 4;

        return sizeArea;
    }

    public void setHeight() {
        final int sizeArea = getSizeArea();
        final int defaultFontSize = defaultFont.getSize();
        final int internalDefaultSize = defaultFontSize + defaultFontSize / 3;

        int size;
        if (!showSenderIcon) {
            if (defaultFontSize < 15) {
                size = 18;
            } else {
                size = internalDefaultSize;
            }
        } else if (useSmallSenderIcons) {
            if (defaultFontSize < 18) {
                size = 20;
            } else {
                size = internalDefaultSize;
            }
        } else if (defaultFontSize < 30) {
            size = 36;
        } else {
            size = internalDefaultSize;
        }

        setRowHeight(Math.max(size, sizeArea));
    }

    public void initTabelle() {
        // Tabelle das erste Mal initialisieren,
        // mit den gespeicherten Daten oder
        // den Standardwerten
        // erst die Breite, dann die Reihenfolge
        try {
            if (nrDatenSystem == null) {
                // wird nur für eingerichtete Tabellen gemacht
                return;
            }
            String b = "", r = "", s = "", upDown = "";
            boolean ok = false;
            if (!MVConfig.get(nrDatenSystem).isEmpty()) {
                ok = true;
                int f1, f2, f3;
                //String d = Daten.system[nrDatenSystem];
                final String strNrDatenSystem = MVConfig.get(nrDatenSystem);

                if ((f1 = strNrDatenSystem.indexOf(FELDTRENNER)) != -1) {
                    b = strNrDatenSystem.substring(0, f1);
                    if ((f2 = strNrDatenSystem.indexOf(FELDTRENNER, f1 + 1)) != -1) {
                        r = strNrDatenSystem.substring(f1 + 1, f2);
                    }
                    if ((f3 = strNrDatenSystem.indexOf(FELDTRENNER, f2 + 1)) != -1) {
                        s = strNrDatenSystem.substring(f2 + 1, f3);
                        upDown = strNrDatenSystem.substring(f3 + 1);
                    }
                }
                if (!arrLesen(b, breite)) {
                    ok = false;
                }
                if (!arrLesen(r, reihe)) {
                    ok = false;
                }
                SortKey sk = sortKeyLesen(s, upDown);
                if (sk != null) {
                    final ArrayList<SortKey> listSortKeys_ = new ArrayList<>();
                    listSortKeys_.add(sk);
                    this.getRowSorter().setSortKeys(listSortKeys_);
                }
            }
            if (ok) {
                setSpaltenEinAus(breite, spaltenAnzeigen);
                setSpalten();
                setHeight();
            } else {
                resetTabelle();
            }
        } catch (Exception ex) {
            //vorsichtshalber
        }
    }

    private boolean anzeigen(int i, boolean[] spaltenAnzeigen) {
        return spaltenAnzeigen[i];
    }

    private void setSpaltenEinAus(int[] nr, boolean[] spaltenAnzeigen) {
        for (int i = 0; i < spaltenAnzeigen.length; ++i) {
            spaltenAnzeigen[i] = nr[i] > 0;
        }
    }

    boolean[] getSpaltenEinAus(boolean[] spaltenAnzeigen, int MAX_ELEM) {
        for (int i = 0; i < MAX_ELEM; ++i) {
            spaltenAnzeigen[i] = true;
        }
        return spaltenAnzeigen;
    }

    public void fireTableDataChanged(boolean setSpalten) {
        if (setSpalten) {
            getSelected();
        }
        ((TModel) getModel()).fireTableDataChanged();
        if (setSpalten) {
            setSelected();
        }
    }

    public void scrollToSelection() {
        final int rowCount = getRowCount();

        if (rowCount > 0) {
            int i = getSelectedRow();
            if (i < 0) {
                i = 0;
                setRowSelectionInterval(i, i);
            }
            if (i >= rowCount) {
                i = rowCount - 1;
            }
            scrollToSelection(i);
        }
    }

    private void scrollToSelection(int rowIndex) {
        if (!(getParent() instanceof JViewport)) {
            return;
        }
        JViewport viewport = (JViewport) getParent();
        Rectangle rect = getCellRect(rowIndex, 0, true);
        Rectangle viewRect = viewport.getViewRect();
        rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);

        int centerX = (viewRect.width - rect.width) / 2;
        int centerY = (viewRect.height - rect.height) / 2;
        if (rect.x < centerX) {
            centerX = -centerX;
        }
        if (rect.y < centerY) {
            centerY = -centerY;
        }
        rect.translate(centerX, centerY);
        viewport.scrollRectToVisible(rect);
    }

    public void getSelected() {
        // Einstellungen der Tabelle merken
        selRow = getSelectedRow();
        selRows = getSelectedRows();
    }

    protected void setSelected() {
        if (selRows != null) {
            if (selRows.length > 0) {
                selectionModel.setValueIsAdjusting(true);
                for (int selectedRow : selRows) {
                    if (selectedRow < getRowCount()) {
                        addRowSelectionInterval(selectedRow, selectedRow);
                    }
                }
                selectionModel.setValueIsAdjusting(false);
            }
        }
    }

    private void changeColumnWidth2() {
        final TableColumnModel model = getColumnModel();
        for (int i = 0; i < breite.length && i < getColumnCount(); ++i) {
            final int colIndex = convertColumnIndexToView(i);
            if (breite[i] == 0) {
                model.getColumn(colIndex).setMinWidth(0);
                model.getColumn(colIndex).setPreferredWidth(0);
                model.getColumn(colIndex).setMaxWidth(0);
            } else {
                model.getColumn(colIndex).setMinWidth(10);
                model.getColumn(colIndex).setMaxWidth(3000);
                model.getColumn(colIndex).setPreferredWidth(breite[i]);
            }
        }
    }

    private void changeColumnWidth() {
        for (int i = 0; i < breite.length && i < getColumnCount(); ++i) {
            if (!anzeigen(i, spaltenAnzeigen)) {
                // geänderte Ansicht der Spalten abfragen
                breite[i] = 0;
            } else if (breite[i] == 0) {
                breite[i] = 100; // damit sie auch zu sehen ist :)
            }
        }
    }

    public void spaltenEinAus() {
        getSpalten(); // die aktuelle Breite holen
        changeColumnWidth();
        changeColumnWidth2();

        validate();
    }

    public void getSpalten() {
        // Einstellungen der Tabelle merken
        getSelected();

        for (int i = 0; i < reihe.length && i < getModel().getColumnCount(); ++i) {
            reihe[i] = convertColumnIndexToModel(i);
        }

        for (int i = 0; i < breite.length && i < getModel().getColumnCount(); ++i) {
            breite[i] = getColumnModel().getColumn(convertColumnIndexToView(i)).getWidth();
        }
        if (this.getRowSorter() != null) {
            listeSortKeys = getRowSorter().getSortKeys();
        } else {
            listeSortKeys = null;
        }
    }

    public void setSpalten() {
        // gemerkte Einstellungen der Tabelle wieder setzten
        try {
            changeColumnWidth();

            final TableColumnModel model = getColumnModel();
            changeColumnWidth2();

            for (int i = 0; i < reihe.length && i < getColumnCount(); ++i) {
                model.moveColumn(convertColumnIndexToView(reihe[i]), i);
            }

            if (listeSortKeys != null) {
                if (!listeSortKeys.isEmpty()) {
                    getRowSorter().setSortKeys(listeSortKeys);
                }
            }

            setSelected();

            validate();
        } catch (Exception ex) {
            Log.errorLog(965001463, ex);
        }
    }

    /**
     * Perform common reset steps for all subclasses.
     */
    public void resetTabelle() {
        listeSortKeys = null;

        getRowSorter().setSortKeys(null);
        setRowSorter(null);
        setAutoCreateRowSorter(true);
        spaltenAusschalten();
        setSpaltenEinAus(breite, spaltenAnzeigen);
        setSpalten();
        setHeight();
    }

    protected abstract void spaltenAusschalten();

    public void tabelleNachDatenSchreiben() {
        // Tabellendaten ind die Daten.system schreiben
        // erst die Breite, dann die Reihenfolge
        String b, r, s = "", upDown = "";
        int[] reihe_ = new int[maxSpalten];
        int[] breite_ = new int[maxSpalten];
        for (int i = 0; i < reihe_.length && i < getModel().getColumnCount(); ++i) {
            reihe_[i] = convertColumnIndexToModel(i);
        }

        final TableColumnModel model = getColumnModel();
        for (int i = 0; i < breite_.length && i < getModel().getColumnCount(); ++i) {
            breite_[i] = model.getColumn(convertColumnIndexToView(i)).getWidth();
        }

        b = Integer.toString(breite_[0]);
        r = Integer.toString(reihe_[0]);
        for (int i = 1; i < breite.length; i++) {
            b = b + ',' + breite_[i];
            r = r + ',' + reihe_[i];
        }

        listeSortKeys = this.getRowSorter().getSortKeys();
        if (listeSortKeys != null) {
            if (!listeSortKeys.isEmpty()) {
                SortKey sk = listeSortKeys.get(0);
                s = String.valueOf(sk.getColumn());
                upDown = sk.getSortOrder() == SortOrder.ASCENDING ? SORT_ASCENDING : SORT_DESCENDING;
            }
        }

        MVConfig.add(nrDatenSystem, b + FELDTRENNER + r + FELDTRENNER + s + FELDTRENNER + upDown);
        if (iconAnzeigenStr != null) {
            MVConfig.add(iconAnzeigenStr, String.valueOf(showSenderIcon));
        }
        if (iconKleinStr != null) {
            MVConfig.add(iconKleinStr, String.valueOf(useSmallSenderIcons));
        }
    }

    private boolean arrLesen(String s, int[] arr) {
        String sub;
        if (maxSpalten != countString(s)) {
            // dann hat sich die Anzahl der Spalten der Tabelle geändert: Versionswechsel
            return false;
        } else {
            for (int i = 0; i < maxSpalten; i++) {
                if (!s.isEmpty()) {
                    if (s.contains(",")) {
                        sub = s.substring(0, s.indexOf(','));
                        s = s.replaceFirst(sub + ',', "");
                    } else {
                        sub = s;
                        s = "";
                    }
                    try {
                        arr[i] = Integer.parseInt(sub);
                    } catch (Exception ex) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private int countString(String s) {
        int ret = 0;
        for (int i = 0; i < s.length(); ++i) {
            if (s.charAt(i) == ',') {
                ++ret;
            }
        }
        return ++ret;
    }

}
