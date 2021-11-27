package com.hoddmimes.te.management.gui.table;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.*;

public class TableAttributeHandler
{
    Map<Integer, TableAttributeEnity> mAttributes = null;


    public TableAttributeHandler( Class pTableObjectClass ) {
        parse( pTableObjectClass );
    }

    public String[] getHeaders() {
        List<TableAttributeEnity> tAttributes = new ArrayList<>();
        tAttributes.addAll( mAttributes.values());
        Collections.sort( tAttributes );
        String[] tHeader = new String[ tAttributes.size()];
        for( int i = 0; i < tAttributes.size(); i++) {
          tHeader[i] = tAttributes.get(i).mHeader;
        }
        return tHeader;
    }

    public int[] getWidths() {
        List<TableAttributeEnity> tAttributes = new ArrayList<>();
        tAttributes.addAll( mAttributes.values());
        Collections.sort( tAttributes );
        int[] tWidths = new int[ tAttributes.size()];
        for( int i = 0; i < tAttributes.size(); i++) {
            tWidths[i] = tAttributes.get(i).mWidth;
        }
        return tWidths;
    }

    public int[] getDecimals() {
        List<TableAttributeEnity> tAttributes = new ArrayList<>();
        tAttributes.addAll( mAttributes.values());
        Collections.sort( tAttributes );
        int[] tWidths = new int[ tAttributes.size()];
        for( int i = 0; i < tAttributes.size(); i++) {
            tWidths[i] = tAttributes.get(i).mWidth;
        }
        return tWidths;
    }

    public boolean[] getEditables() {
        List<TableAttributeEnity> tAttributes = new ArrayList<>();
        tAttributes.addAll( mAttributes.values());
        Collections.sort( tAttributes );
        boolean[] tEditables = new boolean[ tAttributes.size()];
        for( int i = 0; i < tAttributes.size(); i++) {
            tEditables[i] = tAttributes.get(i).mEditable;
        }
        return tEditables;
    }

    public int[] getAlignements() {
        List<TableAttributeEnity> tAttributes = new ArrayList<>();
        tAttributes.addAll( mAttributes.values());
        Collections.sort( tAttributes );
        int[] tAlignments = new int[ tAttributes.size()];
        for( int i = 0; i < tAttributes.size(); i++) {
            tAlignments[i] = tAttributes.get(i).mAlignment;
        }
        return tAlignments;
    }
    public void setAttribute( int pColumn, Object pTableObject, Object pValue ) {
        if ((pColumn < 0) || (pColumn > mAttributes.size())) {
            return;
        }
        TableAttributeEnity ta = mAttributes.get( pColumn + 1);
        try {
            String tValueStr = String.valueOf( pValue );
            Field tField = pTableObject.getClass().getDeclaredFields()[pColumn];
            if (tField.getType() == String.class)  {
                tField.set( pTableObject, tValueStr );
            }
            if ((tField.getType() == double.class) || (tField.getType() == Double.class)) {
                tField.setDouble( pTableObject, Double.parseDouble( tValueStr.replace(",",".") ));
            }
            if ((tField.getType() == int.class) || (tField.getType() == Integer.class)) {
                tField.setInt( pTableObject, Integer.parseInt( tValueStr ));
            }
            if ((tField.getType() == float.class) || (tField.getType() == Float.class)) {
                tField.setFloat( pTableObject, Float.parseFloat( tValueStr ));
            }
            if ((tField.getType() == long.class) || (tField.getType() == Long.class)){
                tField.setLong( pTableObject, Long.parseLong( tValueStr ));
            }
            if ((tField.getType() == boolean.class) || (tField.getType() == Boolean.class)){
                tField.setBoolean( pTableObject, Boolean.parseBoolean( tValueStr ));
            }
        }
        catch( Exception e) {
            e.printStackTrace();
        }
    }

    public Object getAttribute( int pColumn, Object pTableObject ) {
        if ((pColumn < 0) || (pColumn > mAttributes.size())) {
            return null;
        }
        TableAttributeEnity ta = mAttributes.get( pColumn + 1);
        try {
           Object tObj = ta.mMethod.invoke( pTableObject );
           if ((tObj instanceof Double) && (ta.mDecimals > 0)) {
               NumberFormat nf = NumberFormat.getInstance();
               nf.setMinimumFractionDigits( ta.mDecimals);
               nf.setMaximumFractionDigits( ta.mDecimals);
               return nf.format( (double) tObj);
           }
           return tObj;
        }
        catch( Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    public Class[] getAttributeTypes() {
        List<TableAttributeEnity> tAttributes = new ArrayList<>();
        tAttributes.addAll( mAttributes.values());
        Collections.sort( tAttributes );
        Class[] tClasses = new Class[ tAttributes.size()];
        for( int i = 0; i < tAttributes.size(); i++) {
            tClasses[i] = tAttributes.get(i).mMethod.getReturnType();
        }

        return tClasses;
    }



    private void parse(  Class pTableObjectClass ) {
        mAttributes = new HashMap<>();
        Method[] tMethods = pTableObjectClass.getMethods();
        for( int i = 0; i < tMethods.length; i++ ) {
            Method m = tMethods[i];
            TableAttribute ta = m.getAnnotation( TableAttribute.class );
            if (ta != null) {
                mAttributes.put( ta.column(), new TableAttributeEnity( ta.header(), ta.column(), ta.width(), ta.decimals(), ta.editable(), ta.alignment(), m));
            }
        }
    }


    public class  TableAttributeEnity implements Comparable<TableAttributeEnity> {
        int     mColumn;
        String  mHeader;
        Method  mMethod;
        int     mWidth;
        int     mDecimals;
        boolean mEditable;
        int mAlignment;

        TableAttributeEnity( String pHeader, int pColumn, int pWidth, int pDecimals, boolean pEditable,  int pAlignment, Method pMethod ) {
            mColumn = pColumn;
            mHeader = pHeader;
            mMethod = pMethod;
            mWidth = pWidth;
            mDecimals = pDecimals;
            mEditable = pEditable;
            mAlignment = pAlignment;
        }

        @Override
        public int compareTo(TableAttributeEnity o) {
            if (this.mColumn < o.mColumn) {
                return -1;
            }
            if (this.mColumn > o.mColumn) {
                return 1;
            }
            return 0;
        }
    }
}
