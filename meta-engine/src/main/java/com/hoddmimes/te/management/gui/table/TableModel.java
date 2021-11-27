package com.hoddmimes.te.management.gui.table;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class TableModel<T> extends AbstractTableModel
{
    List<T> mObjects = null;
    ColumnModel mTableColumnModel = null;
    ObjectRenderer mCellRenderer = new ObjectRenderer();
    HeaderRender mHeaderRender = new HeaderRender();
    TableAttributeHandler mTableAttributeHandle;
    ModelRenderCallback mRenderCallback;


    public TableModel(Class<T> pObjectClass ) {
        mObjects = new ArrayList<>();
        mRenderCallback = null;

        mTableAttributeHandle = new TableAttributeHandler(pObjectClass );

        mTableColumnModel = new ColumnModel( mTableAttributeHandle.getHeaders(), mTableAttributeHandle.getAttributeTypes(),
                mTableAttributeHandle.getWidths(), mTableAttributeHandle.getDecimals(), mTableAttributeHandle.getEditables(), mTableAttributeHandle.getAlignements());
    }



    public void remove( int pRow ) {
       if (pRow < mObjects.size()) {
           mObjects.remove( pRow );
       }
    }

    public int getPreferedWith() {
        int tWidth = 0;
        Enumeration<TableColumn> tEnum = mTableColumnModel.getColumns();
        while(tEnum.hasMoreElements()) {
            tWidth += tEnum.nextElement().getPreferredWidth();
        }
        return tWidth + 4;
    }
    public void addEntry( T pObject ) {
        mObjects.add( pObject );
        super.fireTableDataChanged();
    }

    public void setRenderCallback( ModelRenderCallback pCallback ) {
        mRenderCallback = pCallback;
    }
    public void clear() {
        mObjects.clear();
    }


    @Override
    public void setValueAt(Object pValue, int pRow, int pCol ) {
       T tObject = mObjects.get( pRow );
       mTableAttributeHandle.setAttribute(pCol, tObject, pValue );
       this.fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return mObjects.size();
    }

    @Override
    public int getColumnCount() {
        return mTableColumnModel.getColumnCount();
    }



    @Override
    public Object getValueAt(int pRow, int pCol) {

        if ((pRow < 0) || (pRow > mObjects.size())) {
            return null;
        }
        Object tObject = mObjects.get( pRow );
        Object tRetunObject = mTableAttributeHandle.getAttribute( pCol, tObject );
        return tRetunObject;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        ColumnData tColData = (ColumnData) mTableColumnModel.getColumn(col);
        return tColData.isEditable();
    }

    public List<T> getObjects() {
        return mObjects;
    }


    T getObjectAtRow( int pRow  ) {
        if ((pRow >= 0) && (pRow < mObjects.size())) {
            return mObjects.get( pRow );
        }
        return null;
    }

    TableColumnModel getTableColumnModel() {
        return mTableColumnModel;
    }

    JTableHeader getTableHeader() {
        JTableHeader th = new JTableHeader(mTableColumnModel);
        return th;
    }


    class ColumnModel extends DefaultTableColumnModel
    {
        ColumnModel( String pHeaders[], Class pClasses[], int pWidths[], int pDecimals[], boolean pEditables[], int pAlignments[]  ) {
            super();
            init( pHeaders, pClasses, pWidths, pDecimals, pEditables, pAlignments );
        }

        private void init( String pHeaders[], Class pClasses[], int pWidths[], int pDecimals[], boolean pEditables[], int pAlignments[]) {
            for( int i = 0; i < pHeaders.length; i++) {
                super.addColumn( new ColumnData(i, pHeaders[i], pClasses[i], pWidths[i], pDecimals[i], pEditables[i], pAlignments[i]));
            }
        }
    }




     class ColumnData extends  TableColumn {
        int mJustify;
        Class mClass;
        int   mDecimals;
        boolean   mEditable;

        ColumnData(int pIndex, String pHeader, Class pClass, int pWidth, int pDecimals, boolean pEditable, int pAlignment) {
            super();
            mDecimals = pDecimals;
            mEditable = pEditable;
            super.setHeaderValue(pHeader);
            super.setModelIndex( pIndex );
            super.setHeaderRenderer( mHeaderRender );
            super.setCellRenderer( mCellRenderer );
            super.setPreferredWidth( pWidth );

            if (pAlignment >= 0) {
                mJustify = pAlignment;
            } else {
                mJustify = JLabel.CENTER;
                if (pClass == Integer.class) {
                    mJustify = JLabel.RIGHT;
                }
                if (pClass == Short.class) {
                    mJustify = JLabel.RIGHT;
                }
                if (pClass == Byte.class) {
                    mJustify = JLabel.RIGHT;
                }
                if (pClass == Character.class) {
                    mJustify = JLabel.RIGHT;
                }
                if (pClass == Long.class) {
                    mJustify = JLabel.RIGHT;
                }
                if (pClass == Double.class) {
                    mJustify = JLabel.RIGHT;
                }
                if (pClass == Float.class) {
                    mJustify = JLabel.RIGHT;
                }
            }
        }

        public boolean isEditable() {
            return mEditable;
        }

    }

    class ObjectRenderer extends JLabel implements TableCellRenderer
     {
        public Component getTableCellRendererComponent(
            JTable pJTable,
            Object pValue,
            boolean pIsSelected, boolean pHasFocus,
            int pRow, int pCol) {

        ColumnModel tColumnModel = (ColumnModel) pJTable.getColumnModel();


        if (pValue != null) {
            this.setText(pValue.toString());
        }

        this.setForeground(Color.BLACK);
        this.setBackground( (pIsSelected) ? Color.lightGray : Color.WHITE );
        this.setOpaque( true );

        ColumnData cd = (ColumnData) tColumnModel.getColumn( pCol );

        setHorizontalAlignment(cd.mJustify);
        if (cd.mJustify == JLabel.RIGHT) {
            setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 5));
        }
        if (cd.mJustify == JLabel.LEFT) {
            setBorder(BorderFactory.createEmptyBorder(1, 5, 0, 0));
        }
        setFont(new Font("Calibri",0,12));

        if (mRenderCallback != null) {
            mRenderCallback.tableCellRendererComponent( this, pJTable, pValue, pRow, pCol  );
        }

        return this;

    }
}
    static class HeaderRender extends DefaultTableCellRenderer
    {
        public Component getTableCellRendererComponent(
                JTable pJTable, Object pValue,
                boolean pIsSelected, boolean pHasFocus,
                int pRow, int pCol) {

            JLabel tLabel = new JLabel(pValue.toString());
            tLabel.setForeground( Color.DARK_GRAY );
            tLabel.setHorizontalAlignment(JLabel.CENTER);
            tLabel.setFont( new Font("Arial", Font.PLAIN, 14));
            tLabel.setBorder( BorderFactory.createBevelBorder(0));
            return tLabel;
        }
    }
}
