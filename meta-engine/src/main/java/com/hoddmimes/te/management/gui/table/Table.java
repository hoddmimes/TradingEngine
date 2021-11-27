package com.hoddmimes.te.management.gui.table;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class Table extends JScrollPane implements MouseListener
{
    private ExtendedJtable      mTable;
    private TableModel mTableModel;
    private TableCallbackInterface mCallbacks;

    public Table(TableModel pTableModel, Dimension pDimension, TableCallbackInterface pCallbacks ) {
        super();


        mTableModel = pTableModel;
        mTable = new ExtendedJtable();
        mTable.setAutoCreateColumnsFromModel(false);
        mTable.setModel( mTableModel );
        mTable.setShowGrid( true );
        mTable.setGridColor( Color.BLACK);
        mTable.setRowSelectionAllowed(true);
        mTable.setColumnSelectionAllowed(false);
        mTable.setColumnModel( mTableModel.getTableColumnModel());
        mTable.setTableHeader( mTableModel.getTableHeader() );
        mTable.addMouseListener( this );
        mTable.setAutoResizeMode(   JTable.AUTO_RESIZE_OFF); //  JTable.AUTO_RESIZE_ALL_COLUMNS);

        mCallbacks = pCallbacks;



        super.setViewportView( mTable );
        if (pDimension != null) {
            super.setPreferredSize( pDimension );
        }
    }



    public Table( TableModel pTableModel ) {
        this( pTableModel, null, null);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int tRow = mTable.rowAtPoint(e.getPoint());
        int tCol = mTable.columnAtPoint(e.getPoint());

        if ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() == 1)) {
           // mTableModel.doubleClickedClear();
           // mTableModel.fireTableRowsUpdated( tRow,tRow);
        } else if ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() == 2)) {
           // mTableModel.setRowDoubleClicked( tRow );
           // mTableModel.fireTableRowsUpdated( tRow,tRow);
        }

        Object tObject = mTableModel.getObjectAtRow( tRow );
        if ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() == 1)) {
            if (mCallbacks != null) {
                mCallbacks.tableMouseClick( tObject, tRow, tCol );
            }
        } else if ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() == 2)) {
            if (mCallbacks != null) {
                mCallbacks.tableMouseDoubleClick( tObject, tRow, tCol );
            }
        } else if ((e.getButton() == MouseEvent.BUTTON3) && (e.getModifiers() == Event.META_MASK)) {
            if (mCallbacks != null) {
                mCallbacks.tableMouseButton2( tObject, tRow, tCol );
            }
        }
    }

    public void setSelectedRow( int pRow ) {
        mTable.setRowSelectionInterval( pRow, pRow );
    }

    public void deSelect() {
        mTable.getSelectionModel().clearSelection();
    }


    @Override
    public void mousePressed(MouseEvent e) {
       // System.out.println("[PRESSED] " + e.toString());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        //System.out.println("[RELEASED] " + e.toString());
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        //System.out.println("[ENTERED] " + e.toString());
    }

    @Override
    public void mouseExited(MouseEvent e) {
        //System.out.println("[EXITED] " + e.toString());
    }

    class ExtendedJtable extends JTable {
        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component component = super.prepareRenderer(renderer, row, column);
            int rendererWidth = component.getPreferredSize().width;
            TableColumn tableColumn = getColumnModel().getColumn(column);
            tableColumn.setPreferredWidth(Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth()));
            return component;
        }
    };


}
