package com.hoddmimes.te.management.gui.table;

public interface TableCallbackInterface<E>
{
    public void tableMouseButton2( E pObject, int pRow, int pCol );
    public void tableMouseClick( E pObject, int pRow, int pCol );
    public void tableMouseDoubleClick( E pObject, int pRow, int pCol );
}
